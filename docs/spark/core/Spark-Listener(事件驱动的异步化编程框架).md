# 事件驱动的异步化编程

我们以前经常看到基于事件的监控，基于事件的数据采集等等，Spark-Core内部的事件框架实现了<font color=red>基于事件的异步化编程模式。它的最大好处是可以提升应用程序对物理资源的充分利用，能最大限度的压榨物理资源，提升应用程序的处理效率</font>。缺点比较明显，降低了应用程序的可读性。Spark的基于事件的异步化编程框架由事件框架和异步执行线程池组成，应用程序产生的Event发送给ListenerBus，ListenerBus再把消息广播给所有的Listener，每个Listener收到Event判断是否自己感兴趣的Event，若是，会在Listener独享的线程池中执行Event所对应的逻辑程序块。下图展示Event、ListenerBus、Listener、Executor的关系，从事件生成、事件传播、事件解释三个方面的视角来看。

![在这里插入图片描述](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2FzZDQ5MTMxMA==,size_16,color_FFFFFF,t_70.png)

我们从线程的视角来看，看异步化处理。异步化处理体现在事件传播、事件解释两个阶段，其中事件解释的异步化实现了我们的基于事件的异步化编程。

![在这里插入图片描述](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/20190411144456755.png)

假设我们基于上面的设计思路来实现一个自己的事件框架。首先定义一个Trait SparkEvent，所有的Event都实现SparkEvent。其次定义一个ListenerBus的实现者SparkListenerBus，管理所有Listener，以及把收到的Event传播给对应的Listener。最后Listener对Event进行解释，异步化处理相应的逻辑。这一切看起来都很好，但是随着业务的发展，Event、Listener数量从100增长到1000，甚至几千的时候，你会发现这是一个庞大、复杂难以管理的框架，事件分发效率会随着事件类型变多越来越慢。碰到复杂的事情，架构做的事情就是归纳、分类，做矩阵。Spark-Core、Spark-Streaming采用了分类的思路（分而治之）进行管理，每一大类事件都有独自的Event、ListenerBus。

![在这里插入图片描述](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/20190411144524351.png)

Spark-Core、Spark-Streaing的事件框架主要是解决单JVM环境下异步化编程设计的，不考虑分布式和持久化等相关的高可用。毕竟作为一个计算类框架把单进程的并行度做到极限是很有必要。

## Event

Spark-Core的核心事件trait是SparkListenerEvent，Spark-Straming的核心事件trait是StreamingListenerEvent，两者各自代表的是一类事件的抽象，每个事件之间是独立的。(注意：下面的子类并不全，只是随意列举了三个)

![在这里插入图片描述](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2FzZDQ5MTMxMA==,size_16,color_FFFFFF,t_70-20191215145307846.png)

我们在定义事件需要注意哪些方面呢？我们以SparkListenerTaskStart为例，分析一个事件拥有哪些特征。

见名知义，SparkListenerTaskStart，一看名字我们就能猜到是SparkListener的一个任务启动事件。
**触发条件**，一个事件的触发条件必须清晰，能够清晰的描述一个行为，且行为宿主最好是唯一的。SparkListenerTaskStart事件生成的宿主是DAGScheduler，在DAGScheduler产生BeginEvent事件后生成SparkListenerTaskStart。
**事件传播**，事件传播可选择Point-Point或者BroadCast，这个可根据业务上的需要权衡、选择。Spark-Core、Spark-Streaming的事件框架采用BroadCast模式。
**事件解释**，一个事件可以有一个或者多个解释。Spark-Core、Spark-Streaming由于采用BroadCast模式，所以支持Listener对事件解释，原则一个Listener对一个事件只有一种解释。AppStatusListener、EventLoggingListener、ExecutorAllocationManager等分别对SparkListenerTaskStart做了解释。
我们在设计事件框架上可根据实际需要借鉴以上四点，设计一个最恰当的事件框架。

## Listener

Listener定义了一系列的API，来对Event做解释。如SparkListenerInterface的Stage提交和任务启动两个API定义：
 下面两个API处理不同的事件

```scala
/**
  *Called when a stage is submitted
 **/
def onStageSubmitted(stageSubmitted: SparkListenerStageSubmitted): Unit

/**
 *Called when a task starts
 **/
def onTaskStart(taskStart: SparkListenerTaskStart): Unit
```

## ListenerBus

ListenerBus用于管理所有的Listener，Spark-Core和Spark-Streaming公用相同的trait ListenerBus， 最终都是使用AsyncEventQueue类对Listener进行管理。

![在这里插入图片描述](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/20190411144845899.png)

### LiveListenerBus：

