[Spark SQL系列文章](http://marsishandsome.github.io/SparkSQL-Internal/01-introduction/)

[spark-sql gitbook](https://jaceklaskowski.gitbooks.io/mastering-spark-sql/content/spark-sql-joins.html)

# Cache table

SparkSQL的cache与RDD的cache有下面几点不同：

1. SparkSQL的cache采用列存储
2. SparkSQL的cache有两种模式可以选择lazy和Eager，而RDD的cache是lazy的

值得注意的是，SparkSQL 1.1.0中，SQL Cache是lazy模式的，而在1.2.0中，SQL Cache默认是eager模式。

## 用法

### API调用

通过SqlContext的cacheTable函数调用Cache，该函数是eager类型调用

```
cacheTable("rddtable")
```

###  sql调用

通过SqlContext的sql函数调用Cache，如果没有加lazy关键字，默认是eager类型调用

```
sql("cache table rddtable")
```

### lazy调用

通过SqlContext的sql函数调用Cache，如果添加lazy关键字，只有在触发action的时候才会去cache

```
sql("cache lazy table rddtable")
```

### SchemaRDD.cache

调用SchemaRDD的cache同样会触发Cache操作，该函数也是eager类型调用

```
createSchemaRDD(rddpeople).cache()
```

