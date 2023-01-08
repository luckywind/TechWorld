[大数据兵工厂: 2万字50张图玩转Flink面试体系](https://bbs.huaweicloud.com/blogs/367983)

[参考](https://cloud.tencent.com/developer/article/1506784)

[参考1](https://www.modb.pro/db/108234)

[参考](https://chowdera.com/2022/02/202202120531451058.html)

[参考](https://blog.csdn.net/a805814077/article/details/108095451)

[K6K4面试题/圈子/技能提升/开发工具](http://www.k6k4.com/simple_question/qlist/4/20)

# 介绍一下Flink

**Flink 是一个框架和分布式处理引擎，用于对无界和有界数据流进行有状态计算。并且 Flink 提供了数据分布、容错机制以及资源管理等核心功能。**
Flink提供了诸多高抽象层的API以便用户编写分布式任务：

- DataSet API，
  对静态数据进行批处理操作，将静态数据抽象成分布式的数据集，用户可以方便地使用Flink提供的各种操作符对分布式数据集进行处理，支持Java、Scala和Python。
- DataStream
  API，对数据流进行流处理操作，将流式的数据抽象成分布式的数据流，用户可以方便地对分布式数据流进行各种操作，支持Java和Scala。
- Table
  API，对结构化数据进行查询操作，将结构化数据抽象成关系表，并通过类SQL的DSL对关系表进行各种查询操作，支持Java和Scala。

此外，Flink 还针对特定的应用领域提供了领域库，例如： Flink ML，Flink 的机器学习库，提供了机器学习Pipelines API并实现了多种机器学习算法。 Gelly，Flink 的图计算库，提供了图计算的相关API及多种图计算算法实现。

根据官网的介绍，Flink 的特性包含：

1. 支持高吞吐、低延迟、高性能的流处理 支持带有事件时间的窗口 （Window） 操作 支持有状态计算的 Exactly-once
   语义
2. 支持高度灵活的窗口 （Window） 操作，
3. 支持基于 time、count、session 以及 data-driven 的窗口操作
4. 支持具有 Backpressure 功能的持续流模型
5. 支持基于轻量级分布式快照（Snapshot）实现的容错 一个运行时同时支持 Batch on Streaming 处理和
   Streaming 处理 Flink 在 JVM 内部实现了自己的内存管理
6. 支持迭代计算
7. 支持程序自动优化：避免特定情况下 Shuffle、排序等昂贵操作，中间结果有必要进行缓存



## 个人总结

​        时间语义： 支持事件时间、摄入时间、处理时间。摄入时间/处理时间可以达到更低的延迟。 事件时间是数据自带的时间，采用事件时间时，所有算子都能看到相同的时间，但是事件时间不能作为时钟使用，因为事件有可能迟到而导致时钟后退。 Flink提供了水位线作为时钟，水位线是特殊的时间标记，是基于某个事件的时间得到的，只会递增。  对于迟到的数据，水位线不会更新，但如何处理迟到的数据呢？ 我们可以给水位线减去一个延时来正确处理迟到数据，但是这样相当于调慢了系统时钟，牺牲了时效性，更推荐的办法是通过窗口的延迟关闭来处理。

​		水位线生成策略：DataStream有个assignTimestampsAndWatermarks方法，接受一个WatermarkStrategy参数，我们可以自己实现时间戳生成器和水位线生成器。  当然Flink提供了内置的水位线生成器，WatermarkStrategy.forMonotonousTimestamps()和WatermarkStrategy. forBoundedOutOfOrderness() 

​		水位线的传递： 一个算子如果有多个上游分区，则其水位线会取所有分区的水位线的最小值，如果下游有多个分区，则会广播水位线。

​        窗口

​        运行时架构： jobManager(类似spark的master)， taskManager(类似spark的worker)； 其中jobManager包含jobMaster







## flink vs spark

1. 不同的世界观： Spark一切皆批处理； Flink一切皆流

   > spark总要攒一批数据，无法做到极致的低延迟；但批处理这块仍然优势明显

2. 数据模型和架构: 

   1. 数据模型： spark是RDD，而Flink的基本数据模型是数据流以及事件(Event)序列
   2. 架构不同:  Spark将DAG划分stage，Flink是流式执行模式，一个节点处理完后可以直接发往下一个节点进行处理

### why flink

主要考虑的是 flink 的**低延迟**、**高吞吐量**和对**流式数据**应用场景更好的支持；另外，flink 可以很好地处理**乱序**数据，而且可以保证 **exactly-once** 的状态一致性。

### Flink架构中的角色和作用

JobManager：

JobManager是Flink系统的协调者，它负责接收Flink Job，调度组成Job的多个Task的执行。同时，JobManager还负责收集Job的状态信息，并管理Flink集群中从节点TaskManager。

TaskManager：

TaskManager也是一个Actor，它是实际负责执行计算的Worker，在其上执行Flink Job的一组Task。每个TaskManager负责管理其所在节点上的资源信息，如内存、磁盘、网络，在启动的时候将资源的状态向JobManager汇报。

Client：

当用户提交一个Flink程序时，会首先创建一个Client，该Client首先会对用户提交的Flink程序进行预处理，并提交到Flink集群中处理，所以Client需要从用户提交的Flink程序配置中获取JobManager的地址，并建立到JobManager的连接，将Flink Job提交给JobManager。Client会将用户提交的Flink程序组装一个JobGraph， 并且是以JobGraph的形式提交的。一个JobGraph是一个Flink Dataflow，它由多个JobVertex组成的DAG。其中，一个JobGraph包含了一个Flink程序的如下信息：JobID、Job名称、配置信息、一组JobVertex等。

### flink并行度设置需要注意什么

Flink程序由多个任务（Source、Transformation、Sink）组成。任务被分成多个并行实例来执行，每个并行实例处理任务的输入数据的子集。任务的并行实例的数量称之为并行度。

并行度可以从多个不同层面设置：

操作算子层面(Operator Level)、执行环境层面(Execution Environment Level)、客户端层面(Client Level)、系统层面(System Level)。

### **Flink支持哪几种重启策略？分别如何配置？** 

重启策略种类：

固定延迟重启策略（Fixed Delay Restart Strategy）

故障率重启策略（Failure Rate Restart Strategy）

无重启策略（No Restart Strategy）

Fallback重启策略（Fallback Restart Strategy）

详细参考：https://www.jianshu.com/p/22409ccc7905



### checkpoint 的理解

Checkpoint是Flink实现容错机制最核心的功能，它能够根据配置周期性地基于Stream中各个Operator/task的状态来生成快照，从而将这些状态数据定期持久化存储下来，当Flink程序一旦意外崩溃时，重新运行程序时可以有选择地从这些快照进行恢复，从而修正因为故障带来的程序数据异常。他可以存在内存，文件系统，或者 RocksDB。

### exactly-once保证

端到端的 exactly-once 对 sink 要求比较高，具体实现主要有幂等写入和 事务性写入两种方式。幂等写入的场景依赖于业务逻辑，更常见的是用事务性写入。 而事务性写入又有预写日志（WAL）和两阶段提交（2PC）两种方式。

        如果外部系统不支持事务，那么可以用预写日志的方式，把结果数据先当成状态保存，然后在收到 checkpoint 完成的通知时，一次性写入 sink 系统。
### 状态机制

**状态始终与特定算子相关联**。Flink 会以 **checkpoint** 的形式对各个任务的 状态进行快照，用于保证故障恢复时的**状态一致**性。Flink 通过状态后端来管理状态 和 checkpoint 的存储，状态后端也可以有不同的配置选择。

### 海量key去重

由于数据量太大，set结构在内存中存不下。   考虑使用布隆过滤器去重

### watermark机制

在进行窗口计算的时候，不能无限期等待所有数据， 必须要有个触发的机制，比如基于时间的窗口，由于数据可能会乱序，所以事件时间来触发的话，可能导致延迟的数据丢失，导致计算不准确。 那么就需要一个时间机制即可以及时触发窗口计算又可以等待延迟数据，这个机制就是watermark。

### 三种时间语义

Event Time：这是实际应用最常见的时间语义，指的是事件创建的时间，往往跟watermark结合使用

Processing Time：指每一个执行基于时间操作的算子的本地系统时间，与机器相关。适用场景：没有事件时间的情况下，或者对实时性要求超高的情况

Ingestion Time：指数据进入Flink的时间。适用场景：存在多个 Source Operator 的情况下，每个 Source Operator 可以使用自己本地系统时钟指派 Ingestion Time。后续基于时间相关的各种操作， 都会使用数据记录中的 Ingestion Time

### 数据高峰的处理

使用大容量的 Kafka 把数据先放到消息队列里面作为数据源，再使用 Flink 进行消费，不过这样会影响到一点实时性。

### flink checkpoint

1. 什么时候触发checkpoint?

2. checkpoint过程？

   

3. 检查点是什么结构？

   > CheckpointBarrier extends RuntimeEvent类，有id, timestamp

4. checkpoint listener?

5. checkpoint有哪些接口？

   > - checkpointMode:  exactly_once/atleast_once
   > - checkpoint触发间隔、超时时间、最小间隔