

# 性能提升四个方面

[视频](https://www.databricks.com/session_na20/deep-dive-into-the-new-features-of-apache-spark-3-0)

[blog](https://www.databricks.com/blog/2020/05/29/adaptive-query-execution-speeding-up-spark-sql-at-runtime.html)

过去，通过优化查询优化器、查询计划以提升Spark SQL的性能 ，其中改善最大的就是基于成本优化框架，通过各种数据统计来选择最优的计划，例如选择正确的Join策略，调整join顺序等。但是过期的统计值等可能导致选择了非最优的查询计划。

## AQE自适应执行

什么时候优化？各stage之间RDD需要物化，这样可以知道每个中间结果分区的大小，且后续task还没开始，这里是重新优化的机会。

AQE框架首先检查最开始的stage(它们不依赖其他stage)， 一旦完成，框架就基于运行时的统计结果更新逻辑计划，运行优化器，物理计划和物理优化规则，例如缩减分区，倾斜join处理等。然后框架使用新的执行计划执行下一个stage，循环下去直到执行完成。

spark3.0 的AQE主要有三个特性：

1. 动态缩减shuffle分区
2. 动态切换join策略
3. 动态优化倾斜join



### 动态缩减shuffle分区

shuffle是非常耗时的，shuffle的一个关键属性就是分区数，最优的分区数依赖数据的大小，这个不是很好调优：

1. 分区数太少，那么处理这个大分区的task可能需要落盘
2. 分区数太大，每个分区可能非常小，导致大量小的网络数据拉取

思路就是开始设置一个比较大的分区数，运行过程中根据实际情况合并分区。

例如，`SELECT max(i)FROM tbl GROUP BY j`   表有两个分区，开始时shuffle到了5个分区，其中有三个分区非常小，如果没有AQE， Spark就会启用5个task聚合这5个分区。

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/blog-adaptive-query-execution-2.png" alt="Spark Shuffle without AQE partition coalescing." style="zoom:50%;" />

有了AQE， 则会把三个小分区合并为一个大分区，然后只有启动3个task就可以了：

![Spark Shuffle with AQE partition coalescing.](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/blog-adaptive-query-execution-3.png)

### 动态切换join策略

·如果有一个表比较小，那么broadcast hash join是一个比较高效的join策略，但是因为过滤或者join关系复杂可能导致表大小计算不准。

为了解决这个问题，AQE在运行时基于准确的表大小重新规划join策略。

例如，AQE在运行时发现tb2实际大小比估计的小，从而把sort mege join策略修正为broadcast join策略



<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/blog-adaptive-query-execution-4.png" alt="Example reoptimization performed by Adaptive Query Execution at runtime, which automatically uses broadcast hash joins wherever they can be used to make the query faster." style="zoom:150%;" />



### 动态优化倾斜join

AQE统计shuffle文件检测数据倾斜，并把倾斜的分区分割成多个小分区，再与其他表join。

例如，表A有一个倾斜分区A0，AQE会把A0且分成两个分区，从而发起多个task与B0进行join

![Skew join without AQE skew join optimization.](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/blog-adaptive-query-execution-5.png)

![Skew join with AQE skew join optimization.](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/blog-adaptive-query-execution-6.png)

## 动态分区裁剪

在[星型模型](https://www.malaoshi.top/show_1IX2IO6OP7Ul.html)中，Spark2的优化器很难在编译时确定哪些分区可以跳过不读，导致读了一些不需要的数据。但Spark3之后，Spark会首先过滤维表，根据过滤后的结果找到只需要读事实表的分区，这样就能极大的提升性能

## 查询编译加速(Query Compilation Speedup)

## Join Hints



# 启用AQE

```sql
set spark.sql.adaptive.enabled = true;
```











https://www.waitingforcode.com/apache-spark-sql/whats-new-apache-spark-3-join-skew-optimization/read#configuration

当一个分区远大于其他分区时将会导致数据倾斜问题，倾斜的分区影响网络传输和task执行时间

spark2中处理数据倾斜的方法大多是手动，可设置*spark.sql.shuffle.partitions*把数据分的更均匀，也可以尝试broadcast join代替 sort-merge join。最后，还可以更改倾斜的key以改变数据分布。只是，这些操作都需要人为干预。  数据倾斜优化做的远不止这些。

配置：
最重要的参数是*spark.sql.adaptive.skewJoin.enabled*决定是否开启数据倾斜优化

另外两个参数

*spark.sql.adaptive.skewJoin.skewedPartitionFactor*控制非倾斜分区的最大大小

*spark.sql.adaptive.skewJoin.skewedPartitionThresholdInBytes*判断一个分区是否倾斜

这两个测试出现在方法*isSkewed(size: Long, medianSize: Long)*里，都为true时，说明分区倾斜了。

*spark.sql.adaptive.advisoryPartitionSizeInBytes*.定义优化后的分区大小

算法： 

算法执行的条件：

1. 开启倾斜join优化
2. 查询只有两个join
3. 优化后的计划不引入额外的shuffle

```scala
 val optimizePlan = optimizeSkewJoin(plan)
      val numShuffles = ensureRequirements.apply(optimizePlan).collect {
        case e: ShuffleExchangeExec => e
      }.length

      if (numShuffles > 0) {
        logDebug("OptimizeSkewedJoin rule is not applied due" +
          " to additional shuffles will be introduced.")
        plan
      }

```

4. join是一个(inner, left outer, right outer, cross, left anti, left semi)类型的sort merge join
5. 两个数据集有相同的shuffle分区数
6. 至少有一边倾斜

```sql
logDebug("number of skewed partitions: " +
        s"left ${leftSkewDesc.numPartitions}, right ${rightSkewDesc.numPartitions}")
      if (leftSkewDesc.numPartitions > 0 || rightSkewDesc.numPartitions > 0) {
        val newLeft = CustomShuffleReaderExec(
          left.shuffleStage, leftSidePartitions, leftSkewDesc.toString)
        val newRight = CustomShuffleReaderExec(
          right.shuffleStage, rightSidePartitions, rightSkewDesc.toString)
        smj.copy(
          left = s1.copy(child = newLeft), right = s2.copy(child = newRight), isSkewJoin = true)
      } else {
        smj
      }
```

倾斜检测做了啥： 

1. 每个数据集计算分区大小中位数
2. 根据配置计算优化后的分区大小
3. 算法遍历所有分区
   1. 确定哪边倾斜
   2. 检查哪边coalesced(分区数缩小了)，只要有一个缩小了，则跳过这个分区

```sql
val isLeftCoalesced = leftPartSpec.startReducerIndex + 1 < leftPartSpec.endReducerIndex
val isRightCoalesced = rightPartSpec.startReducerIndex + 1 < rightPartSpec.endReducerIndex
```

​		

​		3. 调用*createSkewPartitionSpecs* 它是倾斜管理的主逻辑

*createSkewPartitionSpecs*做了啥：

1. 获取对给定reducer的map文件大小，*ShufflePartitionsUtil.splitSizeListByTargetSize* 把它们切分成并行task

   内部遍历所有map文件，当累计达到目标大小就创建一个map group

   ```scala
   def splitSizeListByTargetSize(sizes: Seq[Long], targetSize: Long): Array[Int] = {
   // …
       val partitionStartIndices = ArrayBuffer[Int]()
       partitionStartIndices += 0
       while (i < sizes.length) {
         // If including the next size in the current partition exceeds the target size, package the
         // current partition and start a new partition.
         if (i > 0 && currentPartitionSize + sizes(i) > targetSize) {
           tryMergePartitions()
           partitionStartIndices += i
           currentPartitionSize = sizes(i)
         } else {
           currentPartitionSize += sizes(i)
         }
         i += 1
       }
       tryMergePartitions()
       partitionStartIndices.toArray
   ```

   其实，除此之外，该方法还通过tryMergePartitions()合并小分区：

   ![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/aqe_skew_join_algorithm.png)

   

遍历完分区后，两边都被包进一个新的节点*CustomShuffleReaderExec*， sort merge 节点设置*isSkewJoin为true

```scala
val newLeft = CustomShuffleReaderExec(
          left.shuffleStage, leftSidePartitions, leftSkewDesc.toString)
        val newRight = CustomShuffleReaderExec(
          right.shuffleStage, rightSidePartitions, rightSkewDesc.toString)
        smj.copy(
          left = s1.copy(child = newLeft), right = s2.copy(child = newRight), isSkewJoin = true)
```



物理执行计划
倾斜算法的结果是，在SortExec节点前的所有操作用一个额外的节点包住：

```scala
SortMergeJoin(skew=true) [id4#4], [id5#10], Inner
:- Sort [id4#4 ASC NULLS FIRST], false, 0
:  +- CustomShuffleReader 1 skewed partitions with size(max=14 KB, min=14 KB, avg=14 KB)
:     +- ShuffleQueryStage 0
:        +- Exchange hashpartitioning(id4#4, 10), true, [id=#40]
:           +- LocalTableScan [id4#4]
+- Sort [id5#10 ASC NULLS FIRST], false, 0
   +- CustomShuffleReader no skewed partition
      +- ShuffleQueryStage 1
         +- Exchange hashpartitioning(id5#10, 10), true, [id=#46]
            +- LocalTableScan [id5#10]
```



join的执行：

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/aqe_skew_join_execution.png)

如图所示，一些倾斜的分区并没有切分，因为没有达到目标分区的大小。该方法的缺点是同一个reducer的文件读取多次。



```scala
object SkewedJoinOptimizationConfiguration {

  val sparkSession = SparkSession.builder()
    .appName("Spark 3.0: Adaptive Query Execution - join skew optimization")
    .master("local[*]")
    .config("spark.sql.adaptive.enabled", true)
    // First, disable all configs that would create a broadcast join
    .config("spark.sql.autoBroadcastJoinThreshold", "1")
    .config("spark.sql.join.preferSortMergeJoin", "true")
    .config("spark.sql.adaptive.logLevel", "TRACE")
    // Disable coalesce to avoid the coalesce condition block the join skew
    // optimization happen
    .config("spark.sql.adaptive.coalescePartitions.enabled", "false")
    // Finally, configure the skew demo
    .config("spark.sql.shuffle.partitions", "10")
    .config("spark.sql.adaptive.skewJoin.enabled", "true")
    .config("spark.sql.adaptive.skewJoin.skewedPartitionFactor", "1")
    .config("spark.sql.adaptive.skewJoin.skewedPartitionThresholdInBytes", "10KB")
    // Required, otherwise the skewed partitions are too small to be optimized
    .config("spark.sql.adaptive.advisoryPartitionSizeInBytes", "1B")
    .getOrCreate()
  import sparkSession.implicits._


  val input4 = (0 to 50000).map(nr => {
    if (nr < 30000) {
      1
    } else {
      nr
    }
  }).toDF("id4")
  input4.createOrReplaceTempView("input4")
  val input5 =  (0 to 300).map(nr => nr).toDF("id5")
  input5.createOrReplaceTempView("input5")
  val input6 = (0 to 300).map(nr => nr).toDF("id6")
  input6.createOrReplaceTempView("input6")

}

object SkewedJoinOptimizationNotAppliedDemo extends App {

  SkewedJoinOptimizationConfiguration.sparkSession.sql(
    """
      |SELECT * FROM input4 JOIN input5 ON input4.id4 = input5.id5
      |JOIN input6 ON input6.id6 = input5.id5 """.stripMargin
  ).collect()

}

object SkewedJoinOptimizationAppliedDemo extends App  {

  SkewedJoinOptimizationConfiguration.sparkSession
    .sql("SELECT * FROM input4 JOIN input5 ON input4.id4 = input5.id5")
    .collect()
}
```





join倾斜优化是AQE组件的一个构建块，在特定条件下，优化规则检测倾斜分区并切分到多个组并行执行来消除倾斜。后面关于AQE可以发现一些新的内容





优化参数

| **参数**                                                    | **描述**                                                     | **默认值** |
| ----------------------------------------------------------- | ------------------------------------------------------------ | ---------- |
| spark.sql.adaptive.enabled                                  | 自适应执行特性的总开关。注意：AQE特性与DPP（动态分区裁剪）特性同时开启时，SparkSQL任务执行中会优先执行DPP特性，从而使得AQE特性不生效。集群中DPP特性是默认开启的，因此我们开启AQE特性的同时，需要将DPP特性关闭。 | false      |
| spark.sql.optimizer.dynamicPartitionPruning.enabled         | 动态分区裁剪功能的开关。                                     | true       |
| spark.sql.adaptive.skewJoin.enabled                         | 当此配置为true且spark.sql.adaptive.enabled设置为true时，启用运行时自动处理join运算中的数据倾斜功能。 | true       |
| spark.sql.adaptive.skewJoin.skewedPartitionFactor           | 此配置为一个倍数因子，用于判定分区是否为数据倾斜分区。单个分区被判定为数据倾斜分区的条件为：当一个分区的数据大小超过除此分区外其他所有分区大小的中值与该配置的乘积，并且大小超过spark.sql.adaptive.skewJoin.skewedPartitionThresholdInBytes配置值时，此分区被判定为数据倾斜分区 | 5          |
| spark.sql.adaptive.skewjoin.skewedPartitionThresholdInBytes | 分区大小（单位：字节）大于该阈值且大于spark.sql.adaptive.skewJoin.skewedPartitionFactor与分区中值的乘积，则认为该分区存在倾斜。理想情况下，此配置应大于spark.sql.adaptive.advisoryPartitionSizeInBytes. | 256MB      |
| spark.sql.adaptive.shuffle.targetPostShuffleInputSize       | 每个task处理的shuffle数据的最小数据量。单位：Byte。          | 67108864   |