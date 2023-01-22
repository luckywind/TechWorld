# DataSourceAPI

## 执行环境

从1.12.0版本起，Flink实现了API上的流批统一。DataStream API新增了一个重要特性： 可以支持不同执行模式，通过简单设置就可以让一段Flink程序在流处理和批处理之间切换。这样以来，DataSet API就没有存在的必要了。

1. 流执行模式，是默认的执行模式
2. 批执行模式，执行方式类似于MapReduce框架
3. 自动模式，程序根据输入数据源是否有界自动选择执行模式
4. 手动指定批处理模式：
   1. 命令行传参数： -Dexecution.runtime-mode=BATCH
   2. 代码指定

##   Source

需要实现 SourceFunction 接口

```scala
//读取集合
env.fromCollection(clicks)

//读取序列
env.fromElements(
Event("Mary", "/.home", 1000L),
Event("Bob", "/.cart", 2000L))

//读取文件
env.readTextFile("clicks.csv")

//读socket
env.socketTextStream("localhost", 7777)
```

注意：读取hdfs目录时，需要加依赖

```xml
<dependency>
   <groupId>org.apache.hadoop</groupId>
   <artifactId>hadoop-client</artifactId>
   <version>2.7.5</version>
   <scope>provided</scope>
</dependency>
```

## flink支持的数据类型

使用TypeInfomation来统一表示数据类型

1. Flink 对 POJO 类型的要求如下:

2. - ⚫  类是公共的(public)和独立的(standalone，也就是说没有非静态的内部类);

   - ⚫  类有一个公共的无参构造方法;

   - ⚫  类中的所有字段是public且非final修饰的;或者有一个公共的getter和setter方法，

     这些方法需要符合 Java bean 的命名规范。
      Scala 的样例类就类似于 Java 中的 POJO 类，所以我们看到，之前的 Event，就是我们创

     建的一个 Scala 样例类，使用起来非常方便。

## 聚合算子

### keyBy(按键分区)

对于 Flink 而言，DataStream 是没有直接进行聚合的 API 的。因为我们对海量数据做聚合 肯定要进行分区并行处理，这样才能提高效率。所以在 Flink 中，要做聚合，需要先进行分区; 这个操作就是通过 keyBy()来完成的。分区字段可以通过lambda表达式，属性，自定义KeySelector指定

keyBy()是聚合前必须要用到的一个算子。keyBy()通过指定键(key)，可以将一条流从逻 辑上划分成不同的分区(partitions)。这里所说的分区，其实就是并行处理的子任务，也就对 应着任务槽(task slots)。

### min/max/sum简单聚合

Flink 为我们 内置实现了一些最基本、最简单的聚合 API

```scala
    val stream=env.fromElements(
      ("a", 1), ("a", 3), ("b", 3), ("b", 4)
    )

    stream.keyBy(_._1).sum(1).print() //对元组的索引 1 位置数据求和
    stream.keyBy(_._1).sum("_2").print() //对元组的第 2 个位置数据求和
    stream.keyBy(_._1).max(1).print() //对元组的索引 1 位置求最大值
    stream.keyBy(_._1).max("_2").print() //对元组的第 2 个位置数据求最大值
    stream.keyBy(_._1).min(1).print() //对元组的索引 1 位置求最小值
    stream.keyBy(_._1).min("_2").print() //对元组的第 2 个位置数据求最小值
    stream.keyBy(_._1).maxBy(1).print() //对元组的索引 1 位置求最大值
    stream.keyBy(_._1).maxBy("_2").print() //对元组的第 2 个位置数据求最大值
    stream.keyBy(_._1).minBy(1).print() //对元组的索引 1 位置求最小值
    stream.keyBy(_._1).minBy("_2").print() //对元组的第 2 个位置数据求最小值


    val stream1 = env.fromElements(
      Event("Mary", "./home", 1000L),
      Event("Bob", "./cart", 2000L)
    )
    // 使用 user 作为分组的字段，并计算最大的时间戳 
    stream1.keyBy(_.user).max("timestamp").print()
```

### (reduce)规约聚合

与简单聚合类似，reduce()操作也会将 KeyedStream 转换为 DataStream。它不会改变流的 元素数据类型，所以输出类型和输入类型是一样的。调用 KeyedStream 的 reduce()方法时，需要传入一个参数，实现 ReduceFunction 接口

## udf

1. 函数类
   Flink 暴露了所有 UDF 函数的接口，具体实现方式为接口或者抽象类， 例如 MapFunction、FilterFunction、ReduceFunction 等
   当然也可以用Lambda表达式
2. 富函数类

“富函数类”也是DataStream API提供的一个函数类的接口，所有的Flink函数类都有其 Rich 版本。富函数类一般是以抽象类的形式出现的。例如:RichMapFunction、RichFilterFunction、 RichReduceFunction 等。

<font color=red>与常规函数类的不同主要在于，富函数类可以获取运行环境的上下文，并拥有一些生命周 期方法，所以可以实现更复杂的功能。典型的生命周期方法有:</font>
 ⚫ open()方法，是RichFunction的初始化方法，也就是会开启一个算子的生命周期。当

一个算子的实际工作方法例如 map()或者 filter()方法被调用之前，open()会首先被调 用。所以像文件 IO 流的创建，数据库连接的创建，配置文件的读取等等这样一次性 的工作，都适合在 open()方法中完成。

⚫ close()方法，是生命周期中的最后一个调用的方法，类似于解构方法。一般用来做一 些清理工作。

## 重分区

flink中的重分区算子定义上下游subtask之间数据传递的方式。SubTask之间进行数据传递模式有两种，一种是one-to-one(forwarding)模式，另一种是redistributing的模式。

