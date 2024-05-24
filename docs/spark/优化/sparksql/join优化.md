# join优化

1. 增大shuffle分区数，spark默认join时的分区数为200(即spark.sql.shuffle.partitions=200), 所以增大这个分区数, 即调整该参数为800, 即spark.sql.shuffle.partitions=800

2. 广播小表

   | 参数                                 | 默认值            | 描述                                                         |
   | ------------------------------------ | ----------------- | ------------------------------------------------------------ |
   | spark.sql.autoBroadcastJoinThreshold | 10485760<br />10M | 当进行join操作时，配置广播的最大值。当SQL语句中涉及的表中相应字段的大小小于该值时，进行广播。配置为-1时，将不进行广播。参见https://spark.apache.org/docs/3.1.1/sql-programming-guide.html |

被广播的表执行超时，导致任务结束。

默认情况下，BroadCastJoin只允许被广播的表计算5分钟，超过5分钟该任务会出现超时异常，而这个时候被广播的表的broadcast任务依然在执行，造成资源浪费。

这种情况下，有两种方式处理：

- 调整**“spark.sql.broadcastTimeout”**的数值，加大超时的时间限制。
- 降低**“spark.sql.autoBroadcastJoinThreshold”**的数值，不使用BroadCastJoin的优化。



# join原理

[Spark SQL之Join优化](https://www.shangmayuan.com/a/ad7733f6e98f4bddb9b36f2e.html) [原文](https://cloud.tencent.com/developer/article/1005502)

