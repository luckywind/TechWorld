本文cover自顶向下、自底向上、混合和联邦方法的基本设计概念。

kimball和inmon的数仓架构设计有显著的不同，inmon提倡独立数据集市架构，kimball提倡数仓总线架构。

# 独立数据集市:自顶向下方法

把分散的OLTP系统的数据转换成集中存储的数据以供分析，inmon认为数据应该被组织成面向主题的、集成的、非易失的、随时间变化的数据结构。数据可以通过下钻获取明细，上卷获取汇总。数据集市就是数仓的子集。每个数据集市服务于一个特定的部门，为该部门分析需要进行优化。

![image-20200630140206158](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20200630140206158.png)



​	自顶向下OLAP环境从操作型数据源抽取数据，加载到暂存区，为了保证精确性，还进行了验证和综合，接着传输到ODS层。如果ODS阶段就是操作型数据库的副本，则可以跳过。于此同时，一个并行的进程会把数据加载到数仓以免去从ODS抽取。详细数据通常从ODS抽取并且暂时存储在暂存区，以便聚合、汇总、抽取并加载到数仓。ODS是否必要存在由业务需要决定。

​	一旦数仓聚合汇总过程结束，数据集市刷新循环将从数仓抽取数据到暂存区并执行一系列新的转换操作。这有助于把数据组织成数据集市需要的特定结构。之后，数据集市加载了数据并且OLAP环境就可以使用了。







[原文](http://www.ceine.cl/design-of-the-data-warehouse-kimball-vs-inmon/)