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

![img](https://gitee.com/luckywind/PigGo/raw/master/image/1620.png)

三个stage，从下面看到一共产生一个Job,3个stage,30个task； 这是因为每个stage包含10个分片产生10个task， 30=3*10

![image-20220503092817614](https://gitee.com/luckywind/PigGo/raw/master/image/image-20220503092817614.png)

![image-20220503092843931](https://gitee.com/luckywind/PigGo/raw/master/image/image-20220503092843931.png)

从时间线发现stage0/1重叠度很大，说明它们是并行的，stage2是在它们之后运行的，因为它依赖stage0/1。

![image-20220503092904653](https://gitee.com/luckywind/PigGo/raw/master/image/image-20220503092904653.png)

reduceByKey是shuffle操作，该操作分别是stage0/1的最后一个操作，但是产生的RDD划分到shuffle操作的下一个stage里了。stage0/1在该shuffle过程中都进行了write操作，写入了480B的数据，Stage2读取它们俩的输出，产生960B的Shuffle Read。