# 实例一：

```scala
    val rdd1: RDD[String] = sc.parallelize(Seq("a c", "a b", "b c", "b d", "c d"), 10)
    val word_count1 = rdd1.flatMap(a => a.split(' ')).map(a => (a, 1)).reduceByKey((x, y) => x + y)
    val rdd2: RDD[String] = sc.parallelize(Seq("a c", "a b", "b c", "b d", "c d"), 10)
    val word_count2 = rdd2.flatMap(a => a.split(' ')).map(a => (a, 1)).reduceByKey((x, y) => x + y)
    val joined: RDD[(String, (Int, Int))] = word_count1.join(word_count2)
    println(word_count1.toDebugString)
    println(word_count2.toDebugString)
    println(joined.dependencies)
    joined.collect()
    Thread.sleep(10000*1000)
```

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/1620.png)

三个stage，从下面看到一共产生一个Job,3个stage,30个task； 



![image-20220503092817614](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220503092817614.png)

## stage并行

![image-20220503092843931](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220503092843931.png)

从时间线发现stage0/1重叠度很大，说明它们是并行的(我本机只有8个核，所以实际并行度为8，开始被stage0抢占了，所以看起来会比stage1早一些)，stage2是在它们之后运行的，因为它依赖stage0/1。

![image-20220503092904653](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220503092904653.png)

reduceByKey是shuffle操作(<font color=red>但未必会发生shuffle</font>)，该操作分别是stage0/1的最后一个操作，但是产生的RDD划分到shuffle操作的下一个stage里了。stage0/1在该shuffle过程中都进行了write操作，写入了480B的数据，Stage2读取它们俩的输出，产生960B的Shuffle Read。

## task并行度 vs 分区数

rdd1和rdd2分别包含10个分区，所以各产生10个task，但实际上并行度并未必是10，我的机器只有8个核，所以实际并行度是8:

![image-20230515160313420](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230515160313420.png)



## join产生的task数？

问题一：一个RDD产生的task个数与它的分区数是一样的，上面例子中，join并未指定分区数，那么它的分区数怎么计算的呢？

问题二：仔细看DAG, 会发现join并没有产生新的stage， 这是为什么？

问题三：为什么会产生10个task？

> 因为join结果RDD和两个父RDD是OneToOne依赖关系，分区数取最大值

接着看第二节

# Join分区方式与关系

先说结论

## Join结果的分区方式

两个RDD join时，结果RDD(以下记为RDDX)的分区器，分区数，以及与两个父RDD的依赖是如何确定的？ 

1. 当两个RDD都没有分区器时，RDDX使用默认的HashPartitioner,分区数取最大值，且与父RDD是shuffleDependency
2. 当两个RDD其中有一个有分区器时，RDDX的分区方式与它保持一致(分区器和分区数都一样)，从而与它是OneToOneDependency，与另一个RDD是shuffleDependency
3. 当两个RDD都有分区器，且分区数不一样时，RDDX的分区器与分区数大的那个RDD保持一致，从而与它是OneToOneDependency，与另一个RDD是shuffleDependency
4. 当两个RDD都有分区器，且分区数一样，但分区器不一样时， RDDX的分区器优先取HashPartitioner(父RDD有的话)，从而与它是OneToOneDependency，与另一个RDD是shuffleDependency
5. 当两个RDD都有分区器，都是默认的HashPartitioner且分区数都一样时，RDDX的分区方式与它们保持一致，从而与两个RDD都是OneToOneDependency

   > 自定义分区器不能达到这个目的

> 有的transformation例如map会丢失partitioner哦

## case 4

