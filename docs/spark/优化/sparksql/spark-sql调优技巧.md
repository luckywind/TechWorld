[spark-sql调优技巧](https://blog.csdn.net/weixin_40035337/article/details/108018058)

# Spark on hive 与 Hive on Spark 的区别

Spark on hive
Spark通过Spark-SQL使用hive 语句,操作hive,底层运行的还是 spark rdd。

（1）就是通过sparksql，加载hive的配置文件，获取到hive的元数据信息

（2）spark sql获取到hive的元数据信息之后就可以拿到hive的所有表的数据

（3）接下来就可以通过spark sql来操作hive表中的数据

Hive on Spark
      是把hive查询从mapreduce 的mr (Hadoop计算引擎)操作替换为spark rdd（spark 执行引擎） 操作. 相对于spark on hive,这个要实现起来则麻烦很多, 必须重新编译你的spark和导入jar包，不过目前大部分使用的是spark on hive。

# sparkSQL参数调优

## 数据缓存

性能调优主要是将数据放入内存中操作，spark缓存注册表的方法

缓存spark表：

spark.catalog.cacheTable("tableName")  缓存表

释放缓存表：

spark.catalog.uncacheTable("tableName")解除缓存

## 性能优化相关参数

Sparksql仅仅会缓存必要的列，并且自动调整压缩算法来减少内存和GC压力。

| 属性                                         | 默认值 | 描述                                                         |
| -------------------------------------------- | ------ | ------------------------------------------------------------ |
| spark.sql.inMemoryColumnarStorage.compressed | TRUE   | Spark SQL 将会基于统计信息自动地为每一列选择一种压缩编码方式。 |
| spark.sql.inMemoryColumnarStorage.batchSize  | 10000  | 缓存批处理大小。缓存数据时, 较大的批处理大小可以提高内存利用率和压缩率，但同时也会带来 OOM（Out Of Memory）的风险。 |
| spark.sql.files.maxPartitionBytes            | 128 MB | 读取文件时单个分区可容纳的最大字节数（不过不推荐手动修改，可能在后续版本自动的自适应修改） |
| spark.sql.files.openCostInBytes              | 4M     | 打开文件的估算成本, 按照同一时间能够扫描的字节数来测量。当往一个分区写入多个文件的时候会使用。高估更好, 这样的话小文件分区将比大文件分区更快  (先被调度)。 |

## 表数据广播

在进行表join的时候，将小表广播可以提高性能，spark2.+中可以调整以下参数、

| 属性                                 | 默认值 | 描述                                                         |      |
| ------------------------------------ | ------ | ------------------------------------------------------------ | ---- |
| spark.sql.broadcastTimeout           | 300    | 广播等待超时时间，单位秒,默认情况下，BroadCastJoin只允许被广播的表计算5分钟，超过5分钟该任务会出现超时异常，而这个时候被广播的表的broadcast任务依然在执行，造成资源浪费。 |      |
| spark.sql.autoBroadcastJoinThreshold | 10M    | 用于配置一个表在执行 join 操作时能够广播给所有 worker  节点的最大字节大小。通过将这个值设置为 -1 可以禁用广播。注意，当前数据统计仅支持已经运行了 ANALYZE TABLE COMPUTE STATISTICS noscan 命令的 Hive  Metastore 表。 |      |

## 分区数的控制

spark任务并行度的设置中，spark有两个参数可以设置

| 属性                         | 默认值                                                       | 描述                                                    |
| ---------------------------- | ------------------------------------------------------------ | ------------------------------------------------------- |
| spark.sql.shuffle.partitions | 200， 推荐线程数的2~3倍                                      | 用于配置 join 或aggregate   shuffle数据时使用的分区数。 |
| spark.default.parallelism    | 对于分布式shuffle操作像reduceByKey和join，父RDD中分区的最大数目。对于无父RDD的并行化等操作，它取决于群集管理器：-本地模式：本地计算机上的核心数-Mesos  fine grained mode：8-其他：所有执行节点上的核心总数或2，以较大者为准 | 分布式shuffle操作的分区数                               |
| auto.repartition             |                                                              | sql自动分区                                             |

spark.default.parallelism只有在处理RDD时才会起作用，对Spark SQL的无效。
spark.sql.shuffle.partitions则是对sparks SQL专用的设置

## 参数总结

```scala
//1.下列Hive参数对Spark同样起作用。
set hive.exec.dynamic.partition=true; // 是否允许动态生成分区   默认false
set hive.exec.dynamic.partition.mode=nonstrict; // 是否容忍指定分区全部动态生成  默认strict至少需要指定一个分区值
set hive.exec.max.dynamic.partitions = 100; // 动态生成的最多分区数
 
//2.运行行为
set spark.sql.autoBroadcastJoinThreshold; // 大表 JOIN 小表，小表做广播的阈值
set spark.dynamicAllocation.enabled; // 开启动态资源分配  默认false，开启后executor空虚60s之后被释放
set spark.dynamicAllocation.maxExecutors; //开启动态资源分配后，最多可分配的Executor数
set spark.dynamicAllocation.minExecutors; //开启动态资源分配后，最少可分配的Executor数
set spark.sql.shuffle.partitions; // 需要shuffle是mapper端写出的partition个数
set spark.sql.adaptive.enabled; // 是否开启调整partition功能，如果开启，spark.sql.shuffle.partitions设置的partition可能会被合并到一个reducer里运行
set spark.sql.adaptive.shuffle.targetPostShuffleInputSize; //开启spark.sql.adaptive.enabled后，两个partition的和低于该阈值会合并到一个reducer
set spark.sql.adaptive.minNumPostShufflePartitions; // 开启spark.sql.adaptive.enabled后，最小的分区数
set spark.hadoop.mapreduce.input.fileinputformat.split.maxsize; //当几个stripe的大小大于该值时，会合并到一个task中处理
 
//3.executor能力
set spark.executor.memory; // executor用于缓存数据、代码执行的堆内存以及JVM运行时需要的内存
set spark.yarn.executor.memoryOverhead; //Spark运行还需要一些堆外内存，直接向系统申请，如数据传输时的netty等。
set spark.sql.windowExec.buffer.spill.threshold; //当用户的SQL中包含窗口函数时，并不会把一个窗口中的所有数据全部读进内存，而是维护一个缓存池，当池中的数据条数大于该参数表示的阈值时，spark将数据写到磁盘
set spark.executor.cores; //单个executor上可以同时运行的task数

//4.开启堆外内存
spark.memory.offHeap.enabled & spark.memory.offHeap.size=30g

//5. 调度模式，或许有用
spark.scheduler.mode=FAIR， 默认是FIFO
```

# sql逻辑优化

https://blog.51cto.com/wang/4376310