# code

```scala
    val conf = new SparkConf().setAppName("local-test").setMaster("local")
    val sc = new SparkContext(conf)
    val spark = SparkSession
      .builder()
      .appName(this.getClass.getSimpleName)
      .enableHiveSupport()
      .getOrCreate()

    val rdd1: RDD[String] = sc.parallelize(Seq("a c","a b","b c","b d","c d"),10)
    val word_count1 = rdd1.flatMap(a=>a.split(' ')).map(a=>(a,1)).reduceByKey((x,y)=>x+y)
    val rdd2: RDD[String] = sc.parallelize(Seq("a c","a b","b c","b d","c d"),10)
    val word_count2 = rdd2.flatMap(a=>a.split(' ')).map(a=>(a,1)).reduceByKey((x,y)=>x+y)
    word_count1.join(word_count2).collect()
    Thread.sleep(10000*1000)
```

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/1620.png)

三个stage，从下面看到一共产生一个Job,3个stage,30个task； 这是因为每个stage包含10个分片产生10个task， 30=3*10

![image-20220503092817614](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220503092817614.png)

![image-20220503092843931](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220503092843931.png)

从时间线发现stage0/1重叠度很大，说明它们是并行的，stage2是在它们之后运行的，因为它依赖stage0/1。

![image-20220503092904653](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220503092904653.png)

reduceByKey是shuffle操作，该操作分别是stage0/1的最后一个操作，但是产生的RDD划分到shuffle操作的下一个stage里了。stage0/1在该shuffle过程中都进行了write操作，写入了480B的数据，Stage2读取它们俩的输出，产生960B的Shuffle Read。

# Join分区方式与关系

两个RDD join时，结果RDD(以下记为RDDX)的分区器，分区数，以及与两个父RDD的依赖是如何确定的？ 

1. 当两个RDD都没有分区器时，RDDX使用默认的HashPartitioner,分区数取最大值，且与父RDD是shuffleDependency
2. 当两个RDD其中有一个有分区器时，RDDX的分区方式与它保持一致(分区器和分区数都一样)，从而与它是OneToOneDependency，与另一个RDD是shuffleDependency
3. 当两个RDD都有分区器，且分区数不一样时，RDDX的分区器与分区数大的那个RDD保持一致，从而与它是OneToOneDependency，与另一个RDD是shuffleDependency
4. 当两个RDD都有分区器，且分区数一样，但分区器不一样时， RDDX的分区器优先取HashPartitioner(父RDD有的话)，从而与它是OneToOneDependency，与另一个RDD是shuffleDependency
5. 当两个RDD都有分区器，且分区器与分区数都一样时，RDDX的分区方式与它们保持一致，从而与两个RDD都是OneToOneDependency

> 有的transformation例如map会丢失partitioner哦



![image-20221116170523489](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221116170523489.png)

![image-20221116170905333](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221116170905333.png)

Description列显示的是stage最后一个tranformation的名字，stage内task个数只跟该stage的最开始的RDD的分区数有关系

1. stage里如果只有一个算子，那一定是shuffle类算子，该stage的task个数就是这个shuffle算子指定的分区数。
2. 如果stage里有多个算子，则除了第一个算子是shuffle类算子(除开读文件的情况)外，后续都是transformation，那么该stage的task个数就是第一个算子的并行度，也就是第一个RDD的分区数

# Spark作业并行度

## 默认并行度spark.default.parallelism

首先，**并行度**是指同时运行的task个数,与task个数或者RDD的分区数无关

具体到某个stage中，并行度是多少呢？ 

>  我们知道，transformation操作并不会改变分区数，也不会改变并行度，也不会划分stage。 所以一个stage一定是读取外部文件为一个RDD或者是遇到shuffle操作时读取上一个stage的输出(也就是上一个stage的最后一个RDD的分区)

分两种情况

对于没有父RDD的的算子，在创建RDD又没有设置分区数时，比如parallelize（或makeRDD），默认并行度依赖Spark运行的模式。

（1）local模式

默认并行度取决于本地机器的核数，即

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