```scala
    val col1 = Range(1, 5000).map(idx => (random.nextInt(10), s"user$idx"))
    val col2 = Array((0, "BJ"), (1, "SH"), (2, "GZ"), (3, "SZ"), (4, "TJ"), (5, "CQ"), (6, "HZ"), (7, "NJ"), (8, "WH"), (0,"CD"))


    val rdd1: RDD[(Int, String)] = sc.makeRDD(col1)
      .partitionBy(new HashPartitioner(3))
    val rdd2: RDD[(Int, String)] = sc.makeRDD(col2)
      .map(r => (r._1, r._2 + "__"))
      .partitionBy(new MyPartitioner(4))
    println("rdd1 的partitioner")
    println(rdd1.partitioner.getOrElse("没有"))
    println("rdd2 的partitioner")
    println(rdd2.partitioner.getOrElse("没有"))

    val joined: RDD[(Int, (String, String))] = rdd1.join(rdd2)
    println("joined依赖")
    joined.dependencies.foreach(println(_))
    println("joined分区器"+joined.partitioner)
    println("joined分区数"+joined.partitioner.get.numPartitions)
    println(joined.toDebugString)
    println(joined.count())
默认并行度8
rdd1 的partitioner
org.apache.spark.HashPartitioner@3
rdd2 的partitioner
com.xxx.bigdata.rdd.MyPartitioner@16ade133
joined依赖
org.apache.spark.OneToOneDependency@69da0b12
joined分区器Some(com.xxx.bigdata.rdd.MyPartitioner@16ade133)
joined分区数4
(4) MapPartitionsRDD[7] at join at NoShuffleJoinTest.scala:62 []
 |  MapPartitionsRDD[6] at join at NoShuffleJoinTest.scala:62 []
 |  CoGroupedRDD[5] at join at NoShuffleJoinTest.scala:62 []
 +-(3) ShuffledRDD[1] at partitionBy at NoShuffleJoinTest.scala:39 []
 |  +-(8) ParallelCollectionRDD[0] at makeRDD at NoShuffleJoinTest.scala:38 []
 |  ShuffledRDD[4] at partitionBy at NoShuffleJoinTest.scala:42 []
 +-(8) MapPartitionsRDD[3] at map at NoShuffleJoinTest.scala:41 []
    |  ParallelCollectionRDD[2] at makeRDD at NoShuffleJoinTest.scala:40 []
5014
```



![image-20221116170523489](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221116170523489.png)

![image-20221116170905333](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221116170905333.png)

Stage：包含数据读取、数据处理、数据写出

Description列显示的是stage最后一个tranformation的名字，<font color=red>stage内task个数只跟该stage要处理的RDD的分区数一致</font>

1. **重分区操作决定了待生成的RDD的分区数，也决定了该操作所在Stage的Task数。**
   **partitionBy产生的RDD的分区数由用户指定，由于需要Shuffle操作，partitionBy算子被划到下一个Stage, 该Stage包含的task个数就是重分区后的分区数。** <u>这些task各自负责当前RDD一个分区后续的处理：计算以及分区。</u>
1. stage里如果只有一个算子，那一定是shuffle类算子，该stage的task个数就是这个shuffle算子指定的分区数(就是说该shuffle算子要求产生的RDD的分区数，也是当前stage reader的个数)。
2. 如果stage里有多个算子，则除了第一个算子是shuffle类算子(除开读文件的情况)外，后续都是transformation，那么该stage的task个数就是第一个算子的并行度，也就是该Stage处理的第一个RDD的分区数



## case 5: 不需要shuffle 的join

```scala
    val col1 = Range(1, 50).map(idx => (random.nextInt(10), s"user$idx"))
    val col2 = Array((0, "BJ"), (1, "SH"), (2, "GZ"), (3, "SZ"), (4, "TJ"), (5, "CQ"), (6, "HZ"), (7, "NJ"), (8, "WH"), (0, "CD"))
    val rdd1: RDD[(Int, String)] = sc.makeRDD(col1)
    val rdd2: RDD[(Int, String)] = sc.makeRDD(col2)

    println(rdd1.toDebugString)
    println(rdd2.toDebugString)
    val rdd4: RDD[(Int, (String, String))] =
      rdd1
        .partitionBy(new HashPartitioner(3))
        .join(
          rdd2
            .partitionBy(new HashPartitioner(3)))

    println(rdd4.dependencies)
    println(rdd4.count())
```

