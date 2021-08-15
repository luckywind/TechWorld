参考[Spark源码解读之Shuffle原理剖析与源码分析](https://blog.csdn.net/qq_37142346/article/details/81875249)

[华为云-shuffle原理](https://www.cnblogs.com/weiyiming007/p/11244192.html)

从Spark-2.0.0开始，Spark 把 Hash Shuffle 移除，可以说目前 Spark-2.0 中只有一种 Shuffle，即为 Sort Shuffle。本文详细分析SortShuffleManager的实现原理

# SortShuffleManager

记录按照目标分区id排序，然后写入单个map输出文件，Reducer读取自己的一个连续片段。当输出文件太大时，会spill到磁盘文件，这些磁盘文件最终合并输出一个输出文件。

两种写map 输出文件的方式：

1.  序列化排序(钨丝计划)：符合以下三个条件：

   - Shuffle 依赖没有指定聚合或者排序
   - shuffle序列化器支持序列化值的重新定位(目前由KryoSerializer和Spark SQL的自定义序列化器支持)。
   - shuffle产生的输出分区少于16777216。
2.  非序列化排序:用于处理所有其他情况。

序列化排序模式下，传入的记录在传递给shuffle writer时就被序列化，并且在排序过程中以序列化的形式缓冲。实现了一些优化：

- 它的排序作用于序列化的二进制数据而不是Java对象，这减少了内存消耗和GC开销。这种优化要求记录序列化程序具有一定的属性，允许序列化记录在不需要反序列化的情况下重新排序
- 它使用一种特殊的缓存高效排序器([[ShuffleExternalSorter]])，它可以对压缩的记录指针和分区id进行排序。通过在排序数组中只使用8个字节的空间，这更适合于数组的缓存
- spill合并过程运行在属于同一分区的序列化记录块上，在合并过程中不需要对记录进行反序列化。
- 高校数据复制：当spill压缩codec支持压缩数据的连接时，spill合并简单地连接序列化和压缩的溢出分区，以产生最终的输出分区

## shuffle write

### 是否启用Bypass机制？

```scala
private[spark] object SortShuffleWriter {
  def shouldBypassMergeSort(conf: SparkConf, dep: ShuffleDependency[_, _, _]): Boolean = {
    // We cannot bypass sorting if we need to do map-side aggregation.
    if (dep.mapSideCombine) {
      require(dep.aggregator.isDefined, "Map-side combine without Aggregator specified!")
      false
    } else {
      val bypassMergeThreshold: Int = conf.getInt("spark.shuffle.sort.bypassMergeThreshold", 200)
      dep.partitioner.numPartitions <= bypassMergeThreshold
    }
  }
}
```

![image-20210815222029237](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210815222029237.png)

### 是否可用钨丝计划？

```scala
 def canUseSerializedShuffle(dependency: ShuffleDependency[_, _, _]): Boolean = {
    val shufId = dependency.shuffleId
    val numPartitions = dependency.partitioner.numPartitions
    if (!dependency.serializer.supportsRelocationOfSerializedObjects) {
      //1. 序列化器不支持序列化对象重定向（重排序列化字节等价于序列化前重排序）
      log.debug(s"Can't use serialized shuffle for shuffle $shufId because the serializer, " +
        s"${dependency.serializer.getClass.getName}, does not support object relocation")
      false
    } else if (dependency.aggregator.isDefined) {
      //2. 没定义聚合算子
      log.debug(
        s"Can't use serialized shuffle for shuffle $shufId because an aggregator is defined")
      false
    } else if (numPartitions > MAX_SHUFFLE_OUTPUT_PARTITIONS_FOR_SERIALIZED_MODE) {
      //3. 输出分区数超过1600万
      log.debug(s"Can't use serialized shuffle for shuffle $shufId because it has more than " +
        s"$MAX_SHUFFLE_OUTPUT_PARTITIONS_FOR_SERIALIZED_MODE partitions")
      false
    } else {
      log.debug(s"Can use serialized shuffle for shuffle $shufId")
      true
    }
```



### Shuffle处理器选择逻辑

这个逻辑会用到上面Bypass机制的启用以及钨丝计划的启用

```scala
 override def registerShuffle[K, V, C](
      shuffleId: Int,
      numMaps: Int,
      dependency: ShuffleDependency[K, V, C]): ShuffleHandle = {
    if (SortShuffleWriter.shouldBypassMergeSort(conf, dependency)) {
      /** 如果有少于 spark.shuffle.sort.bypassMergeThreshold分区数，我们不需要map端的聚合，然后直接编写numPartitions文件，
        * 并在最后将它们连接起来。这避免了序列化和反序列化两次，以合并溢出的文件
        * 缺点是在一个时间内打开多个文件，从而分配更多的内存到缓冲区。
        * */
      new BypassMergeSortShuffleHandle[K, V](
        shuffleId, numMaps, dependency.asInstanceOf[ShuffleDependency[K, V, V]])
    } else if (SortShuffleManager.canUseSerializedShuffle(dependency)) {
      // Otherwise, try to buffer map outputs in a serialized form, since this is more efficient: UnsafeShuffleWriter
      //  
      new SerializedShuffleHandle[K, V](
        shuffleId, numMaps, dependency.asInstanceOf[ShuffleDependency[K, V, V]])
    } else {
      // Otherwise, buffer map outputs in a deserialized form:
      // 否则，缓冲映射输出以反序列化形式输出: 其实就会使用基础SortShuffleWriter
      new BaseShuffleHandle(shuffleId, numMaps, dependency)
    }
  }
```

![image-20210815232712184](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210815232712184.png)

### 获取writer

这里就很简单了，根据不同的shuffle处理器创建不同的writer：

```scala
override def getWriter[K, V](
      handle: ShuffleHandle,
      mapId: Int,
      context: TaskContext): ShuffleWriter[K, V] = {
    numMapsForShuffle.putIfAbsent(
      handle.shuffleId, handle.asInstanceOf[BaseShuffleHandle[_, _, _]].numMaps)
    val env = SparkEnv.get
    handle match {
      case unsafeShuffleHandle: SerializedShuffleHandle[K @unchecked, V @unchecked] =>
        new UnsafeShuffleWriter(
          env.blockManager,
          shuffleBlockResolver.asInstanceOf[IndexShuffleBlockResolver],
          context.taskMemoryManager(),
          unsafeShuffleHandle,
          mapId,
          context,
          env.conf)
      case bypassMergeSortHandle: BypassMergeSortShuffleHandle[K @unchecked, V @unchecked] =>
        new BypassMergeSortShuffleWriter(
          env.blockManager,
          shuffleBlockResolver.asInstanceOf[IndexShuffleBlockResolver],
          bypassMergeSortHandle,
          mapId,
          context,
          env.conf)
      case other: BaseShuffleHandle[K @unchecked, V @unchecked, _] =>
        new SortShuffleWriter(shuffleBlockResolver, other, mapId, context)
    }
```



## Shuffle Read

对于每个stage来说，它的上边界，要么从外部存储读取数据，要么读取上一个stage的输出。而下边界要么是通过ShuffleMapTask写入到本地文件系统，要么就是最后一个stage,写出结果文件。这里的stage在运行时就可以以流水线的方式进行运行一组Task，除了最后一个stage对应的ResultTask，其余的stage全部对应的shuffle Map Task。

　　除了需要从外部存储读取数据和RDD已经做过cache或者checkPoint的Task。一般的Task都是从Shuffle RDD的ShuffleRead开始的

### 获取Shuffle阅读器

ShuffledRDD:  对接的是ShuffleManager，但当前只有SortShuffleManager了 😢

```scala
  override def compute(split: Partition, context: TaskContext): Iterator[(K, C)] = {
    val dep = dependencies.head.asInstanceOf[ShuffleDependency[K, V, C]]
    SparkEnv.get.shuffleManager.getReader(dep.shuffleHandle, split.index, split.index + 1, context)
      .read()
      .asInstanceOf[Iterator[(K, C)]]
  }
```

SortShuffleManager: 真正负责创建阅读器

```scala
  override def getReader[K, C](
      handle: ShuffleHandle,
      startPartition: Int,
      endPartition: Int,
      context: TaskContext): ShuffleReader[K, C] = {
    new BlockStoreShuffleReader(
      handle.asInstanceOf[BaseShuffleHandle[K, _, C]], startPartition, endPartition, context)
  }
```



### read实现

大体逻辑：

1. 创建数据块读取流， 流的参数很大程度上决定了性能！
2. 活跃序列化器
3. 反序列化读取流，并转为KeyValue
4. 如果定义了聚合器，则进行聚合
5. 如果定义了排序，则使用ExternalSorter进行排序

```scala
override def read(): Iterator[Product2[K, C]] = {
    val wrappedStreams = new ShuffleBlockFetcherIterator(
      context,
      blockManager.shuffleClient,
      blockManager,
      mapOutputTracker.getMapSizesByExecutorId(handle.shuffleId, startPartition, endPartition),
      serializerManager.wrapStream,
      SparkEnv.get.conf.getSizeAsMb("spark.reducer.maxSizeInFlight", "48m") * 1024 * 1024,
      SparkEnv.get.conf.getInt("spark.reducer.maxReqsInFlight", Int.MaxValue),
      SparkEnv.get.conf.get(config.REDUCER_MAX_REQ_SIZE_SHUFFLE_TO_MEM),
      SparkEnv.get.conf.getBoolean("spark.shuffle.detectCorrupt", true))

    val serializerInstance = dep.serializer.newInstance()

    // Create a key/value iterator for each stream
    val recordIter = wrappedStreams.flatMap { case (blockId, wrappedStream) =>
      serializerInstance.deserializeStream(wrappedStream).asKeyValueIterator
    }

    // Update the context task metrics for each record read.
    val readMetrics = context.taskMetrics.createTempShuffleReadMetrics()
    val metricIter = CompletionIterator[(Any, Any), Iterator[(Any, Any)]](
      recordIter.map { record =>
        readMetrics.incRecordsRead(1)
        record
      },
      context.taskMetrics().mergeShuffleReadMetrics())

    // An interruptible iterator must be used here in order to support task cancellation
    val interruptibleIter = new InterruptibleIterator[(Any, Any)](context, metricIter)

    val aggregatedIter: Iterator[Product2[K, C]] = if (dep.aggregator.isDefined) {
      if (dep.mapSideCombine) {
        // We are reading values that are already combined
        val combinedKeyValuesIterator = interruptibleIter.asInstanceOf[Iterator[(K, C)]]
        dep.aggregator.get.combineCombinersByKey(combinedKeyValuesIterator, context)
      } else {
        // We don't know the value type, but also don't care -- the dependency *should*
        // have made sure its compatible w/ this aggregator, which will convert the value
        // type to the combined type C
        val keyValuesIterator = interruptibleIter.asInstanceOf[Iterator[(K, Nothing)]]
        dep.aggregator.get.combineValuesByKey(keyValuesIterator, context)
      }
    } else {
      require(!dep.mapSideCombine, "Map-side combine without Aggregator specified!")
      interruptibleIter.asInstanceOf[Iterator[Product2[K, C]]]
    }

    // Sort the output if there is a sort ordering defined. 排序
    dep.keyOrdering match {
      case Some(keyOrd: Ordering[K]) =>
        // Create an ExternalSorter to sort the data.
        val sorter =
          new ExternalSorter[K, C, C](context, ordering = Some(keyOrd), serializer = dep.serializer)
        sorter.insertAll(aggregatedIter)
        context.taskMetrics().incMemoryBytesSpilled(sorter.memoryBytesSpilled)
        context.taskMetrics().incDiskBytesSpilled(sorter.diskBytesSpilled)
        context.taskMetrics().incPeakExecutionMemory(sorter.peakMemoryUsedBytes)
        CompletionIterator[Product2[K, C], Iterator[Product2[K, C]]](sorter.iterator, sorter.stop())
      case None =>
        aggregatedIter
    }
  }
```

流的参数很大程度上决定了性能！回头看创建流的四个参数：

maxBytesInFlight 同时读取远程块的最大大小，默认48m
maxReqsInFlight 同时读取远程块的最大请求数
maxReqSizeShuffleToMem 一个请求最大能缓存的大小，通过spark.reducer.maxReqSizeShuffleToMem配置，默认Long.Max
detectCorrupt 是否检测读取崩溃，默认true



优化:
默认值：48m参数说明：决定了每次能够拉取多少数据。

> 注意，这里网上很多都说错了，他跟缓存不是一个事情啊

调优建议：如果作业可用的内存资源较为充足的话，可以适当增加这个参数的大小（比如96m），从而减少拉取数据的次数，也就可以减少
网络传输的次数，进而提升性能。在实践中发现，合理调节该参数，性能会有1%~5%的提升。

错误：reduce oom
reduce task去map拉数据，reduce 一边拉数据一边聚合   reduce段有一块聚合内存（executor memory * 0.2）
解决办法：
1、增加reduce 聚合的内存的比例  设置spark.shuffle.memoryFraction
2、 增加executor memory的大小  --executor-memory 5G
3、减少reduce task每次拉取的数据量  设置spark.reducer.maxSizeInFlight  24m

