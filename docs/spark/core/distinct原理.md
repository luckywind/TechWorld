一个简单的例子

```scala
object SparkDistinct {
  def main(args: Array[String]): Unit = {
    val conf: SparkConf = new SparkConf().setMaster("local[4]")
    val spark: SparkSession = SparkSession.builder()
      .config(conf)
      .appName("TEST").getOrCreate()
    val sc: SparkContext = spark.sparkContext
    //定义一个数组
    val array: Array[Int] = Array(1,1,1,2,2,3,3,4)
    //把数组转为RDD算子,后面的数字2代表分区，也可以指定3，4....个分区，也可以不指定。
    val line: RDD[Int] = sc.parallelize(array,2)
    line.distinct().foreach(x => println(x))
    //输出的结果已经去重：1，2，3，4
  }

}
```



distinct方法：

> 有参方法参数用来指定reduceByKey算子产生的RDD的分区数
>
> 无参方法内部调用了有参，只是默认为partitions.length

```scala
  /**
   * Return a new RDD containing the distinct elements in this RDD.
   */
  def distinct(numPartitions: Int)(implicit ord: Ordering[T] = null): RDD[T] = withScope {
    map(x => (x, null)).reduceByKey((x, y) => x, numPartitions).map(_._1)
  }

  /**
   * Return a new RDD containing the distinct elements in this RDD.
   */
  def distinct(): RDD[T] = withScope {
    distinct(partitions.length)
  }
```

核心代码就是

```sql
map(x => (x, null)).reduceByKey((x, y) => x, numPartitions).map(_._1)
```

![image-20210924143420845](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210924143420845.png)

