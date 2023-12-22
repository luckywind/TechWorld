[DPP过滤无关数据](https://medium.com/@prabhakaran.electric/spark-3-0-feature-dynamic-partition-pruning-dpp-to-avoid-scanning-irrelevant-data-1a7bbd006a89)

[DPP特性](https://zhuanlan.zhihu.com/p/548757324)

[谓词下推](https://cloud.tencent.com/developer/article/1906933)

[使用explain分析谓词下推](https://www.cnblogs.com/yeyuzhuanjia/p/16581296.html)

# 两个关键技术

## Partition Prunning

It works based on the [PushDownPredicate](https://jaceklaskowski.gitbooks.io/mastering-spark-sql/spark-sql-Optimizer-PushDownPredicate.html) property. Using this, Spark can read the partitions only that are needed for the processing, rather than processing all the partitions

## Broadcast hashing

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/1*73uT7xKn6bEbBWkxdEOrxg.jpeg)

1. Initially, the Table to be broadcasted is sent to the driver program. The driver program will create a [Hash table](https://en.wikipedia.org/wiki/Hash_table) out of it and getting it broadcasted across all the executors.
2. This Hash table will have the information such as join keys and the memory addresses of where the corresponding rows are located in the memory.
3. In this way, Join becomes significantly faster, as it is broadcasting only the key columns as a hash table.
4. As it is broadcasted, I/O overhead is reduced, and join shuffles are significantly lowered.

# DPP

DPP指的是在大表Join小表的场景中，可以充分利用过滤之后的小表，在运行时动态的来大幅削减大表的数据扫描量，从整体上提升关联计算的执行性能。

在join事实表和纬度表时，DPP从维度表的Filter中提取一个内部子查询、广播并构造一个hash表，然后在scan前应用到事实表的物理计划中。

那么动态分区剪裁是怎么实现的呢？它背后的逻辑是什么呢？我们来举个例子：

```sql
SELECT t1.id, t2.part_column FROM table1 t1
 JOIN table2 t2 ON t1.part_column = t2.part_column
 WHERE t2.id < 5
```

如上述SQL, t1为大表，t2为小表，两表基于分区列进行join, 同时还存在对小表的过滤扫描。

在没有开启DPP的情况下，执行上述语句需要扫描完整的t1表，[这是因为t2.id](https://link.zhihu.com/?target=http%3A//xn--t2-tz2cp45a3itpe1d.id/) < 5只是对小表进行了过滤。

**那么在开启DPP的它是如何转换的？**

首先，由于存在过滤条件（ [t2.id](https://link.zhihu.com/?target=http%3A//t2.id/) < 5)，它会帮助t2表过滤掉部分数据。与此同时，t2表对应的t2.part_column字段也顺带着经过一轮筛选。

然后，在Join关系 t1.part_column = t2.part_column的作用下，过滤效果会通过 小表t2.part_column 字段传导到大表的 t1.part_column字段。这样一来，传导后的t1.part_column值，就是大表 part_column 全集中的一个子集。把满足条件的 t1.part_column作为过滤条件，应用到大表的数据源，就可以做到减少数据扫描量，提升 I/O 效率。

**总而言之，DPP 正是基于上述逻辑实现的，用电商场景来说就是把维度表中的过滤条件，通过关联关系传导到事实表，从而完成事实表的优化。**

## 使用限制

1. 无需额外配置
2. 需要裁剪的表必须以join key为分区列
3. 只支持等值join
4. 适合星型模型

## 源码分析

参考PartitionPruning规则

