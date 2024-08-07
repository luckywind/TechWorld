# Runnable/Callable

Runnable接口只有一个没有返回值的方法。

```scala
trait Runnable {
  def run(): Unit
}
```

Callable与之类似，除了它有一个返回值

```scala
trait Callable[V] {
  def call(): V
}
```

# 线程

Scala并发是建立在Java并发模型基础上的。

在Sun JVM上，对IO密集的任务，我们可以在一台机器运行成千上万个线程。

一个线程需要一个Runnable。你必须调用线程的 `start` 方法来运行Runnable。

```scala
scala> val hello = new Thread(new Runnable {
  def run() {
    println("hello world")
  }
})
hello: java.lang.Thread = Thread[Thread-3,5,main]

scala> hello.start
hello world
```

当你看到一个类实现了Runnable接口，你就知道它的目的是运行在一个线程中。

# 单线程代码

这里有一个可以工作但有问题的代码片断。

```scala
import java.net.{Socket, ServerSocket}
import java.util.concurrent.{Executors, ExecutorService}
import java.util.Date

class NetworkService(port: Int, poolSize: Int) extends Runnable {
  val serverSocket = new ServerSocket(port)

  def run() {
    while (true) {
      // This will block until a connection comes in.
      val socket = serverSocket.accept()
      (new Handler(socket)).run()
    }
  }
}

class Handler(socket: Socket) extends Runnable {
  def message = (Thread.currentThread.getName() + "\n").getBytes

  def run() {
    socket.getOutputStream.write(message)
    socket.getOutputStream.close()
  }
}

(new NetworkService(2020, 2)).run
```

每个请求都会回应当前线程的名称，所以结果始终是 `main` 。

这段代码的主要缺点是在同一时间，只有一个请求可以被相应！

你可以把每个请求放入一个线程中处理。只要简单改变

```
(new Handler(socket)).run()
```

为

```
(new Thread(new Handler(socket))).start()
```

但如果你想重用线程或者对线程的行为有其他策略呢？

# Executors

随着Java 5的发布，它决定提供一个针对线程的更抽象的接口。

你可以通过 `Executors` 对象的静态方法得到一个 `ExecutorService` 对象。这些方法为你提供了可以通过各种政策配置的 `ExecutorService` ，如线程池。

下面改写我们之前的阻塞式网络服务器来允许并发请求。

```scala
import java.net.{Socket, ServerSocket}
import java.util.concurrent.{Executors, ExecutorService}
import java.util.Date

class NetworkService(port: Int, poolSize: Int) extends Runnable {
  val serverSocket = new ServerSocket(port)
  val pool: ExecutorService = Executors.newFixedThreadPool(poolSize)

  def run() {
    try {
      while (true) {
        // This will block until a connection comes in.
        val socket = serverSocket.accept()
        pool.execute(new Handler(socket))
      }
    } finally {
      pool.shutdown()
    }
  }
}

class Handler(socket: Socket) extends Runnable {
  def message = (Thread.currentThread.getName() + "\n").getBytes

  def run() {
    socket.getOutputStream.write(message)
    socket.getOutputStream.close()
  }
}

(new NetworkService(2020, 2)).run
```

这里有一个连接脚本展示了内部线程是如何重用的。

```
$ nc localhost 2020
pool-1-thread-1

$ nc localhost 2020
pool-1-thread-2

$ nc localhost 2020
pool-1-thread-1

$ nc localhost 2020
pool-1-thread-2
```

# Futures

`Future` 代表异步计算。你可以把你的计算包装在Future中，当你需要计算结果的时候，你只需调用一个阻塞的 `get()` 方法就可以了。一个 `Executor` 返回一个 `Future` 。如果使用Finagle RPC系统，你可以使用 `Future` 实例持有可能尚未到达的结果。

一个 `FutureTask` 是一个Runnable实现，就是被设计为由 `Executor` 运行的

```scala
val future = new FutureTask[String](new Callable[String]() {
  def call(): String = {
    searcher.search(target);
}})
executor.execute(future)
```

现在我需要结果，所以阻塞直到其完成。

```
val blockingResult = Await.result(future)
```

