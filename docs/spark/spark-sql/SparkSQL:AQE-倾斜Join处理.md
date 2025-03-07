# OptimizeSkewedJoin(ensureRequirements)

把倾斜的分区切分为小分区(需要spark.sql.adaptive.skewJoin.enabled=true(默认值))，join的另一边对应的分区膨胀多份，从而并行执行。 注意，如果join的另一边也倾斜了，将变成笛卡尔积膨胀。

left:  [L1, L2, L3, L4]
right: [R1, R2, R3, R4]

假如，L2,L4和R3,R4倾斜，且每个被分为两个子分区，那么开始的4个task将被分为9个task:
(L1, R1),  未倾斜的不处理
(L2-1, R2), (L2-2, R2),右边膨胀
(L3, R3-1), (L3, R3-2),左边膨胀
(L4-1, R4-1), (L4-2, R4-1), (L4-1, R4-2), (L4-2, R4-2) 两边都膨胀

## apply

```scala
  override def apply(plan: SparkPlan): SparkPlan = {
    if (!conf.getConf(SQLConf.SKEW_JOIN_ENABLED)) {
      return plan
    }

    //  如果引入了额外的shuffle,则放弃这个优化，除非配置为强制apply这个优化
    // TODO: It's possible that only one skewed join in the query plan leads to extra shuffles and
    //       we only need to skip optimizing that join. We should make the strategy smarter here.
    val optimized = optimizeSkewJoin(plan)
    val requirementSatisfied = if (ensureRequirements.requiredDistribution.isDefined) {
      ValidateRequirements.validate(optimized, ensureRequirements.requiredDistribution.get)
    } else {
      ValidateRequirements.validate(optimized)
    }
    if (requirementSatisfied) {
      optimized.transform {
        case SkewJoinChildWrapper(child) => child
      }
    } else if (conf.getConf(SQLConf.ADAPTIVE_FORCE_OPTIMIZE_SKEWED_JOIN)) {
      ensureRequirements.apply(optimized).transform {
        case SkewJoinChildWrapper(child) => child
      }
    } else {
      plan
    }
  }
```



### optimizeSkewJoin

```scala
  def optimizeSkewJoin(plan: SparkPlan): SparkPlan = plan.transformUp {
    case smj @ SortMergeJoinExec(_, _, joinType, _,
        s1 @ SortExec(_, _, ShuffleStage(left: ShuffleQueryStageExec), _),
        s2 @ SortExec(_, _, ShuffleStage(right: ShuffleQueryStageExec), _), false) =>
      tryOptimizeJoinChildren(left, right, joinType).map {
        case (newLeft, newRight) =>
          smj.copy(
            left = s1.copy(child = newLeft), right = s2.copy(child = newRight), isSkewJoin = true)
      }.getOrElse(smj)

    case shj @ ShuffledHashJoinExec(_, _, joinType, _, _,
        ShuffleStage(left: ShuffleQueryStageExec),
        ShuffleStage(right: ShuffleQueryStageExec), false) =>
      tryOptimizeJoinChildren(left, right, joinType).map {
        case (newLeft, newRight) =>
          shj.copy(left = newLeft, right = newRight, isSkewJoin = true)
      }.getOrElse(shj)
  }
```

#### tryOptimizeJoinChildren

主要有以下步骤：

1. 检查是否有倾斜的shuffle分区
2. 假设左表partition0是倾斜的，有5个mapper（Map0,Map111,...Map4). 根据map大小和最大划分数，我们把他们分为3个mapper ranges[(Map0, Map1), (Map2, Map3), (Map4)]
3. 用shuffle read包装左孩子，每个shuffle read  task读取一个mapper range，这样需要三个shuffle read  task
4. 用一个shuffle read包装右孩子，用3个task读取partition0 3次



是否能进行倾斜优化，有几点硬性要求：

1. 必须是SortMergeJoin或者ShuffledHashJoin

2. 必须是[Inner,Cross,LeftSemi,LeftAnti,LeftOuter,RightOuter]中的一种Join

- left表:Inner,Cross,LeftSemi,LeftAnti,LeftOuter
- right表:Inner,Cross,RightOuter

