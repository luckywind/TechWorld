[参考](https://www.infoq.cn/article/xEwaUj8RN74lvbRpTBa5)

# 优化

## Bucket 改进

在 Spark 里，实际并没有 Bucket Join 算子。这里说的 Bucket Join 泛指不需要 Shuffle 的 SortMergeJoin。

**改进一：支持与 Hive 兼容**

Hive 的一个 Bucket 一般只包含一个文件，而 Spark SQL 的一个 Bucket 可能包含多个文件。解决办法是动态增加一次以 Bucket Key 为 Key 并且并行度与 Bucket 个数相同的 Shuffle。

**改进二：支持倍数关系 Bucket Join**

 Bucket 个数不同的表Join，Task个数可以和小表的Bucket数一样，Bucket多的表，再做一次merge sort保证合集有序，这样两边Bucket变成相同。

但是差距比较大时，需要Task个数与大表Bucket个数相同，把Bucket小的一方多读几次

**改进三：支持 BucketJoin 降级**

**改进四：支持超集**

只要 Join Key Set 包含了 Bucket Key Set，即可进行 Bucket Join。

## 物化列

Spark SQL 处理嵌套类型数据时，存在以下问题：

1. **读取大量不必要的数据**
2. **无法进行向量化读取**
3. **不支持 Filter 下推**
4. **重复计算**

物化列

- 新增一个 Primitive 类型字段，比如 Integer 类型的 age 字段，并且指定它是 people.age 的物化字段；
- 插入数据时，为物化字段自动生成数据，并在 Partition Parameter 内保存物化关系。因此对插入数据的作业完全透明，表的维护方不需要修改已有作业；
- 查询时，检查所需查询的所有 Partition，如果都包含物化信息（people.age 到 age 的映射），直接将 select people.age 自动重写为 select age，从而实现对下游查询方的完全透明优化。同时兼容历史数据。

## 物化视图

# shuffle框架优化

问题： 

1. shuffle 的map 输出可能因为节点故障丢失，从而stage重试。
2. shuffle的map输出，被大量reducer进行随机读，性能差

方案： map结果输出完成后，上传HDFS, reducer读取时，如果出现失败再尝试从HDFS读取