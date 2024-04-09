# RDD[本质](https://www.shuzhiduo.com/A/obzbPP2M5E/)

2、弹性：分别表现在如下几个方面
血缘：可以使用cache、checkpoint等机制灵活改变血缘继承关系。
计算：计算时可以灵活的使用内存和磁盘（早期shuffle无磁盘参与，存在性能瓶颈和OOM风险，后期加入）。
分区：在创建RDD和转换过程中可以灵活的调整数据分区。
容错：利用checkpoint持久化机制，可以在流程不同点位实现容错。

# RDD实现类

## MapPartitionsRDD

### 算子

flatmap

把函数应用到父RDD的每个分区上

## ShuffledRDD

shuffle操作(例如repartition/groupByKey)产生的RDD

