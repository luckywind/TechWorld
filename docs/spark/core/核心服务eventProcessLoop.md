Spark中大量采用事件监听方式，实现driver端的组件之间的通信。本文就来解释一下Spark中事件监听是如何实现的

# 观察者模式和监听器

在设计模式中有一个观察者模式，该模式建立一种对象与对象之间的依赖关系，一个对象状态发生改变时立即通知其他对象，其他对象就据此作出相应的反应。其中发生改变的对象称之为观察目标（也有叫主题的），被通知的对象称之为观察者，可以有多个观察者注册到一个观察目标中，这些观察者之间没有联系，其数量可以根据需要增减。

![image-20210710162453712](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210710162453712.png)



# 事件驱动的异步化编程

Spark-Core内部的事件框架实现了基于事件的异步化编程模式。它的最大好处是可以提升应用程序对物理资源的充分利用，能最大限度的压榨物理资源，提升应用程序的处理效率。缺点比较明显，降低了应用程序的可读性。Spark的基于事件的异步化编程框架由事件框架和异步执行线程池组成，应用程序产生的Event发送给ListenerBus，ListenerBus再把消息广播给所有的Listener，每个Listener收到Event判断是否自己感兴趣的Event，若是，会在Listener独享的线程池中执行Event所对应的逻辑程序块。下图展示Event、ListenerBus、Listener、Executor的关系，从事件生成、事件传播、事件解释三个方面的视角来看。
![img](https://gitee.com/luckywind/PigGo/raw/master/image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2FzZDQ5MTMxMA==,size_16,color_FFFFFF,t_70.png)

我们从线程的视角来看，看异步化处理。异步化处理体现在事件传播、事件解释两个阶段，其中事件解释的异步化实现了我们的基于事件的异步化编程。

![在这里插入图片描述](https://gitee.com/luckywind/PigGo/raw/master/image/20190411144456755.png)

# Spark的实现

Spark-Core、Spark-Streaming采用了分类的思路（分而治之）进行管理，每一大类事件都有独自的Event、ListenerBus

![在这里插入图片描述](https://gitee.com/luckywind/PigGo/raw/master/image/20190411144524351.png)

## Event

Spark-Core的核心事件trait是SparkListenerEvent，Spark-Straming的核心事件trait是StreamingListenerEvent

下图是各种事件实体类：

![image-20210709232059314](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210709232059314.png)

![image-20210709232140439](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210709232140439.png)

我们在定义事件需要注意哪些方面呢？我们以SparkListenerTaskStart为例，分析一个事件拥有哪些特征。

1. 见名知义，SparkListenerTaskStart，一看名字我们就能猜到是SparkListener的一个任务启动事件。
2. 触发条件，一个事件的触发条件必须清晰，能够清晰的描述一个行为，且行为宿主最好是唯一的。SparkListenerTaskStart事件生成的宿主是DAGScheduler，在DAGScheduler产生BeginEvent事件后生成SparkListenerTaskStart。
3. 事件传播，事件传播可选择Point-Point或者BroadCast，这个可根据业务上的需要权衡、选择。Spark-Core、Spark-Streaming的事件框架采用BroadCast模式。
4. 事件解释，一个事件可以有一个或者多个解释。Spark-Core、Spark-Streaming由于采用BroadCast模式，所以支持Listener对事件解释，原则一个Listener对一个事件只有一种解释。AppStatusListener、EventLoggingListener、ExecutorAllocationManager等分别对SparkListenerTaskStart做了解释。
   我们在设计事件框架上可根据实际需要借鉴以上四点，设计一个最恰当的事件框架。

## Listner

Spark-Core的核心监听triat是SparkListener，Spark-Streaming的核心监听triat StreamingListener，两者都代表了一类监听的抽象

下图是一些监听实体类：

![image-20210709232805895](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210709232805895.png)

![image-20210709232901521](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210709232901521.png)

## ListenerBus

监听器总线对象，Spark程序在运行的过程中，Driver端的很多功能都依赖于事件的传递和处理，而事件总线在这中间发挥着至关重要的纽带作用。事件总线通过异步线程，提高了Driver执行的效率。Listener注册到ListenerBus对象中，然后通过ListenerBus对象来实现事件监听(类似于计算机与周边设备之间的关系)

其start方法直接启动一个dispatchThread，其核心逻辑就是不停地在一个事件队列eventQueue里取出事件，如果事件合法且LiverListenerBus没有被关停，就将事件通知给所有注册的listener中

其dispatch方法就是向事件队列里添加相应的事件。

ListenerBus用于管理所有的Listener，Spark-Core和Spark-Streaming公用相同的trait ListenerBus， 最终都是使用AsyncEventQueue类对Listener进行管理。

![image-20210709233404950](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210709233404950.png)

### LiveListenerBus：

管理所有注册的Listener，为一类Listener创建一个唯一的AsyncEventQueue，广播Event到所有的Listener。默认可提供四类AsyncEventQueue分别为‘shared’、‘appStatus’、‘executorManagement’、‘eventLog’。目前Spark-Core并没有放开类别设置，意谓着最多只能有上述四类，从设计的严谨上来讲分类并不是越多越好，每多一个类别，就会多一个AsyncEventQueue实例，每个实例中会包含一个事件传播的线程，对系统的资源占用还是比较多的。

#### 异步事件处理线程listenerThread

```scala
  private val listenerThread = new Thread(name) {
    setDaemon(true) //线程本身设为守护线程 
    override def run(): Unit = Utils.tryOrStopSparkContext(sparkContext) {
      LiveListenerBus.withinListenerThread.withValue(true) {
        while (true) {
          eventLock.acquire()//不断获取信号量，信号量减一，能获取到说明还有事件未处理
          self.synchronized {
            processingEvent = true
          }
          try {
            val event = eventQueue.poll  //获取事件， remove() 和 poll() 方法都是从队列中删除第一个元素（head）。
            if (event == null) {
              // 此时说明没有事件，但还是拿到信号量了，这说明stop方法被调用了
              // 跳出while循环，关闭守护进程线程
              if (!stopped.get) {
                throw new IllegalStateException("Polling `null` from eventQueue means" +
                  " the listener bus has been stopped. So `stopped` must be true")
              }
              return
            }
            // 调用ListenerBus的postToAll(event: E)方法
            postToAll(event)
          } finally {
            self.synchronized {
              processingEvent = false
            }
          }
        }
      }
    }
  }
```



#### 核心属性

```scala
private val started = new AtomicBoolean(false)
private val stopped = new AtomicBoolean(false)
//存放事件
private lazy val eventQueue = new LinkedBlockingQueue[SparkListenerEvent]
// 表示队列中产生和使用的事件数量的计数器，这个信号量是为了避免消费者线程空跑
private val eventLock = new Semaphore(0)
```

#### 核心方法

##### start

LiveListenerBus在SparkContext的setupAndStartListenerBus中被初始化，并调用start方法启动LiveListenerBus。

```scala
  def start(): Unit = {
    if (started.compareAndSet(false, true)) { 
      listenerThread.start() //启动消费者线程
    } else {
      throw new IllegalStateException(s"$name already started!")
    }

```

##### stop

停止LiveListenerBus，它将等待队列事件被处理，但在停止后丢掉所有新的事件。需要注意stop可能会导致长时间的阻塞，执行stop方法的线程会被挂起，直到所有的AsyncEventQueue(默认四个)中的dispatch线程都退出后执行stop主法的线程才会被唤醒。

```scala
  def stop(): Unit = {
    if (!started.get()) {
      throw new IllegalStateException(s"Attempted to stop $name that has not yet started!")
    }
    if (stopped.compareAndSet(false, true)) {
      // Call eventLock.release() so that listenerThread will poll `null` from `eventQueue` and know
      // `stop` is called.
      // 释放一个信号量，但此时是没有事件的，从而listenerThread会拿到一个空事件，从而知道该停止了
      eventLock.release()
      //然后等待消费者线程自动关闭
      listenerThread.join()
    } else {
      // Keep quiet
    }
  }

```

##### post

采用广播的方式事件传播，这个过程很快，主线程只需要把事件传播给AsyncEventQueue即可，最后由AsyncEventQueue再广播给相应的Listener

```scala
def post(event: SparkListenerEvent): Unit = {
    if (stopped.get) {
      // Drop further events to make `listenerThread` exit ASAP
      logError(s"$name has already stopped! Dropping event $event")
      return
    }
    // 在事件队列队尾添加事件
    // add（）和offer（）区别：两者都是往队列尾部插入元素，不同的时候，当超出队列界限的时候，add（）方法是抛出异常让你处理，而offer（）方法是直接返回false
    val eventAdded = eventQueue.offer(event)
    if (eventAdded) {
      //如果成功加入队列，则在信号量中加一
      eventLock.release()
    } else {
      // 如果事件队列超过其容量，则将删除新的事件，这些子类将被通知到删除事件。
      onDropEvent(event)
      droppedEventsCounter.incrementAndGet()
    }

    val droppedEvents = droppedEventsCounter.get
    if (droppedEvents > 0) {
      // Don't log too frequently   日志不要太频繁
      // 如果上一次，队列满了EVENT_QUEUE_CAPACITY=1000设置的值，就丢掉，然后记录一个时间，如果一直持续丢掉，那么每过60秒记录一次日志，不然日志会爆满的
      if (System.currentTimeMillis() - lastReportTimestamp >= 60 * 1000) {
        if (droppedEventsCounter.compareAndSet(droppedEvents, 0)) {
          val prevLastReportTimestamp = lastReportTimestamp
          lastReportTimestamp = System.currentTimeMillis()
          // 记录一个warn日志，表示这个事件，被丢弃了
          logWarning(s"Dropped $droppedEvents SparkListenerEvents since " +
            new java.util.Date(prevLastReportTimestamp))
        }
      }
    }
  }
```

## 完整流程

![image-20210710220515744](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210710220515744.png)



1. 图中的DAGScheduler、SparkContext、BlockManagerMasterEndpoint、DriverEndpoint及LocalSchedulerBackend都是LiveListenerBus的事件来源，它们都是通过调用LiveListenerBus的post方法将消息提交给事件队列，每post一个事件，信号量就加一。

2. listenerThread不停的获取信号量，然后从事件队列中取出事件，取到事件，则调用postForAll把事件分发给已注册的监听器，否则，就是取到空事件，它明白这是事件总线搞的鬼，它调用了stop但是每post事件，从而停止事件总线线程。

# 参考

源码：org/apache/spark/scheduler/LiveListenerBus.scala

Spark-Listener(事件驱动的异步化编程框架)](https://blog.csdn.net/asd491310/article/details/89210932)

Spark消息总线实现](https://www.jianshu.com/p/7304d9c702a3)

深入理解事件总线](https://www.cnblogs.com/jiaan-geng/p/10137655.html)

Spark事件监听详解](https://wongxingjun.github.io/2017/01/01/Spark%E4%BA%8B%E4%BB%B6%E7%9B%91%E5%90%AC%E8%AF%A6%E8%A7%A3/)