left和right的分区数必须一致

```scala
  private def tryOptimizeJoinChildren(
      left: ShuffleQueryStageExec,
      right: ShuffleQueryStageExec,
      joinType: JoinType): Option[(SparkPlan, SparkPlan)] = {

    //由join类型决定可分割性，如果都不可分割，则返回None,即不改变原Join计划
    val canSplitLeft = canSplitLeftSide(joinType)
    val canSplitRight = canSplitRightSide(joinType)
    if (!canSplitLeft && !canSplitRight) return None

    //从mapstat获取左右表的各分区大小
    val leftSizes = left.mapStats.get.bytesByPartitionId
    val rightSizes = right.mapStats.get.bytesByPartitionId
    assert(leftSizes.length == rightSizes.length)
    val numPartitions = leftSizes.length
    // We use the median size of the original shuffle partitions to detect skewed partitions.
    //用中位数决定两边的倾斜分区
    val leftMedSize = Utils.median(leftSizes, false)
    val rightMedSize = Utils.median(rightSizes, false)
    logDebug(
      s"""
         |Optimizing skewed join.
         |Left side partitions size info:
         |${getSizeInfo(leftMedSize, leftSizes)}
         |Right side partitions size info:
         |${getSizeInfo(rightMedSize, rightSizes)}
      """.stripMargin)
    //默认中位数的5倍(spark.sql.adaptive.skewJoin.skewedPartitionFactor)
    val leftSkewThreshold = getSkewThreshold(leftMedSize)
    val rightSkewThreshold = getSkewThreshold(rightMedSize)
    //分区目标大小=max(64M(可配置spark.sql.adaptive.advisoryPartitionSizeInBytes)，  非倾斜分区大小均值)
    val leftTargetSize = targetSize(leftSizes, leftSkewThreshold)
    val rightTargetSize = targetSize(rightSizes, rightSkewThreshold)

    val leftSidePartitions = mutable.ArrayBuffer.empty[ShufflePartitionSpec]
    val rightSidePartitions = mutable.ArrayBuffer.empty[ShufflePartitionSpec]
    var numSkewedLeft = 0
    var numSkewedRight = 0
    //逐个分区处理
    for (partitionIndex <- 0 until numPartitions) {
      val leftSize = leftSizes(partitionIndex)
      val isLeftSkew = canSplitLeft && leftSize > leftSkewThreshold
      val rightSize = rightSizes(partitionIndex)
      val isRightSkew = canSplitRight && rightSize > rightSkewThreshold
      //对于非倾斜的分区，创建CoalescedPartitionSpec并读取一个reducer所需数据，CoalescedPartitionSpec可以读取多个reducer的数据
      
      val leftNoSkewPartitionSpec =
        Seq(CoalescedPartitionSpec(partitionIndex, partitionIndex + 1, leftSize))
      val rightNoSkewPartitionSpec =
        Seq(CoalescedPartitionSpec(partitionIndex, partitionIndex + 1, rightSize))
      
      //如果未倾斜，则返回一个CoalescedPartitionSpec，如果倾斜了，则返回一个Seq(CoalescedPartitionSpec)
      val leftParts = if (isLeftSkew) {
        //如果左表分区倾斜了，那么切分左表分区，分区ID就是reducerID
        val skewSpecs = ShufflePartitionsUtil.createSkewPartitionSpecs(
          left.mapStats.get.shuffleId, partitionIndex, leftTargetSize)
        if (skewSpecs.isDefined) {
          //从这段log也能看出，skewSpecs数组长度就是原分区切分后的分区数(虽然切分后仍然有可能倾斜)
          logDebug(s"Left side partition $partitionIndex " +
            s"(${FileUtils.byteCountToDisplaySize(leftSize)}) is skewed, " +
            s"split it into ${skewSpecs.get.length} parts.")
          numSkewedLeft += 1
        }
        skewSpecs.getOrElse(leftNoSkewPartitionSpec)
      } else {
        leftNoSkewPartitionSpec
      }

      val rightParts = if (isRightSkew) {
        val skewSpecs = ShufflePartitionsUtil.createSkewPartitionSpecs(
          right.mapStats.get.shuffleId, partitionIndex, rightTargetSize)
        if (skewSpecs.isDefined) {
          logDebug(s"Right side partition $partitionIndex " +
            s"(${FileUtils.byteCountToDisplaySize(rightSize)}) is skewed, " +
            s"split it into ${skewSpecs.get.length} parts.")
          numSkewedRight += 1
        }
        skewSpecs.getOrElse(rightNoSkewPartitionSpec)
      } else {
        rightNoSkewPartitionSpec
      }

      // 如果一边倾斜，发生切分，则另一边进行复制；如果两边都倾斜了，那么两边都复制
      // 双层for循环，相当于做了个笛卡尔积：
      for {
        leftSidePartition <- leftParts
        rightSidePartition <- rightParts
      } {
        leftSidePartitions += leftSidePartition
        rightSidePartitions += rightSidePartition
      }
    }
    logDebug(s"number of skewed partitions: left $numSkewedLeft, right $numSkewedRight")
    if (numSkewedLeft > 0 || numSkewedRight > 0) {
    //如果有一边倾斜，则返回新的计划，且用SkewJoinChildWrapper包含
      Some((
        SkewJoinChildWrapper(AQEShuffleReadExec(left, leftSidePartitions.toSeq)),
        SkewJoinChildWrapper(AQEShuffleReadExec(right, rightSidePartitions.toSeq))
      ))
    } else {
      None
    }
  }
```

