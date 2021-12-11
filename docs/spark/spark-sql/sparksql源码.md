[SparkSQL源码解析系列一](https://zhuanlan.zhihu.com/p/367590611)

[是sql](https://mp.weixin.qq.com/s/awT4aawtTIkNKGI_2zn5NA)

[InfoQ Sparksql内核剖析](https://xie.infoq.cn/article/2a70e9fb993bed9bc9ed02c46)

[是时候学习真正的 spark 技术了](https://mp.weixin.qq.com/s/awT4aawtTIkNKGI_2zn5NA)写的不错

spark sql 中 join 操作根据各种条件选择不同的 join 策略，分为 BroadcastHashJoin， SortMergeJoin， ShuffleHashJoin。



- BroadcastHashJoin：spark 如果判断一张表存储空间小于 broadcast 阈值时（Spark 中使用参数 spark.sql.autoBroadcastJoinThreshold 来控制选择 BroadcastHashJoin 的阈值，默认是 10MB），就是把小表广播到 Executor， 然后把小表放在一个 hash 表中作为查找表，通过一个 map 操作就可以完成 join 操作了，避免了性能代码比较大的 shuffle 操作，不过要注意， BroadcastHashJoin 不支持 full outer join， 对于 right outer join， broadcast 左表，对于 left outer join，left semi join，left anti join ，broadcast 右表， 对于 inner join，那个表小就 broadcast 哪个。



- SortMergeJoin：如果两个表的数据都很大，比较适合使用 SortMergeJoin， SortMergeJoin 使用shuffle 操作把相同 key 的记录 shuffle 到一个分区里面，然后两张表都是已经排过序的，进行 sort merge 操作，代价也可以接受。



- ShuffleHashJoin：就是在 shuffle 过程中不排序了，把查找表放在hash表中来进行查找 join，那什么时候会进行 ShuffleHashJoin 呢？查找表的大小需要超过 spark.sql.autoBroadcastJoinThreshold 值，不然就使用  BroadcastHashJoin 了，每个分区的平均大小不能超过  spark.sql.autoBroadcastJoinThreshold ，这样保证查找表可以放在内存中不 OOM， 还有一个条件是 大表是小表的 3 倍以上，这样才能发挥这种 Join 的好处。