One-to-one：数据不需要重新分布，上游SubTask生产的数据与下游SubTask受到的数据完全一致，数据不需要重分区，也就是数据不需要经过IO，比如上图中source->map的数据传递形式就是One-to-One方式。常见的map、fliter、flatMap等算子的SubTask的数据传递都是one-to-one的对应关系。类似于spark中的窄依赖。
Redistributing：数据需要通过shuffle过程重新分区，需要经过IO，比如上图中的map->keyBy。创建的keyBy、broadcast、rebalance、shuffle等算子的SubTask的数据传递都是Redistributing方式，但它们具体数据传递方式是不同的。类似于spark中的宽依赖； [参考](https://blog.csdn.net/qq_37555071/article/details/122415430)

“分区”(partitioning)操作就是要将数据进行重新分布，**传递到不同的流分区去进行下一 步计算**。keyBy()是一种逻辑分区(logical partitioning)操作。

flink中的重分区算子除了keyBy以外，还有broadcast、rebalance、shuffle、rescale、global、partitionCustom等多种算子，它们的分区方式各不相同。需要注意的是，这些算子中除了keyBy能将DataStream转化为KeyedStream外，其它重分区算子均不会改变Stream的类型

1. shuffle()方法，随机均匀分配到下游算子的并行任务中去
2. rebalance()方法，轮询分区(Round-Robin), 平均分配到下游的并行任务中，上下游并行度不同时的默认方式
3. rescale()方法，重缩放分区,底层也是Round-Robin算法轮询，<font color=red>但只会轮询到下游并行任务中的一部分任务中</font>
4. broadcast()方法，广播， 数据会在不同的分区都保留一份，可能进行重复处理
5.  global()方法，全局分区，全发送到下游第一个字任务中去
6. 自定义分区，实现Partitioner

## Sink

实现的是 SinkFunction 接口

![image-20220827013410474](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220827013410474.png)

![image-20220827013420963](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220827013420963.png)

对于输出到文件系统，Flink 为此专门提供了一个流式文件系统的连接器:StreamingFileSink，它继承自抽象类 RichSinkFunction，而且集成了 Flink 的检查点(checkpoint)机制，用来保证精确一次(exactly once)的一致性语义。

StreamingFileSink 为批处理和流处理提供了一个统一的 Sink，它可以将分区文件写入 Flink 支持的文件系统。它可以保证精确一次的状态一致性，大大改进了之前流式文件输出的方式。 它的主要操作是将数据写入桶(buckets)，每个桶中的数据都可以分割成一个个大小有限的分 区文件。

StreamingFileSink 支持行编码(Row-encoded)和批量编码(Bulk-encoded，比如 Parquet) 格式。这两种不同的方式都有各自的构建器(builder)，调用方法也非常简单，可以直接调用 StreamingFileSink 的静态方法:

⚫ 行编码:StreamingFileSink.forRowFormat t(basePath,rowEncoder)。
 ⚫ 批量编码:StreamingFileSink.forBulkFormat(basePath,bulkWriterFactory)。



# 运行时架构

Flink底层执行分为`客户端`（Client）、`Job管理器`（JobManager）、`任务执行器`（TaskManager）三种角色组件。其中

1. Client负责Job提交；
2. JobManager负责协调Job执行和Task任务分配；
3. TaskManager负责Task任务执行。

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/5845e9e5-91f9-4d02-8824-26c33831c242.png" alt="Flink基础工作原理：Standalone模式" style="zoom:50%;" />

Flink常见执行流程如下（调度器不同会有所区别）：

- 1）用户提交流程序Application。
- 2）Flink`解析StreamGraph`。`Optimizer`和Builder模块解析程序代码，生成初始`StreamGraph`并提交至`Client`。
- 3）Client`生成JobGraph`。上述StreamGraph由一系列operator chain构成，在client中会被转换为`JobGraph`，即优化多个chain为一个节点，最终提交到`JobManager`。
- 4）JobManager`调度Job`。JobManager和Client的`ActorSystem`保持通信，并生成`ExecutionGraph`（并行化JobGraph）。随后Schduler和Coordinator模块协调并调度Job执行。
- 5）TaskManager`部署Task`。`TaskManager`和`JobManager`的`ActorSystem`保持通信，接受`job调度计划`并在内部划分`TaskSlot`部署执行`Task任务`。
- 6）Task`执行`。Task执行期间，JobManager、TaskManager和Client之间保持通信，回传`任务状态`和`心跳信息`，监控任务执行。

## Client(客户端)

## JobManager(作业管理器)

JobManager 是一个 Flink 集群中任务管理和调度的核心，是控制应用执行的主进程,包含3个不同的组件。

1. **JobMaster**,多个，每个负责处理单独的Job
   1. 和Job一一对应，多个job可以同时运行在一个 Flink 集群中, 每个 job 都有一个自己的 JobMaster。
   2. jobMaster把JobGraph转换成一个执行图，包含了所有可并发执行的任务
   3. 向资源管理器申请资源，并把执行图分发到TaskManager上
   4. 运行过程中，还会负责检查点等协调工作
2. **资源管理器**(ResourceManager)
   1. 在Flink集群中只有一个
   2. ”资源“ 就是TaskManager的任务槽(task slot)
3. **分发器**(Dispatcher)
   1. Dispatcher 主要负责提供一个 REST 接口，用来提交作业
   2. 负责为每一个新提交的作 业启动一个新的 JobMaster 组件
   3. Dispatcher 也会启动一个 Web UI，用来方便地展示和监控作 业执行的信息

## TaskManager(任务管理器)

TaskManager 是 Flink 中的工作进程，负责数据流的具体计算任务(task)。  启动之后，TaskManager 会向资源管理器注册它的 slots

## 作业提交流程

### 抽象视角

![image-20221223211249390](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221223211249390.png)

(1)一般情况下，由客户端(App)通过分发器提供的 REST 接口，将作业提交给 JobManager。

(2)由分发器启动 JobMaster，并将作业(包含 JobGraph)提交给 JobMaster。

(3)JobMaster 将 JobGraph 解析为可执行的 ExecutionGraph（此时才知道并行度），得到所需的资源数量，然后 向资源管理器请求任务槽资源(slots)。

(4)资源管理器判断当前是否由足够的可用资源;如果没有，启动新的 TaskManager。

 (5)TaskManager 启动之后，向 ResourceManager 注册自己的可用任务槽(slots)。 

(6)资源管理器通知 TaskManager 为新的作业提供 slots。
 (7)TaskManager 连接到对应的 JobMaster，提供 slots。

(8)JobMaster 将需要执行的任务分发给 TaskManager。

 (9)TaskManager 执行任务，互相之间可以交换数据。

### Yarn集群

[参考](https://blog.csdn.net/nazeniwaresakini/category_9705501.html)

#### session模式

Session模式是预分配资源的，也就是提前根据指定的资源参数初始化一个Flink集群，并常驻在YARN系统中，拥有固定数量的JobManager和TaskManager（注意JobManager只有一个）。**提交到这个集群的作业可以直接运行，免去每次分配资源的overhead。但是Session的资源总量有限，多个作业之间又不是隔离的，故可能会造成资源的争用；如果有一个TaskManager宕机，它上面承载着的所有作业也都会失败。另外，启动的作业越多，JobManager的负载也就越大。所以，Session模式一般用来部署那些对延迟非常敏感但运行时长较短的作业。**

#### 单作业(Per-Job)模式

顾名思义，在Per-Job模式下，<font color=red>每个提交到YARN上的作业会各自形成单独的Flink集群，拥有专属的JobManager和TaskManager</font>。可见，以Per-Job模式提交作业的启动延迟可能会较高，但是作业之间的资源完全隔离，一个作业的TaskManager失败不会影响其他作业的运行，JobManager的负载也是分散开来的，不存在单点问题。当作业运行完成，与它关联的集群也就被销毁，资源被释放。所以，Per-Job模式一般用来部署那些长时间运行的作业。

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221223214332978.png" alt="image-20221223214332978" style="zoom:50%;" />

(1)客户端将作业提交给 YARN 的资源管理器，这一步中会同时将 Flink 的 Jar 包和配置 上传到 HDFS，以便后续启动 Flink 相关组件的容器。

(2)YARN的资源管理器分配容器(container)资源，启动Flink JobManager，并将作业 提交给 JobMaster。这里省略了 Dispatcher 组件。

(3)JobMaster 向资源管理器请求资源(slots)。
 (4)资源管理器向 YARN 的资源管理器请求容器(container)。
 (5)YARN 启动新的 TaskManager 容器。
 (6)TaskManager 启动之后，向 Flink 的资源管理器注册自己的可用任务槽。 

(7)资源管理器通知 TaskManager 为新的作业提供 slots。 

(8)TaskManager 连接到对应的 JobMaster，提供 slots。
 (9)JobMaster 将需要执行的任务分发给 TaskManager，执行任务。

#### 应用(Application)模式

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221223213458922.png" alt="image-20221223213458922" style="zoom: 25%;" />

对于上面的两种模式，在main()方法开始执行直到env.execute()方法之前，客户端也需要做一些工作，即：

- 获取作业所需的依赖项；
- 通过执行环境分析并取得逻辑计划，即StreamGraph→JobGraph；
- 将依赖项和JobGraph上传到集群中。

如果所有用户都在同一个客户端节点上提交作业，较大的依赖会消耗更多的带宽，而较复杂的作业逻辑翻译成JobGraph也需要吃掉更多的CPU和内存，客户端的资源反而会成为瓶颈——不管Session还是Per-Job模式都存在此问题。为了解决它，社区在传统部署模式的基础上实现了Application模式。

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221223213533150.png" alt="image-20221223213533150" style="zoom:25%;" />



可见，原本需要客户端做的三件事被转移到了JobManager里，也就是说main()方法在集群中执行（入口点位于ApplicationClusterEntryPoint），客户端只需要负责发起部署请求了。<font color=red>另外，如果一个main()方法中有多个env.execute()/executeAsync()调用，在Application模式下，这些作业会被视为属于同一个应用，在同一个集群中执行（如果在Per-Job模式下，就会启动多个集群）。可见，Application模式本质上是Session和Per-Job模式的折衷。</font>

#### slot与slot共享

可以通过集群的配置文件来设定 TaskManager 的 slots 数量: `taskmanager.numberOfTaskSlots: 8`

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221224085118283.png" alt="image-20221224085118283" style="zoom: 50%;" />

默认情况下，Flink 允许子任务共享 slots。如图所示，只要属于同一个作业，那么对 于不同任务节点的并行子任务，就可以放到同一个 slot 上执行。

如果希望某个算子对应的任务完全独占一个 slot，或者只有某一部分算子共享 slots，我们也可以通过设置“slot 共享组”





# 时间

**时间语义**： <font color=red>我们定义的窗口操作以哪种时间作为衡量标准？</font>

1. 处理时间：执行处理操作的机器的系统时间，最简单的时间语义
2. 事件时间：每个事件在对应设备上发生的时间，即数据生成的时间，是数据的时间戳

> 但是由于网络等原因，数据的时间戳不能直接当作时钟，因为总有乱序数据，导致时间倒退的情况，这需要用到事件时间的**水位线**来标志事件时间进展

## 水位线

是处理延迟数据的优化机制

**watermark的定义是**：比如在一个窗口内，当位于窗口最大`watermark`（水位线）的数据达到后，表明（约定）该窗口内的所有数据均已达到，此时不再等待数据，直接触发窗口计算。

### 什么是水位线

<font color=red>水位线可以看作一条特殊的数据记录，是插入到数据流中的一个标记点，主要内容就是一个时间戳，用来指示当前的事件时间。而它插入流中的位置，就应该是在某个 数据到来之后;这样就可以从这个数据中提取时间戳，作为当前水位线的时间戳了</font>

![image-20220827141440516](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220827141440516.png)

为了提高效率，一般会每隔一段时间生成一个水位 线，这个水位线的时间戳，就是当前最新数据的时间戳。 插入时间戳时先判断时间戳是否比之前的大，否则就不再生成新的时间戳。

![image-20220827141530085](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220827141530085.png)

为了让窗口能够正确收集到迟到的数据，我们可以等上 2 秒;也就是用当前已有数据的最

大时间戳减去 2 秒，就是要插入的水位线的时间戳：

![image-20220827143849490](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220827143849490.png)

<font color=red>**我们可以总结一下水位线的特性:**</font>

-  水位线是插入到数据流中的一个标记，可以认为是一个特殊的数据

- 水位线主要的内容是一个时间戳，用来表示当前事件时间的进展

- 水位线是基于数据的时间戳生成的

- 水位线的时间戳必须单调递增，以确保任务的事件时间时钟一直向前推进

- <font color=red>水位线可以通过设置延迟，来保证正确处理乱序数据（这其实相当于把系统时钟延迟了,牺牲了时效性。更推荐的方式是通过设置窗口等待来处理迟到数据）</font>

-  一个水位线 Watermark(t)，表示在当前流中事件时间已经达到了时间戳 t, 这代表 t 之

  前的所有数据都到齐了，之后流中不会出现时间戳 t’ ≤ t 的数据
   水位线是 Flink 流处理中保证结果正确性的核心机制，它往往会跟窗口一起配合，完成对

  乱序数据的正确处理。Flink 中的水位线，其实是流处理中对低延迟和结果正确性的一个权衡机制，而且把 控制的权力交给了程序员，我们可以在代码中定义水位线的生成策略。

### 水位线的生成策略

  <font color=red>stream.assignTimestampsAndWatermarks(WatermarkStrategy)</font>

它主要用来为流中的数据分配时间戳，并生成水位线来指 示事件时间。

  ```scala
  public interface WatermarkStrategy<T>
  extends TimestampAssignerSupplier<T>,
            WatermarkGeneratorSupplier<T>{
  //(一)时间戳分配器
     TimestampAssigner<T>
  createTimestampAssigner(TimestampAssignerSupplier.Context context);
  //(二)基于时间戳生成水位线
     WatermarkGenerator<T>
  createWatermarkGenerator(WatermarkGeneratorSupplier.Context context);
  }
  /**
  WatermarkGenerator 接口中，主要又有两个方法:onEvent()和 onPeriodicEmit()。
  ⚫ onEvent:每个事件(数据)到来都会调用的方法，它的参数有当前事件、时间戳， 以及允许发出水位线的一个 WatermarkOutput，可以基于事件做各种操作
  ⚫ onPeriodicEmit:周期性调用的方法，可以由WatermarkOutput发出水位线。周期时间
  为处理时间，可以调用环境配置的 setAutoWatermarkInterval()方法来设置，默认为
  200ms。 env.getConfig.setAutoWatermarkInterval(60 * 1000L)
  */
  
  ```



#### 内置水位线生成策略

水位线的生成策略可以分为两类：

- 基于事件时间生成
- 基于时间周期生成

通过调用 WatermarkStrategy 的静态辅助方法来创建。它们都是周期性 生成水位线的，分别对应着处理有序流和乱序流的场景。

1. 有序流

   直接调用WatermarkStrategy.forMonotonousTimestamps()方法就可以实现。简单来说，就是直接拿当前最 大的时间戳作为水位线就可以了。这里需要注意的是，时间戳和水位线的单位，必须都是毫秒。

   ```scala
   stream.assignTimestampsAndWatermarks( WatermarkStrategy.forMonotonousTimestamps[Event]()
      //指定当前水位线生成策略的时间戳生成器                                  
       .withTimestampAssigner(
         new SerializableTimestampAssigner[Event] {
           /**
           t: 被赋予时间戳的事件
           l: 当前事件的时间戳，或者一个负值(未赋予时间戳)
           */
           override def extractTimestamp(t: Event, l: Long): Long = t.timestamp
         }
       ))
   ```

   

2. 乱序流
   WatermarkStrategy. forBoundedOutOfOrderness() 设置最大乱序时间
   
   ```scala
       stream.assignTimestampsAndWatermarks( WatermarkStrategy.forBoundedOutOfOrderness[Event](Duration.ofSeconds(5))
       .withTimestampAssigner(
         new SerializableTimestampAssigner[Event] {
           override def extractTimestamp(t: Event, l: Long): Long = t.timestamp
         }
       ))
   ```
   
   

### 自定义水位线策略

在 WatermarkStrategy 中，时间戳分配器 TimestampAssigner 都是大同小异的，指定字段提 取时间戳就可以了;而不同策略的关键就在于 WatermarkGenerator 的实现。<font color=red>整体说来，Flink 有两种不同的生成水位线的方式:一种是周期性的(Periodic)，另一种是断点式的(Punctuated)。</font>

(1)周期性水位线生成器(Periodic Generator)
 周期性生成器一般是通过 onEvent()观察判断输入的事件，而在 onPeriodicEmit()里发出水位线。
 (2)断点式水位线生成器(Punctuated Generator)
不停地检测 onEvent()中的事件，当发现带有水位线信息的特殊事件时， 就立即发出水位线

```scala
    stream.assignTimestampsAndWatermarks( new WatermarkStrategy[Event] {
      //(一) 提取时间戳，当然提取时间戳也可以像上面内置水位线生成器那样放到外面程序里提取
      override def createTimestampAssigner(context: TimestampAssignerSupplier.Context): TimestampAssigner[Event] = {
        new SerializableTimestampAssigner[Event] {
          override def extractTimestamp(t: Event, l: Long): Long = t.timestamp
        }
      }
     //(二) 实现水位器生成器， 这里是周期性发送时间戳，如果想要断点式生成时间戳，则在onEvent里更新时间戳并发送即可，onPeriodicEmit无需实现
      override def createWatermarkGenerator(context: WatermarkGeneratorSupplier.Context): WatermarkGenerator[Event] = {
        new WatermarkGenerator[Event] {
          // 定义一个延迟时间
          val delay = 5000L
          //1 定义属性保存最大时间戳
          var maxTs = Long.MinValue + delay + 1  //+xx防止溢出  

          override def onEvent(t: Event, l: Long, watermarkOutput: WatermarkOutput): Unit = {
            //2 根据当前事件更新当前最大时间戳
            maxTs = math.max(maxTs, t.timestamp)
          }

          override def onPeriodicEmit(watermarkOutput: WatermarkOutput): Unit = {
            //3 框架会定期调用这个方法，该方法周期性发送时间戳
            val watermark = new Watermark(maxTs - delay - 1)
            watermarkOutput.emitWatermark(watermark)
          }
        }
      }
    } )
```

断点式生成器会不停地检测 onEvent()中的事件，当发现带有水位线信息的特殊事件时， 就立即发出水位线。一般来说，断点式生成器不会通过 onPeriodicEmit()发出水位线。

**在自定义数据源中生成水位线和在程序中使用 assignTimestampsAndWatermarks 方法生成水位线二者只能取其一。**

### 水位线的传递

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221224114514677.png" alt="image-20221224114514677" style="zoom:50%;" />

**上游不同分区进度不同时 ，以最慢的那个时钟，即最小的那个水位线为准。**

# 窗口

将无限的数据流切割成有限的“数据块”进行处理，这就是窗口，是无界流处理的核心。

1. 为了明确数据划分到哪一个窗口，定义窗口都是包含起始时间、不包含结束时间 的，用数学符号表示就是一个<font color=red>左闭右开</font>的区间。

2. Flink 中窗口并不是静态准备好的，而是<font color=red>动态创建</font>——当有落在这个 窗口区间范围的数据达到时，才创建对应的窗口

3. <font color=red>我们可以把窗口理解成一个桶，每个数据都会分发到对应的桶中</font> 

   > 窗口[0,10)因为设置了两秒延时，虽然它在水位线到达12时才关闭窗口，但11/12这两条数据是不会进入到这个窗口的

![image-20220827161712320](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220827161712320.png)

## 窗口的分类

按驱动类型分：

1. 时间窗口，左闭右开
2. 计数窗口

按数据分配规则分类

1. 滚动窗口：首尾相接、不重复

   1. 基于数据个数

      >  stream.keyBy(...)
      >      .countWindow(10)

   2. 基于时间长度: 

      > TumblingProcessingTimeWindows.of(size)
      >
      > TumblingEventTimeWindows.of(size)

2. 滑动窗口：窗口大小和滑动步长可调

   1. 基于数据个数

      >  stream.keyBy(...)
      >      .countWindow(size,  slide)

   2. 基于时间: 

      > SlidingProcessingTimeWindows.of( size , slide ) 
      >
      > SlidingEventTimeWindows.of

3. 会话窗口，只需指定会话间隔（可固定，也可自定义）

   > ProcessingTimeSessionWindows.withGap 
   >
   > EventTimeSessionWindows.withGap

4. 全局窗口

   >  stream.keyBy(...)
   >      .window(GlobalWindows.create())

把相同key的所有数据分配到同一个窗口中， 因为数据无界，所以窗口也不会结束，默认不会触发计算，但也可自定义**触发器**来计算

## 窗口API

### 按键分区和非按键分区

1. 按键分区窗口

   过按键分区 keyBy()操作后，数据流会按照 key 被分为多条逻辑流(logical streams)，这

   就是 KeyedStream。基于 KeyedStream 进行窗口操作时, 窗口计算会在多个并行子任务上同时 执行。相同 key 的数据会被发送到同一个并行子任务，而窗口操作会基于每个 key 进行单独的 处理。所以可以认为，每个 key 上都定义了一组窗口，各自独立地进行统计计算。
   
   ![image-20220913104205899](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220913104205899.png)

```scala
 stream.keyBy(...)
      .window(...)
```

2. 非按键分区

如果没有进行 keyBy()，那么原始的 DataStream 就不会分成多条逻辑流。这时窗口逻辑只

能在一个任务(task)上执行，就相当于并行度变成了 1。所以在实际应用中一般不推荐使用 这种方式。

在代码中，直接基于 DataStream 调用 windowAll()定义窗口。

```scala
stream.windowAll(...)
```

### 窗口API使用

主要两个部分， 窗口分配器窗口函数

```scala
stream.keyBy(<key selector>) 
.window/windowAll(<window assigner>) 
.aggregate(<window function>)
```

### 窗口分配器

所谓的分配，就是分配窗口数据，和窗口的分配是一样的。

![image-20220913104752924](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220913104752924.png)

1. 时间窗口

   - 滚动时间窗口

   ```scala
   stream.keyBy(...)
   .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
   .aggregate(...)
   ```

   - 滑动时间窗口

   ```scala
   SlidingEventTimeWindows.of(Time.seconds(10),Time.seconds(5))
   SlidingProcessingTimeWindows.of(Time.seconds(10),Time.seconds(5))
   ```

   - 事件时间会话窗口

   ```scala
   EventTimeSessionWindows.withGap(Time.seconds(10)))
   ```

2. 计数窗口

- 滚动计数

  ```scala
   stream.keyBy(...)
       .countWindow(10)
  ```

- 滑动计数

  ```scala
   stream.keyBy(...)
       .countWindow(10,3)
  ```

3. 全局窗口

```scala
 stream.keyBy(...)
     .window(GlobalWindows.create())
```



### 窗口函数

![image-20220913111338447](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220913111338447.png)



<font color=red>窗口函数总结</font>： 

- 增量聚合函数(计算分摊到窗口收集数据过程中，更加高效)

1. reduce后跟ReduceFunction，**聚合状态、结果类型必须和输入一样**
2. aggregate后跟AggregateFunction，**聚合状态、结果类型和输入可以不一样**

- 全窗口函数(提供了更多信息)

1. apply后跟WindowFunction  能提供的上下文信息较少，逐渐弃用
2. process后跟ProcessWindowFunction 最底层的通用窗口函数接口

两者优点可以结合在一起使用， 方法就是在reduce/aggregate里多传一个全窗口函数。大体思路，就是使用增量聚合函数聚合中间结果，等到窗口需要触发计算时，调用全窗口函数；这样即加速了计算，又不用缓存全部数据。





根据处理方式，可分成两类：

1. **增量聚合函数**

   1. ReduceFunction

      > **聚合状态、结果类型必须和输入一样**

      ```scala
      WindowedStream
      .reduce( new ReduceFunction)
      ```

   2. **AggretateFunction**

      > **输出类型可以和输入不一样**

      AggregateFunction 接口中有四个方法:
       ⚫ createAccumulator():创建一个累加器，这就是为聚合创建了一个初始状态，每个聚合任务只会调用一次。
       ⚫ add():将输入的元素添加到累加器中。这就是基于聚合状态，对新来的数据进行进

      一步聚合的过程。方法传入两个参数:当前新到的数据 value，和当前的累加器 accumulator;返回一个新的累加器值，也就是对聚合状态进行更新。每条数据到来之 后都会调用这个方法。

      ⚫ getResult():从累加器中提取聚合的输出结果。也就是说，我们可以定义多个状态， 然后再基于这些聚合的状态计算出一个结果进行输出。比如之前我们提到的计算平均 值，就可以把 sum 和 count 作为状态放入累加器，而在调用这个方法时相除得到最终 结果。这个方法只在窗口要输出结果时调用。

      ⚫ merge():合并两个累加器，并将合并后的状态作为一个累加器返回。这个方法只在 需要合并窗口的场景下才会被调用;最常见的合并窗口(Merging Window)的场景 就是会话窗口(Session Windows)。

   3. 另外，Flink 也为窗口的聚合提供了一系列预定义的简单聚合方法，可以直接基于 WindowedStream 调用。主要包括 sum()/max()/maxBy()/min()/minBy()，与 KeyedStream 的简单 聚合非常相似。它们的底层，其实都是通过 AggregateFunction 来实现的。

2. **全窗口函数**

   1. WindowFunction

      ```scala
      stream
      .keyBy(<key selector>) 
      .window(<window assigner>) 
      .apply(new MyWindowFunction())
      ```

      可拿到窗口所有元素和窗口本身的信息， 但缺乏上下文信息以及更高级的功能，逐渐被ProcessWindowFunction替代。

   2. **ProcessWindowFunction**

Window API 中最底层的通用窗口函数接口，最强大，它不仅可以获取窗口信息还可以获取到一个 “上下文对象”(Context)。这个上下文对象非常强大，不仅能够获取窗口信息，还可以访问当 前的时间和状态信息。

基于 WindowedStream 调用 process()方法，传入一个 ProcessWindowFunction 的实现类

3. **增量聚合和全窗口聚合的结合**

- **增量聚合函数处理计算会更高效。增量聚合相当于把计算量“均摊”到了窗口收集数据的 过程中，自然就会比全窗口聚合更加高效、输出更加实时。**
- **而全窗口函数的优势在于提供了更多的信息，可以认为是更加“通用”的窗口操作，窗口 计算更加灵活，功能更加强大**。

但reduce/aggretate方法可以组合增量窗口和全量窗口一起使用：
处理机制是:基于第一个参数(增量聚合函数)来处理窗口数据，每来一个数 据就做一次聚合;等到窗口需要触发计算时，则调用第二个参数(全窗口函数)的处理逻辑输 出结果。需要注意的是，这里的全窗口函数就不再缓存所有数据了，而是直接将增量聚合函数 的结果拿来当作了 Iterable 类型的输入。一般情况下，这时的可迭代集合中就只有一个元素了

### 其他API

触发器主要是用来控制窗口什么时候触发计算。所谓的“触发计算”，本质上就是执行窗 口函数，所以可以认为是计算得到结果并输出的过程。

1. **触发器**
   WindowedStream 调用 trigger()方法，每个窗口分配器(WindowAssigner)都会对应一个默认 的触发器;对于 Flink 内置的窗口类型，它们的触发器都已经做了实现。例如，所有事件时间 窗口，默认的触发器都是 EventTimeTrigger;类似还有 ProcessingTimeTrigger 和 CountTrigger。 所以一般情况下是不需要自定义触发器的，不过我们依然有必要了解它的原理。

   Trigger 是一个抽象类，自定义时必须实现下面四个抽象方法:
    ⚫ onElement():窗口中每到来一个元素，都会调用这个方法。
    ⚫ onEventTime():当注册的事件时间定时器触发时，将调用这个方法。
    ⚫ onProcessingTime ():当注册的处理时间定时器触发时，将调用这个方法。

   ⚫ clear():当窗口关闭销毁时，调用这个方法。一般用来清除自定义的状态。
   Trigger 除了可以控制触发计算，还可以定义窗口什么时候关闭(销毁)

2. **移除器**(Evictor)

   Evictor 接口定义了两个方法:
    ⚫ evictBefore():定义执行窗口函数之前的移除数据操作
    ⚫ evictAfter():定义执行窗口函数之后的以处数据操作 默认情况下，预实现的移除器都是在执行窗口函数(window fucntions)之前移除数据的。

3. **允许延迟**(Allowed Lateness)

我们可以设定允许延迟一段时间，在这段时 间内，窗口不会销毁，继续到来的数据依然可以进入窗口中并触发计算。直到水位线推进到了 窗口结束时间 + 延迟时间，才真正将窗口的内容清空，正式关闭窗口。基于 WindowedStream 调用 allowedLateness()方法，传入一个 Time 类型的延迟时间，就可 以表示允许这段时间内的延迟数据。

4. **迟到数据放入侧输出流**

基于 WindowedStream 调用 sideOutputLateData() 方法，就可以实现这个功能。方法需要 传入一个“输出标签”(OutputTag)，用来标记分支的迟到数据流。因为保存的就是流中的原 始数据，所以 OutputTag 的类型与流中数据类型相同

```scala
val winAggStream = stream.keyBy(...)
      .window(TumblingEventTimeWindows.of(Time.hours(1)))
     .sideOutputLateData(outputTag)
.aggregate(new MyAggregateFunction)
val lateStream = winAggStream.getSideOutput(outputTag)
```

## 处理函数的分类

Flink 中的处理函数其实是一个大家族,ProcessFunction 只是其中一员。
Flink 提供了 8 个不同的处理函数:
(1) ProcessFunction
最基本的处理函数,基于 DataStream 直接调用 process()时作为参数传入。
(2) KeyedProcessFunction
对流按键分区后的处理函数,基于 KeyedStream 调用 process()时作为参数传入。要想使用定时器,必须基于 KeyedStream。
(3) ProcessWindowFunction
开窗之后的处理函数,也是全窗口函数的代表。基于 WindowedStream 调用 process()时作为参数传入。
(4) ProcessAllWindowFunction
同样是开窗之后的处理函数,基于 AllWindowedStream 调用 process()时作为参数传入。
(5) CoProcessFunction
合并(connect)两条流之后的处理函数,基于 ConnectedStreams 调用 process()时作为参数传入。
(6) ProcessJoinFunction
间隔连接(interval join)两条流之后的处理函数,基于 IntervalJoined 调用 process()时作为参数传入。
(7) BroadcastProcessFunction
广播连接流处理函数,基于 BroadcastConnectedStream 调用 process()时作为参数传入。这里的“广播连接流”BroadcastConnectedStream,是一个未 keyBy 的普通 DataStream 与一个广播流(BroadcastStream)做连接(conncet)之后的产物。
(8) KeyedBroadcastProcessFunction
按键分区的广播连接流处理函数,同样是基于 BroadcastConnectedStream 调用 process()时作为参数传入。与 BroadcastProcessFunction 不同的是,这时的广播连接流,是一个 KeyedStream与广播流(BroadcastStream)做连接之后的产物。
接下来,我们就对 KeyedProcessFunction 和 ProcessWindowFunction 的具体用法展开详细说明。


## 迟到数据的处理

1. **设置水位线延迟时间**
   调整整个应用的全局逻辑时钟，水位线是所有事件时间定时器触发的判断标准，不易设置的过大，否则影响实时性。

2. **允许窗口处理迟到数据**

   水位线到达窗口结束时间时，快速输出一个近似结果，然后保持窗口继续等待延迟数据，每来一条数据窗口就重新计算更新结果。

3. **迟到数据放入侧输出流**

# 处理函数

在更底层，我们可以不定义任何具体的算子(比如 map()，filter()，或者 window())，而只 是提炼出一个统一的“处理”(process)操作——它是所有转换算子的一个概括性的表达，可 以自定义处理逻辑，所以这一层接口就被叫作“处理函数”(process function)。

处理函数主要是定义数据流的转换操作，所以也可以把它归到转换算子中。我们知道在 Flink 中几乎所有转换算子都提供了对应的函数类接口，处理函数也不例外;它所对应的函数 类，就叫作 ProcessFunction。

![image-20220913142358829](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220913142358829.png)

## 基本处理函数

### 处理函数功能

1. MapFunction 只能获取当前的数据
2. AggregateFunction可以获取当前的状态（以累加器形式出现）
3. RichMapFunction富函数类，getRuntimeContext(),可以拿到状态,还有并行度、任务名
   称之类的运行时信息
4. ProcessFunction可以访问事件的时间戳、水位线信息

<font color=red>处理函数提供了一个“定时服务”(TimerService),我们可以通过它访问流中的事件(event)、时间戳(timestamp)、水位线(watermark),甚至可以注册“定时事件”。而且处理函数继承了 AbstractRichFunction 抽象类,所以拥有富函数类的所有特性,同样可以访问状态(state)和其他运行时信息。此外,处理函数还可以直接将数据输出到侧输出流(side output)中。所以,处理函数是最为灵活的处理方法,可以实现各种自定义的业务逻辑;同时也是整个DataStream API 的底层基础。</font>

1. 抽象方法 processElement()
该方法用于“处理元素”
,定义了处理的核心逻辑。这个方法对于流中的每个元素都会调
用一次,参数包括三个:输入数据值 value,上下文 ctx,以及“收集器”
(Collector)out。方
法没有返回值,处理之后的输出数据是通过收集器 out 来定义的。
⚫ value:当前流中的输入元素,也就是正在处理的数据,类型与流中数据类型一致。
⚫ ctx:类型是 ProcessFunction 中定义的内部抽象类 Context,表示当前运行的上下文,
可以获取到当前的时间戳,并提供了用于查询时间和注册定时器的“定时服务”
(TimerService),以及可以将数据发送到“侧输出流”(side output)的方法 output()。
Context 抽象类定义如下:

```scala
public abstract class Context {
public abstract Long timestamp();
public abstract TimerService timerService();
public abstract <X> void output(OutputTag<X> outputTag, X value);
}
```

⚫out:“收集器”
(类型为 Collector),用于返回输出数据。使用方式与 flatMap()算子中的收集器完全一样,直接调用 out.collect()方法就可以向下游发出一个数据。这个方法可调用,也可以不调用。通过几个参数的分析不难发现,ProcessFunction 可以轻松实现 flatMap 这样的基本转换功能(当然 map()、filter()更不在话下);
而通过富函数提供的获取上下文方法.getRuntimeContext(),也可以自定义状态(state)进行处理,这也就能实现聚合操作的功能了。关于自定义状态的具
体实现,我们会在后续“状态管理”一章中详细介绍。

2. 非抽象方法 onTimer()
该方法用于定义定时触发的操作,这是一个非常强大、也非常有趣的功能。这个方法只有在注册好的定时器触发的时候才会调用,而定时器是通过“定时服务” TimerService 来注册的。打个比方,注册定时器(timer)就是设了一个闹钟,到了设定时间就会响;而 onTimer()中定义的,就是闹钟响的时候要做的事。所以它本质上是一个基于时间的“回调”(callback)方法,通过时间的进展来触发;在事件时间语义下就是由水位线(watermark)来触发了。与 processElement()类似,定时方法 onTimer()也有三个参数:时间戳(timestamp),上下文(ctx),以及收集器(out)。这里的 timestamp 是指设定好的触发时间,事件时间语义下当然就是水位线了。另外这里同样有上下文和收集器,所以也可以调用定时服务(TimerService),以及任意输出处理之后的数据。既然有.onTimer()方法做定时触发,我们用 processFunction 也可以自定义数据按照时间分组、定时触发计算输出结果;这其实就实现了窗口(window)的功能。这里需要注意的是,上面的 onTimer()方法只是定时器触发时的操作,而定时器(timer)真正的设置需要用到上下文 ctx 中的定时服务。在 Flink 中,只有“按键分区流”KeyedStream才支持设置定时器的操作,
所以之前的代码中我们并没有使用定时器。所以基于不同类型的流,可以使用不同的处理函数,它们之间还是有一些微小的区别的。接下来我们就介绍一下处理函数的分类

### 处理函数分类

(1)**ProcessFunction**

最基本的处理函数，基于 DataStream 直接调用 process()时作为参数传入。

 (2)**KeyedProcessFunction**

对流按键分区后的处理函数，基于 KeyedStream 调用 process()时作为参数传入。要想使用 定时器，必须基于 KeyedStream。

(3)**ProcessWindowFunction**

开窗之后的处理函数，也是全窗口函数的代表。基于 WindowedStream 调用 process()时作 为参数传入。

(4)ProcessAllWindowFunction
 同样是开窗之后的处理函数，基于 AllWindowedStream 调用 process()时作为参数传入。

(5)CoProcessFunction

合并(connect)两条流之后的处理函数，基于 ConnectedStreams 调用 process()时作为参 数传入。

(6)ProcessJoinFunction

间隔连接(interval join)两条流之后的处理函数，基于 IntervalJoined 调用 process()时作为 参数传入。

(7)BroadcastProcessFunction

广播连接流处理函数，基于 BroadcastConnectedStream 调用 process()时作为参数传入。这 里的“广播连接流”BroadcastConnectedStream，是一个未 keyBy 的普通 DataStream 与一个广 播流(BroadcastStream)做连接(conncet)之后的产物。

(8)KeyedBroadcastProcessFunction

按键分区的广播连接流处理函数，同样是基于 BroadcastConnectedStream 调用 process()时 作为参数传入。与 BroadcastProcessFunction 不同的是，这时的广播连接流，是一个 KeyedStream 与广播流(BroadcastStream)做连接之后的产物。

## KeyedProcessFunction

```scala
    stream.keyBy(data => true)
      .process( new KeyedProcessFunction[Boolean, Event, String] {
        override def processElement(value: Event, ctx: KeyedProcessFunction[Boolean, Event, String]#Context, out: Collector[String]): Unit = {
          val currentTime = ctx.timerService().currentProcessingTime()
          out.collect("数据到达，当前时间是：" + currentTime)
          // 注册一个5秒之后的定时器
          ctx.timerService().registerProcessingTimeTimer(currentTime + 5 * 1000)
        }

        // 定义定时器触发时的执行逻辑
        override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[Boolean, Event, String]#OnTimerContext, out: Collector[String]): Unit = {
          out.collect("定时器触发，触发时间为：" + timestamp)
        }
      } )
      .print()
```



## topN

每5s统计过去10s的Top2热度url

方法一: 全窗口函数

> windowAll里传一个滑动窗口,
>
> process里传一个ProcessAllWindowFunction，用HashMap统计url访问次数,转成数组取top N输出即可
>
> 缺点： 全窗口相当于把并行度置为1了，性能必然受到影响

方法二：思路： 增量聚合，窗口出发计算时只需要排序就行了

1. 先keyBy后开窗
2. 执行聚合： 先增量聚合，再结合全窗口聚合，输出当前key的统计包括窗口首尾时间。
3. 再keyBy<font color=red>窗口结束时间，按窗口进行分组</font>，这样得到同一个窗口所有key的统计
4. 再process： processElement使用状态收集所有key的统计，注册一个定时器, 到期时间为windowEnd+1ms（<font color=red>Flink 框架会自动忽略同一时间的重复注册</font> ），windowEnd+1` 的定时器被触发时，意味着收到了`windowEnd+1`的 Watermark，即收齐了该`windowEnd`下的所有用户窗口统计值。我们在 `onTimer()中获取状态并取top2输出



# 多流转换

## union/connect

这里其实，就是把两个流的数据放到一起

但，也可以通过keyBy()指定键进行分组后合并， 实现了类似于 SQL 中的 join 操作

1. 分流：一般通过侧输出流

2. 合流：合流算子比较丰富，union()/connect()/join()

   1. union的多个流必须类型相同(一次可以合并多个流)

   2. connect允许数据类型不同(一次只能合并两个流)

      > 在代码实现上，需要分为两步:首先基于一条 DataStream 调用 connect()方法，传入另外 一条 DataStream 作为参数，将两条流连接起来，得到一个 ConnectedStreams;然后再调用同处 理方法得到DataStream。这里可以的调用的同处理方法有map()/flatMap()，以及process()方法

```scala
val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    val stream1: DataStream[Int] = env.fromElements(1,2,3)
    val stream2: DataStream[Long] = env.fromElements(1L,2L,3L,4L)
    val connectedStream: ConnectedStreams[Int, Long] = stream1.connect(stream2)
    val result=connectedStream
      .map(new CoMapFunction[Int,Long,String] {
        //处理第一条流
        override def map1(in1: Int): String = {
          "Int:"+in1
        }
                                                                                                                                                                                          //处理第二条流
        override def map2(in2: Long): String = {
          "Long:"+in2
        }
      })

    result.print()
    env.execute()
```

对于连接流 ConnectedStreams 的处理操作，需要分别定义对两条流的处理转换，因此接口 中就会有两个相同的方法需要实现，用数字“1”“2”区分，在两条流中的数据到来时分别调 用。我们把这种接口叫作“**协同处理函数**”(co-process function)。与 CoMapFunction 类似，如 果是调用 flatMap()就需要传入一个 CoFlatMapFunction，需要实现 flatMap1()、flatMap2()两个 方法;而调用 process()时，传入的则是一个 CoProcessFunction。

CoProcessFunction 也是“处理函数”家族中的一员，同样可以通过上下文ctx来 访问 timestamp、水位线，并通过 TimerService 注册定时器;另外也提供了 onTimer()方法，用 于定义定时触发的处理操作。

## join(基于时间的合流)

对于两条流的合并，很多情况我们并不是简单地将所有数据放在一起，而是希望根据某个 字段的值将它们联结起来，“配对”去做处理。类似于SQL中的join

1. connect: 最复杂，通过keyBy()分组后合并,自定义状态、触发器
2. window: 中间
3. join算子：最简单

### 窗口join

1. 接口API

```scala
stream1
.join(stream2)//的到JoinedStreams 
.where(<KeySelector>) //左key
.equalTo(<KeySelector>) //右key
.window(<WindowAssigner>) //出现在同一个窗口
.apply(<JoinFunction>)//同时处理
```



join逻辑

```scala
public interface JoinFunction<IN1, IN2, OUT> extends Function, Serializable {
   OUT join(IN1 first, IN2 second) throws Exception;
}
```



2. 处理流程

JoinFunction 中的两个参数，分别代表了两条流中的匹配的数据。

窗口中每有一对数据成功联结匹配，JoinFunction 的 join()方法就会被调用一次，并输出一 个结果。

除了 JoinFunction，在 apply()方法中还可以传入 FlatJoinFunction，用法非常类似，只是内 部需要实现的 join()方法没有返回值。

> **注意，join的结果是笛卡尔积**

### 间隔join

隔联结的思 路就是针对一条流的每个数据，开辟出其时间戳前后的一段时间间隔，看这期间是否有来自另 一条流的数据匹配。

1. 原理：
   对第一条流中的每一条数据a ：开辟一段时间间隔:[a.timestamp + lowerBound, a.timestamp + upperBound], 如果另一条流的数据在这个范围内，则成功匹配

![image-20220906075209055](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220906075209055.png)

与窗口联结不同的是，interval join 做匹配的**时间段是基于流中数据的**，所以并不确定;**而且流 B 中的数据可以不只在一个区间内被匹配。**

2. 接口API


```scala
stream1
.keyBy(_._1) //基于keyed流做内连接
.intervalJoin(stream2.keyBy(_._1))
.between(Time.milliseconds(-2),Time.milliseconds(1))
.process(new ProcessJoinFunction[(String, Long), (String, Long), String] {
    override def processElement(left: (String, Long), right: (String, Long), ctx: ProcessJoinFunction[(String, Long), (String, Long), String]#Context, out: Collector[String]) = {
           out.collect(left + "," + right)
     }
});
```

# 状态编程

状态：就是程序用来计算输出结果所依赖的所有数据

## 状态分类

1. **托管状态**(Managed State)和**原始状态**(Raw State)

推荐托管状态：Flink运行时维护

原始状态时自定义的，需要自己实现状态的序列化和故障恢复



<font color=red>状态在任务间是天然隔离(不同slot计算资源是物理隔离的)的，对于有状态的操作，状态应该在key之间隔离。</font>

所以，按照状态的隔离可以这样分(只考虑托管状态)：

2. **算子状态**(Operator State)和**按键分区状态**(Keyed State)

- Flink 能管理的状态在并行任务间是无法共享的，每个状态只能针对当前子任务的实例有效

> 在 Flink 中，一个算子任务会按照并行度分为多个并行子任务执行，而不同的子
>
> 任务会占据不同的任务槽(task slots)。由于不同的 slot 在计算资源上是物理隔离的

算子状态可以用在所有算子上，使用的时候其实就跟一个本地变量没什么区别——因为本 地变量的作用域也是当前任务实例。在使用时，我们还需进一步实现 CheckpointedFunction 接 口。

- **keyed流之后的算子应该只能访问当前key**

  > 一个分区上可以有多个key

  按键分区状态应用非常广泛。之前讲到的聚合算子必须在 keyBy()之后才能使用，就是因 为聚合的结果是以Keyed State的形式保存的。另外，也可以通过富函数类(Rich Function) 来自定义 Keyed State，所以只要提供了富函数类接口的算子，也都可以使用 Keyed State。

  所以即使是 map()、filter()这样无状态的基本转换算子，我们也可以通过富函数类给它们 “追加”Keyed State，或者实现CheckpointedFunction接口来定义Operator State;从这个角度
  
  讲，Flink 中所有的算子都可以是有状态的，不愧是“有状态的流处理”。在富函数中，我们可以调用.getRuntimeContext() 获取当前的运行时上下文(RuntimeContext)，进而获取到访问状态的句柄;这种富函数中自 定义的状态也是 Keyed State。

### 按键分区状态

### 状态结构类型

1. 值状态ValueState

```scala
public interface ValueState<T> extends State {
   T value() throws IOException;
   void update(T value) throws IOException;
}
```

2. 列表状态ListState

用方式 与一般的 List 非常相似。

3. 映射状态(MapState)
4. 归约状态(ReducingState)
5. 聚合状态(AggregatingState)

在具体使用时，为了让运行时上下文清楚到底是哪个状态，我们还需要创建一个“状态描

述器”(StateDescriptor)来提供状态的基本信息

### 状态使用

```scala
class MyFlatMapFunction extends RichFlatMapFunction[Long, String] {
// 声明状态,如果不使用懒加载的 方式初始化状态变量，则应该在 open()方法中也就是生命周期的开始初始化状态变量
lazy val state = getRuntimeContext.getState(new
ValueStateDescriptor[Long]("my state", classOf[Long]))
override def flatMap(input: Long, out: Collector[String] ): Unit = { // 访问状态
       var currentState = state.value()
currentState += 1 // 状态数值加 1 // 更新状态 state.update(currentState)
if (currentState >= 100) {
          out.collect("state: " + currentState)
state.clear() // 清空状态 }
} }
```

### 状态生存时间(TTL)

需要创建一个 StateTtlConfig 配置对象，然后调用状态描述器的

enableTimeToLive()方法启动 TTL 功能

```scala
val ttlConfig = StateTtlConfig
   .newBuilder(Time.seconds(10))//状态生存时间
   .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)//什么时候更新状态失效时间
//状态可见性，状态的清理并非实时的，过期了仍然可能没被清理而被读取到；然而我们可控制是否读取这个过期值
   .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
   .build()
val stateDescriptor = new ValueStateDescriptor[String](
"my-state",
     classOf[String]
   )
stateDescriptor.enableTimeToLive(ttlConfig)
```

## 算子状态

从某种意义上说，算子状态是更底层的状态类型，因为它只针对当前算子并行任务有效，不需 要考虑不同 key 的隔离。算子状态功能不如按键分区状态丰富，应用场景较少(没有key定义的场景)，它的调用方法 也会有一些区别。

算子状态(Operator State)就是一个算子并行实例上定义的状态，作用范围被限定为当前 算子任务。算子状态跟数据的 key 无关，所以不同 key 的数据只要被分发到同一个并行子任务， 就会访问到同一个 Operator State。

当算子的并行度发生变化时，算子状态也支持在并行的算子任务实例之间做重组分配。根 据状态的类型不同，重组分配的方案也会不同。

### 状态类型

- ListState

> 每个并行子任务只保留一个list来保存当前子任务的状态项；当算子并行度进行缩放调整时，算子的列表状态中的所有元素项会被统一收集起来，相当 于把多个分区的列表合并成了一个“**大列表**”，然后再均匀地分配给所有并行任务

- UnionListState 

> 在并行度调整时，常规列表状态是轮询分 配状态项，而联合列表状态的算子则会直接广播状态的完整列表。这样，并行度缩放之后的并 行子任务就获取到了联合后完整的“大列表”

-  BroadcastState。

> 算子并行子任务都保持同一份“全局”状态，用来做统一的配置和规则设定。 这时所有分区的所有数据都会访问到同一个状态，状态就像被“广播”到所有分区一样，这种 特殊的算子状态，就叫作广播状态(BroadcastState)

### 代码实现

Flink提供了管理算子状态的接口，我们根据业务需要自行设计状态的快照保存和恢复逻辑

- CheckpointedFunction接口

  ```scala
  public interface CheckpointedFunction {
  // 保存状态快照到检查点时，调用这个方法； 注意这个快照Context可提供检查点相关信息，但无法获取状态
     void snapshotState(FunctionSnapshotContext context) throws Exception
  // 初始化状态时调用这个方法，也会在恢复状态时调用；这个函数初始化上下文是真的Context，可以获取到状态内容
  void initializeState(FunctionInitializationContext context) throws Exception;
  }
  ```

## 状态持久化和状态后端

Flink 对状态进行持久化的方式，就是将当前所 有分布式状态进行“快照”保存，写入一个“检查点”(checkpoint)或者保存点(savepoint) 保存到外部存储系统中

- 检查点： 

  > 至少一次：检查点之后又处理了一部分数据，如果出现故障，就需要源能重放这部分数据，否则就会丢失。例如，可以保存kafka的偏移量来重放数据

- 状态后端：

![image-20220913173114327](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220913173114327.png)

状态后端主要负责两件事:一是本地的状态管理，二是将检查 点(checkpoint)写入远程的持久化存储。

状态后端分类：

- ​	哈希表状态后端(HashMapStateBackend)
  - 内嵌 RocksDB 状态后端(EmbeddedRocksDBStateBackend)


> RocksDB 默认存储在 TaskManager 的本地数据目录里。EmbeddedRocksDBStateBackend 始终执行的是异步快照，也就是不会因为保存检查点而阻 塞数据的处理;而且它还提供了增量式保存检查点的机制，这在很多情况下可以大大提升保存 效率。
>
> 由于它会把状态数据落盘，而且支持增量化的检查点，所以在状态非常大、窗口非常长、 键/值状态很大的应用场景中是一个好选择，同样对所有高可用性设置有效。
>
> 

IDE中使用RocksDB状态后端，需要加依赖

```xml
<dependency>
   <groupId>org.apache.flink</groupId>
   <artifactId>flink-statebackend-rocksdb_2.11</artifactId>
   <version>1.13.0</version>
</dependency>
```

# 容错机制

## 检查点

检查点是 Flink 容错机制的核心。这里所谓的“检查”，其实是针对故障恢复的结果而言 的:故障恢复之后继续处理的结果，应该与发生故障前完全一致，我们需要“检查”结果的正 确性。所以，有时又会把 checkpoint 叫作“一致性检查点”。

### 参数

```java
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

// 每 60s 做一次 checkpoint
env.enableCheckpointing(60000);

// 高级配置：

// checkpoint 语义设置为 EXACTLY_ONCE，这是默认语义
env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);

// 两次 checkpoint 的间隔时间至少为 1 s，默认是 0，立即进行下一次 checkpoint
env.getCheckpointConfig().setMinPauseBetweenCheckpoints(1000);

// checkpoint 必须在 60s 内结束，否则被丢弃，默认是 10 分钟
env.getCheckpointConfig().setCheckpointTimeout(60000);

// 同一时间只能允许有一个 checkpoint
env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);

// 最多允许 checkpoint 失败 3 次
env.getCheckpointConfig().setTolerableCheckpointFailureNumber(3);

// 当 Flink 任务取消时，保留外部保存的 checkpoint 信息
env.getCheckpointConfig().enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

// 当有较新的 Savepoint 时，作业也会从 Checkpoint 处恢复
env.getCheckpointConfig().setPreferCheckpointForRecovery(true);

// 允许实验性的功能：非对齐的 checkpoint，以提升性能
env.getCheckpointConfig().enableUnalignedCheckpoints();
```

### checkpoint实现

[参考](https://cloud.tencent.com/developer/article/1766130)

Flink 的 checkpoint coordinator （JobManager 的一部分）会周期性的在流事件中插入一个 barrier 事件（栅栏），用来隔离不同批次的事件，如下图红色的部分。checkpoint barrier n-1 处的 barrier 是指 Job 从开始处理到 barrier n -1 所有的状态数据，checkpoint barrier n 处的 barrier 是指 Job 从开始处理到 barrier n 所有的状态数据。

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/1620.png)

每个算子遇到barrier，就把状态保存，并向 CheckpointCoordinator报告自己快照制作情况，同时向自身所有下游算子广播该barrier：

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/1620-20221224174054612.png" alt="img" style="zoom: 67%;" /><img src="https://ask.qcloudimg.com/http-save/yehe-4153409/20tnnqi0ji.png?imageView2/2/w/1620" alt="img" style="zoom: 67%;" />

当 Operator 2 收到barrier后，会触发自身进行快照，把自己当时的状态存储下来，向 CheckpointCoordinator 报告 自己快照制作情况。因为这是一个 sink ，状态存储成功后，意味着本次 checkpoint 也成功了。

<font color=red>总结： </font>

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221224182144200.png" alt="image-20221224182144200" style="zoom:50%;" />

1. JobManager 端的 CheckPointCoordinator 会定期向所有 SourceTask 发送 CheckPointTrigger，Source Task 会在数据流中安插 Checkpoint barrier；
2. 当 task 收到上游所有实例的 barrier 后，向自己的下游继续传递 barrier，然后自身同步进行快照，并将自己的状态异步写入到持久化存储中

- 如果是增量 Checkpoint，则只是把最新的一部分更新写入到外部持久化存储中
- 为了下游尽快进行 Checkpoint，所以 task 会先发送 barrier 到下游，自身再同步进行快照；

3. 当 task 将状态信息完成备份后，会将备份数据的地址（state handle）通知给 JobManager 的CheckPointCoordinator，如果 Checkpoint 的持续时长超过了 Checkpoint 设定的超时时间CheckPointCoordinator 还没有收集完所有的 State Handle，CheckPointCoordinator 就会认为本次 Checkpoint 失败，会把这次 Checkpoint 产生的所有状态数据全部删除；
4. 如果 CheckPointCoordinator 收集完所有算子的 State Handle，CheckPointCoordinator 会把整个 StateHandle 封装成 completed Checkpoint Meta，写入到外部存储中，Checkpoint 结束；

### 检查点的保存

- 周期性触发
- 保存的时间点： 一个数据**被所有任务处理完时保存**，或者就完全不保存该数据的状态

  > 如果出现故障， 故障时正在处理的所有数据都需要重新处理，所以我们只需要让源任务请求重放数据即可。
  >
  > 触发检查点保存的这个数据也叫做<font color=red>检查点分界线</font>，是由Source算子注入的特殊数据，其位置是限定好的， 在任何算子中不能被其他数据超过，它也不会超过其他任何数据。
  
  举例来说：下面这个wordcount例子里，所有算子处理完前3条数据时，可以进行检查点保存

```scala
val wordCountStream = env.addSource(...)
       .map((_,1))
       .keyBy(_._1)
       .sum(1)
```



![image-20220907084434583](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220907084434583.png)

### 从检查点恢复

发生故障时，找到最近一次成功保存的检查点来恢复状态：

重启任务，读取检查点，把状态分别填充到对应的状态中。Source任务重放故障时未处理完成的数据

### 检查点算法

#### 检查点分界线

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220914102032904.png" alt="image-20220914102032904" style="zoom:50%;" />

我们现在的目标是，在不暂停流处理的前提下，让每个任务“认出”触发检查点保存的那 个数据。

**可以借鉴水位线(watermark)的设计，在数据流中插入一个特殊的数据结构(就叫CheckpointBarrier,有id,timestamp)，专门 用来表示触发检查点保存的时间点。收到保存检查点的指令后，Source 任务可以在当前数据 流中插入这个结构;之后的所有任务只要遇到它就开始对状态做持久化快照保存。这种特殊的数据形式，把一条流上的数据按照不同的检查点分隔开，所以就叫作检查点的 “分界线”(Checkpoint Barrier)。**

与水位线很类似，检查点分界线也是一条特殊的数据，由 Source 算子注入到常规的数据 流中，它的位置是限定好的，不能超过其他数据，也不能被后面的数据超过。检查点分界线中 带有一个检查点 ID，这是当前要保存的检查点的唯一标识。

在 JobManager 中有一个“检查点协调器”(checkpoint coordinator)，专门用来协调处理检 查点的相关工作。检查点协调器会定期向 TaskManager 发出指令，要求保存检查点(带着检查 点 ID);**TaskManager 会让所有的 Source 任务把自己的偏移量(算子状态)保存起来，并将带 有检查点 ID 的分界线(barrier)插入到当前的数据流中**，然后像正常的数据一样像下游传递; 之后 Source 任务就可以继续读入新的数据了。中间算子接收到所有上游的barrier后，也产生一个barrier并分发到所有下游流中。Sink算子接收到所有上游的barrier后通知coordinator，所有Sink算子都通知coordinator后，就意味着这个快照完成了。

每个算子任务只要处理到这个 barrier，就把当前的状态进行快照;在收到 barrier 之前， 还是正常地处理之前的数据，完全不受影响。

#### 分布式快照算法

<font color=red>处理多分区分界线之间的传递，一传多时直接广播，多传一时等待所有分区的分界线都到齐。</font>

​        处理多个分区的分界线传递，一个分区向多个分区传递分界线比较简单，直接广播传递即可；复杂的是一个分区上游有多个分区的情况，所谓的分界线，就是分界线到来之前的数据都已经处理(需要保存到当前检查点)，分界线到来之后的数据都不处理(不需要保存检查点)；所以它需要等待所有上游分区的分界线都到齐，即所谓的分界线对齐。

​        对于分界线先到的分区，在等待其他分区分界线的过程中可能会接受到新的数据，这些数据属于分界线后的，不能处理，只能缓存起来，当出现背压时，会堆积大量的缓冲数据，检查点可能需要很久才能保存完毕。为了应对这种场景，Flink 1.11之后提供了不对齐的检查点保存方式，可以<font color=red>将未处理的缓 冲数据(in-flight data)也保存进检查点</font>。这样，当我们遇到一个分区barrier时就不需等待对 齐，而是可以直接启动状态的保存了。

​        对于分界线后到的分区，分界线到来之前的数据正常处理。

​		所有算子遇到分界线就做检查点，保存完状态都需要向JobManager确认

​        

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220914110452165.png" alt="image-20220914110452165" style="zoom:50%;" />

#### 非对齐检查点

顾名思义，非对齐检查点取消了屏障对齐操作

a) 当算子的所有输入流中的第一个屏障到达算子的输入缓冲区时，立即将这个屏障发往下游（输出缓冲区）。

b) 由于第一个屏障没有被阻塞，它的步调会比较快，超过一部分缓冲区中的数据。算子会标记两部分数据：一是屏障首先到达的那条流中被超过的数据，二是其他流中位于当前检查点屏障之前的所有数据（当然也包括进入了输入缓冲区的数据），如下图中标黄的部分所示。

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/format,png.png)

