[参考](https://dataninjago.com/2022/02/03/spark-sql-query-engine-deep-dive-17-dynamic-partition-pruning/)

主要通过两个规则完成：一个是Optimizer rule, *PartitionPruning*, 另一个是 Spark planner rule, *PlanDynamicPruningFilters*.

# PartitionPruning

DPP基本的机制就是在满足条件时插入一个包含来自另一边的filter的复杂的子查询：

1. 需要裁剪的表可通过Join key过滤
2. Join类型：inner, left semi,  left outer(右边分区的), right outer(左边分区的)

为了在broadcast中直接启动分区裁剪，