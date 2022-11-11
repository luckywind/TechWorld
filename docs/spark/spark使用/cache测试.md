```scala
var rdd1=spark.sparkContext.parallelize(Seq(("a",1), ("a",2),("b",3),4))
    rdd1.cache()
```

![image-20221109205030059](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221109205030059.png)



再次赋值后不手动再次cache，该RDD不会被cache:

```scala
    var rdd1=spark.sparkContext.parallelize(Seq(("a",1), ("a",2),("b",3),4))
    rdd1.cache()
   //重新赋值
    rdd1=spark.sparkContext.parallelize(Seq(("a",1), ("a",2),("b",3),("b",4)),8)


    println(rdd1.count())
```

![image-20221109205451232](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221109205451232.png)

重新赋值后再次cache可以成功

```scala
   var rdd1=spark.sparkContext.parallelize(Seq(("a",1), ("a",2),("b",3),4))
    rdd1.cache()

    rdd1=spark.sparkContext.parallelize(Seq(("a",1), ("a",2),("b",3),("b",4)),8)
    rdd1.cache()

    println(rdd1.count())
```

![image-20221109205624999](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221109205624999.png)

而且可以更改存储级别

```scala
 var rdd1=spark.sparkContext.parallelize(Seq(("a",1), ("a",2),("b",3),4))
    rdd1.cache()

    rdd1=spark.sparkContext.parallelize(Seq(("a",1), ("a",2),("b",3),("b",4)),8)
    rdd1.persist(StorageLevel.MEMORY_AND_DISK)

    println(rdd1.count())
```



![image-20221109205728461](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221109205728461.png)

