[参考](https://cloud.tencent.com/developer/article/1506784)

[参考1](https://www.modb.pro/db/108234)

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