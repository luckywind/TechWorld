# 列操作

## 创建DF

```scala

val spark:SparkSession = SparkSession.builder()
   .master("local[1]").appName("SparkByExamples.com")
   .getOrCreate()

import spark.implicits._
val columns = Seq("language","users_count")
val data = Seq(("Java", "20000"), ("Python", "100000"), ("Scala", "3000"))

val rdd = spark.sparkContext.parallelize(data)


//一使用rdd， 也可指定列名
rdd.toDF()
//二使用createDataFrame
val dfFromRDD2 = spark.createDataFrame(rdd).toDF(columns:_*)
//三使用Row type

import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.Row
val schema = StructType( Array(
                 StructField("language", StringType,true),
                 StructField("language", StringType,true)
             ))
val rowRDD = rdd.map(attributes => Row(attributes._1, attributes._2))
val dfFromRDD3 = spark.createDataFrame(rowRDD,schema)
//四
val df2 = spark.read.csv("/src/resources/file.csv")
//五

val df2 = spark.read
.text("/src/resources/file.txt")
//六

val df2 = spark.read
.json("/src/resources/file.json")
//七

val df = spark.read
      .format("com.databricks.spark.xml")
      .option("rowTag", "person")
      .xml("src/main/resources/persons.xml")
//八 hive

val hiveContext = new org.apache.spark.sql.hive.HiveContext(spark.sparkContext)
val hiveDF = hiveContext.sql(“select * from emp”)

```

## 列操作

```scala
// 一创建和更新
withColumn(colName : String, col : Column) : DataFrame
可用来创建新列，也可直接更新列，只需要colname和原来一样即可

如果一次修改多个列，建议先建一个临时表，然后用sql操作，性能比多个withColumn好
df2.createOrReplaceTempView("PERSON")
spark.sql("SELECT salary*100 as salary, salary*-1 as CopiedColumn, 'USA' as country FROM PERSON").show()

//改类型
 df.withColumn("salary",col("salary").cast("Integer"))
//改名
df.withColumnRenamed("gender","sex")
//删
df.drop("CopiedColumn")
```



# StructType/StructField

这俩API用于指定DF的schema

1. StructField可指定列名/列类型、是否可空等元数据

2. StructType是StructField的集合，且可嵌套定义

## 构造schema

```scala

  val structureData = Seq(
    Row(Row("James ","","Smith"),"36636","M",3100),
    Row(Row("Michael ","Rose",""),"40288","M",4300),
    Row(Row("Robert ","","Williams"),"42114","M",1400),
    Row(Row("Maria ","Anne","Jones"),"39192","F",5500),
    Row(Row("Jen","Mary","Brown"),"","F",-1)
  )

  val structureSchema = new StructType()
    .add("name",new StructType()
      .add("firstname",StringType)
      .add("middlename",StringType)
      .add("lastname",StringType))
    .add("id",StringType)
    .add("gender",StringType)
    .add("salary",IntegerType)

  val df2 = spark.createDataFrame(
     spark.sparkContext.parallelize(structureData),structureSchema)
  df2.printSchema()
  df2.show()
```

## 更新schema

```scala
//把id/gender/salary拷贝到一个新的struct结构里，新增一个列，并删除原始列
 val updatedDF = df4.withColumn("OtherInfo", 
    struct(  col("id").as("identifier"),
    col("gender").as("gender"),
    col("salary").as("salary"),
    when(col("salary").cast(IntegerType) &lt 2000,"Low")
      .when(col("salary").cast(IntegerType) &lt 4000,"Medium")
      .otherwise("High").alias("Salary_Grade")
  )).drop("id","gender","salary")

  updatedDF.printSchema()
  updatedDF.show(false)

root
 |-- name: struct (nullable = true)
 |    |-- firstname: string (nullable = true)
 |    |-- middlename: string (nullable = true)
 |    |-- lastname: string (nullable = true)
 |-- OtherInfo: struct (nullable = false)
 |    |-- identifier: string (nullable = true)
 |    |-- gender: string (nullable = true)
 |    |-- salary: integer (nullable = true)
 |    |-- Salary_Grade: string (nullable = false)

```

## 使用Array/Map

```scala

  val arrayStructureData = Seq(
    Row(Row("James ","","Smith"),List("Cricket","Movies"),Map("hair"->"black","eye"->"brown")),
    Row(Row("Michael ","Rose",""),List("Tennis"),Map("hair"->"brown","eye"->"black")),
    Row(Row("Robert ","","Williams"),List("Cooking","Football"),Map("hair"->"red","eye"->"gray")),
    Row(Row("Maria ","Anne","Jones"),null,Map("hair"->"blond","eye"->"red")),
    Row(Row("Jen","Mary","Brown"),List("Blogging"),Map("white"->"black","eye"->"black"))
  )

  val arrayStructureSchema = new StructType()
    .add("name",new StructType()
      .add("firstname",StringType)
      .add("middlename",StringType)
      .add("lastname",StringType))
    .add("hobbies", ArrayType(StringType))
    .add("properties", MapType(StringType,StringType))

  val df5 = spark.createDataFrame(
     spark.sparkContext.parallelize(arrayStructureData),arrayStructureSchema)
  df5.printSchema()
  df5.show()

```

## Case class转StructType

spark提供了编码器Encoders来执行转换，老版本可使用scala hack