最终join会变成下图这样，第一个倾斜的分区被拆成了两个子分区，其余两个虽然判断为倾斜，但并未达到target大小，所以未切分。

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/aqe_skew_join_execution.png)

##### createSkewPartitionSpecs

基于map分区大小和目标分区大小切分倾斜分区，返回PartialReducerPartitionSpec 列表，PartialReducerPartitionSpec可以读取一个reducer的部分输入数据，也称为<font color=red>map group files</font>。

首先，它获取这个reducer对应的上游map文件大小，并用splitSizeListByTargetSize切分算法切分为并行task。

 为什么这里代码看起来像是做合并呢，因为mapPartitionSizes对应一个reducePart在上游需要读取的所有文件组成的一个part分区，但是这里将其合并为多个子分区，  每个子分区在AQE之后，都会单独启动一个reduce task；  

```scala
  def createSkewPartitionSpecs(
      shuffleId: Int,
      reducerId: Int,
      targetSize: Long,
      smallPartitionFactor: Double = SMALL_PARTITION_FACTOR)
  : Option[Seq[PartialReducerPartitionSpec]] = {
    //获取reducer的map端数据
    val mapPartitionSizes = getMapSizesForReduceId(shuffleId, reducerId)
    if (mapPartitionSizes.exists(_ < 0)) return None
    //调用切分算法, 原本同一个reduce需要处理的上游map输出，现在由多个reduce task处理，这里划分出了每个reduce task需要处理哪些map的输出
    val mapStartIndices = splitSizeListByTargetSize(
      mapPartitionSizes, targetSize, smallPartitionFactor)
    //如果split完的分片数大于1个，则获取这个分片需要读取的startMapIndex和endMapIndex，封装为PartialReducerPartitionSpec，这样，在实际计算的时候，通过shuffleId+reduceId+startMapIndex+endMapIndex就可以得到对应task需要拉取的数据了。
    if (mapStartIndices.length > 1) {
      Some(mapStartIndices.indices.map { i =>
        val startMapIndex = mapStartIndices(i)
        val endMapIndex = if (i == mapStartIndices.length - 1) {
          mapPartitionSizes.length
        } else {
          mapStartIndices(i + 1)
        }
        var dataSize = 0L
        var mapIndex = startMapIndex
        while (mapIndex < endMapIndex) {
          dataSize += mapPartitionSizes(mapIndex)
          mapIndex += 1
        }
        PartialReducerPartitionSpec(reducerId, startMapIndex, endMapIndex, dataSize)
      })
    } else {
      None
    }
  }

//指定shuffleId, reducerId, 返回map端数据量
  private def getMapSizesForReduceId(shuffleId: Int, partitionId: Int): Array[Long] = {
    //从SparkEnv获取MapOutputTrackerMaster
    val mapOutputTracker = SparkEnv.get.mapOutputTracker.asInstanceOf[MapOutputTrackerMaster]
    //获取指定shuffleId的MapStatus、指定分区大小
    mapOutputTracker.shuffleStatuses(shuffleId).withMapStatuses(_.map { stat =>
      if (stat == null) -1 else stat.getSizeForBlock(partitionId)
    })
  }
```

