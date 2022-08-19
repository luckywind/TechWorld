[官方文档](https://spark.apache.org/docs/3.0.0-preview/sql-ref-syntax-aux-cache-cache-table.html)

一次数据抽样过程中，发现数据关联不上，排查好久，最后发现是with as语句多次执行的结果不一样：[参考上](https://tech.xiaomi.com/#/pc/article-detail?id=7309)

[参考下](https://tech.xiaomi.com/#/pc/article-detail?id=13891)

验证：

```sql
 with tempTable as (
select name, rand_number
from
   (select 'Withas' as name, rand() as rand_number) as t1;
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



改用cache，发现结果还是会执行两次

```sql
 cache table tempTable 
select name, rand_number
from
   (select 'Withas' as name, rand() as rand_number) ;

select name, rand_number
from tempTable
union all
select name, rand_number
from tempTable
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

[参考](https://towardsdatascience.com/best-practices-for-caching-in-spark-sql-b22fb0f02d34)

### dataframe API

```scala
df.cache() 
df.persist() 
```

这两个API几乎是等价的，区别在于persist() 可以指定*storageLevel*，默认是*MEMORY_AND_DISK*，意味着在内存空闲时数据可放入内存，否则放入磁盘。

cache是一个lazy transformation, 调用它时，缓存管理器(*Cache Manager*)添加新算子(*InMemoryRelation*)并更新查询计划，并不会对数据有任何操作，它只是后续执行计划要用的信息而已。Spark将会在缓存层查找数据，当查不到时，将负责把数据拉取到缓存，然后再直接使用。

### 缓存管理器

[cache最佳实践](https://blog.csdn.net/monkeyboy_tech/article/details/113986074)

with语句

 https://blog.csdn.net/weixin_44441757/article/details/120450555
