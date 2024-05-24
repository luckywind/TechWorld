我们知道rdd的transformation operators最终都会被转化成shuffle map task或者result task，然后分配到exectuor端去执行。那么这些map operators是怎么被pipeline起来执行的呢？也就是说shuffle map task和result task是怎么把这些operators串联起来的呢？

![image-20210727223458133](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210727223458133.png)

# Task逻辑

shuffle map task和result task都会对taskBinary做反序列化得到rdd对象并且调用rdd.iterator函数去获取对应partition的数据。我们来看看rdd.iterator函数做了什么：

ShuffleMapTask:

```scala
 /**
    *
    * 运行的主要逻辑其实只有两步，如下：
        1、通过使用广播变量反序列化得到RDD和ShuffleDependency：
              1.1、获得反序列化的起始时间deserializeStartTime；
              1.2、通过SparkEnv获得反序列化器ser；
              1.3、调用反序列化器ser的deserialize()进行RDD和ShuffleDependency的反序列化，数据来源于taskBinary，得到rdd、dep；
              1.4、计算Executor进行反序列化的时间_executorDeserializeTime；
         2、利用shuffleManager的writer进行数据的写入：
               2.1、通过SparkEnv获得shuffleManager；
               2.2、通过shuffleManager的getWriter()方法，获得shuffle的writer，其中的partitionId表示的是当前RDD的某个partition,也就是说write操作作用于partition之上；
               2.3、针对RDD中的分区partition，调用rdd的iterator()方法后，再调用writer的write()方法，写数据；
               2.4、停止writer，并返回标志位。
    */
  override def runTask(context: TaskContext): MapStatus = {
    // Deserialize the RDD using the broadcast variable.
    val threadMXBean = ManagementFactory.getThreadMXBean
    // 反序列化的起始时间
    val deserializeStartTime = System.currentTimeMillis()
    val deserializeStartCpuTime = if (threadMXBean.isCurrentThreadCpuTimeSupported) {
      threadMXBean.getCurrentThreadCpuTime
    } else 0L

    // 获得反序列化器closureSerializer
    val ser = SparkEnv.get.closureSerializer.newInstance()
    // 调用反序列化器closureSerializer的deserialize()进行RDD和ShuffleDependency的反序列化，数据来源于taskBinary
    val (rdd, dep) = ser.deserialize[(RDD[_], ShuffleDependency[_, _, _])](
      ByteBuffer.wrap(taskBinary.value), Thread.currentThread.getContextClassLoader)
    // 计算Executor进行反序列化的时间
    _executorDeserializeTime = System.currentTimeMillis() - deserializeStartTime
    _executorDeserializeCpuTime = if (threadMXBean.isCurrentThreadCpuTimeSupported) {
      threadMXBean.getCurrentThreadCpuTime - deserializeStartCpuTime
    } else 0L

    var writer: ShuffleWriter[Any, Any] = null
    try {
      // 获得shuffleManager
      val manager = SparkEnv.get.shuffleManager
      // 通过shuffleManager的getWriter()方法，获得shuffle的writer
      // 启动的partitionId表示的是当前RDD的某个partition,也就是说write操作作用于partition之上
      writer = manager.getWriter[Any, Any](dep.shuffleHandle, partitionId, context)
      // 针对RDD中的分区调用rdd的iterator()方法后，再调
      // 用writer的write()方法，写数据
      writer.write(rdd.iterator(partition, context).asInstanceOf[Iterator[_ <: Product2[Any, Any]]])
      // 停止writer，并返回标志位
      writer.stop(success = true).get
    } catch {
      case e: Exception =>
        try {
          if (writer != null) {
            writer.stop(success = false)
          }
        } catch {
          case e: Exception =>
            log.debug("Could not stop writer", e)
        }
        throw e
    }
  }
```

# rdd迭代器的pipeline

RDD顶层接口的iterator方法定义如下：

```scala
  final def iterator(split: Partition, context: TaskContext): Iterator[T] = {
    if (storageLevel != StorageLevel.NONE) {
      // 如果存储级别不是NONE，那么先检查是否有缓存，没有缓存则需要进行计算
      getOrCompute(split, context)
    } else {
      // 如果有checkpoint,那么直接读取结果，否则直接进行计算
      computeOrReadCheckpoint(split, context)
    }
  }
```

getOrCompute方法最终会由RDD的一个实现的compute()方法调用，map/flatmap/filter这些方法产生的RDD都是MapPartitionsRDD,其compute方法实现如下：

```scala
private[spark] class MapPartitionsRDD[U: ClassTag, T: ClassTag](
    var prev: RDD[T],
    f: (TaskContext, Int, Iterator[T]) => Iterator[U],  // (TaskContext, partition index, iterator)
    preservesPartitioning: Boolean = false)
  extends RDD[U](prev) {
  override def compute(split: Partition, context: TaskContext): Iterator[U] =
    f(context, split.index, firstParent[T].iterator(split, context))

  override def clearDependencies() {
    super.clearDependencies()
    prev = null
  }
}
```

