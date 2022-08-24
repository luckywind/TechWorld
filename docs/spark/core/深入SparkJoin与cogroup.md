[参考](https://dzone.com/articles/deep-dive-into-join-execution-in-apache-spark)

[spark支持的join方式以及join策略](https://blog.csdn.net/weixin_42325002/article/details/119353977)

# Join操作的要点

## 数据大小

数据大小直接影响join操作的执行效率和可靠性。输入数据集的相对大小也会影响join机制的选择，当然也会影响执行效率和可靠性。

## join条件

join条件分为相等join和不等join两类。

1. 相等join，可包含一个或多个必须同时满足的等式条件。例如(A.x == B.x) 或 ((A.x == B.x) and (A.y == B.y))
2. 不等join，不涉及等式条件，但可能允许多个不同时满足的等式条件，例如(A.x < B.x) 或者 ((A.x == B.x) or (A.y == B.y)) 

## join类型

join的类型影响join条件应用后的输出：

1. Inner Join: 只输出匹配到的
2. Outer Join:  又分为left /right/full out join
3. Semi Join: 输出不同时出现的记录，也称为反join
4. Cross Join: 笛卡尔Join

# Join执行机制

Spark提供五种Join操作机制

- Shuffle Hash Join
- Broadcast Hash Join
- Sort Merge Join
- Cartesian Join
- Broadcast Nested Loop Join

## **Broadcast Hash Join**

强制条件

1. 只用于等值Join
2. 不适用于Full Outer Join

除此之外， 还得满足一个条件：

1. 左表没有hint,且Join类型是‘Right Outer’, ‘Right Semi’, or ‘Inner’.
2. 左表可广播，且Join类型是‘Right Outer’, ‘Right Semi’, or ‘Inner’.
3. 右表有广播hint，且Join类型是‘Left Outer’, ‘Left Semi’, or ‘Inner’.
4. 右表可广播，且Join类型是‘Left Outer’, ‘Left Semi’, or ‘Inner’.
5. 两表均有广播hint,且Join类型是‘Left Outer’, ‘Left Semi’, ‘Right Outer’, ‘Right Semi’, or ‘Inner’.
6. 两表均可广播，且Join类型是‘Left Outer’, ‘Left Semi’, ‘Right Outer’, ‘Right Semi’ or ‘Inner’.

## **Shuffle Hash Join**

强制条件：

1. 等值连接
2. 不适用Full Outer连接
3. *spark.sql.join.prefersortmergeJoin* =true

除此之外，

## **Sort Merge Join**

强制条件：

1. 等值连接
2. key可排序
3. spark.sql.join.prefersortmergeJoin=true

除此之外，还需要一个

1. 任意一方hint merge
2. 没有hint

## **Cartesian Join**

## **Broadcast Nested Loop Join**

默认的Join机制

# join算法

[参考](https://blog.knoldus.com/joins-in-apache-spark/)

## Broadcast Hash Join

## Sort Merge Join

对两个分支执行shuffle和sort，使得两边正确分区以及排序，例如：

![Joins in Apache Spark: Sort Merge Join](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/1*HW3YcSgA2KdS2MYGbPsxpw.png)

包含两个步骤： 

1. exchange和sort 
2. merge: 在分区内根据join key去join相同值的行

spark2.3开始，这是默认的join算法，可通过***spark.sql.join.preferSortMergeJoin*** 更改

## Shuffled Hash Join

Map-reduce的概念，使用join列作为key把数据map出去，再shuffle数据。这样不同数据集的相同key会在一个机器上。reduce阶段，spark 在连接数据。

![Joins in Apache Spark: Shuffled Hash Join](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/1*-IDRGpNF7C-IwlMYoOEvAA.png)

即使***spark.sql.join.preferSortMergeJoin*** =false, spark也只会在满足以下条件时才会选择shuffled hash join:

1. 一个数据集比另一个小三倍以上
2. 每个分区的平均大小小于***autoBroadcastJoinThreshold***.

Hash join也要求数据正确分区，但不要求排序，可能会比sortMergeJoin快

# 拓展join算法实现

[参考](https://zhuanlan.zhihu.com/p/271910611)

# Join源码

[参考](https://blog.csdn.net/gaoshui87/article/details/78272791)

PairRDD通过隐式转换从PairRDDFunctions类获取Join相关算子，join方法的有两个重载方法：

```scala
只提供一个参数，不指定分区函数时默认使用HashPartitioner;提供numPartitions参数时，其内部的分区函数是HashPartitioner(numPartitions)
  def join[W](other: RDD[(K, W)]): RDD[(K, (V, W))] = self.withScope {
    join(other, defaultPartitioner(self, other))
  }

  def join[W](other: RDD[(K, W)], numPartitions: Int): RDD[(K, (V, W))] = self.withScope {
    join(other, new HashPartitioner(numPartitions))
  }
```

这俩join方法最终调用cogroup方法，内部直接使用这俩rdd和分区器构造了一个CoGroupedRDD

**使用cogroup的结果产生一个笛卡尔积，而cogroup算子内部构造了一个CoGroupedRDD,所以关键就是看这个CoGroupedRDD如何实现**

```scala
  def join[W](other: RDD[(K, W)], partitioner: Partitioner): RDD[(K, (V, W))] = self.withScope {
    this.cogroup(other, partitioner).flatMapValues( pair =>
      for (v <- pair._1.iterator; w <- pair._2.iterator) yield (v, w)
    )

    
def cogroup[W](other: RDD[(K, W)], partitioner: Partitioner)
      : RDD[(K, (Iterable[V], Iterable[W]))] = self.withScope {
    if (partitioner.isInstanceOf[HashPartitioner] && keyClass.isArray) {
      throw new SparkException("HashPartitioner cannot partition array keys.")
    }
    val cg = new CoGroupedRDD[K](Seq(self, other), partitioner)
    cg.mapValues { case Array(vs, w1s) =>
      (vs.asInstanceOf[Iterable[V]], w1s.asInstanceOf[Iterable[W]])
    }
  }
```

## CoGroupedRDD

CoGroupedRDD的原理： 对于父RDD的每个key,返回该key对应的值列表

**其本身是一个RDD,只需要弄清楚这个RDD的依赖、分区以及compute方法如何计算分区结果**

看看这个RDD的依赖以及如何分区的

### 依赖

返回与上游每个rdd的依赖

参与join的rdd和join后的rdd之间是窄依赖还是宽依赖，取决于rdd和join时指定的分区器是否相同。

```scala
  override def getDependencies: Seq[Dependency[_]] = {
    //rdds是参与cogroup的rdd列表
    rdds.map { rdd: RDD[_] =>
      if (rdd.partitioner == Some(part)) {
         /*如果RDD的分区和join时指定的分区函数相同，则对应窄依赖，不用进行hash拆分*/
        logDebug("Adding one-to-one dependency with " + rdd)
        new OneToOneDependency(rdd)
      } else {
        //否则就是宽依赖，需要用ShuffleDependency 进行hash分片数据，然后在正确的split分片处理业务进程中进行处理。
        logDebug("Adding shuffle dependency with " + rdd)
        new ShuffleDependency[K, Any, CoGroupCombiner](
          rdd.asInstanceOf[RDD[_ <: Product2[K, _]]], part, serializer)
      }
    }
  }
```

### 分区

**返回二维数组，第一维是rdd序号，第二维是某个rdd下的分区序号**

CoGroupPartition是结果rdd的分区，每个分区和上游所有rdd之间都有依赖关系。

1. 如果是窄依赖关系，则直接根据上游rdd的对应分区创建一个NarrowCoGroupSplitDep分区
2. 如果是宽依赖，则没法直接创建，这个分区暂时是None



<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20211128204907200.png" alt="image-20211128204907200" style="zoom:50%;" />

```scala
  /*
  * 这里返回一个带有Partitioner.numPartitions个分区类型为CoGroupPartition的数组
  */
  override def getPartitions: Array[Partition] = {
    val array = new Array[Partition](part.numPartitions)
    for (i <- 0 until array.length) {
      // Each CoGroupPartition will have a dependency per contributing RDD
 
      //rdds.zipWithIndex 这个是生成一个（rdd,rddIndex）的键值对，可以查看Seq或者Array的API
      //继续跟到CoGroupPartition这个Partition,其是和Partition其实区别不到，只是多了一个变量narrowDeps
      //回来看NarrowCoGroupSplitDep的构造，就是传入了每一个rdd和分区索引，以及分区,其可以将分区序列化
      array(i) = new CoGroupPartition(i, rdds.zipWithIndex.map { case (rdd, j) =>
        // Assume each RDD contributed a single dependency, and get it
        dependencies(j) match {
          case s: ShuffleDependency[_, _, _] => None
          case _ => Some(new NarrowCoGroupSplitDep(rdd, i, rdd.partitions(i)))
        }
      }.toArray)
    }
    array
  }
```

### compute

既然宽依赖的分区没法直接创建，我们看接下来看compute方法怎么处理的：

```sql
  override def compute(s: Partition, context: TaskContext): Iterator[(K, Array[Iterable[_]])] = {
    val split = s.asInstanceOf[CoGroupPartition]
    val numRdds = dependencies.length

    // A list of (rdd iterator, dependency number) pairs
    val rddIterators = new ArrayBuffer[(Iterator[Product2[K, Any]], Int)]

    // 遍历所有上游依赖
    for ((dep, depNum) <- dependencies.zipWithIndex) dep match {
      case oneToOneDependency: OneToOneDependency[Product2[K, Any]] @unchecked =>
         //如果是窄依赖，则根据依赖序号从分区的窄依赖信息中拿到依赖的分区
        val dependencyPartition = split.narrowDeps(depNum).get.split
        // Read them from the parent 从父rdd中读取这些分区
        val it = oneToOneDependency.rdd.iterator(dependencyPartition, context)
        rddIterators += ((it, depNum))

      case shuffleDependency: ShuffleDependency[_, _, _] =>
        // Read map outputs of shuffle
        // 先获取shuffleManager，再读取shuffle结果
        val it = SparkEnv.get.shuffleManager
          .getReader(shuffleDependency.shuffleHandle, split.index, split.index + 1, context)
          .read()
        rddIterators += ((it, depNum))
    }

    //构造一个ExternalAppendOnlyMap， 并插入记录，注意insertAll
    //
    val map = createExternalMap(numRdds)
    for ((it, depNum) <- rddIterators) {
    /**map会增长，内存不足时会spill 到磁盘*/
      map.insertAll(it.map(pair => (pair._1, new CoGroupValue(pair._2, depNum))))
    }
    context.taskMetrics().incMemoryBytesSpilled(map.memoryBytesSpilled)
    context.taskMetrics().incDiskBytesSpilled(map.diskBytesSpilled)
    context.taskMetrics().incPeakExecutionMemory(map.peakMemoryUsedBytes)
    new InterruptibleIterator(context,
      map.iterator.asInstanceOf[Iterator[(K, Array[Iterable[_]])]])
  }
```

一个compute方法的调用实际上是计算某个分区的数据，

1. 第一步是创建该分区依赖结果的迭代器、分区序号

例如，计算cop1分区，对于窄依赖，直接获取父分区的迭代器即可，对于宽依赖，则需要获取shuffle reader作为数据迭代器

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20211128210750056.png" alt="image-20211128210750056" style="zoom:50%;" />

2. 第二步构造ExternalAppendOnlyMap并插入数据

如果是oneToOneDependency就直接读取那个分片数据，否则就要启动对RDD的shuffle的过程
把一个RDD通过hash分到多个分片当中，然后该函数拉取自己需求的那一个分片数据

3. ExternalAppendOnlyMap对数据排序和关联

> *note:当spark.shuffle.spill=true时会启用ExternalAppendOnlyMap，默认为true. 为false时就启用AppendOnlyMap*

[cogroup](https://blog.csdn.net/u014393917/article/details/50602461)

```sql
  private def createExternalMap(numRdds: Int)
  : ExternalAppendOnlyMap[K, CoGroupValue, CoGroupCombiner] = {

    val createCombiner: (CoGroupValue => CoGroupCombiner) = value => {
      val newCombiner = Array.fill(numRdds)(new CoGroup)
      newCombiner(value._2) += value._1
      newCombiner
    }
    val mergeValue: (CoGroupCombiner, CoGroupValue) => CoGroupCombiner =
      (combiner, value) => {
        combiner(value._2) += value._1
        combiner
      }
    val mergeCombiners: (CoGroupCombiner, CoGroupCombiner) => CoGroupCombiner =
      (combiner1, combiner2) => {
        var depNum = 0
        while (depNum < numRdds) {
          combiner1(depNum) ++= combiner2(depNum)
          depNum += 1
        }
        combiner1
      }
    new ExternalAppendOnlyMap[K, CoGroupValue, CoGroupCombiner](
      createCombiner, mergeValue, mergeCombiners)
  }
```





[ExternalAppendOnlyMap](https://dataknocker.github.io/2014/07/23/spark-appendonlymap/)

AppendOnlyMap是spark自己实现的Map，只能添加数据，不能remove。该Map是使用开放定址法中的二次探测法，不用自带的HashMap等应该是节省空间，提高性能。

ExternalAppendOnlyMap也在内存维护了一个SizeTrackingAppendOnlyMap(*继承于AppendOnlyMap*),当该Map元素数超过一定值时就spill到磁盘。最后ExternalAppendOnlyMap其实是维护了一个内存Map:currentMap以及多个diskMap:spillMaps。







这里主要看下CoGroupPartition,它其实是一个Partition实现，只不过将分区序号作为哈希code进行分区,这样保证窄依赖分区是一一对应的

```scala
private[spark] class CoGroupPartition(
    override val index: Int, val narrowDeps: Array[Option[NarrowCoGroupSplitDep]])
  extends Partition with Serializable {
  override def hashCode(): Int = index
  override def equals(other: Any): Boolean = super.equals(other)
}
```

### 总结

1. join 算子内部使用了cogroup算子，这个算子返回的是（key,(v1,v2)）这种形式的元组
2. 深入cogroup算子，发现其根据rdd1,rdd2创建了一个CoGroupedRDD
3. 分析了CoGroupedRDD的依赖以及分区的计算，如果和上游某个rdd的依赖关系是窄依赖，则创建NarrowCoGroupSplitDep分区，并计算时直接读取上游rdd的分区；如果是宽依赖，就需要对上游rdd重hash进行shuffle,并通过shuffle reader拉取属于当前分区的数据。
4. CoGroupedRDD的分区函数就是将两个rdd的相同分区索引的分区合成一个新的分区，并且通过NarrowCoGroupSplitDep这个类实现了序列化



```
spark.shuffle.file.buffer
默认值：32k参数说明：该参数用于设置shuffle write task的BufferedOutputStream的buffer缓冲大小。
将数据写到磁盘文件之前，会先写入buffer缓冲中，待缓冲写满之后，才会溢写到磁盘。调优建议：如果作业可用的内存资源较为充足的话，
可以适当增加这个参数的大小（比如64k），从而减少shuffle write过程中溢写磁盘文件的次数，也就可以减少磁盘IO次数，进而提升性能。
在实践中发现，合理调节该参数，性能会有1%~5%的提升。
```