c) 将上述两部分数据连同算子的状态一起做异步快照。

既然不同检查点的数据都混在一起了，非对齐检查点还能保证exactly once语义吗？答案是肯定的。当任务从非对齐检查点恢复时，除了对齐检查点也会涉及到的Source端重放和算子的计算状态恢复之外，未对齐的流数据也会被恢复到各个链路，三者合并起来就是能够保证exactly once的完整现场了。

### 保存点(savepoint)

从名称就可以看出，这也是一个存盘的备份，它的原理和算法与检查点完全相同，只是多 了一些额外的元数据。事实上，保存点就是通过检查点的机制来创建流式作业状态的一致性镜 像(consistent image)的。Savepoint 包含了两个主要元素：

1. 首先，Savepoint 包含了一个目录，其中包含（通常很大的）二进制文件，这些文件表示了整个流应用在 Checkpoint/Savepoint 时的状态。
2. 以及一个（相对较小的）元数据文件，包含了指向 Savapoint 各个文件的指针，并存储在所选的分布式文件系统或数据存储中。

#### 用途

<font color=red>保存点与检查点最大的区别，就是触发的时机。检查点是由 Flink 自动管理的，定期创建， 发生故障之后自动读取进行恢复，这是一个“自动存盘”的功能;而保存点不会自动创建，必 须由用户明确地手动触发保存操作，所以就是“手动存盘”。因此两者尽管原理一致，但用途 就有所差别了:  检查点主要用来做故障恢复，是容错机制的核心;保存点则更加灵活，可以用 来做有计划的手动备份和恢复。</font>

