从Spark 2.0开始，Dataset开始具有两种不同类型的API特征：有明确类型的API和无类型的API。从概念上来说，你可以把DataFrame当作一些通用对象Dataset[Row]的集合的一个别名，而一行就是一个通用的无类型的JVM对象。与之形成对比，Dataset就是一些有明确类型定义的JVM对象的集合，通过你在Scala中定义的Case Class或者Java中的Class来指定。

DataSets是结构化API的基础类型，DataFrame是Row类型的Datasets，支持多语言。Datasets只支持基于JVM的Java和scala。使用Datasets,我们可以定义每一行构成的对象。

```scala
object DatasetDemo extends App with Context{
  val flightsDF = sparkSession.read
  .parquet("src/main/resources/data/flight-data/parquet/2010-summary.parquet/")
  import sparkSession.implicits._
  val flights = flightsDF.as[Flight]
  flights.show()
  
  println(flights.first.DEST_COUNTRY_NAME)
  println(flights.filter(flight_row => originIsDestination(flight_row)).first())

  def originIsDestination(flight_row: Flight):Boolean={
    return flight_row.ORIGIN_COUNTRY_NAME == flight_row.DEST_COUNTRY_NAME
  }
}
case class Flight(DEST_COUNTRY_NAME: String,
                  ORIGIN_COUNTRY_NAME: String, count: BigInt)

```

Dataset API的优点

1. 静态类型与运行时类型安全

Dataset在编译期就能发现错误，节省开发者的时间和代价

![image-20201022142227869](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20201022142227869.png)

2. 更易用的结构化API

事实上，用Dataset的高级API可以完成大多数的计算。比如，它比用RDD数据行的数据字段进行agg、select、sum、avg、map、filter或groupBy等操作简单得多，只需要处理Dataset类型的DeviceIoTData对象即可。

用一套特定领域内的API来表达你的算法，比用RDD来进行关系代数运算简单得多。比如，下面的代码将用filter()和map()来创建另一个不可变Dataset。

```scala
val dsAvgTmp = ds.filter(d => {d.temp > 25}).map(d => (d.temp, d.humidity, d.cca3)).groupBy($"_3").avg()


```

