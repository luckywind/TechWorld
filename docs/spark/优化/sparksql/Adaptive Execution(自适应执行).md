自适应执行实际上是动态调整shuffle的分区数。

# 原有shuffle的问题

使用 Spark SQL 时，可通过 `spark.sql.shuffle.partitions` 指定 Shuffle 时 Partition 个数，也即 Reducer 个数

该参数决定了一个 Spark SQL Job 中包含的**所有** Shuffle 的 Partition 个数

## partition数不宜过大

1. partition数过大，意味着reducer数过大，大量小Task造成不必要的Task调度与资源调度开销(如果开启了动态资源调整)。
2. reducer个数过大，如果直接写文件会生成大量小文件，不管是addBlock以及后续读取这些小文件的getBlock请求都会给NameNode造成压力。

## partition数不宜过小

1. 每个reducer处理的数据量太大，spill到磁盘开销增大
2. reducer的GC时间长
3. 如果写HDFS,每个Reducer写入数据量太大，无法充分发挥并行处理的优势

## 很难保证所有shuffle都最优

不同的shuffle操作对应的数据量不一样，最优的partition个数也不一样

# 自适应partition原理

## 开启前

这里描述一个shuffle, 2个mapper与5个reducer。不管每个reducer读取的数据量

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/spark_ae_fix_reducer_detail.png" alt="Spark Shuffle 过程" style="zoom:50%;" />

## 开启后(合并后拉取)

我们看自适应执行是如何分配分区给reducer的：

Reducer 0 读取 Partition 0，Reducer 1 读取 Partition 1、2、3，Reducer 2 读取 Partition 4

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/spark_ae_auto_reducer_detail_1.png" alt="Spark SQL adaptive reducer 1" style="zoom:50%;" />

1. shuffle写结束后，会统计每个分区的数据量， ExchangeCoordinator 根据 Shuffle Write 统计信息计算出合适的reducer的个数，这里计算出的reducer数为3，然后启动3个reducer任务。
2. 数据分配：
   1. 一个reducer可以读取多个小分区
   2. 每个reducer读取数据量不超过64M;  一个reducer尽量读取相邻的分区以保证顺序读，提高磁盘IO性能；
3. 该方案只会合并多个小的partition,不会拆分大的，因此默认partition个数可以大一点，否则自适应执行可能无效。

但这还不是最理想的，因为reducer1从每个mapper读取1，2，3分区都是分开读取的，需要多次读取磁盘，相当于随机IO。 为了解决这个问题，Spark新增接口，一次shuffle读可以读多个分区的数据：
<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/spark_ae_auto_reducer_detail_2.png" alt="Spark SQL adaptive reducer 2" style="zoom:50%;" />

由于 Adaptive Execution 的自动设置 Reducer 是由 ExchangeCoordinator 根据 Shuffle Write 统计信息决定的，因此即使在同一个 Job 中不同 Shuffle 的 Reducer 个数都可以不一样，从而使得每次 Shuffle 都尽可能最优。

# 动态调整执行计划 

## 固定执行计划的不足

不开启，执行计划一旦确定，即使发现后续执行计划可以优化，也不可更改。如下图所示，SortMergJoin 的 Shuffle Write 结束后，发现 Join 一方的 Shuffle 输出只有 46.9KB，仍然继续执行 SortMergeJoin

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/spark_ae_fix_dag.png" alt="Spark SQL with fixed DAG" style="zoom:50%;" />

此时完全可将 SortMergeJoin 变更为 BroadcastJoin 从而提高整体执行效率。

## 使用与优化方法

- 当 `spark.sql.adaptive.enabled` 与 `spark.sql.adaptive.join.enabled` 都设置为 `true` 时，开启 Adaptive Execution 的动态调整 Join 功能
- `spark.sql.adaptiveBroadcastJoinThreshold` 设置了 SortMergeJoin 转 BroadcastJoin 的阈值。如果不设置该参数，该阈值与 `spark.sql.autoBroadcastJoinThreshold` 的值相等
- 除了本文所述 SortMergeJoin 转 BroadcastJoin，Adaptive Execution 还可提供其它 Join 优化策略。部分优化策略可能会需要增加 Shuffle。`spark.sql.adaptive.allowAdditionalShuffle` 参数决定了是否允许为了优化 Join 而增加 Shuffle。其默认值为 false



