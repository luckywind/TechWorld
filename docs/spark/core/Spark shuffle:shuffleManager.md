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

- **它的排序作用于序列化的二进制数据而不是Java对象，这减少了内存消耗和GC开销**。这种优化要求记录序列化程序具有一定的属性，允许序列化记录在不需要反序列化的情况下重新排序
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
      // 尝试把map输出以序列化形式缓存起来，使用UnsafeShuffleWriter 
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

