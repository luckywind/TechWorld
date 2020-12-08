# SparkSQL

spark可以连接hive的元数据库获取表信息，需要设置如下值

```shell
spark.sql.hive.metastore.version
set spark.sql.hive.metastore.jars
```

## 查询接口

spark提供了几个执行sql的接口

### spark sql cli

./bin/spark-sql

### spark编程sql接口

这个接口可以通过sparkSession.sql获得

### Thrift jdbc/odbc 服务

./sbin/start-thriftserver.sh  启动服务

./bin/beeline 

beeline>!connect jdbc:hive2://localhost:10000 测试连接

## catalog

sparkSQL 的最高级抽象就是Catalog,它是关于表中数据、数据库、表、函数、视图等元数据存储的抽象。

提供了一些列出数据库、表、函数的功能。

# 体系结构

[参考](http://hbasefly.com/2017/02/16/sparksql-dataframe/)

![11](https://gitee.com/luckywind/PigGo/raw/master/image/11.png)

# DataFrame

## 第一次变革：MR编程模型 -> DAG编程模型

和MR计算模型相比，DAG计算模型有很多改进：

1. 可以支持**更多的算子**，比如filter算子、sum算子等，不再像MR只支持map和reduce两种

2. **更加灵活的存储机制**，RDD可以支持本地硬盘存储、缓存存储以及混合存储三种模式，用户可以进行选择。而MR目前只支持HDFS存储一种模式。很显然，HDFS存储需要将中间数据存储三份，而RDD则不需要，这是DAG编程模型效率高的一个重要原因之一。

3. DAG模型带来了**更细粒度的任务并发**，不再像MR那样每次起个任务就要起个JVM进程，重死了；另外，DAG模型带来了另一个利好是很好的容错性，一个任务即使中间断掉了，也不需要从头再来一次。

4. **延迟计算**机制一方面可以使得同一个stage内的操作可以合并到一起落在一块数据上，而不再是所有数据先执行a操作、再扫描一遍执行b操作，太浪费时间。另一方面给执行路径优化留下了可能性，随便你怎么优化…

所有这些改进使得DAG编程模型相比MR编程模型，性能可以有10～100倍的提升！然而，DAG计算模型就很完美吗？要知道，用户手写的RDD程序基本或多或少都会有些问题，性能也肯定不会是最优的。如果没有一个高手指点或者优化，性能依然有很大的优化潜力。这就是促成了第二次变革，从DAG编程模型进化到DataFrame编程模型。

## 第二次变革：DAG编程模型 -> DataFrame编程模型

相比RDD，DataFrame增加了scheme概念，从这个角度看，DataFrame有点类似于关系型数据库中表的概念。可以根据下图对比RDD与DataFrame数据结构的差别：

![12](https://gitee.com/luckywind/PigGo/raw/master/image/12.png)

直观上看，DataFrame相比RDD多了一个表头，这个小小的变化带来了很多优化的空间：



1. RDD中每一行纪录都是一个整体，因此你不知道内部数据组织形式，这就使得你对数据项的操作能力很弱。表现出来就是支持很少的而且是比较粗粒度的算子，比如map、filter算子等。而DataFrame将一行切分了多个列，每个列都有一定的数据格式，这与数据库表模式就很相似了，数据粒度相比更细，因此就能支持更多更细粒度的算子，比如select算子、groupby算子、where算子等。更重要的，后者的表达能力要远远强于前者，比如同一个功能用RDD和DataFrame实现：

![13](https://gitee.com/luckywind/PigGo/raw/master/image/13.png)

2. DataFrame的Schema的存在，数据项的转换也都将是类型安全的，这对于较为复杂的数据计算程序的调试是十分有利的，很多数据类型不匹配的问题都可以在编译阶段就被检查出来，而对于不合法的数据文件，DataFrame也具备一定分辨能力。

3. DataFrame schema的存在，开辟了另一种数据存储形式：列式数据存储。列式存储是相对于传统的行式存储而言的，简单来讲，就是将同一列的所有数据物理上存储在一起。有两个作用
   - 提升数据压缩效率
   - 列值裁剪直接跳过不需要的列，减少IO
4. DataFrame编程模式集成了一个优化神奇－Catalyst,会进行常见的谓词下推优化。

