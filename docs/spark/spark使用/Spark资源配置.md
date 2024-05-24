[高效配置](https://medium.com/expedia-group-tech/part-3-efficient-executor-configuration-for-apache-spark-b4602929262)

1. 每个节点保留一个cpu核给操作系统和集群管理器，这里以每个节点16个cpu核为例，每个节点只有15个可以分配给spark作业。
2. 每个executor分配多少cpu核？
   ![1*gFbEobXhMdA7tNJBkNcVSA](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/1*gFbEobXhMdA7tNJBkNcVSA.jpeg)

第一种情况： 一个executor支持15个cpu核，内存池太大，GC耗时拖慢作业。
第四种情况：15个executor，每个分配一个cpu核，没有利用并行计算能力
