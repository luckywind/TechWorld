# 数据框使用

1. 修改日志级别

```scala
val sparkSession = SparkSession
    .builder()
    .config(sparkConf)
    .getOrCreate()
sparkSession.sparkContext.setLogLevel("WARN")
```

## stat

### 相似分位数

stat函数提供了一些高级的统计方法。[官方文档](https://spark.apache.org/docs/2.0.2/api/java/org/apache/spark/sql/DataFrameStatFunctions.html)

approxQuantile()的第一个参数是列名，第二个参数是分位数概率，第三个参数是准确率因子。 例如，我们想要找出分数的最低值、中位数和最高值，需要传递一个Array(0,0.5,1)

```scala
  val quantiles = dfQuestions
    .stat
    .approxQuantile("score", Array(0, 0.5, 1), 0.25)
    println(s"Qauntiles segments = ${quantiles.toSeq}")
Qauntiles segments = WrappedArray(-27.0, 2.0, 4443.0)
```

## 创建

```scala
// 从seq创建
val donuts = Seq(("plain donut", 1.50), ("vanilla donut", 2.0), ("glazed donut", 2.50))
val df= sparkSession.createDataFrame(donuts).toDF("Donut Name","Price")

val rdd=spark.sparkContext.parallelize(Seq(("Java", 20000), 
  ("Python", 100000), ("Scala", 3000)))
rdd.foreach(println)


// 从json
  val tagsDF = sparkSession.read
    .option("multiLine", true)
    .option("inferSchema", true)// 推断每一列的类型
    .json("src/main/resources/tags_sample.json")
//从csv
  val dfTags = sparkSession
    .read
    .option("header", "true")
    .option("inferSchema", "true")
    .csv("src/main/resources/question_tags_10K.csv")
    .toDF("id", "tag")
```



## 常用接口

```scala
//列类型
val (columns, columnDataTypes) = df.dtypes.unzip
//所有列
df.columns
//打印schema
df.printSchema()
//过滤
df.filter
//列重命名
df.withColumnRenamed
//新增列
df.withColumn
//分组统计
df.groupBy("xx").聚合函数()
//修改列类型
df.select(old_df.col("id").cast("integer"))
//转成case class
1. df.as[类名]
2. 使用map
df.map(row=>toCaseClass(row)) //需要写一个函数toCaseClass
//注册为临时表
df.createOrReplaceTempView
//udf
sparkSession.udf.register("函数名",函数定义 _)


```

函数

```scala
sql.function包：
//值为数组的列包含判断
array_contains
//字符串函数
instr   返回子字符串的位置，下标从1开始算，未出现返回0
length
trim
ltrim
rtrim
reverse
substring
isnull
concat_ws
initcap
split
```

# df->rdd

```scala
  val frame: DataFrame = spark.read.parquet(idMappingConfig.DWM_DVC_DEVICE_CHAIN + suffix) //.na.drop()
   val res: DataFrame = frame.select("origin_device_id", "user_id", "start_day", "prod_cat_name", "sn", "del_flg").na.fill("")
 res.rdd.map {
                    case Row(origin_device_id: String,user_id: Long,start_day: Long,prod_cat_name: String,sn: String,del_flg: Int) => {
                        DVC_OT_CHAIN(origin_device_id, user_id, start_day.toString, prod_cat_name, end_day, sn, del_flg)
                    }
                }
```

# 小表join优化

```scala
    val  dimDF = ot_dim_df.rdd.map{
      case Row(prod_cat_name:String,prod_cat_id:Int)=>{
        (prod_cat_name,prod_cat_id)
      }
    }.repartition(partitionNum).collectAsMap()
    val brodDimDF: Broadcast[collection.Map[String, Int]] = getSparkContext.broadcast(dimDF)

    val final_increament= increament_edge.map(
      edge=>{
        val prod_cat_name: String = if (edge.extend_map != null) edge.extend_map.getOrDefault("prod_cat_name", "") else ""
        if (brodDimDF.value.contains(prod_cat_name)) {
          edge.putToExtend_map(KeyConstant.PROD_CAT_ID,brodDimDF.value.get(prod_cat_name).get.toString)
        }
        edge
      }
    )
```

# dataframe处理

## 转rdd

1. 按下标获取字段: get(index) 从0开始
2. `getAs[类型](字段名称) ` 强转，例如null强转Long会转成0
3. getString/getLong等，当无法转换时会报错，例如null不能转成Long，但可以转成string(值为null)

```scala
res.rdd.map{
      e=>
         val value: Any = e.get(1)
        println(value) //null
        if (value == null) {
          println("=====")
          val tmp: Long = e.getAs[Long]("user_id")
          println(tmp)  //0
          val getstring: String = e.getString(1)
          println(getstring)   //null
//          println(e.getLong(1))//会报错java.lang.NullPointerException: Value at index 1 is null
        }
        e.getAs[Long]("user_id")
    }
```

# 语法

数组展开

```scala
filter($"event_name".isin(List("onetrack_upgrade", "onetrack_pa"): _*)
```

别名

```scala
$"properties.type".as("pa_type")
```

保存

```scala
.repartition(5)
.write.partitionBy("app_id") //分区
.mode(SaveMode.Overwrite)
.parquet(outputPath)
```

生成字段

```scala
lit(sdf.format(Calendar.getInstance().getTimeInMillis)).as("etl_tm")

.withColumn("stat", statEvents($"events"))
```

# rdd工具

## 查看分区元素数

```scala
object RDDUtils {
  def getPartitionCounts[T](sc : SparkContext, rdd : RDD[T]) : Array[Long] = {
    sc.runJob(rdd, getIteratorSize _)
  }
  def getIteratorSize[T](iterator: Iterator[T]): Long = {
    var count = 0L
    while (iterator.hasNext) {
      count += 1L
      iterator.next()
    }
    count
  }
}
```

# parquet



```scala
write.parquet(path)
```

