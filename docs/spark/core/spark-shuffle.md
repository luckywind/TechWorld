[[Distributed Systems Architecture](https://0x0fff.com/)](https://0x0fff.com/spark-architecture-shuffle/)

[shuffle](https://gousios.gr/courses/bigdata/spark.html)

相同key的数据一定在一个节点上，内存放不下就存到磁盘

![image-20210719100823183](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210719100823183.png)

# Spark架构:shuffle

相同key的数据在一个节点上，本文遵循MR的命名公约：在shuffle操作中，源头executor上产生数据的task称为mapper，消费数据到目标executor的task称为reducer，两者之间是shuffle。

shuffle通常有两个重要的参数：

1.  ***spark.shuffle.compress\*** –是否对输出进行压缩
2. ***spark.shuffle.spill.compress\*** –是否压缩shuffle过程中spill的中间文件

默认值都是true，都会使用***spark.io.compression.codec\***进行压缩，默认是snappy.

spark中有多种shuffle实现，通过参数***spark.shuffle.manager\*** 指定，可选值有hash、sort和tungsten-sort. 在spark1.2.0之前，sort是默认的shuffle实现。

前一个stage 的 ShuffleMapTask 进行 shuffle write， 把数据存储在 blockManager 上面， 并且把数据位置元信息上报到 driver 的 mapOutTrack 组件中， 下一个 stage 根据数据位置元信息， 进行 shuffle read， 拉取上个stage 的输出数据。

## hash shuffle

​       spark1.2.0之前默认使用的shuffle算法，存在很多缺点，因为该实现创建的文件太多： 一个mapper task会为每个reducer创建一个不同的文件，当有M个mapper，R个reducer时，就会产生M*R个文件。系统的open files个数以及创建/删除这些文件的速度都会因为文件数太多而出现问题。

​        逻辑非常愚蠢：把reducer的个数作为reduce端的分区数，并创建相应个数的文件，然后迭代数据计算每个记录对应的partition并输出到对应的文件。看起来就像下图：

![spark_hash_shuffle_no_consolidation](https://gitee.com/luckywind/PigGo/raw/master/image/spark_hash_shuffle_no_consolidation-1024x484.png)

有一种优化版的实现，通过参数spark.shuffle.consolidateFiles(默认false)控制。当启用时，mapper输出文件的个数是固定的，例如集群有E个executor，且每个executor有C个core，每个task申请T个CPU(spark.task.cpus),那么集群execution slots的个数是`E*C/T`, shuffle过程中创建的文件个数是`E*C/T*R`

实现原理：创建一个输出文件池，而不是给每个reducer创建一个文件，当map task开始输出数据，它会从文件池中申请R个文件。当执行结束，它会返回这R个文件到文件池。因为每个executor只能并行执行C/T个Task,它只会创建C/T组输出文件，每组是R个文件。当第一组C/T个并行map task结束，下一组map task可以从文件池中重用这些文件组。看起来如下：

![spark_hash_shuffle_with_consolidation](https://gitee.com/luckywind/PigGo/raw/master/image/spark_hash_shuffle_with_consolidation-1024x500.png)

Pros:

1. Fast – no sorting is required at all, no hash table maintained;
2. No memory overhead for sorting the data;
3. No IO overhead – data is written to HDD exactly once and read exactly once.

Cons:

1. When the amount of partitions is big, performance starts to degrade due to big amount of output files
2. Big amount of files written to the filesystem causes IO skew towards random IO, which is in general up to 100x slower than sequential IO

# Sort Shuffle

​             从spark1.2.0开始，默认的shuffle算法改成sort了。这个shuffle逻辑类似MR，只输出一个文件，该文件中的记录首先是按照 Partition Id 排序，每个 Partition 内部再按照 Key 进行排序，Map Task 运行期间会顺序写每个 Partition 的数据，**同时生成一个索引文件**记录每个 Partition 的大小和偏移量,该文件按照reducer_id有序且可索引。这样在fread之前做一个fseek就很方便找到某个reducer相关的数据块。总体上看来 **Sort Shuffle 解决了 Hash Shuffle 的所有弊端**，但是因为需要其 Shuffle 过程需要对记录进行排序，所以**在性能上有所损失**。.当然，当reducer个数很小时，hash到多个文件是比sorting到一个文件快的，因此，sort shuffle有一个回退计划：当reducer个数小于spark.shuffle.sort.bypassMergeThreshold(默认200)个时，启用回退计划，hash到多个文件然后再join到一个文件。该实现的源码参考BypassMergeSortShuffleWriter类。

​          有意思的是这个实现在map端排序，但当需要数据顺序时，在reduce端不对排序结果进行merge，而只是re-sort(reduce端re-sort使用TimSort算法实现，它对预排序的数据非常高效)。

​          如果没有足够内存放下map的输出，则需要把中间结果spill到磁盘。spark.shuffle.spill可以控制是否开启，如果不开启当内存放不下时会OOM。

可用于排序map输出的内存大小=`spark.shuffle.memoryFraction * spark.shuffle.safetyFraction`，

默认是`JVM堆*0.2*0.8`即`JVM堆*0.16`。 注意，如果在一个Executor上运行多个线程(设置`spark.executor.cores / spark.task.cpus`>1),则每个map task可用JVM堆大小要做相应除法。

​		spill到磁盘的文件只有在reducer请求时才会时时merge。

![spark_sort_shuffle](https://gitee.com/luckywind/PigGo/raw/master/image/spark_sort_shuffle-1024x459.png)

# Unsafe Shuffle or Tungsten Sort(钨丝计划)

spark1.5开始，Spark 开始了钨丝计划（Tungsten），目的是优化内存和CPU的使用，进一步提升spark的性能。由于使用了堆外内存，而它基于 JDK Sun Unsafe API，故 Tungsten-Sort Based Shuffle 也被称为 Unsafe Shuffle。

可以通过`spark.shuffle.manager= tungsten-sort` 开启, 这是Tungsten[项目的一部分](https://issues.apache.org/jira/browse/SPARK-7081)。优化可以总结如下：

1. 无需反序列化，直接操作序列化后的二进制数据。它使用unsafe内存拷贝函数直接拷贝数据，操作字节数组非常高效。
2. 使用特殊的cache-efficient排序器[ShuffleExternalSorter](https://github.com/apache/spark/blob/master/core/src/main/java/org/apache/spark/shuffle/sort/ShuffleExternalSorter.java) ，直接排序压缩数组的指针和分区id
3. 因为数据没有反序列化，所以spill过程非常快
4. 当压缩编码支持序列化流的连接，则自动应用spill-merge优化

但这种方式无法利用mapper端的预排序优势，且貌似不稳定。但是使用 Tungsten-Sort Based Shuffle 有几个限制，Shuffle 阶段不能有 aggregate 操作，分区数不能超过一定大小（2^24-1，这是可编码的最大 Parition Id），所以像 reduceByKey 这类有 aggregate 操作的算子是不能使用 Tungsten-Sort Based Shuffle，它会退化采用 Sort Shuffle。图示如下：

![spark_tungsten_sort_shuffle](https://gitee.com/luckywind/PigGo/raw/master/image/spark_tungsten_sort_shuffle-1024x457.png)

# Sort ShuffleV2

从 Spark-1.6.0 开始，把 Sort Shuffle 和 Tungsten-Sort Based Shuffle 全部统一到 Sort Shuffle 中，如果检测到满足 Tungsten-Sort Based Shuffle 条件会自动采用 Tungsten-Sort Based Shuffle，否则采用 Sort Shuffle。从Spark-2.0.0开始，Spark 把 Hash Shuffle 移除，可以说目前 Spark-2.0 中只有一种 Shuffle，即为 Sort Shuffle。

# Shuffle Read

1. **在什么时候获取数据**，Parent Stage 中的一个 ShuffleMapTask 执行完还是等全部 ShuffleMapTasks 执行完？
   当 Parent Stage 的所有 ShuffleMapTasks 结束后再 fetch。
2. **边获取边处理还是一次性获取完再处理？**
   因为 Spark 不要求 Shuffle 后的数据全局有序，因此没必要等到全部数据 shuffle 完成后再处理，所以是边 fetch 边处理。
3. 获取来的**数据存放到哪里**？
   刚获取来的 FileSegment 存放在 softBuffer 缓冲区，经过处理后的数据放在内存 + 磁盘上。
   内存使用的是AppendOnlyMap ，类似 Java 的HashMap，内存＋磁盘使用的是ExternalAppendOnlyMap，如果内存空间不足时，ExternalAppendOnlyMap可以将 records 进行 sort 后 spill（溢出）到磁盘上，等到需要它们的时候再进行归并
4. 怎么获得**数据的存放位置**？
   通过请求 Driver 端的 MapOutputTrackerMaster 询问 ShuffleMapTask 输出的数据位置。

# Spark Shuffle 相关调优

从上述 Shuffle 的原理介绍可以知道，Shuffle 是一个涉及到 CPU（序列化反序列化）、网络 I/O（跨节点数据传输）以及磁盘 I/O（shuffle中间结果落地）的操作，用户在编写 Spark 应用程序的时候应当尽可能考虑 Shuffle 相关的优化，提升 Spark应用程序的性能。下面简单列举几点关于 Spark Shuffle 调优的参考。

- 尽量减少 Shuffle次数

```text
// 两次shuffle
rdd.map(...).repartition(1000).reduceByKey(_ + _, 3000)

// 一次shuffle
rdd.map(...).repartition(3000).reduceByKey(_ + _)
```

- 必要时主动 Shuffle，通常用于改变并行度，提高后续分布式运行速度

```text
rdd.repartiton(largerNumPartition).map(...)...
```

- 使用 treeReduce & treeAggregate 替换 reduce & aggregate。数据量较大时，reduce & aggregate 一次性聚合，Shuffle 量太大，而 treeReduce & treeAggregate 是分批聚合，更为保险。
- 触发shuffle的操作算子往往可以指定分区数的，也即是numPartitions代表下个stage会有多少个分区

## shuffle参数

>**spark.reducer.maxSizeInFlight**
>默认48m。从每个reduce任务同时拉取的最大map数，每个reduce都会在完成任务后，需要一个堆外内存的缓冲区来存放结果，如果没有充裕的内存就尽可能把这个调小一点。。相反，堆外内存充裕，调大些就能节省gc时间。
>**spark.reducer.maxBlocksInFlightPerAddress**
>限制了每个主机每次reduce可以被**多少台远程主机**拉取文件块，调低这个参数可以有效减轻node manager的负载。（默认值Int.MaxValue）
>**spark.reducer.maxReqsInFlight**
>限制远程机器拉取本机器文件块的**请求数**，随着集群增大，需要对此做出限制。否则可能会使本机负载过大而挂掉。。（默认值为Int.MaxValue）
>**spark.reducer.maxReqSizeShuffleToMem**
>shuffle请求的文件块大小 超过这个参数值，就会被强行落盘，防止一大堆并发请求把内存占满。（默认Long.MaxValue）
>**spark.shuffle.compress**
>是否压缩map输出文件，默认压缩 true
>**spark.shuffle.spill.compress**
>shuffle过程中溢出的文件是否压缩，默认true，使用`spark.io.compression.codec压缩。`
>**spark.shuffle.file.buffer**
>在内存输出流中 每个shuffle文件占用内存大小，适当提高 可以减少磁盘读写 io次数，初始值为32k
>**spark.shuffle.memoryFraction**
>该参数代表了Executor内存中，分配给shuffle read task进行聚合操作的内存比例，默认是20%。
>cache少且内存充足时，可以调大该参数，给shuffle read的聚合操作更多内存，以避免由于内存不足导致聚合过程中频繁读写磁盘。
>**spark.shuffle.manager**
>当ShuffleManager为SortShuffleManager时，如果shuffle read task的数量小于这个阈值（默认是200），则shuffle write过程中不会进行排序操作，而是直接按照未经优化的HashShuffleManager的方式去写数据，但是最后会将每个task产生的所有临时磁盘文件都合并成一个文件，并会创建单独的索引文件。
>当使用SortShuffleManager时，如果的确不需要排序操作，那么建议将这个参数调大一些，大于shuffle read task的数量。那么此时就会自动启用bypass机制，map-side就不会进行排序了，减少了排序的性能开销。但是这种方式下，依然会产生大量的磁盘文件，因此shuffle write性能有待提高。
>**spark.shuffle.consolidateFiles**
>如果使用HashShuffleManager，该参数有效。如果设置为true，那么就会开启consolidate机制，会大幅度合并shuffle write的输出文件，对于shuffle read task数量特别多的情况下，这种方法可以极大地减少磁盘IO开销，提升性能。
>如果的确不需要SortShuffleManager的排序机制，那么除了使用bypass机制，还可以尝试将spark.shuffle.manager参数手动指定为hash，使用HashShuffleManager，同时开启consolidate机制。
>**spark.shuffle.io.maxRetries**
>shuffle read task从shuffle write task所在节点拉取属于自己的数据时，如果因为网络异常导致拉取失败，是会自动进行重试的。该参数就代表了可以重试的最大次数。如果在指定次数之内拉取还是没有成功，就可能会导致作业执行失败。
>对于那些包含了特别耗时的shuffle操作的作业，建议增加重试最大次数（比如60次），以避免由于JVM的full gc或者网络不稳定等因素导致的数据拉取失败。在实践中发现，对于针对超大数据量（数十亿~上百亿）的shuffle过程，调节该参数可以大幅度提升稳定性。
>**spark.shuffle.io.retryWait**
>同上，默认5s，建议加大间隔时长（比如60s），以增加shuffle操作的稳定性。
>**spark.io.encryption.enabled + spark.io.encryption.keySizeBits + spark.io.encryption.keygen.algorithm**
>io加密，默认关闭

# Spark shuffle vs MR shuffle

[Spark的Shuffle和MR的Shuffle异同](https://zhuanlan.zhihu.com/p/136466667)

