[Spark Streaming基于kafka的Direct详解](https://blog.csdn.net/erfucun/article/details/52275369)

# 有Receiver 

基于receiver的方式是使用kafka消费者高阶API实现的。
对于所有的receiver，它通过kafka接收的数据会被存储于spark的executors上，底层是写入BlockManager中，默认200ms生成一个block（通过配置参数spark.streaming.blockInterval决定）。然后由spark streaming提交的job构建BlockRdd，最终以spark core任务的形式运行。

## 关于receiver方式，有以下几点需要注意：

1. receiver作为一个常驻线程调度到executor上运行，占用一个cpu
2. receiver个数由KafkaUtils.createStream调用次数决定，一次一个receiver
3. kafka中的topic分区并不能关联产生在spark streaming中的rdd分区
4. 增加在KafkaUtils.createStream()中的指定的topic分区数，仅仅增加了单个receiver消费的topic的线程数，它不会增加处理数据中的并行的spark的数量【topicMap[topic,num_threads]map的value对应的数值是每个topic对应的消费线程数】
5. receiver默认200ms生成一个block，建议根据数据量大小调整block生成周期。
   receiver接收的数据会放入到BlockManager，每个executor都会有一个BlockManager实例，由于数据本地性，那些存在receiver的executor会被调度执行更多的task，就会导致某些executor比较空闲

建议通过参数spark.locality.wait调整数据本地性。该参数设置的不合理，比如设置为10而任务2s就处理结束，就会导致越来越多的任务调度到数据存在的executor上执行，导致任务执行缓慢甚至失败（要和数据倾斜区分开）

1. 多个kafka输入的DStreams可以使用不同的groups、topics创建，使用多个receivers接收处理数据
2. 两种receiver可靠的receiver：
   可靠的receiver在接收到数据并通过复制机制存储在spark中时准确的向可靠的数据源发送ack确认不可靠的receiver：

不可靠的receiver不会向数据源发送数据已接收确认。 这适用于用于不支持ack的数据源当然，我们也可以自定义receiver。

1. receiver处理数据可靠性默认情况下，receiver是可能丢失数据的。
   可以通过设置spark.streaming.receiver.writeAheadLog.enable为true开启预写日志机制，将数据先写入一个可靠地分布式文件系统如hdfs，确保数据不丢失，但会失去一定性能
2. 限制消费者消费的最大速率涉及三个参数：
   spark.streaming.backpressure.enabled：默认是false，设置为true，就开启了背压机制；

spark.streaming.backpressure.initialRate：默认没设置初始消费速率，第一次启动时每个receiver接收数据的最大值；
spark.streaming.receiver.maxRate：默认值没设置，每个receiver接收数据的最大速率（每秒记录数）。每个流每秒最多将消费此数量的记录，将此配置设置为0或负数将不会对最大速率进行限制

1. 在产生job时，会将当前job有效范围内的所有block组成一个BlockRDD，一个block对应一个分区
2. kafka082版本消费者高阶API中，有分组的概念，建议使消费者组内的线程数（消费者个数）和kafka分区数保持一致。如果多于分区数，会有部分消费者处于空闲状态

# 	Direct方式

## 特点

（1）Direct的方式是会直接操作kafka底层的元数据信息，这样如果计算失败了，可以把数据重新读一下，重新处理。即数据一定会被处理。拉数据，是RDD在执行的时候直接去拉数据。
（2）由于直接操作的是kafka，kafka就相当于你底层的文件系统。这个时候能保证严格的事务一致性，即一定会被处理，而且只会被处理一次。而Receiver的方式则不能保证，因为Receiver和ZK中的数据可能不同步，spark Streaming可能会重复消费数据，这个调优可以解决，但显然没有Direct方便。<font color=red>而Direct api直接是操作kafka的，spark streaming自己负责追踪消费这个数据的偏移量或者offset，并且自己保存到checkpoint，所以它的数据一定是同步的，一定不会被重复</font>。即使重启也不会重复，因为checkpoint了，但是程序升级的时候，不能读取原先的checkpoint，面对升级checkpoint无效这个问题，怎么解决呢?升级的时候读取我指定的备份就可以了，即手动的指定checkpoint也是可以的，这就再次完美的确保了事务性，有且仅有一次的事务机制。那么怎么手动checkpoint呢？构建SparkStreaming的时候，有getorCreate这个api，它就会获取checkpoint的内容，具体指定下这个checkpoint在哪就好了。

（3）<font color=red>由于底层是直接读数据，没有所谓的Receiver，直接是周期性(Batch Intervel)的查询kafka，处理数据的时候，我们会使用基于kafka原生的Consumer api来获取kafka中特定范围(offset范围)中的数据。这个时候，Direct Api访问kafka带来的一个显而易见的性能上的好处就是，如果你要读取多个partition，Spark也会创建RDD的partition，这个时候RDD的partition和kafka的partition是一致的。而Receiver的方式，这2个partition是没任何关系的。这个优势是你的RDD，其实本质上讲在底层读取kafka的时候，kafka的partition就相当于原先hdfs上的一个block。这就符合了数据本地性。RDD和kafka数据都在这边。所以读数据的地方，处理数据的地方和驱动数据处理的程序都在同样的机器上，这样就可以极大的提高性能。不足之处是由于RDD和kafka的patition是一对一的，想提高并行度就会比较麻烦。提高并行度还是repartition，即重新分区，因为产生shuffle，很耗时。</font>

（4）不需要开启wal机制，从数据零丢失的角度来看，极大的提升了效率，还至少能节省一倍的磁盘空间。从kafka获取数据，比从hdfs获取数据，因为zero copy的方式，速度肯定更快。

## SparkStreaming on Kafka Direct与Receiver 的对比

direct approach是spark streaming不使用receiver集成kafka的方式，一般在企业生产环境中使用较多。相较于receiver，有以下特点：
1.不使用receiver

- 不需要创建多个kafka streams并聚合它们
- 减少不必要的CPU占用
- 减少了receiver接收数据写入BlockManager，然后运行时再通过blockId、网络传输、磁盘读取等来获取数据的整个过程，提升了效率
- 无需wal，进一步减少磁盘IO操作

2.direct方式生的rdd是KafkaRDD，它的分区数与kafka分区数保持一致一样多的rdd分区来消费，更方便我们对并行度进行控制
注意：在shuffle或者repartition操作后生成的rdd，这种对应关系会失效

3.可以手动维护offset，实现exactly once语义

4.数据本地性问题。在KafkaRDD在compute函数中，使用SimpleConsumer根据指定的topic、分区、offset去读取kafka数据。
但在010版本后，又存在假如kafka和spark处于同一集群存在数据本地性的问题

5.限制消费者消费的最大速率
spark.streaming.kafka.maxRatePerPartition：从每个kafka分区读取数据的最大速率（每秒记录数）。这是针对每个分区进行限速，需要事先知道kafka分区数，来评估系统的吞吐量。
