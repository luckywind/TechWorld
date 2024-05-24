# RDD

R： Resilient代表容错，

DD： Distributed Dataset分布式数据集



分布式数据集，是Spark中最基本的数据抽象，代表一个不可变、可分区、元素可并行计算的集合。

具有数据流模型的特点： 自动容错、位置感知性和可伸缩性。

RDD允许用户在执行多个查询时显示地将工作集缓存在内存中，便于后续使用。

## 属性

1. 一组分片，即数据集的基本组成单位。

每个分片都会被一个计算任务处理，并决定并行计算的粒度。用户可指定分片数，默认就是程序分配到的CPU C o re的数目。

2. 一个计算每个分区的函数

RDD的计算是以分片为单位的，每个RDD都会实现compute函数以达到这个目的，compute函数会对迭代器进行复合，不需要保存每次计算的结果。

3. RDD之间的依赖关系

RDD每次转换都会进行一个新的RDD，所以RDD之间会形成类似于流水线一样的前后依赖关系，在部分分区丢失时，Spark可以

通过这个依赖关系重新计算丢失的分区数据，而不是对RDD的所有分区重新计算。

4. 一个Partitioner，即RDD的分片函数

Spark中实现了两种类型的分片函数，一个是基于哈希的HashPartitioner，另外一个是基于范围的RangePartitioner。只有

对于key-value的RDD才会有Partitioner。Partitioner函数不但决定了RDD本身的分片数量，也决定了parent RDD shuffle输出的分片数量。

5. 一个列表，存储每个Partition的优先位置，对于一个HDFS文件来说，这个列表保存的就是每个Partition所在的块位置。

   Saprk尽量将计算任务分配到其所要处理数据块的存储位置。

```scala
    val sc = new SparkContext(conf)

    val lines: RDD[String] = sc.textFile(args(0))
    val words: RDD[String] = lines.flatMap(_.split(" "))
    val paired: RDD[(String, Int)] = words.map((_,1))
    val reduced: RDD[(String, Int)] = paired.reduceByKey(_+_)
    val res: RDD[(String, Int)] = reduced.sortBy(_._2,false)
    //保存

    res.saveAsTextFile(args(1))
//    println(res.collect().toBuffer)
    //结束任务
    sc.stop()
```

## RDD分区数如何确定

spark读[hdfs](https://so.csdn.net/so/search?q=hdfs&spm=1001.2101.3001.7020)文件的分区数由hdfs文件占用的文件块数决定。

shuffle算子可指定产生的RDD的分区数

# 生成RDD的两种方式

```scala
sc.textFile
sc.parallize()
```

