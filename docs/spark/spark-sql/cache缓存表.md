[官方文档](https://spark.apache.org/docs/3.0.0-preview/sql-ref-syntax-aux-cache-cache-table.html)

一次数据抽样过程中，发现数据关联不上，排查好久，最后发现是with as语句多次执行的结果不一样：[参考上](https://tech.xiaomi.com/#/pc/article-detail?id=7309)

[参考下](https://tech.xiaomi.com/#/pc/article-detail?id=13891)

验证：

```sql
 with tempTable as (
select name, rand_number
from
   (select 'Withas' as name, rand() as rand_number) as t1
)
select name, rand_number
from tempTable
union all
select name, rand_number
from tempTable
输出结果：
name	rand_number
Withas	0.6387383170731592
Withas	0.23821274450502994
```

两个随机数不一样，说明with as 语句执行了两次！

> 注意，这个问题已经修复了，经验证spark3.4.2只执行了一次。

改用cache，发现结果还是会执行两次

> 每次的执行结果都不同

```sql
 cache table tempTable 
select name, rand_number
from
   (select 'Withas' as name, rand() as rand_number) ;

select name, rand_number
from tempTable
union all
select name, rand_number
from tempTable;
name	rand_number
Withas	0.20437213954492972
Withas	0.5678513751386931
```



# Cache

⚠️： 仍然执行多次！

解决办法就是使用cache语法，该语法可以把一个表或者查询结果以指定的存储级别缓存，减少原始文件的扫描。语法如下：

```sql
CACHE [ LAZY ] TABLE table_name
    [ OPTIONS ( 'storageLevel' [ = ] value ) ] [ [ AS ] query ]
```

storageLevel有如下选项（默认MEMORY_AND_DISK）：

```sql
NONE
DISK_ONLY
DISK_ONLY_2
MEMORY_ONLY
MEMORY_ONLY_2
MEMORY_ONLY_SER
MEMORY_ONLY_SER_2
MEMORY_AND_DISK
MEMORY_AND_DISK_2
MEMORY_AND_DISK_SER
MEMORY_AND_DISK_SER_2
OFF_HEAP
```



query可以是如下：

```sql
a SELECT statement
a TABLE statement
a FROM statement
```

# 清除缓存

## 删除所有缓存表/视图

```sql
CLEAR CACHE;
```

## 删除指定缓存表/视图

```sql
UNCACHE TABLE [ IF EXISTS ] table_name
```

## 刷新表

```sql
REFRESH [TABLE] tableIdentifier
```

例如：

```sql
-- The cached entries of the table will be refreshed  
-- The table is resolved from the current database as the table name is unqualified.
REFRESH TABLE tbl1;

-- The cached entries of the view will be refreshed or invalidated
-- The view is resolved from tempDB database, as the view name is qualified.
REFRESH TABLE tempDB.view1;   
```

# SparkSQL缓存最佳实践

[cache最佳实践](https://blog.csdn.net/monkeyboy_tech/article/details/113986074)， [对应的英文原文](https://towardsdatascience.com/best-practices-for-caching-in-spark-sql-b22fb0f02d34)

### dataframe API

```scala
df.cache() 
df.persist() 
```

这两个API几乎是等价的，区别在于persist() 可以指定*storageLevel*，默认是*MEMORY_AND_DISK*，意味着在内存空闲时数据可放入内存，否则放入磁盘。

cache是一个lazy transformation, 调用它时，缓存管理器(*Cache Manager*)添加新算子(*InMemoryRelation*)并更新查询计划，并不会对数据有任何操作，它只是后续执行计划要用的信息而已。Spark将会在缓存层查找数据，当查不到时，将负责把数据拉取到缓存，然后再直接使用。

### 缓存管理器

Cache manager用来追踪记录对于当前查询计划哪些计算已经被缓存了。当缓存函数被调用的时候，Cache Manager被直接调用,并且把被调用的dataFrame从逻辑计划中脱离出来，从而储存在名为cachedData的顺序结构中

cache manager阶段是逻辑计划的一部分，作用在sql分析器之后，sql优化器之前。
![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/c6a9d834f0219bddb9aaee98aae54005.png)

当我们触发一个query的时候，查询计划将会被处理以及转化，在处理到cachemanager阶段（在优化阶段之前）的时候,spark将会检查分析计划的每个子计划是否存储在cachedData里。如果存在，则说明同样的计划已经被执行过了，所以可以重新使用该缓存，并且使用InMemoryRelation操作来标示该缓存的计划。InMemoryRelation将在物理计划阶段转换为InMemoryTableScan

```scala
df = spark.table("users").filer(col(col_name) > x).cache  
df.count() # now check the query plan in Spark UI
```

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/cda06825bd4e995820cd4ff63eb4b6cc.png)

缓存一个查询

```scala
df = spark.read.parquet(data_path)
df.select(col1, col2).filer(col2 > 0).cache()
```

考虑如下三个查询

```sql
1) df.filter(col2 > 0).select(col1, col2)
2) df.select(col1, col2).filter(col2 > 10)
3) df.select(col1).filter(col2 > 0) 
```

实际上语句1和2都不会用到缓存，因为他们的分析计划和缓存计划是不同的，即使优化后可能相同。

语句3反而会用到缓存，ResolveMissingRefecences解析规则会把filter中出现的列加到select里，从而分析计划和缓存计划相同。

语句2只要做一个简单的修改就可以用到缓存，原则就是构造和缓存的分析计划相同的分析计划：

```sql
 df.select(col1, col2).filter(col2 > 0).filter(col2 > 10)
```

**最佳实践**

1. 缓存df时直接创建一个变量cacheDF = df.cache()， 需要用缓存的时候直接使用该变量就行 
2. 使用cacheDF.unpersist()释放不用的缓存的计划
3. 只缓存需要的列，而非所有列

**比缓存更快**

1. 直接读取parquet文件会有几个优化：

   - 只读取元数据进行统计运算
   - 对于过滤则会进行列裁剪，无需扫描整个数据集

2. 从缓存读的缺点：

   - 带cache的计算有把数据写入内存的开销

   - 读缓存会读取整个数据集

     

**SQL中使用缓存**

```sql
spark.sql("cache table table_name") #立即缓存
spark.sql("cache lazy table table_name")#懒执行
spark.sql("uncache table table_name")
```



























with语句

 https://blog.csdn.net/weixin_44441757/article/details/120450555