它适用的具体场景有:

-  版本管理和归档存储 对重要的节点进行手动备份，设置为某一版本，<font color=red>归档(archive)存储应用程序的状态。</font>

-  更新Flink版本
   目前 Flink 的底层架构已经非常稳定，所以当 Flink 版本升级时，程序本身一般是兼容的。

这时不需要重新执行所有的计算，只要创建一个保存点，停掉应用、升级 Flink 后，从保存点 重启就可以继续处理了。

- 更新应用程序

我们不仅可以在应用程序不变的时候，更新 Flink 版本;还可以直接更新应用程序。前提 是程序必须是兼容的，也就是说更改之后的程序，状态的拓扑结构和数据类型都是不变的，这 样才能正常从之前的保存点去加载。

> 保存点能够在程序更改的时候依然兼容，前提是状态的拓扑结构和数据类 型不变。我们知道保存点中状态都是以算子 ID-状态名称这样的 key-value 组织起来的，算子 ID 可以在代码中直接调用 SingleOutputStreamOperator 的.uid()方法来进行指定.
>
> 对于没有设置 ID 的算子，Flink 默认会自动进行设置，所以在重新启动应用后可能会导致 ID 不同而无法兼容以前的状态。所以为了方便后续的维护，强烈建议在程序中为每一个算子 手动指定 ID。

