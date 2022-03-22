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