# 自动处理数据倾斜

## 原理

思路：将部分倾斜的分区用多个task处理。

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/spark_ae_skew_join.png" alt="Spark SQL resolve joinm skew" style="zoom:50%;" />

在上图中，左右两边分别是参与 Join 的 Stage 0 与 Stage 1 (实际应该是两个 RDD 进行 Join，但如同上文所述，这里不区分 RDD 与 Stage)，中间是获取 Join 结果的 Stage 2

明显 Partition 0 的数据量较大，这里假设 Partition 0 符合“倾斜”的条件，其它 4 个 Partition 未倾斜

以 Partition 对应的 Task 2 为例，它需获取 Stage 0 的三个 Task 中所有属于 Partition 2 的数据，并使用 MergeSort 排序。同时获取 Stage 1 的两个 Task 中所有属于 Partition 2 的数据并使用 MergeSort 排序。然后对二者进行 SortMergeJoin

对于 Partition 0，可启动多个 Task

- 在上图中，**启动了两个 Task 处理 Partition 0 的数据，分别名为 Task 0-0 与 Task 0-1**
- **Task 0-0 读取 Stage 0 Task 0 中属于 Partition 0 的数据**
- **Task 0-1 读取 Stage 0 Task 1 与 Task 2 中属于 Partition 0 的数据，并进行 MergeSort**
- Task 0-0 与 Task 0-1 都从 Stage 1 的两个 Task 中所有属于 Partition 0 的数据
- Task 0-0 与 Task 0-1 使用 Stage 0 中属于 Partition 0 的部分数据与 Stage 1 中属于 Partition 0 的全量数据进行 Join

通过该方法，原本由一个 Task 处理的 Partition 0 的数据由多个 Task 共同处理，每个 Task 需处理的数据量减少，从而避免了 Partition 0 的倾斜

<font color=red>对于 Partition 0 的处理，有点类似于 BroadcastJoin 的做法。但区别在于，Stage 2 的 Task 0-0 与 Task 0-1 同时获取 Stage 1 中属于 Partition 0 的全量数据，是通过正常的 Shuffle Read 机制实现，而非 BroadcastJoin 中的变量广播实现</font>

## 使用方法

- 将 `spark.sql.adaptive.skewedJoin.enabled` 设置为 true 即可自动处理 Join 时数据倾斜
- `spark.sql.adaptive.skewedPartitionMaxSplits` 控制处理一个倾斜 Partition 的 Task 个数上限，默认值为 5
- `spark.sql.adaptive.skewedPartitionRowCountThreshold` 设置了一个 Partition 被视为倾斜 Partition 的行数下限，也即行数低于该值的 Partition 不会被当作倾斜 Partition 处理。其默认值为 10L * 1000 * 1000 即一千万
- `spark.sql.adaptive.skewedPartitionSizeThreshold` 设置了一个 Partition 被视为倾斜 Partition 的大小下限，也即大小小于该值的 Partition 不会被视作倾斜 Partition。其默认值为 64 * 1024 * 1024 也即 64MB
- `spark.sql.adaptive.skewedPartitionFactor` 该参数设置了倾斜因子。如果一个 Partition 的大小大于 `spark.sql.adaptive.skewedPartitionSizeThreshold` 的同时大于各 Partition 大小中位数与该因子的乘积，或者行数大于 `spark.sql.adaptive.skewedPartitionRowCountThreshold` 的同时大于各 Partition 行数中位数与该因子的乘积，则它会被视为倾斜的 Partition

# 参考

http://www.jasongj.com/spark/adaptive_execution/