Shuffle过程中，上游的Mapper端生成数据后，是按照reduceId来排序的，并整体放在一个data文件中，同时生成一个索引文件。Reduce端可以根据索引文件中起始的reduceId来读取data中对应片段的数据，由于reduce端会依赖多个mapper，所以这个方法返回了一个Array[Long]类型，代表需要从对应mapId拉取的bytes大小，mapId就是对应数组下标。通过这个方法我们起其实也可以知道，通过shuffleId+reduceId即可知道当前reduce都需要拉取哪些数据了。



# 切分算法

给定一个list， 对list进行分区，使得每个分区的和接近目标大小。主要逻辑就是相邻两个分区加起来不超过1.2倍，或者两者有一个小于0.8倍，就合并。

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/aqe_skew_join_algorithm.png)

例如，*splitSizeListByTargetSize*(*Array*(0,1,2,3,4,5), 4, 0.8)的切分结果为[0,4,5], 意味着第一个分区包含的mapid从0开始，第二个分区包含的mapid从4开始，第三个分区包含的mapid从5开始，结果就是前面4个map产生的数据由一个task处理，后面两个map产生的数据分别由另外两个task处理。

但是怎么感觉像是分区合并而不是分区切割呢？实际上，这个数组是同一个shuffleid需要读取的上游map task的输出，原本只能由一个reduce task读取，现在把这个数组进行了切分，然后由多个reduce task读取，从而达到分区切割的目的。<font color=blue>实际上，这仍然有缺陷，如果某个map产生的数据很大，那么这种办法是没办法切开的</font>.

```scala
  private[sql] def splitSizeListByTargetSize(
      sizes: Array[Long],
      targetSize: Long,
      smallPartitionFactor: Double): Array[Int] = {
    val partitionStartIndices = ArrayBuffer[Int]()
    partitionStartIndices += 0
    var i = 0
    var currentPartitionSize = 0L
    var lastPartitionSize = -1L

    //先定义一个分区合并函数
    def tryMergePartitions() = {
      // When we are going to start a new partition, it's possible that the current partition or
      // the previous partition is very small and it's better to merge the current partition into
      // the previous partition.
      //MERGED_PARTITION_FACTOR=1.2
      //两个分区加起来不超过1.2倍，或者两者有一个小于0.8倍，就合并
      val shouldMergePartitions = lastPartitionSize > -1 &&
        ((currentPartitionSize + lastPartitionSize) < targetSize * MERGED_PARTITION_FACTOR ||
        (currentPartitionSize < targetSize * smallPartitionFactor ||
          lastPartitionSize < targetSize * smallPartitionFactor))
      if (shouldMergePartitions) {
        // 与前面分区合并，所以不生成新分区，那么删除当前分区的索引
        partitionStartIndices.remove(partitionStartIndices.length - 1)
        lastPartitionSize += currentPartitionSize
      } else {
        lastPartitionSize = currentPartitionSize
      }
    }

    while (i < sizes.length) {
      // If including the next size in the current partition exceeds the target size, package the
      // current partition and start a new partition.
      // 如果加上下一个分区大小后超过了目标，那么开启一个新分区
      if (i > 0 && currentPartitionSize + sizes(i) > targetSize) {
        tryMergePartitions()
        partitionStartIndices += i
        currentPartitionSize = sizes(i)
      } else {
        //加上后一个分区
        currentPartitionSize += sizes(i)
      }
      i += 1
    }
    tryMergePartitions()
    partitionStartIndices.toArray
  }
```