![image-20230515163821147](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230515163821147.png)



## case 6分区器丢失

我们对case 4 中rdd2做一个修改，把map放到partitionBy后面

```sql
    val rdd2: RDD[(Int, String)] = sc.makeRDD(col2)
      .partitionBy(new MyPartitioner(3))
      .map(r=>(r._1,r._2+"__"))
默认并行度8
rdd1 的partitioner
org.apache.spark.HashPartitioner@3
rdd2 的partitioner
没有
joined依赖
org.apache.spark.OneToOneDependency@73ad4ecc
joined分区器Some(org.apache.spark.HashPartitioner@3)
joined分区数3
(3) MapPartitionsRDD[7] at join at NoShuffleJoinTest.scala:62 []
 |  MapPartitionsRDD[6] at join at NoShuffleJoinTest.scala:62 []
 |  CoGroupedRDD[5] at join at NoShuffleJoinTest.scala:62 []
 |  ShuffledRDD[1] at partitionBy at NoShuffleJoinTest.scala:39 []
 +-(8) ParallelCollectionRDD[0] at makeRDD at NoShuffleJoinTest.scala:38 []
 +-(4) MapPartitionsRDD[4] at map at NoShuffleJoinTest.scala:42 []
    |  ShuffledRDD[3] at partitionBy at NoShuffleJoinTest.scala:41 []
    +-(8) ParallelCollectionRDD[2] at makeRDD at NoShuffleJoinTest.scala:40 []
5010      
```

rdd2的分区器是什么呢？ 答案是没有！因为map操作不保留分区器， 而mapValues操作保留分区器，因为它不会改变key。

所以，尽量在重分区前进行map；或者重分区后采用mapValues这样的算子来保留分区器。

同理，mapPartition/ flatMap也会丢失分区器





# 关于shuffle的理解：

在 Spark 中，Shuffle Read 是由 Reduce Task（即 ShuffleMapTask 的后续任务）执行的，而不是 ShuffleMapTask 本身。

1. **ShuffleMapTask（Map 阶段）：** 在 Spark 中，一个 Spark 作业通常由多个阶段组成，其中包括 Map 阶段和 Reduce 阶段。在 Map 阶段，Spark 会将数据按照某个键进行分区，这些分区会被发送到不同的 Executor 上执行。这个过程就是 ShuffleMapTask。
2. **Shuffle Write（Shuffle 阶段）：** Map 阶段执行完毕后，数据需要通过网络进行混洗（Shuffle），以便按照键重新分布到不同的 Executor 上，以供后续的 Reduce 阶段使用
3. **Shuffle Read（Reduce 阶段）：** Reduce 阶段是 Shuffle 过程的一部分，这时每个 Reduce Task 会通过网络从多个 Map Task 的输出中读取它所需的数据，这个过程就是 Shuffle Read。

5. 一个Job可以有多个Map Stage,但只有一个Result Stage，包含了一个或多个ResultTask。 但是注意，ResultTask 不仅仅出现在 Result 阶段，也会在 Reduce 阶段的其他阶段中出现，例如，在进行 Shuffle Write 阶段时，会有 ResultTask 处理数据的写入。

   

# Spark作业并行度

## 默认并行度spark.default.parallelism

首先，**并行度**是指同时运行的task个数,与task个数或者RDD的分区数无关

具体到某个stage中，并行度是多少呢？ 

>  我们知道，transformation操作并不会改变分区数，也不会改变并行度，也不会划分stage。 所以一个stage一定是读取外部文件为一个RDD或者是遇到shuffle操作时读取上一个stage的输出(也就是上一个stage的最后一个RDD的分区)

分两种情况

