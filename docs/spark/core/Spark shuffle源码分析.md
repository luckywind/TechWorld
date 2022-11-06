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

### bypass机制

当reducer个数很小时，hash到多个文件是比sorting到一个文件快的，因此，sort shuffle有一个回退计划：当reducer个数小于spark.shuffle.sort.bypassMergeThreshold(默认200)个时，启用回退计划，hash到多个文件然后再join到一个文件。该实现的源码参考BypassMergeSortShuffleWriter类。

<font color=red>如果map端没有聚合与排序，且想启用hash shuffle，可以调大spark.shuffle.sort.bypassMergeThreshold</font>

Hash-style shuffle:  每个reducer一个文件，写完后再拼接成一个输出文件

>  SPARK-6026计划删除这个代码路径

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

![image-20210815222029237](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210815222029237.png)

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

![image-20210815232712184](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210815232712184.png)

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

### MapOutputTracker

MapOutputTracker是 SparkEnv初始化是初始化的重要组件之一  是master-slave的结构
用来跟踪记录shuffleMapTask的输出位置 （shuffleMapTask要写到哪里去），
shuffleReader读取shuffle文件之前就是去请求MapOutputTrackerMaster 要自己处理的数据 在哪里？
MapOutputTracker给它返回一批 MapOutputTrackerWorker的列表（地址，port等信息）
shuffleReader开始读取文件  进行后期处理

数据结构

![image-20210827153403865](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210827153403865.png)

```scala


  /** 在map out的集合mapStatuses中注册新的Shuffle，参数为Shuffle id和map的个数。  */
  def registerShuffle(shuffleId: Int, numMaps: Int) {
    if (mapStatuses.put(shuffleId, new Array[MapStatus](numMaps)).isDefined) {
      throw new IllegalArgumentException("Shuffle ID " + shuffleId + " registered twice")
    }
    // add in advance
    shuffleIdLocks.putIfAbsent(shuffleId, new Object())
  }

  /** 根据Shuffle id在mapStatuses中为Shuffle添加map out的状态（存储的map out其实就是map out的状态）。 */
  def registerMapOutput(shuffleId: Int, mapId: Int, status: MapStatus) {
    val array = mapStatuses(shuffleId)
    array.synchronized {
      array(mapId) = status
    }
  }
```



![image-20210827151326184](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210827151326184.png)



注册调用链：

![image-20210827152340387](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210827152340387.png)

注册新的shuffle

```scala
    if (mapOutputTracker.containsShuffle(shuffleDep.shuffleId)) {
      // A previously run stage generated partitions for this shuffle, so for each output
      // that's still available, copy information about that output location to the new stage
      // (so we don't unnecessarily re-compute that data).
      // 如果当前shuffle已经在MapOutputTracker中注册过
      val serLocs = mapOutputTracker.getSerializedMapOutputStatuses(shuffleDep.shuffleId)
      val locs = MapOutputTracker.deserializeMapStatuses(serLocs)
      // 更新Shuffle的Shuffle Write路径
      (0 until locs.length).foreach { i =>
        if (locs(i) ne null) {
          // locs(i) will be null if missing
          stage.addOutputLoc(i, locs(i))
        }
      }
    } else {
      // 如果当前Shuffle没有在MapOutputTracker中注册过
      // Kind of ugly: need to register RDDs with the cache and map output tracker here
      // since we can't do it in the RDD constructor because # of partitions is unknown
      logInfo("Registering RDD " + rdd.id + " (" + rdd.getCreationSite + ")")
      // 注册
      mapOutputTracker.registerShuffle(shuffleDep.shuffleId, rdd.partitions.length)
    }
```



### Shuffle Map过程

#### mapTask按什么规则进行output?

```scala
private[spark] class ShuffleMapTask(
    stageId: Int,
    taskBinary: Broadcast[Array[Byte]],
    partition: Partition,
    @transient private var locs: Seq[TaskLocation])
  extends Task[MapStatus](stageId, partition.index) with Logging {
    
  override def runTask(context: TaskContext): MapStatus = {
    val ser = SparkEnv.get.closureSerializer.newInstance()
    val (rdd, dep) = ser.deserialize[(RDD[_], ShuffleDependency[_, _, _])](
      ByteBuffer.wrap(taskBinary.value), Thread.currentThread.getContextClassLoader)

    metrics = Some(context.taskMetrics)
    var writer: ShuffleWriter[Any, Any] = null
    try {
      val manager = SparkEnv.get.shuffleManager
      writer = manager.getWriter[Any, Any](dep.shuffleHandle, partitionId, context)
      writer.write(rdd.iterator(partition, context).asInstanceOf[Iterator[_ <: Product2[Any, Any]]])
      return writer.stop(success = true).get
    } catch {
      case e: Exception =>
        try {
          if (writer != null) {
            writer.stop(success = false)
          }
        } catch {
          case e: Exception =>log.debug("Could not stop writer", e)
        }
        throw e
    }
  }
}
```

