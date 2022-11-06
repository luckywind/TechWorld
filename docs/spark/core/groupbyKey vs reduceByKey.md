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

image

你可以想象一个非常大的数据集，在使用 reduceByKey 和 groupByKey 时他们的差别会被放大更多倍。