**参考** [Scala School的Finagle介绍](http://twitter.github.io/scala_school/zh_cn/finagle.html)中大量使用了`Future`，包括一些把它们结合起来的不错的方法。以及 Effective Scala 对[Futures](https://twitter.github.com/effectivescala/#Twitter's standard libraries-Futures)的意见。

# 线程安全问题

```
class Person(var name: String) {
  def set(changedName: String) {
    name = changedName
  }
}
```

这个程序在多线程环境中是不安全的。如果有两个线程有引用到同一个Person实例，并调用 `set` ，你不能预测两个调用结束后 `name` 的结果。

在Java内存模型中，允许每个处理器把值缓存在L1或L2缓存中，所以在不同处理器上运行的两个线程都可以有自己的数据视图。

让我们来讨论一些工具，来使线程保持一致的数据视图。

## 三种工具

### 同步

互斥锁（Mutex）提供所有权语义。当你进入一个互斥体，你拥有它。同步是JVM中使用互斥锁最常见的方式。在这个例子中，我们会同步Person。

在JVM中，你可以同步任何不为null的实例。

```scala
class Person(var name: String) {
  def set(changedName: String) {
    this.synchronized {
      name = changedName
    }
  }
}
```

### volatile

随着Java 5内存模型的变化，volatile和synchronized基本上是相同的，除了volatile允许空值。

`synchronized` 允许更细粒度的锁。 而 `volatile` 则对每次访问同步。

```scala
class Person(@volatile var name: String) {
  def set(changedName: String) {
    name = changedName
  }
}
```

### AtomicReference

此外，在Java 5中还添加了一系列低级别的并发原语。 `AtomicReference` 类是其中之一

```scala
import java.util.concurrent.atomic.AtomicReference

class Person(val name: AtomicReference[String]) {
  def set(changedName: String) {
    name.set(changedName)
  }
}
```

### 这个成本是什么？

`AtomicReference` 是这两种选择中最昂贵的，因为你必须去通过方法调度（method dispatch）来访问值。

`volatile` 和 `synchronized` 是建立在Java的内置监视器基础上的。如果没有资源争用，监视器的成本很小。由于 `synchronized` 允许你进行更细粒度的控制权，从而会有更少的争夺，所以 `synchronized` 往往是最好的选择。

当你进入同步点，访问volatile引用，或去掉AtomicReferences引用时， Java会强制处理器刷新其缓存线从而提供了一致的数据视图。

如果我错了，请大家指正。这是一个复杂的课题，我敢肯定要弄清楚这一点需要一个漫长的课堂讨论。

### Java5的其他灵巧的工具

正如前面提到的 `AtomicReference` ，Java5带来了许多很棒的工具。

#### CountDownLatch

`CountDownLatch` 是一个简单的多线程互相通信的机制。

```
val doneSignal = new CountDownLatch(2)
doAsyncWork(1)
doAsyncWork(2)

doneSignal.await()
println("both workers finished!")
```

先不说别的，这是一个优秀的单元测试。比方说，你正在做一些异步工作，并要确保功能完成。你的函数只需要 `倒数计数（countDown）` 并在测试中 `等待（await）` 就可以了。

#### AtomicInteger/Long

由于对Int和Long递增是一个经常用到的任务，所以增加了 `AtomicInteger` 和 `AtomicLong` 。

#### AtomicBoolean

我可能不需要解释这是什么。

#### ReadWriteLocks

`读写锁（ReadWriteLock）` 使你拥有了读线程和写线程的锁控制。当写线程获取锁的时候读线程只能等待。

## 让我们构建一个不安全的搜索引擎

下面是一个简单的倒排索引，它不是线程安全的。我们的倒排索引按名字映射到一个给定的用户。

这里的代码天真地假设只有单个线程来访问。

注意使用了 `mutable.HashMap` 替代了默认的构造函数 `this()`

```scala
import scala.collection.mutable

case class User(name: String, id: Int)

class InvertedIndex(val userMap: mutable.Map[String, User]) {

  def this() = this(new mutable.HashMap[String, User])

  def tokenizeName(name: String): Seq[String] = {
    name.split(" ").map(_.toLowerCase)
  }

  def add(term: String, user: User) {
    userMap += term -> user
  }

  def add(user: User) {
    tokenizeName(user.name).foreach { term =>
      add(term, user)
    }
  }
}
```

这里没有写如何从索引中获取用户。稍后我们会补充。

## 让我们把它变为线程安全

在上面的倒排索引例子中，userMap不能保证是线程安全的。多个客户端可以同时尝试添加项目，并有可能出现前面 `Person` 例子中的视图错误。

由于userMap不是线程安全的，那我们怎样保持在同一个时间只有一个线程能改变它呢？

你可能会考虑在做添加操作时锁定userMap。

```
def add(user: User) {
  userMap.synchronized {
    tokenizeName(user.name).foreach { term =>
      add(term, user)
    }
  }
}
```

不幸的是，这个粒度太粗了。一定要试图在互斥锁以外做尽可能多的耗时的工作。还记得我说过如果不存在资源争夺，锁开销就会很小吗。如果在锁代码块里面做的工作越少，争夺就会越少。

```
def add(user: User) {
  // tokenizeName was measured to be the most expensive operation.
  val tokens = tokenizeName(user.name)

  tokens.foreach { term =>
    userMap.synchronized {
      add(term, user)
    }
  }
}
```

## SynchronizedMap

我们可以通过SynchronizedMap特质将同步混入一个可变的HashMap。

我们可以扩展现有的InvertedIndex，提供给用户一个简单的方式来构建同步索引。

```
import scala.collection.mutable.SynchronizedMap

class SynchronizedInvertedIndex(userMap: mutable.Map[String, User]) extends InvertedIndex(userMap) {
  def this() = this(new mutable.HashMap[String, User] with SynchronizedMap[String, User])
}
```

如果你看一下其实现，你就会意识到，它只是在每个方法上加同步锁来保证其安全性，所以它很可能没有你希望的性能。

## Java ConcurrentHashMap

Java有一个很好的线程安全的ConcurrentHashMap。值得庆幸的是，我们可以通过JavaConverters获得不错的Scala语义。

事实上，我们可以通过扩展老的不安全的代码，来无缝地接入新的线程安全InvertedIndex。

```scala
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._

class ConcurrentInvertedIndex(userMap: collection.mutable.ConcurrentMap[String, User])
    extends InvertedIndex(userMap) {

  def this() = this(new ConcurrentHashMap[String, User] asScala)
}
```

## 让我们加载InvertedIndex

### 原始方式

```scala
trait UserMaker {
  def makeUser(line: String) = line.split(",") match {
    case Array(name, userid) => User(name, userid.trim().toInt)
  }
}

class FileRecordProducer(path: String) extends UserMaker {
  def run() {
    Source.fromFile(path, "utf-8").getLines.foreach { line =>
      index.add(makeUser(line))
    }
  }
}
```

对于文件中的每一行，我们可以调用 `makeUser` 然后 `add` 到 InvertedIndex中。如果我们使用并发InvertedIndex，我们可以并行调用add因为makeUser没有副作用，所以我们的代码已经是线程安全的了。

我们不能并行读取文件，但我们 *可以* 并行构造用户并且把它添加到索引中。

### 一个解决方案：生产者/消费者

异步计算的一个常见模式是把消费者和生产者分开，让他们只能通过 `队列（Queue）` 沟通。让我们看看如何将这个模式应用在我们的搜索引擎索引中。

```scala
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

// Concrete producer
class Producer[T](path: String, queue: BlockingQueue[T]) extends Runnable {
  def run() {
    Source.fromFile(path, "utf-8").getLines.foreach { line =>
      queue.put(line)
    }
  }
}

// Abstract consumer
abstract class Consumer[T](queue: BlockingQueue[T]) extends Runnable {
  def run() {
    while (true) {
      val item = queue.take()
      consume(item)
    }
  }

  def consume(x: T)
}

val queue = new LinkedBlockingQueue[String]()

// One thread for the producer
val producer = new Producer[String]("users.txt", q)
new Thread(producer).start()

trait UserMaker {
  def makeUser(line: String) = line.split(",") match {
    case Array(name, userid) => User(name, userid.trim().toInt)
  }
}

class IndexerConsumer(index: InvertedIndex, queue: BlockingQueue[String]) extends Consumer[String](queue) with UserMaker {
  def consume(t: String) = index.add(makeUser(t))
}

// Let's pretend we have 8 cores on this machine.
val cores = 8
val pool = Executors.newFixedThreadPool(cores)

// Submit one consumer per core.
for (i <- i to cores) {
  pool.submit(new IndexerConsumer[String](index, q))
}
```