- 调整并行度、扩容

如果应用运行的过程中，发现需要的资源不足或已经有了大量剩余，也可以通过从保存点 重启的方式，将应用程序的并行度增大或减小。

- 暂停应用程序

有时候我们不需要调整集群或者更新程序，只是单纯地希望把应用暂停、释放一些资源来 处理更重要的应用程序。使用保存点就可以灵活实现应用的暂停和重启，可以对有限的集群资 源做最好的优化配置。

#### 用法

- 创建保存点

  ```shell
  bin/flink savepoint :jobId [:targetDirectory]
  ```

- 创建保存点并停掉作业

```shell
bin/flink stop --savepointPath [:targetDirectory] :jobId
```

- 从保存点重启应用

```shell
bin/flink run -s :savepointPath [:runArgs]
```

#### 与checkpoint的关系

<font color=red>三个不同点</font>：

**目标：**从概念上讲，Flink 的 Savepoint 和 Checkpoint 的不同之处很像传统数据库中备份与恢复日志之间的区别。Checkpoint 的主要目标是充当 Flink 中的恢复机制，确保能从潜在的故障中恢复。相反，Savepoint 的主要目标是充当手动备份、恢复暂停作业的方法。

**实现：**Checkpoint 和 Savepoint 在实现上也有不同。<font color=red>Checkpoint 被设计成轻量和快速的机制。它们可能（但不一定必须）利用底层状态后端的不同功能尽可能快速地恢复数据。例如，基于 RocksDB 状态后端的增量检查点，能够加速 RocksDB 的 checkpoint 过程，这使得 checkpoint 机制变得更加轻量。相反，Savepoint 旨在更多地关注数据的可移植性，并支持对作业做任何更改而状态能保持兼容，这使得生成和恢复的成本更高。</font>