核心就是ShuffleMapTask.runTask的实现, 每个运行在Executor上的Task, 通过SparkEnv获取shuffleManager对象, 然后调用getWriter来当前MapID=partitionId的一组Writer.
然后将rdd的迭代器传递给writer.write函数, 由每个Writer的实现去实现具体的write操作;

#### reduceTask按什么规则进行reduce?

ShuffleBlockManager, 和Spark内部的BlockManager相似, 只是把Shuffle的write/reduce都抽象为block的操作, 并由ShuffleBlockManager进行Block管理;

## Shuffle Read

> 上面我们谈到了ShuffleMapStage,其实是Shuffle Map的过程,即ShuffleMapStage的ShuffleMapTask按照一定的规则将数据写到相应的文件中,并把写的文件"位置信息"
> 以MapStatus返回给DAGScheduler ,MapStatus将它更新到特定位置就完成了整个Shuffle Map过程.  
> 在Spark中,Shuffle reduce过程抽象化为ShuffledRDD,即这个RDD的compute方法计算每一个分片即每一个reduce的数据是通过拉取ShuffleMap输出的文件并返回Iterator来实现的

对于每个stage来说，它的上边界，要么从外部存储读取数据，要么读取上一个stage的输出。而下边界要么是通过ShuffleMapTask写入到本地文件系统，要么就是最后一个stage,写出结果文件。这里的stage在运行时就可以以流水线的方式进行运行一组Task，除了最后一个stage对应的ResultTask，其余的stage全部对应的shuffle Map Task。

　　除了需要从外部存储读取数据和RDD已经做过cache或者checkPoint的Task。一般的Task都是从Shuffle RDD的ShuffleRead开始的

### ShuffledRDD

,Shuffle过程是包括Map和Reduce两个过程;其中Shuffle Map以ShuffleMapStage的形式存在, Shuffle Reduce被抽象为一个RDD,该RDD的compute函数有点特殊而已

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
2. 获取序列化器
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
maxReqsInFlight 同时读取远程块的最大请求数, 默认Int.Max
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





[spark shuffle文件寻址流程](https://mp.weixin.qq.com/s?__biz=MzIwMjA2MTk4Ng==&mid=2247485116&idx=1&sn=82bd42f76836e67206d317135aa0c89c&chksm=96e52771a192ae6784d3f21fef53ed0c2b43e1caeefea94cc6f8f6725f883b3b7d6c65cf6ffb&scene=21#wechat_redirect)

**MapOutPutTracker：**MapOutPutTracker 是 spark 里面的一个模块，主从架构，用来管理磁盘小文件的地址。

MapOutPutTrackerMaster 是主，存在于 Driver 中；

MapOutPutTrackerWorker 是从，存在于 Executor 中；

**BlockManager：**块管理者，也是一个 spark 中的一个模块，主从架构。

BlockManagerMaster 是主，存在于 Driver 中。用于在集群中传递广播变量或缓存数据或删除数据的时候通知其他的 跟随节点来进行相应的操作。说白了就是指挥。

BlockManagerWorker是从，存在于 Executor 中。会与 BlockManagerMaster节点进行通信。

无论在 Driver 端的 BlockManager 还是在 Excutor 端的BlockManager 都含有四个对象：

① DiskStore:负责磁盘的管理。

② MemoryStore：负责内存的管理。

③ConnectionManager负责连接其他BlockManagerWorker。

④ BlockTransferService:负责数据的传输。



![image-20210903164055570](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210903164055570.png)

1. map task运行完毕之后，会将 task 执行之后的产生的磁盘小文件的地址封装到 MapStatus 对象中。通过 MapOutpuTrackerWorker对象向 Driver 中的 MapOutputTrackerMaster 汇报。

2. 在所有的 map task 执行完毕后，Driver 中就掌握了所有的磁盘小文件的地址。

3. 在 reduce task 执行之前，会通过Executor 中MapOutPutTrackerWorker 向 Driver 端的 MapOutputTrackerMaster 获取磁盘小文件的地址。

4. 获取到磁盘小文件的地址后，会通过 BlockManager 中的 ConnectionManager 连接数据所在节点上的 ConnectionManager, 然后通过 BlockTransferService 进行数据的传输。

5. BlockTransferService 默认启动 5 个 task 去节点拉取数据。默认情况下，5 个 task 拉取数据量不能超过 48 M。拉取过来的数据放在 Executor端的shuffle聚合内存中（spark.shuffle.memeoryFraction 0.2）, 如果5 个 task 一次拉取的数据放不到shuffle内存中会有 OOM,如果放下一次，不会有 OOM，以后放不下的会放磁盘。
