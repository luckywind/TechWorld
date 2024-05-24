![image-20221219213816120](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221219213816120.png)

Hive与HBase利用两者本身对外的API来实现整合，主要是靠HBaseStorageHandler进行通信，利用

HBaseStorageHandler，Hive可以获取到Hive表对应的HBase表名，列簇以及列，InputFormat和OutputFormat类，创建和删 除HBase表等。

Hive访问HBase中表数据，实质上是通过MapReduce读取HBase表数据，其实现是在MR中，使用 HiveHBaseTableInputFormat完成对HBase表的切分，获取RecordReader对象来读取数据。

对HBase表的切分原则是一个Region切分成一个Split,即表中有多少个Regions,MR中就有多少个Map;

读取HBase表数据都是通过构建Scanner，对表进行全表扫描，如果有过滤条件，则转化为Filter。当过滤条件为rowkey 时，则转化为对rowkey的过滤;

Scanner通过RPC调用RegionServer的next()来获取数据;