管理所有注册的Listener，为一类Listener创建一个唯一的AsyncEventQueue，广播Event到所有的Listener。默认可提供四类AsyncEventQueue分别为‘shared’、‘appStatus’、‘executorManagement’、‘eventLog’。目前Spark-Core并没有放开类别设置，意谓着最多只能有上述四类，从设计的严谨上来讲分类并不是越多越好，每多一个类别，就会多一个AsyncEventQueue实例，每个实例中会包含一个事件传播的线程，对系统的资源占用还是比较多的。

#### 核心属性：

```scala
//控制LiveListenerBus的生命周期，选用AtomicBoolean可确保多线程环境下started值的内存可见性
//也可以选择使用boolean，但需要加上volatile，如：@volatile private var started=false，但这种方案笔者觉的没有AtomicBoolean好
private val started = new AtomicBoolean(false)
private val stopped = new AtomicBoolean(false)
//用于存储LiveListenerBus还没有启动好但已经有Event需要传播的情况下，临时把事件存储在queueEvents中
@volatile private[scheduler] var queuedEvents = new mutable.ListBuffer[SparkListenerEvent]()
//存储事件，同类型的事件在同一个AsyncEventQueue--->Listener
private val queues = new CopyOnWriteArrayList[AsyncEventQueue]()
//控制SparkContext、StreamingContext是否可以执行stop方法，如果为true不可以，为false则可以
//AsyncEventQueue每次消息传播的时均会设置为true，传播结束后设置为false
val withinListenerThread: DynamicVariable[Boolean] = new DynamicVariable[Boolean](false)

```

#### 核心方法：

**1. start**
LiveListenerBus在SparkContext的setupAndStartListenerBus中被初始化，并调用start方法启动LiveListenerBus。

```scala
def start(sc: SparkContext, metricsSystem: MetricsSystem): Unit = synchronized {
          if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("LiveListenerBus already started.")
          }
          this.sparkContext = sc
        //遍历所有的AsyncEventQueue
          queues.asScala.foreach { q =>
        //启动每个AsyncEventQueue，促使每个AsyncEventQueue有传播事件的能力
            q.start(sc)
            queuedEvents.foreach(q.post)
          }
   //在LiveListenerBus没有启动完成前临时存储Event，启动完成后queuedEvents置空
          queuedEvents = null
          metricsSystem.registerSource(metrics)
        }

```

**2. stop**

停止使用LiveListenerBus，需要注意stop可能会导致长时间的阻塞，执行stop方法的线程会被挂起，直到所有的AsyncEventQueue(默认四个)中的dispatch线程都退出后执行stop主法的线程才会被唤醒。

```scala
def stop(): Unit = {
      if (!started.get()) {
        throw new IllegalStateException(s"Attempted to stop bus that has not yet started!")
      }
      if (!stopped.compareAndSet(false, true)) {
        return
      }
      synchronized {
      //停止所有的AsyncEventQueue，这里可能被阻塞
        queues.asScala.foreach(_.stop())
        queues.clear()
      }
    }

```

![在这里插入图片描述](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2FzZDQ5MTMxMA==,size_16,color_FFFFFF,t_70-20191215145900761.png)

**3. post**

采用广播的方式事件传播，这个过程很快，主线程只需要把事件传播给AsyncEventQueue即可，最后由AsyncEventQueue再广播给相应的Listener

```scala
def post(event: SparkListenerEvent): Unit = {
      if (stopped.get()) {
        return
      }
      metrics.numEventsPosted.inc()
    //如果queuedEvents不为空，表示LiveListenerBus还没有启动好，消息暂时存放到queuedEvents中
      if (queuedEvents == null) {
    //事件传播给所有的AsyncEventQueue
        postToQueues(event)
        return
      }
      synchronized {
        if (!started.get()) {
          queuedEvents += event
          return
        }
      }
    //事件传播给所有的AsyncEventQueue
      postToQueues(event)
    }

```

### ***AsyncEventQueue：***

事件异步传播队列的实现类，采用了生产者-消费者模式。在start()方法被调用后Event才可以被分发到对应的Listener。
***核心属性：***

```scala
//存储生产者的Event，默认队列大小为10000，线程安全的队列
private val eventQueue = new LinkedBlockingQueue[SparkListenerEvent](
  conf.get(LISTENER_BUS_EVENT_QUEUE_CAPACITY))
//存储注册到当前AsyncEventQueue中的Listener，此属性由ListenerBus定义(每个Listener对应一个Timer，关于Metrics框架后面介绍)
private[this] val listenersPlusTimers = new CopyOnWriteArrayList[(L, Option[Timer])]
//事件传播异步化线程
private val dispatchThread = new Thread(s"spark-listener-group-$name") {
  setDaemon(true)
  override def run(): Unit = Utils.tryOrStopSparkContext(sc) {
	//Spark-Core、Spark-Streaming有两个重要实现分别为SparkListenerBus、StreamingListenerBus。在两个ListenerBus匹配相应的Listener
    dispatch()
  }
}

```

