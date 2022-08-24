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