**生命周期：**<font color=red>Checkpoint 是自动和定期的，它们由 Flink 自动地周期性地创建和删除，无需用户的交互。相反，Savepoint 是由用户手动地管理（调度、创建、删除）的。</font>







## 状态一致性

通过检查点的保存来保证状态恢复后结果的正确。 状态一致性有三种级别；

- 最多一次
- 至少一次
- 精确一次

### 端到端状态一致性

完整的流处理应用，应该包括了数据源、流处理器和外部存储系统三个部分。这个完 整应用的一致性，就叫作“端到端(end-to-end)的状态一致性”，它取决于三个组件中最弱的 那一环。一般来说，能否达到 at-least-once 一致性级别，主要看数据源能够重放数据;而能否 达到 exactly-once 级别，流处理器内部、数据源、外部存储都要有相应的保证机制

Flink通过检查点(checkpoint)来保证exactly-onece语义。所以，端到端一致性的关键点，就在于输入的数据源端和输出的外部存储端。

**输入端**： 数据源必须能够重放，例如Kafka。

**输出端**： 一条数据被所有任务处理完并写入了外部系统，但是检查点还没来得及保存最新状态任务就失败了，此时最新的那个检查点认为该数据还没有处理，恢复时会重放那条数据，导致该数据重复写入。
能够保证 exactly-once 一致性的写入方式有两种:

- 幂等写入：多次写入，结果不会变。

> 需要注意，对于幂等写入，遇到故障进行恢复时，有可能会出现短暂的不一致。因为保存 点完成之后到发生故障之间的数据，其实已经写入了一遍，回滚的时候并不能消除它们。如果 有一个外部应用读取写入的数据，可能会看到奇怪的现象:短时间内，结果会突然“跳回”到 之前的某个值，然后“重播”一段之前的数据。不过当数据的重放逐渐超过发生故障的点的时 候，最终的结果还是一致的。

- 事务写入:  利用事务写入的数据可以收回

事务写入的基本思想就是: <font color=red>用一个事务来进行数据向外部系统的写入，这个事务是与检查点绑定在一起的</font>。当 Sink 任务 遇到 barrier 时，开始保存状态的同时就开启一个事务，接下来所有数据的写入都在这个事务 中;待到当前检查点保存完毕时，将事务提交，所有写入的数据就真正可用了。如果中间过程 出现故障，状态会回退到上一个检查点，而当前事务没有正常关闭(因为当前检查点没有保存 完)，所以也会回滚，写入到外部的数据就被撤销了。

事务写入又有两种实现方式:预写日志(WAL)和两阶段提交(2PC)

**预写日志：**

1先把结果数据作为日志(log)状态保存起来 