对于没有父RDD的的算子，在创建RDD又没有设置分区数时，比如parallelize（或makeRDD），默认并行度依赖Spark运行的模式。

（1）local模式

在 Spark 的 local 模式下，并行度的设置主要通过调整 SparkContext 的 `local[*]` 参数来实现。

`*` 可以替换为一个数字，用来指定本地启动的线程数。如果使用 `local` 模式，Spark 会使用单个线程来运行任务，而 `local[*]` 则会使用所有可用的 CPU 核心来运行任务。

但是需要注意的是，在 `local[*]` 模式下，并行度并不等于 CPU 核心数。这是因为 `local[*]` 模式会将线程分配给不同的本地执行器（local executor），而每个本地执行器的 CPU 核心数是不确定的。

在实际使用中，可以根据任务的复杂度和机器的性能来调整并行度。如果任务比较简单，可以适当减少并行度，以充分利用机器资源。如果任务比较复杂，可以适当增加并行度，以加速任务执行。

local: 没有指定CPU核数，则所有计算都运行在一个线程当中，没有任何并行计算
local[K]:指定使用K个Core来运行计算，比如local[2]就是运行2个Core来执行
local[*]: 自动帮你按照CPU的核数来设置线程数。比如CPU有4核，Spark帮你自动设置4个线程计算。

(2) 集群模式

默认并行度取决于所有executor上的总核数与2的最大值

1.  `reduceByKey` 、 `join  ` 这类shuffle操作，其task个数是父RDD的分区数的最大值，但是并行度是min(父RDD的分区数的最大值，spark.default.parallelism),    当然我们可以手动指定并行度；
2. 读取外部文件，其默认并行度是文件块(slice，不能跨文件)个数





Spark中所谓的并行度是指RDD中的分区数，即RDD中的Task数。

当初始RDD没有设置分区数（numPartitions或numSlice）时，则分区数采用spark.default.parallelism的取值。

Spark作业并行度的设置代码如下：

```scala
val conf = new SparkConf()
  .set("spark.default.parallelism", "500")
```



1. spark.sql.shuffle.partitions: 设置的是 RDD1做shuffle处理后生成的结果RDD2的分区数.
   默认值: 200

对于Spark SQL中的shuffle类语句，比如group by、join等，需要设置一个参数，即spark.sql.shuffle.partitions，该参数代表了shuffle read task的并行度

推荐一个core至少分配2-3个task， 每个task处理100-200MB数据

spark基于输入文件大小决定分区数，可通过spark.sql.files.maxPartitionBytes调整

2. spark.default.parallelism: 设置的是 RDD1做shuffle处理/并行处理(窄依赖算子)后生成的结果RDD2的分区数

在对RDD执行shuffle算子时，给shuffle算子传入一个参数，比如reduceByKey(1000)，该参数就设置了这个**shuffle算子执行时shuffle read task的数量**。

默认值:

- 对于分布式的shuffle算子, 默认值使用了结果RDD2所依赖的所有父RDD中分区数最大的, 作为自己的分区数.
- 对于并行处理算子(窄依赖的), 有父依赖的, 结果RDD分区数=父RDD分区数, 没有父依赖的看集群配置:
  - Local mode:给定的core个数
  - Mesos fine grained mode: 8
  - Others: max(RDD分区数为总core数, 2)

并行度其实就是指的是spark作业中, 各个stage的taskset中的task的数量, 代表了spark作业中各个阶段的并行度, 而taskset中的task数量 = task任务的父RDD中分区数。

## [如何高效利用CPU](https://learn.lianglianglee.com/%E4%B8%93%E6%A0%8F/Spark%E6%80%A7%E8%83%BD%E8%B0%83%E4%BC%98%E5%AE%9E%E6%88%98/14%20CPU%E8%A7%86%E8%A7%92%EF%BC%9A%E5%A6%82%E4%BD%95%E9%AB%98%E6%95%88%E5%9C%B0%E5%88%A9%E7%94%A8CPU%EF%BC%9F.md)