可以看到，compute方法调用了parent RDD的iterator方法，然后apply了当前MapPartitionsRDD的f参数. 那这个f又是什么function呢？我们需要回到RDD.scala中看一下map, filter, flatMap的code：

```scala
 /**
   * Return a new RDD by applying a function to all elements of this RDD.
   */
  def map[U: ClassTag](f: T => U): RDD[U] = withScope {
    val cleanF = sc.clean(f)
    new MapPartitionsRDD[U, T](this, (context, pid, iter) => iter.map(cleanF))
  }

  /**
   *  Return a new RDD by first applying a function to all elements of this
   *  RDD, and then flattening the results.
   */
  def flatMap[U: ClassTag](f: T => TraversableOnce[U]): RDD[U] = withScope {
    val cleanF = sc.clean(f)
    new MapPartitionsRDD[U, T](this, (context, pid, iter) => iter.flatMap(cleanF))
  }

  /**
   * Return a new RDD containing only the elements that satisfy a predicate.
   */
  def filter(f: T => Boolean): RDD[T] = withScope {
    val cleanF = sc.clean(f)
    new MapPartitionsRDD[T, T](
      this,
      (context, pid, iter) => iter.filter(cleanF),
      preservesPartitioning = true)
  }
```

从上面的源码可以看出，MapPartitionsRDD中的f函数就是用户给定的function. 

所以这是一个逐层嵌套的rdd.iterator方法调用，子rdd调用父rdd的iterator方法并在其结果之上调用scala.collection.Iterator的map/flatmap/filter函数以执行用户给定的function，逐层调用，直到调用到最初的iterator（比如hadoopRDD partition的iterator）。

现在，我们最初的问题：“多个连续的spark map operators是如何pipeline起来执行的？” 就转化成了“scala.collection.Iterator的多个连续map操作是如何pipeline起来的？”

# scala迭代器的pipeline

scala.collection.Iterator的map operators是怎么构成pipeline的？看下map操作的源码：

```scala
  /** Creates a new iterator that maps all produced values of this iterator
   *  to new values using a transformation function.
   *  创建一个新的iterator，该iterator把当前iterator的值value映射成f(value)
   *  @param f  the transformation function
   *  @return a new iterator which transforms every value produced by this
   *          iterator by applying the function `f` to it.
   *  @note   Reuse: $consumesAndProducesIterator
   */
  def map[B](f: A => B): Iterator[B] = new AbstractIterator[B] {
    def hasNext = self.hasNext
    def next() = f(self.next())  //重写新iterator的next方法
  }

```

从上面的源码可以看出，Iterator的map方法返回的Iterator就是基于当前Iterator （self）override了next和hasNext方法的Iterator实例。比如，对于map函数，新的Iterator的hasNext就是直接调用了当前iterator的hasNext，next方法就是在当前iterator的next方法的结果上调用了指定的map函数.

flatMap和filter函数稍微复杂些，但本质上一样，都是通过调用self iterator的hasNext和next方法对数据进行遍历和处理。

所以，当我们调用最终结果iterator的hasNext和next方法进行遍历时，每遍历一个data element都会逐层调用父层iterator的hasNext和next方法。各层的map function组成了一个pipeline，每个data element都经过这个pipeline的处理得到最终结果数据。

# 总结

1. task通过调用对应rdd的iterator方法获取对应partition的数据，而这个iterator方法又会逐层调用父rdd的iterator方法获取数据。所以这个过程是一个嵌套迭代的过程，最终实现是scala.collection.iterator的嵌套，而这个scala.collection.iteratorde 嵌套通过覆写scala.collection.iterator的hasNext和next方法实现的。

2. RDD/DataFrame上的连续的map, filter, flatMap函数会自动构成operator pipeline一起对每个data element进行处理，单次循环即可完成多个map operators, 无需多次遍历。

3. pipeline管道计算模式,pipeline只是一种计算思想，模式。

   每个task相当于执行了一个高阶函数，f4（f3（f2（f1（“.....”）））），这种模式就称为pipeline管道计算模式。

   MapReduce 是每一步都要disk io. Spark 都过pipeline 模式进行内存迭代计算

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20211123164407843.png" alt="image-20211123164407843" style="zoom:50%;" />

Task的使用 rdd.iterator 获得迭代器, iterator方法调用compute, compute的输入参数是父RDD的Iterator, 因此调用 父RDD.iterator,  继续递归获得祖父rdd的Iterator

shufflemapTask 通过 shuffleManager 获得shuffleWriter, writer使用iterator方法获得自己的Iterator进行 drop to disk 落盘操作. Iterator 触发父rdd们的递归调用自己的iterator方法

shufflemapTask 和 ResultTask 通过 shuffleManager 获得shuffleReader, Reader 调用 ShuffledRDD的iterator 方法, 通过网络fetch远程的block. 这些block 在map阶段被写入磁盘.
