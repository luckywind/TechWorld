[参考](https://jaceklaskowski.gitbooks.io/mastering-spark-sql/content/spark-sql-Row.html)

[Spark SQL and DataFrames: Interacting with External Data Sources](https://databricks.com/notebooks/gallery/SparkSQLAndUDFs.html)

`Row` is also called **Catalyst Row**.

The traits of `Row`:
import org.apache.spark.sql.Row

- `length` or `size` - `Row` knows the number of elements (columns).
- `schema` - `Row` knows the schema

1. 创建Row

```scala
import org.apache.spark.sql.Row
//伴生对象的工厂方法
scala> val row = Row(1, "hello","",null)
Row.fromSeq(Seq(1, "hello"))
Row.fromTuple((0, "hello"))
```

2. 获取字段

```scala
row.get(0)        //返回Any类型
row.getAs[Int/String...](0) //返回指定类型，类型不匹配时报错。 null不会报错，但转成Int时是0，转成String时是null
```

3. schema
   Row实例可以定义schema

> 除非自己定义Row, Row总是有schema
>
> RowEncoder负责在toDF或者初始化DataFrame时赋予Row一个schema

4. Row操作

```scala
scala> Row.merge(Row(1), Row("hello"))
res17: org.apache.spark.sql.Row = [1,hello]

scala> Row.empty == Row()
res18: Boolean = true
```



