

# 原理

## 总结

1. reduceBykey未必会shuffle,得看rdd本身的分区器和传入的分区是否相同(不传则是默认的hash分区器)，如果相同，则不会进行shuffle
2. reduceBykey传入的代码逻辑将作为数据聚合的逻辑，就是聚合器

![在这里插入图片描述](https://gitee.com/luckywind/PigGo/raw/master/image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM1Njg4MTQw,size_16,color_FFFFFF,t_70.png)


需要特别说明的是对join操作有两种情况：
（1）图中左半部分join：如果两个RDD在进行join操作时，一个RDD的partition仅仅和另一个RDD中已知个数的Partition进行join，那么这种类型的join操作就是窄依赖，例如图1中左半部分的join操作(join with inputs co-partitioned)；
（2）图中右半部分join：其它情况的join操作就是宽依赖,例如图1中右半部分的join操作(join with inputs not co-partitioned)，由于是需要父RDD的所有partition进行join的转换，这就涉及到了shuffle，因此这种类型的join操作也是宽依赖。

## 源码

```scala
(一) func也就是传入的聚合函数，将用作ShuffledRDD的数据聚合器aggregator的mergeValue与mergeCombiners
def reduceByKey(partitioner: Partitioner, func: (V, V) => V): RDD[(K, V)] = self.withScope {
    combineByKeyWithClassTag[V]((v: V) => v, func, func, partitioner)
  }




(二)
def combineByKeyWithClassTag[C](
                                   createCombiner: V => C,
                                   mergeValue: (C, V) => C,
                                   mergeCombiners: (C, C) => C,
                                   partitioner: Partitioner,
                                   mapSideCombine: Boolean = true,
                                   serializer: Serializer = null)(implicit ct: ClassTag[C]): RDD[(K, C)] = self.withScope {
  //创建聚合器，aggregator中的mergeValue和mergeCombiners类似于mapreduce中的map和reduce过程。逻辑就是reduceBykey传入的代码
    val aggregator = new Aggregator[K, V, C](
      self.context.clean(createCombiner),
      self.context.clean(mergeValue),
      self.context.clean(mergeCombiners))
   
  //RDD本身的partitioner和传入的partitioner相等时, 即不需要重新shuffle, 直接map即可，则在partitioner使用mapPartitions对每个元素使用combineValuesByKey进行聚合
    if (self.partitioner == Some(partitioner)) {
      self.mapPartitions(iter => {
        val context = TaskContext.get()
        new InterruptibleIterator(context, aggregator.combineValuesByKey(iter, context))
      }, preservesPartitioning = true)
    } else {
      // 生成1个ShuffledRDD
      new ShuffledRDD[K, V, C](self, partitioner)
        .setSerializer(serializer)
        .setAggregator(aggregator)
        .setMapSideCombine(mapSideCombine)
    }
  }
```

## combineValuesByKey

往ExternalAppendOnlyMap里插入数据

```scala
（一）创建ExternalAppendOnlyMap并插入
def combineValuesByKey(
      iter: Iterator[_ <: Product2[K, V]],
      context: TaskContext): Iterator[(K, C)] = {
    val combiners = new ExternalAppendOnlyMap[K, V, C](createCombiner, mergeValue, mergeCombiners)
    combiners.insertAll(iter)
    updateMetrics(context, combiners)
    combiners.iterator
  }

（二）合并
  def insertAll(entries: Iterator[Product2[K, V]]): Unit = {
    if (currentMap == null) {
      throw new IllegalStateException(
        "Cannot insert new elements into a map after calling iterator")
    }

    var curEntry: Product2[K, V] = null
    //合并其实就是用新记录更新聚合结果
    val update: (Boolean, C) => C = (hadVal, oldVal) => {
      if (hadVal) mergeValue(oldVal, curEntry._2) else createCombiner(curEntry._2)
    }

    while (entries.hasNext) {
      curEntry = entries.next()
      val estimatedSize = currentMap.estimateSize()
      if (estimatedSize > _peakMemoryUsedBytes) {
        _peakMemoryUsedBytes = estimatedSize
      }
      if (maybeSpill(currentMap, estimatedSize)) {
        currentMap = new SizeTrackingAppendOnlyMap[K, C]
      }
      currentMap.changeValue(curEntry._1, update)
      addElementsRead()
    }
  }
```

