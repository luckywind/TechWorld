

[[Distributed Systems Architecture](https://0x0fff.com/)](https://0x0fff.com/spark-architecture-shuffle/)

[shuffle](https://gousios.gr/courses/bigdata/spark.html)

相同key的数据一定在一个节点上，内存放不下就存到磁盘

![image-20210719100823183](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210719100823183.png)

# Spark架构:shuffle

相同key的数据在一个节点上，本文遵循MR的命名公约：在shuffle操作中，源头executor上产生数据的task称为mapper，消费数据到目标executor的task称为reducer，两者之间是shuffle。

shuffle通常有两个重要的参数：

1.  ***spark.shuffle.compress\*** –是否对输出进行压缩，默认true
2. ***spark.shuffle.spill.compress\*** –是否压缩shuffle过程中spill的中间文件

默认值都是true，都会使用***spark.io.compression.codec\***进行压缩，默认是snappy.

spark中有多种shuffle实现，通过参数***spark.shuffle.manager\*** 指定，可选值有hash、sort和tungsten-sort. 在spark1.2.0之前，sort是默认的shuffle实现。

前一个stage 的 ShuffleMapTask 进行 shuffle write， 把数据存储在 blockManager 上面， 并且把数据位置元信息上报到 <font color=red>driver</font> 的 mapOutTrack 组件中， 下一个 stage 根据数据位置元信息， 进行 shuffle read， 拉取上个stage 的输出数据。

## Spark shuffle 发展史

Spark 0.8及以前 Hash Based Shuffle
Spark 0.8.1 为Hash Based Shuffle引入File Consolidation机制
Spark 0.9 引入ExternalAppendOnlyMap
Spark 1.1 引入Sort Based Shuffle，但默认仍为Hash Based Shuffle
Spark 1.2 默认的Shuffle方式改为Sort Based Shuffle
Spark 1.4 引入Tungsten-Sort Based Shuffle
Spark 1.6 Tungsten-sort并入Sort Based Shuffle
Spark 2.0 Hash Based Shuffle退出历史舞台

# hash shuffle

<font color=red> 输出文件太多: mapper个数*reducer个数:     下一个stage有多少个task,  当前每个shuffle write task就要写多少个文件</font>

​       spark1.2.0之前默认使用的shuffle算法，存在很多缺点，因为该实现创建的文件太多： 一个mapper task会为每个reducer创建一个不同的文件，当有M个mapper，R个reducer时，就会产生M*R个文件。系统的open files个数以及创建/删除这些文件的速度都会因为文件数太多而出现问题。

​        逻辑非常愚蠢：把reducer的个数作为reduce端的分区数，并创建相应个数的文件，然后迭代数据计算每个记录对应的partition并输出到对应的文件<font color=red>每个文件对应一个缓冲区，这个缓冲区也称为一个bucket</font>。看起来就像下图：

![spark_hash_shuffle_no_consolidation](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/spark_hash_shuffle_no_consolidation-1024x484.png)

有一种优化版的实现，通过参数spark.shuffle.consolidateFiles(默认false)控制。当启用时，mapper输出文件的个数是固定的，例如集群有E个executor，且每个executor有C个core，每个task申请T个CPU(spark.task.cpus),那么集群execution slots的个数是`E*C/T`, 则原本，shuffle过程中创建的文件个数是`E*C/T*R`，现在只需要`C/T*R`个文件，<font color=blue>因为每个Executor都会复用这一组文件。</font>

实现原理：创建一个输出文件池，而不是给每个reducer创建一个文件，当map task开始输出数据，它会从文件池中申请R个文件。当执行结束，它会返回这R个文件到文件池。因为每个executor只能并行执行C/T个Task,它只会创建C/T组输出文件，每组是R个文件。当第一组C/T个并行map task结束，下一组map task可以从文件池中重用这些文件组。看起来如下：

<font color=red>文件数：最大并行的mapper数*reducer数</font>

![spark_hash_shuffle_with_consolidation](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/spark_hash_shuffle_with_consolidation-1024x500.png)

Pros:

1. Fast – no sorting is required at all, no hash table maintained; 无需排序，不需要维护hash表
2. No memory overhead for sorting the data;  无需额外内存来排序
3. No IO overhead – data is written to HDD exactly once and read exactly once. 无需多次IO

Cons:

1. 分区数比较大时，由于文件数过多导致性能下降
2. 大量文件写入导致IO倾斜导致随机IO,比顺序IO慢100倍。

# Sort Shuffle

## shuffle writer

<font color=red>不管下游stage有多少个shuffle read task(下游stage的ShuffleMapTask/ResultTask), 当前每个shuffle write task 最后只输出一个文件(非bypass)，该文件按照partitionId和key进行排序。  另外生成一个索引文件对该大文件进行索引，下游stage的每个shuffle read task只读取自己要处理的那部分数据就行。</font>



### 框架

以下是当前Spark支持的一个shuffle操作对shuffle框架的需求：

![image-20221222215810529](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221222215810529.png)

>  可以发现目前的shuffle算子都不支持在shuffle write端排序，但是当前的框架设计的是支持wirte端排序的，可能未来会有这样的算子出现。

<font color=red>**Shuffle Write框架需要执行的3个步骤是** :  **聚合**-> **排序** ->**分区**</font>

![image-20221223102014783](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221223102014783.png)

**分区数小且不需要聚合与排序**： 

​		直接采用 BypassMergeSortShuffleWriter

> 例如groupByKey(100),  partitionBy(100),  sortByKey(100)
>
> **map输出好<K,V>对并计算好分区ID，Spark根据分区ID把数据分发到不同的buffer中再溢写到文件**

![image-20221222231510544](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221222231510544.png)

**分区数大**：

​		<font color=red>需要排序</font>：基于Array的SortShuffleWriter按照**PartitionId+Key**进行排序

>   Array: 具体为PartitionedPairBuffer<(PID,K), V>按照PartitionId+Key排序后输出到一个文件就OK 
>
>  目前Spark还没有这样的操作，但是不排除未来会提供/用户自定义

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221222233556173.png" alt="image-20221222233556173" style="zoom:50%;" />

​       <font color=red>不需要聚合与排序</font>:基于Array的SortShuffleWriter按照**PartitionId**进行排序 

>   只按照PartitionId排序后输出到一个文件就OK        

<font color=red>需要map端聚合</font>：基于HashMap的SortShuffleWriter按照PartitionId(Key)进行排序

> 只需要把Array结构换成支持聚合与排序的PartitionAppendOnlyMap 
>
> HashMap中的Key是 **partitionId+Key**,  聚合后，如果需要排序 ，则按照PartitionId+Key排序，如果不需要排序，则只按照PartitionId排序; 最终输出到一个文件中。   在实现中，Spark使用一个同时支持聚合和排序的数据结构PartitonedAppendOnlyMap,相当于HashMap和Array的合体。
> ![image-20221222224735310](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221222224735310.png)

### **SortShuffleWriter**

​             从spark1.2.0开始，默认的shuffle算法改成sort了。在该模式下，数据会先写入一个内存数据结构中，此时根据不同的shuffle算子，可能选用不同的数据结构。如果是reduceByKey这种聚合类的shuffle算子，那么会选用Map数据结构，一边通过Map进行聚合，一边写入内存；如果是join这种普通的shuffle算子，那么会选用Array数据结构，直接写入内存。**这个shuffle逻辑类似MR**，只输出一个文件，该文件中的记录首先是按照 Partition Id 排序，每个 Partition 内部再按照 Key 进行排序，Map Task 运行期间会顺序写每个 Partition 的数据，**同时生成一个索引文件**记录每个 Partition 的大小和偏移量,该文件按照reducer_id有序且可索引。这样在fread之前做一个fseek就很方便找到某个reducer相关的数据块。总体上看来 **Sort Shuffle 解决了 Hash Shuffle 的所有弊端**，

**一个task将所有数据写入内存数据结构的过程中，会发生多次磁盘溢写操作，也会产生多个临时文件。最后会将之前所有的临时磁盘文件都进行合并**，由于一个task就只对应一个磁盘文件因此还会单独写一份索引文件，其中标识了下游各个task的数据在文件中的start offset与end offset。
SortShuffleManager由于有一个磁盘文件merge的过程，因此大大减少了文件数量，<font color=red>由于每个task最终只有一个磁盘文件所以文件个数等于上游shuffle write个数。</font>[参考](https://www.cnblogs.com/xiaodf/p/10650921.html)

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221024144015305.png" alt="image-20221024144015305" style="zoom:50%;" />

​          有意思的是这个实现在map端排序，但当需要数据顺序时，在reduce端不对排序结果进行merge，而只是re-sort(reduce端re-sort使用TimSort算法实现，它对预排序的数据非常高效)。

​          如果没有足够内存放下map的输出，则需要把中间结果spill到磁盘。spark.shuffle.spill可以控制是否开启，如果不开启当内存放不下时会OOM。

**可用于排序map输出的内存大小**=`spark.shuffle.memoryFraction * spark.shuffle.safetyFraction`，

默认是`JVM堆*0.2*0.8`即`JVM堆*0.16`。 注意，如果在一个Executor上运行多个线程(设置`spark.executor.cores / spark.task.cpus`>1),则每个map task可用JVM堆大小要做相应除法。

​		spill到磁盘的文件只有在reducer请求时才会时时merge。

总结：**每个Map任务最后只会输出两个文件（一个是索引文件会记录每个分区的偏移量）中间过程采用归并排序,输出完成后，Reducer会根据索引文件得到属于自己的分区。**

![spark_sort_shuffle](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/spark_sort_shuffle-1024x459.png)

Pros:

1. Smaller amount of files created on “map” side
2. Smaller amount of random IO operations, mostly sequential writes and reads

Cons:

1. Sorting is slower than hashing. It might worth tuning the bypassMergeThreshold parameter for your own cluster to find a sweet spot, but in general for most of the clusters it is even too high with its default
2. In case you use SSD drives for the temporary data of Spark shuffles, hash shuffle might work better for you

### **BypassMergeSortShuffleWriter**

但是因为需要其 Shuffle 过程需要对记录进行排序，所以**在性能上有所损失**。.当然，当reducer个数很小时，hash到多个文件是比sorting到一个文件快的，因此，sort shuffle有一个回退计划：**当reducer个数小于spark.shuffle.sort.bypassMergeThreshold(默认200)个且非reduceBykey类的聚合shuffle 算子，启用回退计划，hash到多个文件然后再join到一个文件**。该实现的源码参考BypassMergeSortShuffleWriter类。

> 和Hash Shuffle中的HashShuffleWriter实现基本一致，唯一的区别在于**，map端的多个输出文件会被汇总为一个文件**

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/852983-20190510151151027-712712994.jpg" alt="img" style="zoom:50%;" />

**此时task会为每个reduce端的task都创建一个临时磁盘文件**，并将数据按key进行hash然后根据key的hash值，将key写入对应的磁盘文件之中。当然，写入磁盘文件时也是先写入内存缓冲，缓冲写满之后再溢写到磁盘文件的。最后，同样会将所有临时磁盘文件都合并成一个磁盘文件，并创建一个单独的索引文件。

**该过程的磁盘写机制其实跟未经优化的HashShuffleManager是一模一样的，因为都要创建数量惊人的磁盘文件，只是在最后会做一个磁盘文件的合并而已。因此少量的最终磁盘文件，也让该机制相对未经优化的HashShuffleManager来说，shuffle read的性能会更好。**

而该机制与普通SortShuffleManager运行机制的不同在于：
第一，磁盘写机制不同;
第二，不会进行排序。也就是说，启用该机制的最大好处在于，shuffle write过程中，不需要进行数据的排序操作，也就节省掉了这部分的性能开销。





### 源码解析

[参考](https://www.cnblogs.com/johnny666888/p/11291592.html)

## shuffle read

### 框架

shuffle read数据操作需要3个功能: <font color=red>**跨节点数据获取、聚合、排序**, 可见Shuffle Read是不需要分区的，所以比Shuffle write要简单</font>。

为了支持所有的shuffle算子，Spark也设计了一个通用的Shuffle Read框架。框架的计算顺序就是 数据获取->聚合->排序。
![image-20221222223128683](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221222223128683.png)

1. 无需聚合与排序
   拉取的数据直接放到buffer中， 下游直接从buffer中取即可。   例如partitionBy()

2. 需要排序

   拉取的数据会放到Array结构里，按照Key进行排序，内存不足会spill。  例如sortByKey()、sortBy()

3. 需要聚合

   - 仍然使用HashMap对数据按照Key聚合，value就是当前key的聚合结果。

     > 这里也是用了一个特殊优化的HashMap，ExternalAppendOnlyMap同时支持聚合和排序
     >
     > reduceByKey \ aggretateByKey, foldByKey, distinct等

   - 如果需要按照Key进行排序，则拷贝到一个Array中进行排序

     

### 几个细节

- **在什么时候 fetch？**当 parent stage 的所有 ShuffleMapTasks 结束后再 fetch。理论上讲，一个 ShuffleMapTask 结束后就可以 fetch，但是为了迎合 stage 的概念（即一个 stage 如果其 parent stages 没有执行完，自己是不能被提交执行的），还是选择全部 ShuffleMapTasks 执行完再去 fetch。因为 fetch 来的 FileSegments 要先在内存做缓冲，所以一次 fetch 的 FileSegments 总大小不能太大。Spark 规定这个缓冲界限不能超过 `spark.reducer.maxMbInFlight`，这里用 **softBuffer** 表示，默认大小为 48MB。一个 softBuffer 里面一般包含多个 FileSegment，但如果某个 FileSegment 特别大的话，这一个就可以填满甚至超过 softBuffer 的界限。

- **边 fetch 边处理还是一次性 fetch 完再处理？**边 fetch 边处理。本质上，MapReduce shuffle 阶段就是边 fetch 边使用 combine() 进行处理，只是 combine() 处理的是部分数据。MapReduce 为了让进入 reduce() 的 records 有序，必须等到全部数据都 shuffle-sort 后再开始 reduce()。因为 Spark 不要求 shuffle 后的数据全局有序，因此没必要等到全部数据 shuffle 完成后再处理。**那么如何实现边 shuffle 边处理，而且流入的 records 是无序的？**答案是使用可以 aggregate 的数据结构，比如 HashMap。每 shuffle 得到（从缓冲的 FileSegment 中 deserialize 出来）一个 \<Key, Value\> record，直接将其放进 HashMap 里面。如果该 HashMap 已经存在相应的 Key，那么直接进行 aggregate 也就是 `func(hashMap.get(Key), Value)`，比如上面 WordCount 例子中的 func 就是 `hashMap.get(Key) ＋ Value`，并将 func 的结果重新 put(key) 到 HashMap 中去。这个 func 功能上相当于 reduce()，但实际处理数据的方式与 MapReduce reduce() 有差别，差别相当于下面两段程序的差别。

  ```java
  // MapReduce
  reduce(K key, Iterable<V> values) { 
  	result = process(key, values)
  	return result	
  }
  
  // Spark
  reduce(K key, Iterable<V> values) {
  	result = null 
  	for (V value : values) 
  		result  = func(result, value)
  	return result
  }
  ```

  MapReduce 可以在 process 函数里面可以定义任何数据结构，也可以将部分或全部的 values 都 cache 后再进行处理，非常灵活。而 Spark 中的 func 的输入参数是固定的，一个是上一个 record 的处理结果，另一个是当前读入的 record，它们经过 func 处理后的结果被下一个 record 处理时使用。因此一些算法比如求平均数，在 process 里面很好实现，直接`sum(values)/values.length`，而在 Spark 中 func 可以实现`sum(values)`，但不好实现`/values.length`。更多的 func 将会在下面的章节细致分析。

- **fetch 来的数据存放到哪里？**刚 fetch 来的 FileSegment 存放在 softBuffer 缓冲区，经过处理后的数据放在内存 + 磁盘上。这里我们主要讨论处理后的数据，可以灵活设置这些数据是“只用内存”还是“内存＋磁盘”。如果`spark.shuffle.spill = false`就只用内存。内存使用的是`AppendOnlyMap` ，类似 Java 的`HashMap`，内存＋磁盘使用的是`ExternalAppendOnlyMap`，如果内存空间不足时，`ExternalAppendOnlyMap`可以将 \<K, V\> records 进行 sort 后 spill 到磁盘上，等到需要它们的时候再进行归并，后面会详解。**使用“内存＋磁盘”的一个主要问题就是如何在两者之间取得平衡？**在 Hadoop MapReduce 中，默认将 reducer 的 70% 的内存空间用于存放 shuffle 来的数据，等到这个空间利用率达到 66% 的时候就开始 merge-combine()-spill。在 Spark 中，也适用同样的策略，一旦 ExternalAppendOnlyMap 达到一个阈值就开始 spill，具体细节下面会讨论。

- **怎么获得要 fetch 的数据的存放位置？**在上一章讨论物理执行图中的 stage 划分的时候，我们强调 “一个 ShuffleMapStage 形成后，会将该 stage 最后一个 final RDD 注册到 `MapOutputTrackerMaster.registerShuffle(shuffleId, rdd.partitions.size)`，这一步很重要，因为 shuffle 过程需要 MapOutputTrackerMaster 来指示 ShuffleMapTask 输出数据的位置”。因此，reducer 在 shuffle 的时候是要去 driver 里面的 MapOutputTrackerMaster 询问 ShuffleMapTask 输出的数据位置的。每个 ShuffleMapTask 完成时会将 FileSegment 的存储位置信息汇报给 MapOutputTrackerMaster。



# Unsafe Shuffle or Tungsten Sort(钨丝计划)

spark1.5开始，Spark 开始了钨丝计划（Tungsten），目的是优化内存和CPU的使用，进一步提升spark的性能。由于使用了堆外内存，而它基于 JDK Sun Unsafe API，故 Tungsten-Sort Based Shuffle 也被称为 Unsafe Shuffle。

可以通过`spark.shuffle.manager= tungsten-sort/sort` 开启(在spark2.0后是唯一的选项了), 这是Tungsten[项目的一部分](https://issues.apache.org/jira/browse/SPARK-7081)。优化可以总结如下：

1. 无需反序列化，直接操作序列化后的二进制数据。它使用unsafe内存拷贝函数直接拷贝数据，操作字节数组非常高效。
2. 使用特殊的cache-efficient排序器[ShuffleExternalSorter](https://github.com/apache/spark/blob/master/core/src/main/java/org/apache/spark/shuffle/sort/ShuffleExternalSorter.java) ，直接排序压缩数组的指针和分区id
3. **因为数据没有反序列化，所以spill过程非常快**
4. 当压缩编码支持序列化流的连接(不同spill输出的合并只是简单的concat)，则自动应用spill-merge优化



这种shuffle实现必须要满足如下条件：

1. 没有聚合，聚合意味着需要存储反序列化的值来聚合新值，这样就丢了最大的优势了
2. shuffle序列化器支持序列化值的重定向(当前KryoSerializer and Spark SQL’s custom serializer是支持的)
3. shuffle产生的分区数不超过16,777,216
4. 记录序列化后不能超过128M

但这种方式无法利用mapper端的预排序优势，且貌似不稳定。但是使用 Tungsten-Sort Based Shuffle 有几个限制，Shuffle 阶段不能有 aggregate 操作，分区数不能超过一定大小（2^24-1，这是可编码的最大 Parition Id），所以像 reduceByKey 这类有 aggregate 操作的算子是不能使用 Tungsten-Sort Based Shuffle，它会退化采用 Sort Shuffle。图示如下：

![spark_tungsten_sort_shuffle](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/spark_tungsten_sort_shuffle-1024x457.png)

对每个spill数据，先对指针数组排序输出一个索引的分区文件，然后再合并成一个大的索引文件

## 源码解析

[参考](https://www.cnblogs.com/johnny666888/p/11291546.html)

ShuffleExternalSorter将数据不断溢出到溢出小文件中，溢出文件内的数据是按分区规则排序的，分区内的数据是乱序的。

多个分区的数据同时溢出到一个溢出文件，最后使用三种归并方式中的一种将多个溢出文件归并到一个文件，分区内的数据是乱序的。最终数据的格式跟第一种shuffle写操作的结果是一样的，即有分区的shuffle数据文件和记录分区大小的shuffle索引文件。

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
>默认48m。设置`shuffle read`任务的buff缓冲区大小，每个reduce都会在完成任务后，需要一个堆外内存的缓冲区来存放结果，如果没有充裕的内存就尽可能把这个调小一点。。相反，堆外内存充裕，调大些就能节省gc时间。
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
><font color=red>在内存输出流中 每个shuffle文件占用内存大小，适当提高 可以减少磁盘读写 io次数，初始值为32k</font>
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





### Shuffle Behavior

| Property Name                               | Default      | Meaning                                                      |
| :------------------------------------------ | :----------- | :----------------------------------------------------------- |
| `spark.reducer.maxSizeInFlight`             | 48m          | Maximum size of map outputs to fetch simultaneously from each reduce task. Since each output requires us to create a buffer to receive it, this represents a fixed memory overhead per reduce task, so keep it small unless you have a large amount of memory. |
| `spark.reducer.maxReqsInFlight`             | Int.MaxValue | This configuration limits the number of remote requests to fetch blocks at any given point. When the number of hosts in the cluster increase, it might lead to very large number of in-bound connections to one or more nodes, causing the workers to fail under load. By allowing it to limit the number of fetch requests, this scenario can be mitigated. |
| `spark.shuffle.compress`                    | true         | Whether to compress map output files. Generally a good idea. Compression will use `spark.io.compression.codec`. |
| `spark.shuffle.file.buffer`                 | 32k          | Size of the in-memory buffer for each shuffle file output stream. These buffers reduce the number of disk seeks and system calls made in creating intermediate shuffle files. |
| `spark.shuffle.io.maxRetries`               | 3            | (Netty only) Fetches that fail due to IO-related exceptions are automatically retried if this is set to a non-zero value. This retry logic helps stabilize large shuffles in the face of long GC pauses or transient network connectivity issues. |
| `spark.shuffle.io.numConnectionsPerPeer`    | 1            | (Netty only) Connections between hosts are reused in order to reduce connection buildup for large clusters. For clusters with many hard disks and few hosts, this may result in insufficient concurrency to saturate all disks, and so users may consider increasing this value. |
| `spark.shuffle.io.preferDirectBufs`         | true         | (Netty only) Off-heap buffers are used to reduce garbage collection during shuffle and cache block transfer. For environments where off-heap memory is tightly limited, users may wish to turn this off to force all allocations from Netty to be on-heap. |
| `spark.shuffle.io.retryWait`                | 5s           | (Netty only) How long to wait between retries of fetches. The maximum delay caused by retrying is 15 seconds by default, calculated as `maxRetries * retryWait`. |
| `spark.shuffle.service.enabled`             | false        | Enables the external shuffle service. This service preserves the shuffle files written by executors so the executors can be safely removed. This must be enabled if `spark.dynamicAllocation.enabled` is "true". The external shuffle service must be set up in order to enable it. See [dynamic allocation configuration and setup documentation](https://spark.apache.org/docs/2.1.3/job-scheduling.html#configuration-and-setup) for more information. |
| `spark.shuffle.service.port`                | 7337         | Port on which the external shuffle service will run.         |
| `spark.shuffle.service.index.cache.entries` | 1024         | Max number of entries to keep in the index cache of the shuffle service. |
| `spark.shuffle.sort.bypassMergeThreshold`   | 200          | (Advanced) In the sort-based shuffle manager, avoid merge-sorting data if there is no map-side aggregation and there are at most this many reduce partitions. |
| `spark.shuffle.spill.compress`              | true         | Whether to compress data spilled during shuffles. Compression will use `spark.io.compression.codec`. |
| `spark.io.encryption.enabled`               | false        | Enable IO encryption. Currently supported by all modes except Mesos. It's recommended that RPC encryption be enabled when using this feature. |
| `spark.io.encryption.keySizeBits`           | 128          | IO encryption key size in bits. Supported values are 128, 192 and 256. |
| `spark.io.encryption.keygen.algorithm`      | HmacSHA1     | The algorithm to use when generating the IO encryption key. The supported algorithms are described in the KeyGenerator section of the Java Cryptography Architecture Standard Algorithm Name Documentation. |

# Spark shuffle vs MR shuffle

[Spark的Shuffle和MR的Shuffle异同](https://zhuanlan.zhihu.com/p/136466667)

[Spark/MR Shuffle 对比](https://aaaaaaron.github.io/2018/10/24/Spark-Shuffle/)

Reduce 去拖 map 的输出数据，Spark 提供了两套不同的拉取数据框架：通过 socket 连接去取数据；使用 netty 框架去取数据。

每个节点的 Executor 会创建一个 BlockManager，其中会创建一个 BlockManagerWorker 用于响应请求。当 reduce 的 GET_BLOCK 的请求过来时，读取本地文件将这个 blockId 的数据返回给 reduce。如果使用的是 Netty 框架，BlockManager 会创建 ShuffleSender 用于发送 shuffle 数据。



Reduce 拖过来数据之后以什么方式存储呢？Spark map 输出的数据没有经过排序，spark shuffle 过来的数据也不会进行排序，spark 认为 shuffle 过程中的排序不是必须的，并不是所有类型的 reduce 需要的数据都需要排序，强制地进行排序只会增加 shuffle 的负担。Reduce 拖过来的数据会放在一个 HashMap 中，HashMap 中存储的也是 <key, value> 对，key 是 map 输出的 key，map 输出对应这个 key 的所有 value 组成 HashMap 的 value。Spark 将 shuffle 取过来的每一个 <key, value> 对插入或者更新到 HashMap 中，来一个处理一个。HashMap 全部放在内存中。

Shuffle 取过来的数据全部存放在内存中，对于数据量比较小或者已经在 map 端做过合并处理的 shuffle 数据，占用内存空间不会太大，但是对于比如 group by key 这样的操作，reduce 需要得到 key 对应的所有 value，并将这些 value 组一个数组放在内存中，这样当数据量较大时，就需要较多内存。

当内存不够时，要不就失败，要不就用老办法把内存中的数据移到磁盘上放着。Spark 意识到在处理数据规模远远大于内存空间时所带来的不足，引入了一个具有外部排序的方案。Shuffle 过来的数据先放在内存中，当内存中存储的 <key, value> 对超过 1000 并且内存使用超过 70% 时，判断节点上可用内存如果还足够，则把内存缓冲区大小翻倍，如果可用内存不再够了，则把内存中的 <key, value> 对排序然后写到磁盘文件中。最后把内存缓冲区中的数据排序之后和那些磁盘文件组成一个最小堆，每次从最小堆中读取最小的数据，这个和 MapReduce 中的 merge 过程类似。

|                         |                                                     |                                                              |
| :---------------------- | :-------------------------------------------------- | ------------------------------------------------------------ |
|                         | MapReduce                                           | Spark                                                        |
| collect                 | 在内存中构造了一块数据结构用于 map 输出的缓冲       | 没有在内存中构造一块数据结构用于 map 输出的缓冲，而是直接把输出写到磁盘文件 |
| sort                    | map 输出的数据有排序                                | map 输出的数据没有排序                                       |
| merge                   | 对磁盘上的多个 spill 文件最后进行合并成一个输出文件 | 在 map 端没有 merge 过程，在输出时直接是对应一个 reduce 的数据写到一个文件中，这些文件同时存在并发写，最后不需要合并成一个 |
| copy 框架               | jetty                                               | netty 或者直接 socket 流                                     |
| 对于本节点上的文件      | 仍然是通过网络框架拖取数据                          | 不通过网络框架，对于在本节点上的 map 输出文件，采用本地读取的方式 |
| copy 过来的数据存放位置 | 先放在内存，内存放不下时写到磁盘                    | 一种方式全部放在内存；另一种方式先放在内存                   |
| merge sort              | 最后会对磁盘文件和内存中的数据进行合并排序          | 对于采用另一种方式时也会有合并排序的过程                     |





## MR的shuffle过程

Map方法之后Reduce方法之前这段**系统执行排序的过程**（将map输出作为输入传给reducer）叫Shuffle，MapReduce确保每个reducer的输入都是按键排序的。

hadoop shuffle过程：

Map方法之后，数据首先进入到分区方法（getPartition），把数据标记好分区，然后把数据发送到环形缓冲区；环形缓冲区默认大小100m，环形缓冲区达到80%时，进行**溢写**；溢写前对数据进行排序，排序按照对key的索引进行字典顺序排序，**排序的手段快排**；溢写产生大量溢写文件，需要对溢写文件进行**归并排序**；对溢写的文件也可以进行Combiner操作，前提是汇总操作，求平均值不行。最后将文件按照分区存储到磁盘，**等待Reduce端拉取**。

每个Reduce拉取Map端对应分区的数据。拉取数据后先存储到内存中，内存不够了，再存储到磁盘。拉取完所有数据后，采用**归并排序将内存和磁盘中的数据都进行排**序。在进入Reduce方法前，可以对数据进行分组操作。

**总结**：Hadoop Shuffle过程可以划分为：map()，spill，merge，shuffle，sort，reduce()等，是按照流程顺次执行的，属于Push类型。

Shuffle中的缓冲区大小会影响到mapreduce程序的执行效率，原则上说，缓冲区越大，磁盘io的次数越少，执行速度就越快。

![preview](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/v2-7922486f9a5b271abe91e63f17cf3ca3_r.jpg)

