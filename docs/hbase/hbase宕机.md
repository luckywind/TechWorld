Zookeeper一旦感知到RegionServer宕机之后，就会第一时间通知集群的管理者Master，Master首先会将这台RegionServer上所有Region移到其他RegionServer上，再将HLog分发给其他RegionServer进行回放，这个过程通常会很快。完成之后再修改路由，业务方的读写才会恢复正常。



HDFS写入流程：HDFS总是为第一份副本优先选择本地节点作为存储空间，对于第二份副本，则是优先选择另一个机架的节点。如果前两份副本位于不同机架，第三份副本偏向于选择与第一份副本相同机架的节点，否则选择不同机架

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/clip_image001.png)

# RegionServer宕机后如何处理

为了提升数据本地性，不能依赖HBase的负载均衡，它只会让整体Locality越来越低。

1. 拉起RegionServer
2. 从master日志中找到被迁移走的Region，通过move命令手动迁移回来

# 宕机原因定位

1. 从RegionServer日志中发现无法绑定到端口0(内核会随机分配一个端口)， 说明端口耗尽。 HBase所有读写HDFS操作都会发起RPC请求，请求量太高。
2. 定期重启RegionServer



