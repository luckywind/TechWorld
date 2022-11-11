# 含义

co-partition: 分区器和分区数相同

co-location: 前提是co-partition，且是同一个action产生的两个RDD才是co-location的

# 优化点

spark的transformation是可能会丢掉分区器的，mapValues不会更改key，所以会保留分区信息;而map不会。



```scala
   val spark = SparkSession
      .builder()
      .master("local[4]")
      .appName(this.getClass.getSimpleName)
      .getOrCreate()
    val sc: SparkContext = spark.sparkContext
    sc.setLogLevel("WARN")

    val random = scala.util.Random
    val col1 = Range(1, 50).map(idx => (random.nextInt(10), s"user$idx"))
    val col2 = Array((0, "BJ"), (1, "SH"), (2, "GZ"), (3, "SZ"), (4, "TJ"), (5, "CQ"), (6, "HZ"), (7, "NJ"), (8, "WH"), (0,"CD"))


    val rdd1: RDD[(Int, String)] = sc.makeRDD(col1)
    println(rdd1.toDebugString)
    println(rdd1.partitioner.toString)
    val rdd1_hash3: RDD[(Int, String)] = rdd1.partitionBy(new HashPartitioner(3))
    println("-----hash到3个分区")
    println(rdd1_hash3.toDebugString)
    println("分区器:"+rdd1_hash3.partitioner.toString)


    val rdd1_hash3_mapp=rdd1_hash3.mapPartitions( //对Iterator[(Int, String)]进行处理
         x=>{
        x.toList.groupBy(_._1)
             .map(y=>(y._1,y._2.size))
             .iterator
    }, preservesPartitioning=true)
    println("-----mapPartition 丢失了分区器,但当加入参数preservesPartitioning=true后分区器可以保留")
    println(rdd1_hash3_mapp.toDebugString)
    println("分区器:"+rdd1_hash3_mapp.partitioner.toString)

    
    val rdd1_hash3_swap: RDD[(String, Int)] = rdd1_hash3.map(r=>{r.swap})
    println("-----map丢失了分区器(因为它可能改变了key)")
    println(rdd1_hash3_swap.toDebugString)
    println("分区器:"+rdd1_hash3_swap.partitioner.toString)



    val rdd1_hash3_mapValue= rdd1_hash3.mapValues(x=>x+"==")
    println("-----mapValues保留分区器")
    println(rdd1_hash3_mapValue.toDebugString)
    println("分区器:"+rdd1_hash3_mapValue.partitioner.toString)
```