```scala

  case class Name(first:String,last:String,middle:String)
  case class Employee(fullName:Name,age:Integer,gender:String)

  import org.apache.spark.sql.catalyst.ScalaReflection
  val schema = ScalaReflection.schemaFor[Employee].dataType.asInstanceOf[StructType]

  val encoderSchema = Encoders.product[Employee].schema
  encoderSchema.printTreeString()

root
 |-- fullName: struct (nullable = true)
 |    |-- first: string (nullable = true)
 |    |-- last: string (nullable = true)
 |    |-- middle: string (nullable = true)
 |-- age: integer (nullable = true)
 |-- gender: string (nullable = true)

```

















# join

数据框可以起别名，来引用指定表里的字段：[参考](https://stackoverflow.com/questions/40343625/joining-spark-dataframes-on-the-key)

```scala
    val spark = SparkSession.builder().config(new SparkConf().setMaster("local[*]"))
      .getOrCreate()
    import spark.implicits._
    val data1 = Seq((8, "bat"),(64, "mouse"),(-27, "horse"))
    val data2 = Seq((8, "a"),(62, "b"),(-23, "c"))


    val df1: DataFrame = spark.sparkContext.parallelize(data1).toDF("number", "word").as("t1")
    val df2: DataFrame = spark.sparkContext.parallelize(data2).toDF("number", "r").as("t2")
    df1.join(df2,df1("number")===df2("number"),"left")

      .selectExpr("t1.number","word","r")
      .show()
```



[join类型](https://blog.csdn.net/anjingwunai/article/details/51934921)

“left”,”left_outer”或者”leftouter”代表左连接

“right”,”right_outer”及“rightouter”代表右连接

“full”,”outer”,”full_outer”,”fullouter”代表全连接

# Group by

[官方文档](https://spark.apache.org/docs/latest/sql-ref-syntax-qry-select-groupby.html)

## sql的语法：

```sql
GROUP BY group_expression [ , group_expression [ , ... ] ] [ WITH { ROLLUP | CUBE } ]

GROUP BY { group_expression | { ROLLUP | CUBE | GROUPING SETS } (grouping_set [ , ...]) } [ , ... ]
```

聚合函数

```sql
aggregate_name ( [ DISTINCT ] expression [ , ... ] ) [ FILTER ( WHERE boolean_expression ) ]
```

参数：

1. **group_expression**

可以是group by 列/列下标/表达式(例如a+b)

2. grouping_set

通过()包含的0个或逗号分隔的多个表达式，只有一个时可省略(),例如：  `GROUPING SETS ((a), (b))` 和 `GROUPING SETS (a, b)` 是一样的。

语法： { ( [ expression [ , ... ] ] ) | expression }

3. **GROUPING SETS**

GROUPING SETS后的每个grouping set的聚合，例如：

 `GROUP BY GROUPING SETS ((warehouse, product), (product), ())` 等价于  `GROUP BY warehouse, product`, `GROUP BY product` 和全局聚合结果的union

## dataframe的语法

通常是groupBy算子后面跟着agg算子, 最终结果只会保留groupBy的表达式和agg里的聚合结果列，其余列和sql一样丢弃

groupBy:

```sql
def groupBy(col1: String, cols: String*): RelationalGroupedDataset
def groupBy(cols: Column*): RelationalGroupedDataset
```

Agg:

```sql
def agg(expr: Column, exprs: Column*): DataFrame
def agg(exprs: Map[String, String]): DataFrame
这个蛮有意思，就是key为字段名，value为聚合函数，例如：
df.groupBy("key1").agg("key1"->"count", "key2"->"max", "key3"->"avg")
def agg(aggExpr: (String, String),   aggExprs: (String, String)*): DataFrame
```

注意：很多内置聚合函数在包import org.apache.spark.sql.functions._ 里，注意引入

例如

```scala
    import spark.implicits._
    val df = spark.createDataset(Seq(
      ("aaa",1,2),("bbb",3,4),("ccc",3,5),("bbb",4, 6))   ).toDF("key1","key2","key3")
    df.printSchema()

/*
等价sql:
select key1, count(key1), max(key2), avg(key3)
from table
group by key1
*/
    val agg: DataFrame = df.groupBy("key1").agg(count("key1"), max("key2"), avg("key3"))
    agg.printSchema()
    agg.show()
root
 |-- key1: string (nullable = true)
 |-- key2: integer (nullable = false)
 |-- key3: integer (nullable = false)

root
 |-- key1: string (nullable = true)
 |-- count(key1): long (nullable = false)
 |-- max(key2): integer (nullable = true)
 |-- avg(key3): double (nullable = true)

+----+-----------+---------+---------+
|key1|count(key1)|max(key2)|avg(key3)|
+----+-----------+---------+---------+
| ccc|          1|        3|      5.0|
| aaa|          1|        1|      2.0|
| bbb|          2|        4|      5.0|
+----+-----------+---------+---------+
```

# 选取列

## select

```scala
ds.select($"colA", $"colB" + 1)     选取column表达式
ds.select("colA", "colB")        根据列名选取
```



带类型

```scala
    val ds = Seq(1, 2, 3).toDS()
    val newDS = ds.select(expr("value + 1").as[Int])
```



## selectExpr

选取sql表达式，是select的变体

```scala
下面语句等价   
ds.selectExpr("colA", "colB as newName", "abs(colC)")
ds.select(expr("colA"), expr("colB as newName"), expr("abs(colC)"))
```

