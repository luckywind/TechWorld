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

