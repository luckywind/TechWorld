# No implicits found for parameter evidence

解决：引入sparkSession对象的隐式转换

```scala
import spark.implicits._
*注意，这里的spark是一个sparkSession对象*
```

[参考](https://blog.csdn.net/dz77dz/article/details/88802577)



# 无法toDF

```scala
val spark: SparkSession = SparkSession.builder()
import spark.implicits._
//import spark.sqlContext.implicits._  
```

sqlContext的隐式转换和SparkSession的好像有冲突，把下面的注释掉就可以了