1. 并行度
   指的是为了实现分布式计算，分布式数据集被划分出来的份数,并行度可以通过两个参数来设置，**分别是spark.default.parallelism和spark.sql.shuffle.partitions**。前者用于设置RDD的默认并行度，后者在Spark SQL开发框架下，指定了Shuffle Reduce阶段默认的并行度。
2. 并发度
   Executor的线程池大小由参数spark.executor.cores决定，每个任务在执行期间需要消耗的线程数由spark.task.cpus配置项给定。两者相除得到的商就是并发度，也就是同一时间内，一个Executor内部可以同时运行的最大任务数量。又因为，spark.task.cpus默认数值为1，并且通常不需要调整，所以，**并发度基本由spark.executor.cores参数敲定**。
3.  线程/任务/分区对应关系
   就Executor的线程池来说，尽管线程本身可以复用，但每个线程在同一时间只能计算一个任务，每个任务负责处理一个数据分片。因此，**在运行时，线程、任务与分区是一一对应的关系**。
4. 执行内存计算公式：
   spark.executor.memory * spark.memory.fraction * (1 - spark.memory.storageFraction)。相比之下，堆外执行内存的计算稍微简单一些：spark.memory.offHeap.size * (1 - spark.memory.storageFraction)

### CPU低效原因之一：线程挂起

在给定执行内存总量M和**线程总数**N的情况下，为了保证每个线程都有机会拿到适量的内存去处理数据，Spark用HashMap数据结构，以（Key，Value）的方式来记录每个线程消耗的内存大小，并确保所有的Value值都不超过M/N。在一些极端情况下，**有些线程申请不到所需的内存空间，能拿到的内存合计还不到M/N/2。这个时候，Spark就会把线程挂起，直到其他线程释放了足够的内存空间为止。**

Task线程内存分布：Spark确保每个线程分配的内存区间为[  M/N/2,    M/N ], 其中：

- M代表执行内存，是动态变化的，Executor启动时，M达到上限spark.executor.memory * spark.memory.fraction，随着RDD缓存逐渐填充Storage Memory，M的取值也会跟着回撤。 
- N~代表Executor当前的并发度，也就是Executor中当前并行执行的任务数，N~ <=N，也是动态变化的，但这个一般很快达到N。
- 除此之外，分布式数据集的并行度会影响到每个task需要申请的内存量，每个task申请的内存需要足够小(M/N~, N~足够大)，有限的内存总量才能同时容纳足够多的task来让所有的线程调度起来(否则Executor的线程池会有空闲的线程)。

这三者都是动态变化的， 所以会导致某些极端情况下，有些线程申请不到M/N/2的内存空间

### CPU低效原因之二：调度开销

**数据过于分散会带来严重的副作用：调度开销骤增。**

对于每一个分布式任务，Dirver会将其封装为TaskDescription，然后分发给各个Executor。TaskDescription包含着与任务运行有关的所有信息，如任务ID、尝试ID、要处理的数据分片ID、开发者添加的本地文件和Jar包、任务属性、序列化的任务代码等等。Executor接收到TaskDescription之后，首先需要对TaskDescription反序列化才能读取任务信息，然后将任务代码再反序列化得到可执行代码，最后再结合其他任务信息创建TaskRunner。

### 如何优化CPU利用率？

计算一个能够让数据分片平均大小在（M/N/2, M/N）之间的并行度，这往往是个不错的选择。

总的来说，对CPU利用率来说，并行度、并发度与执行内存的关系就好像是一尊盛满沸水的三足鼎，三足齐平则万事大吉，但凡哪一方瘸腿儿，鼎内的沸水就会倾出伤及无辜。

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/d89eaad68ce4451ab57536d785c886cd.jpg)

## 实验

[并行度性能测试](https://www.confessionsofadataguy.com/how-do-spark-shuffle-configurations-affect-performance/)
