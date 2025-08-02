# core Join

join通常需要不同rdd中相应的key分布在同一个分区，以便于本地合并。如果rdd的分区器未知，则需要shuffle使得两个rdd共享分区器，且相同key的数据在同一个分区中。

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/hpsp_0401.png" alt="Join, full shuffle" style="zoom: 50%;" />

如果他们有相同的分区器，则他们的数据可能是在一起的，从而可以避免网络传输

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/hpsp_0403.png" alt="Colocated join" style="zoom:50%;" />

不管是否有相同的分区器，如果某个rdd已知一个只有窄依赖的分区器，和大多数k/v操作一样，join的花费随着key的数量以及数据移动的距离而增加。

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/hpsp_0402.png" alt="Join one partitioner known" style="zoom:50%;" />

注意：

1. <font color=red>同一个父 RDD，trasformation 到的两个rdd 是copartitioned</font>
2. <font color=red>同一个父 RDD，相同分区器shuffle到的两个rdd 是copartitioned</font>
3. <font color=red>Core 模块的Join是使用cogroup函数实现的</font>

## 选择join类型

**join / leftOuterJoin / rightOuterJoin/ fullOuterJoin**

默认的Join只保留同时存在与两个rdd的记录，最好的场景是两个rdd包含相同的key集合，且都不重复，否则数据可能膨胀引起性能问题，若key只存在于一个rdd中则会丢失。

尽量减少join的数据量。

## 选择一个执行计划

spark的join操作需要数据在同一个分区，spark默认的实现是shuffled hash join。<font color=red>它通过对第二个rdd使用第一个rdd的默认分区器进行分区以确保每个分区包含相同的key，同时相同key的数据也在同一个分区上</font>>。这总是奏效，但因为总是要shuffle而更加昂贵。以下场景可避免shuffle:

1. 两个rdd有相同的分区器
2. 一个rdd足够小以放到内存，从而可以采用广播器

### 提供一个已知分区器来加速join

如果在join之前有aggregateByKey或者reduceByKey等需要shuffle的操作，我们可以这样阻止shuffle的发生：

给join操作前的一个操作添加一个分区数相同的hash分区器

```scala
def joinScoresWithAddress3(scoreRDD: RDD[(Long, Double)],
   addressRDD: RDD[(Long, String)]) : RDD[(Long, (Double, String))]= {
    // If addressRDD has a known partitioner we should use that,
    // otherwise it has a default hash parttioner, which we can reconstruct by
    // getting the number of partitions.
    val addressDataPartitioner = addressRDD.partitioner match {
      case (Some(p)) => p
      case (None) => new HashPartitioner(addressRDD.partitions.length)
    }
    val bestScoreData = scoreRDD.reduceByKey(addressDataPartitioner,
      (x, y) => if(x > y) x else y)
    bestScoreData.join(addressRDD)
  }
```

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/hpsp_0404.png" alt="Join both partitioners known" style="zoom:50%;" />

1. 同一个action算子、同一个分区器物化的rdd，则一定是co-located
2. 最好在重分区后进行persist



分区器均未知的Join

```scala
  val col1 = Range(1, 50).map(idx => (random.nextInt(10), s"user$idx"))
    val col2 = Array((0, "BJ"), (1, "SH"), (2, "GZ"), (3, "SZ"), (4, "TJ"), (5, "CQ"), (6, "HZ"), (7, "NJ"), (8, "WH"), (0,"CD"))


    val rdd1: RDD[(Int, String)] = sc.makeRDD(col1,3)
    val rdd2: RDD[(Int, String)] = sc.makeRDD(col2,3)

    println(rdd1.join(rdd2).count())
```

![image-20221115145858728](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221115145858728.png)

分区器相同但分区数不同的Join， 产生的RDD分区数与最大的那个rdd的分区数相同，从而与分区数

```scala
    val rdd1: RDD[(Int, String)] = sc.makeRDD(col1)
      .partitionBy(new HashPartitioner(3))      //37行
    val rdd2: RDD[(Int, String)] = sc.makeRDD(col2) //38行
      .partitionBy(new HashPartitioner(4))       //39行
    println(rdd1.dependencies)
    println(rdd2.dependencies)

    println(rdd1.join(rdd2).count())
```

![image-20221115150358458](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221115150358458.png)

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221115151549381.png" alt="image-20221115151549381" style="zoom:50%;" />



分区器相同的Join

```scala
    val rdd1: RDD[(Int, String)] = sc.makeRDD(col1)
      .partitionBy(new HashPartitioner(3))
    val rdd2: RDD[(Int, String)] = sc.makeRDD(col2)
      .partitionBy(new HashPartitioner(3))
    println(rdd1.dependencies)
    println(rdd2.dependencies)

    println(rdd1.join(rdd2).count())
```

![image-20221115150447496](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221115150447496.png)

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221115151320128.png" alt="image-20221115151320128" style="zoom:50%;" />

不同分区器Join，结果RDD的分区器和分区数大的RDD的分区器一样，

```scala

    val rdd1: RDD[(Int, String)] = sc.makeRDD(col1)
      .partitionBy(new HashPartitioner(3))
    val rdd2: RDD[(Int, String)] = sc.makeRDD(col2)
      .partitionBy(new MyPartitioner(2))
    println(rdd1.dependencies)
    println(rdd2.dependencies)

    val joined: RDD[(Int, (String, String))] = rdd1.join(rdd2)
    println(joined.dependencies)
    println(joined.partitioner)
    println(joined.toDebugString)
    println(joined.count())
```

![image-20221115165926978](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221115165926978.png)



### 使用广播器hash join

<font color=red>sparkSQL可以自动识别小rdd来自动应用broadcast hash join, 但是spark core中，我们只能手动把小rdd collect后广播出去</font>

广播器hash join把小rdd推送到每个节点，然后在大rdd的每个分区进行一个map端合并。注意使用mapPartitions来合并元素。

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/hpsp_0405.png" alt="Broadcast Hash Join" style="zoom:50%;" />

```sql
 def manualBroadCastHashJoin[K : Ordering : ClassTag, V1 : ClassTag,
 V2 : ClassTag](bigRDD : RDD[(K, V1)],
  smallRDD : RDD[(K, V2)])= {
  val smallRDDLocal: Map[K, V2] = smallRDD.collectAsMap()
  val smallRDDLocalBcast = bigRDD.sparkContext.broadcast(smallRDDLocal)
  bigRDD.mapPartitions(iter => {
   iter.flatMap{
    case (k,v1 ) =>
     smallRDDLocalBcast.value.get(k) match {
      case None => Seq.empty[(K, (V1, V2))]
      case Some(v2) => Seq((k, (v1, v2)))
     }
   }
  }, preservesPartitioning = true)
 }
 //end:coreBroadCast[]
}
代码参考：
https://github.com/high-performance-spark/high-performance-spark-examples/blob/master/src/main/scala/com/high-performance-spark-examples/goldilocks/RDDJoinExamples.scala
```

### Partial manual broadcast hash join

可处理全是大表的join





# 中文博客

https://www.cnblogs.com/code2one/p/9872037.html

[join实现](http://shiyanjun.cn/archives/1816.html)
