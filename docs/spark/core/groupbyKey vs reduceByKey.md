# groupByKey的实现

groupByKey最终是调用combineByKeyWithClassTag实现的，所以只需要看combineByKeyWithClassTag就行

```scala
  def groupByKey(partitioner: Partitioner): RDD[(K, Iterable[V])] = self.withScope {
    //创建三个聚合函数
    val createCombiner = (v: V) => CompactBuffer(v)//初始化聚合值
    val mergeValue = (buf: CompactBuffer[V], v: V) => buf += v  //合并值
    val mergeCombiners = (c1: CompactBuffer[V], c2: CompactBuffer[V]) => c1 ++= c2//分区间合并
    val bufs = combineByKeyWithClassTag[CompactBuffer[V]](
      createCombiner, mergeValue, mergeCombiners, partitioner, mapSideCombine = false)
    bufs.asInstanceOf[RDD[(K, Iterable[V])]]
  }



def combineByKeyWithClassTag[C](
      createCombiner: V => C,
      mergeValue: (C, V) => C,
      mergeCombiners: (C, C) => C,
      partitioner: Partitioner,
      mapSideCombine: Boolean = true,
      serializer: Serializer = null)(implicit ct: ClassTag[C]): RDD[(K, C)] = self.withScope {

  
    val aggregator = new Aggregator[K, V, C](
      self.context.clean(createCombiner),
      self.context.clean(mergeValue),
      self.context.clean(mergeCombiners))
   //如果没有更改分区方式，则在每个分区上执行aggregator.combineValuesByKey
    if (self.partitioner == Some(partitioner)) {
      self.mapPartitions(iter => {
        val context = TaskContext.get()
        new InterruptibleIterator(context, aggregator.combineValuesByKey(iter, context))
      }, preservesPartitioning = true)
    } else {
   //如果更改了分区方式，则执行shuffle,产生shuffledRDD
      new ShuffledRDD[K, V, C](self, partitioner)
        .setSerializer(serializer)
        .setAggregator(aggregator)
        .setMapSideCombine(mapSideCombine)
    }
  }
```



我们先看无需shuffle的方式,combineValuesByKey会把分区数据插入到ExternalAppendOnlyMap里， 逐条插入，每插入一条就和已有数据进行聚合，当内存不足时spill到磁盘。

```scala
  def combineValuesByKey(
      iter: Iterator[_ <: Product2[K, V]],
      context: TaskContext): Iterator[(K, C)] = {
    val combiners = new ExternalAppendOnlyMap[K, V, C](createCombiner, mergeValue, mergeCombiners)
    combiners.insertAll(iter)
    updateMetrics(context, combiners)
    combiners.iterator
  }
```





# groupByKey和reduceByKey的区别

下面来看看groupByKey和reduceByKey的区别：

```scala
    val conf = new SparkConf().setAppName("GroupAndReduce").setMaster("local")
    val sc = new SparkContext(conf)
    val words = Array("one", "two", "two", "three", "three", "three")
    val wordsRDD = sc.parallelize(words).map(word => (word, 1))
    val wordsCountWithReduce = wordsRDD.
      reduceByKey(_ + _).
      collect().
      foreach(println)
    val wordsCountWithGroup = wordsRDD.
      groupByKey().
      map(w => (w._1, w._2.sum)).
      collect().
      foreach(println)
```

虽然两个函数都能得出正确的结果， 但reduceByKey函数更适合使用在大数据集上。 这是因为Spark知道它可以在每个分区移动数据之前将输出数据与一个共用的`key`结合。

借助下图可以理解在reduceByKey里发生了什么。 在数据对被搬移前，同一机器上同样的`key`是怎样被组合的( reduceByKey中的 lamdba 函数)。然后 lamdba 函数在每个分区上被再次调用来将所有值 reduce成最终结果。整个过程如下：

![img](https://raw.githubusercontent.com/jacksu/utils4s/master/spark-knowledge/images/reduceByKey.png)

image

另一方面，当调用 groupByKey时，所有的键值对(key-value pair) 都会被移动,在网络上传输这些数据非常没必要，因此避免使用 GroupByKey。

为了确定将数据对移到哪个主机，Spark会对数据对的`key`调用一个分区算法。 当移动的数据量大于单台执行机器内存总量时`Spark`会把数据保存到磁盘上。 不过在保存时每次会处理一个`key`的数据，所以当单个 key 的键值对超过内存容量会存在内存溢出的异常。 这将会在之后发行的 Spark 版本中更加优雅地处理，这样的工作还可以继续完善。 尽管如此，仍应避免将数据保存到磁盘上，这会严重影响性能。

![img](https://raw.githubusercontent.com/jacksu/utils4s/master/spark-knowledge/images/groupByKey.png)

你可以想象一个非常大的数据集，在使用 reduceByKey 和 groupByKey 时他们的差别会被放大更多倍。