2进行检查点保存时，也会将这些结果数据一并做持久化存储 

3在收到检查点完成的通知时，将所有结果一次性写入外部系统。

> 缺点：有可能会写入失败；需要等待发送成功的返回确认信息，相当于再次确认；如果保存确认信息时出现故障，Flink会认为这次没有成功写入，不会使用这个检查点，而是回退到上一个，导致数据重复写入

#### <u>两阶段提交</u>

​       如果只是一个Sink实例，比较容易实现端到端严格一次，然而在多个Sink并发执行的应用中， 仅仅执行单个Sink的提交或回滚是不够的，一个Sink失败了会导致重放数据，其他成功的Sink并没有回滚导致数据重复写入。  Flink采用两阶段提交协议来让所有Sink对提交/回滚达成共识以保证一致的结果。

1. 预提交阶段
   开始执行检查点的时候进入预提交阶段，Barrier除了触发检查点，还负责将流切分出当前检查点的消息集合。所有的Sink把要写入外部存储的数据以State的形式保存到状态后端以防止提交失败时数据丢失，<font color=red>同时以事务的方式把数据写入外部存储完成预提交(因为事务的原因，此时数据虽然已写入但不可见)，至此预提交阶段完成</font>。

2. 提交阶段

   <font color=red>JobMaster为作业中每个算子发起检查点已完成的回调逻辑，提交外部事务,若提交成功，则Sink的写入真正写入外部存储，变成可见状态。 一旦失败会回滚到预提交完成时的状态尝试重新提交。</font>



