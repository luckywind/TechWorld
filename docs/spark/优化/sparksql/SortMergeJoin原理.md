SortMergeJoin 是常用的分布式 Join 方式，它几乎可使用于所有需要 Join 的场景。但有些场景下，它的性能并不是最好的。

SortMergeJoin 的原理如下图所示

- **将 Join 双方以 Join Key 为 Key 按照同样的HashPartitioner 分区，且保证分区数一致**
- Stage 0 与 Stage 1 的所有 Task 在 Shuffle Write 时，都将数据分为 5 个 Partition，并且每个 Partition 内按 Join Key 排序
- Stage 2 启动 5 个 Task 分别去 Stage 0 与 Stage 1 中所有包含 Partition 分区数据的 Task 中取对应 Partition 的数据。（如果某个 Mapper 不包含该 Partition 的数据，则 Redcuer 无须向其发起读取请求）。
- Stage 2 的 Task 2 分别从 Stage 0 的 Task 0、1、2 中读取 Partition 2 的数据，并且通过 MergeSort 对其进行排序
- Stage 2 的 Task 2 分别从 Stage 1 的 Task 0、1 中读取 Partition 2 的数据，且通过 MergeSort 对其进行排序
- Stage 2 的 Task 2 在**上述两步 MergeSort 的同时，使用 SortMergeJoin 对二者进行 Join**

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/spark_ae_sort_merge_join.png" alt="Spark SQL SortMergeJoin" style="zoom:50%;" />

