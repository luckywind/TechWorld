# 简介

[Introduction to Spark SQL functions](https://mungingdata.com/apache-spark/spark-sql-functions/)

所有函数都存放在`org.apache.spark.sql.functions` object中

如果引入import spark.implicits._，则可通过$符号创建Column对象

```scala
import org.apache.spark.sql.functions._
import spark.implicits._

val df = Seq(2, 3, 4).toDF("number")
df
  .withColumn("number_factorial", factorial($"number"))
  .show()
```

## Lit()

使用字符串创建Column对象，另外，这个函数可以省略，见下面的例子

## When()/otherwise()

```scala
df.withColumn(
    "continent",
    when(col("word") === "china", "asia")
      .when(col("word") === "canada", "north america")
      .when(col("word") === "italy", "europe")
      .otherwise("not sure")
  )
  .show()
```

## 自定义sql函数

```scala
import org.apache.spark.sql.Column

def lifeStage(col: Column): Column = {
  when(col < 13, "child")
    .when(col >= 13 && col <= 18, "teenager")
    .when(col > 18, "adult")
}
可以用toString方法测试自定义的函数
lifeStage(lit("10")).toString
```

# 窗口函数

[Introducing Window Functions in Spark SQL](https://databricks.com/blog/2015/07/15/introducing-window-functions-in-spark-sql.html)

spark支持三类窗口函数：rank函数/分析函数/聚合函数，聚合函数包括sql支持的所有聚合函数作为窗口函数

![image-20210507141744683](../../../../../../Library/Application Support/typora-user-images/image-20210507141744683.png)

函数标记为窗口函数使用的两个途径：

1. 添加一个over子句
2. 在DataFrame上调用over方法

下一步就是确定窗口函数的窗口范围，一个窗口范围包含三个部分：

1. 分区
2. 排序
3. Frame指定： 当前行所在Frame还包含哪些行，例如：当前行的前三行到当前行。 需要指定Frame的开始、结束以及Frame的类型(Row frame和RANGE frame)。

语法：OVER (PARTITION BY ... ORDER BY ...)

## Frame类型

### Row frame

Row frame基于当前行做具体偏移量，CURRENT ROW`, `<value> PRECEDING`, 或者 `<value> FOLLOWING 指定具体偏移量。

ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING  代表当前行的前后行以及当前行。

### RANGE frame

Range frame基于当前行做逻辑偏移量，逻辑偏移量是指当前行的排序值与frame边界的排序值的差值，因此只能使用一个排序表达式。

例如RANGE BETWEEN 2000 PRECEDING AND 1000 FOLLOWING 针对当前行定义了一个区间[当前值-2000，当前值+1000]， 所有落在这个区间的行组成了一个Frame。

语法：

```sql
OVER (PARTITION BY ... ORDER BY ... frame_type BETWEEN start AND end)

这里，frame_type 可以是 ROWS (for ROW frame) 或者 RANGE (for RANGE frame); 
start 包括 UNBOUNDED PRECEDING, CURRENT ROW, <value> PRECEDING, 和 <value> FOLLOWING; 
end 包括 UNBOUNDED FOLLOWING, CURRENT ROW, <value> PRECEDING, 和 <value> FOLLOWING
```

API：

```python
from pyspark.sql.window import Window
# 定义分区和排序
windowSpec = \
  Window \
    .partitionBy(...) \
    .orderBy(...)
# 使用 ROW frame 定义一个窗口
windowSpec.rowsBetween(start, end)
# 使用 RANGE frame定义一个窗口
windowSpec.rangeBetween(start, end)
```



# tips 

list: _* 告诉解释器把list拆开作为参数，而不是把list作为一个参数

```scala
//添加编号
  def addNo(spark: SparkSession, df: DataFrame, df_columns: Array[String], no_name: String): DataFrame = {
    val rdd = df.rdd.zipWithIndex().map(x => {
      val columns = ArrayBuffer.empty[Any]
      columns += x._2.toString
      for (i <- 0 until df_columns.size) {
        columns += x._1.getString(i)
      }
      Row.fromSeq(columns)
    })

    spark.createDataFrame(rdd, buildSchema(no_name, df_columns))
  }

  def buildSchema(no_name: String, coreArray: Array[String]): StructType = {
    val columnNames = ArrayBuffer.empty[String]

    columnNames += no_name
    if (coreArray != null && coreArray.size > 0) coreArray.map(x => columnNames += x)

    StructType(
      columnNames.map(
        fieldName => StructField(fieldName, StringType, true)
      )
    )
  }
```

## zipWithIndex/zipWithUniqueId

### zipWithIndex

```scala
该函数将RDD中的元素和这个元素在RDD中的ID（索引号）组合成键/值对。

scala> var rdd2 = sc.makeRDD(Seq("A","B","R","D","F"),2)
rdd2: org.apache.spark.rdd.RDD[String] = ParallelCollectionRDD[34] at makeRDD at :21
 
scala> rdd2.zipWithIndex().collect
res27: Array[(String, Long)] = Array((A,0), (B,1), (R,2), (D,3), (F,4))
```

### zipWithUniqueId

def zipWithUniqueId(): RDD[(T, Long)]

该函数将RDD中元素和一个唯一ID组合成键/值对，该唯一ID生成算法如下：

每个分区中第一个元素的唯一ID值为：该分区索引号，

每个分区中第N个元素的唯一ID值为：(前一个元素的唯一ID值) + (该RDD总的分区数)

## Row

代表输出的一行，To create a new Row, use `RowFactory.create()` in Java or `Row.apply()` in Scala.

```scala
import org.apache.spark.sql._

 // Create a Row from values.
 Row(value1, value2, value3, ...)
 // Create a Row from a Seq of values.
 Row.fromSeq(Seq(value1, value2, ...))
```

## from 

```sql
from   t1   join t2
insert  overwrite  a   select xxx  from t1,t2
insert overwrite  b  select  yyyy from t1, t2;
```

# udf

[[Spark DataFrame 使用UDF实现UDAF的一种方法](https://segmentfault.com/a/1190000014088377)](https://segmentfault.com/a/1190000014088377)

[udaf](https://www.cnblogs.com/cc11001100/p/9471859.html)

## udf

```scala
// Create cubed function
val cubed = (s: Long) => {
  s * s * s
}
 
// Register UDF
spark.udf.register("cubed", cubed)
 
// Create temporary view
spark.range(1, 9).createOrReplaceTempView("udf_test")
cubed: Long => Long = $Lambda$8102/576597740@65ee6c72
spark.sql("SELECT id, cubed(id) AS id_cubed FROM udf_test").show()
```

