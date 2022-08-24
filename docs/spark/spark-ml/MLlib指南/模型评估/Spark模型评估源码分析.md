[参考](https://blog.csdn.net/wo334499/article/details/51689609)

[参考2](https://blog.csdn.net/snaillup/article/details/75348830?share_token=85e2e3f4-09f0-4dfe-9410-f0df560a4f9e)

最近在做二分类模型，使用了sparkmllib的模型评估，使用了其封装好的类BinaryClassificationMetrics。使用过程中发现大多数API都有一个ByThreshold()的后缀：

```scala
   val metrics = new BinaryClassificationMetrics(predictionAndLabel)

   def fMeasureByThreshold(): RDD[(Double, Double)] = fMeasureByThreshold(1.0)
  @Since("1.0.0")
  def fMeasureByThreshold(beta: Double): RDD[(Double, Double)] = createCurve(FMeasure(beta))
  /**
   * Returns the (threshold, precision) curve.
   */
  @Since("1.0.0")
  def precisionByThreshold(): RDD[(Double, Double)] = createCurve(Precision)

  /**
   * Returns the (threshold, recall) curve.
   */
  @Since("1.0.0")
  def recallByThreshold(): RDD[(Double, Double)] = createCurve(Recall)
```

开始时不太理解，直接调用保存指标，结果发现指标竟然有2个G那么大！于是分析了源代码：

```scala
/**
 * Evaluator for binary classification.
 *
 * @param scoreAndLabels an RDD of (score, label) or (score, label, weight) tuples.
 * @param numBins if greater than 0, then the curves (ROC curve, PR curve) computed internally
 *                will be down-sampled to this many "bins". If 0, no down-sampling will occur.
 *                This is useful because the curve contains a point for each distinct score
 *                in the input, and this could be as large as the input itself -- millions of
 *                points or more, when thousands may be entirely sufficient to summarize
 *                the curve. After down-sampling, the curves will instead be made of approximately
 *                `numBins` points instead. Points are made from bins of equal numbers of
 *                consecutive points. The size of each bin is
 *                `floor(scoreAndLabels.count() / numBins)`, which means the resulting number
 *                of bins may not exactly equal numBins. The last bin in each partition may
 *                be smaller as a result, meaning there may be an extra sample at
 *                partition boundaries.
 */
@Since("1.0.0")
class BinaryClassificationMetrics @Since("3.0.0") (
    @Since("1.3.0") val scoreAndLabels: RDD[_ <: Product],
    @Since("1.3.0") val numBins: Int = 1000)
```

第一个参数解释的比较清楚，就是(预测值，标签)或者(预测值，标签，权重)。 

看第二个参数numBins的解释：如果>0, (ROC曲线,PR曲线)将欠采样到这么多个"箱子"，如果是0则不会欠采样。每个分区每个箱子大小是

floor(scoreAndLabels.count() / numBins)，向下取整，意味着每个分区箱子的个数可能会多出一个。如果不分箱，那会导致计算量巨大，计算的结果也巨大。













发现三个指标都是调用createCurve，只是参数不同，点进createCurve：

```scala
  private def createCurve(y: BinaryClassificationMetricComputer): RDD[(Double, Double)] = {
    confusions.map { case (s, c) =>
      (s, y(c))
    }
  }
```

先看参数： 发现它接受一个BinaryClassificationMetricComputer类型的参数，实际上上面三个参数类型都是它的子类型:

![image-20220524100513681](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220524100513681.png)

再看的实现：confusions转换一下就的到结果，precisionByThreshold返回的RDD，左边是threshold，右边是precision
 所以这里的s，就是threshold，y(c)，就是precision， y是传入的参数，也就是createCurve(Precision)中的Precision； 

下面就先看看Precision是什么 

```scala
private[evaluation] object Precision extends BinaryClassificationMetricComputer {
  override def apply(c: BinaryConfusionMatrix): Double = {
   // totalPositives = TP + FP
    val totalPositives = c.weightedTruePositives + c.weightedFalsePositives
    if (totalPositives == 0.0) {
      1.0
    } else {
      // Precision = TP / (TP + FP)
      c.weightedTruePositives / totalPositives
    }
  }
}
```

这不就是精确率公式嘛！那么c自然就是混淆矩阵了，而c是confusions的第二列。这样confusions的结构就清晰了：它的第一列是阈值threshold,第二列是该阈值对应的混淆矩阵：



这就厉害了，直接把每个阈值对应的混淆矩阵全构造出来了；它是如何做到的，源码如下：

```scala
 confusions: RDD[(Double, BinaryConfusionMatrix)]) = {
    // Create a bin for each distinct score value, count weighted positives and
    // negatives within each bin, and then sort by score values in descending order.
   //按照预测概率值分组聚合
   //(score, BinaryLabelCounter) 每个预测值(降序排列)，都统计其正负样本数,是根据label计算的
    val counts = scoreLabelsWeight.combineByKey(
      createCombiner = (labelAndWeight: (Double, Double)) =>
        new BinaryLabelCounter(0.0, 0.0) += (labelAndWeight._1, labelAndWeight._2),
      mergeValue = (c: BinaryLabelCounter, labelAndWeight: (Double, Double)) =>
        c += (labelAndWeight._1, labelAndWeight._2),
      mergeCombiners = (c1: BinaryLabelCounter, c2: BinaryLabelCounter) => c1 += c2
    ).sortByKey(ascending = false) //保证了分区内和分区间都有序

   // binnedCounts的数量跟numBins有关
    val binnedCounts =
      // ==0不分箱
      if (numBins == 0) {
        // Use original directly
        counts
      } else {//分箱
        val countsSize = counts.count()
        // Group the iterator into chunks of about countsSize / numBins points,
        // so that the resulting number of bins is about numBins
        //每个箱子的大小
        val grouping = countsSize / numBins
        if (grouping < 2) {
          // numBins was more than half of the size; no real point in down-sampling to bins
          logInfo(s"Curve is too small ($countsSize) for $numBins bins to be useful")
          counts
        } else {
          //注意到，这里是每个分区都这么分箱
          counts.mapPartitions { iter =>
            if (iter.hasNext) {
              var score = Double.NaN
              var agg = new BinaryLabelCounter()
              var cnt = 0L
              iter.flatMap { pair =>
                score = pair._1
                agg += pair._2
                cnt += 1
                if (cnt == grouping) {
                  // The score of the combined point will be just the last one's score,
                  // which is also the minimal in each chunk since all scores are already
                  // sorted in descending.
                  // The combined point will contain all counts in this chunk. Thus, calculated
                  // metrics (like precision, recall, etc.) on its score (or so-called threshold)
                  // are the same as those without sampling.
                  /** 箱子最后一个预测值是这个箱子的最小的预测值，超过这个预测值的都被累计了，把这个预测值作为阈值的混淆矩阵
                  已经有了， 从而可以计算指标(准召率等)
                  */
                  val ret = (score, agg)
                  agg = new BinaryLabelCounter()
                  cnt = 0
                  Some(ret)
                } else None
              } ++ {
                if (cnt > 0) {
                  Iterator.single((score, agg))
                } else Iterator.empty
              }
            } else Iterator.empty
          }
        }
      }
  //分区内聚合
   val agg = binnedCounts.values.mapPartitions { iter =>
    val agg = new BinaryLabelCounter()
      iter.foreach(agg += _)
      Iterator(agg)
    }.collect()  //把每个分区的计数收集到一起
   
   //分区内聚合，scanLeft产生了一个数组，聚合所有分区的计数，长度是分区数+1
    val partitionwiseCumulativeCounts =
      agg.scanLeft(new BinaryLabelCounter())((agg, c) => agg.clone() += c)
   
    val totalCount = partitionwiseCumulativeCounts.last
    logInfo(s"Total counts: $totalCount")
   
    val cumulativeCounts = binnedCounts.mapPartitionsWithIndex(
      (index: Int, iter: Iterator[(Double, BinaryLabelCounter)]) => {
        val cumCount = partitionwiseCumulativeCounts(index)
        iter.map { case (score, c) =>
          cumCount += c
          (score, cumCount.clone())
        }
      }, preservesPartitioning = true)
    cumulativeCounts.persist()
   //计算混淆矩阵， 有了混淆矩阵，那些指标根据我们传的公式就可以计算指标了
    val confusions = cumulativeCounts.map { case (score, cumCount) =>
      (score, BinaryConfusionMatrixImpl(cumCount, totalCount).asInstanceOf[BinaryConfusionMatrix])
    }
    (cumulativeCounts, confusions)
  }
```

从源码发现它是一个RDD, 数据为 (cumulativeCounts, confusions)，cumulativeCounts又从binnedCounts映射来，再回到binnedCounts的计算上来：

如果numBins==0，就是counts,那counts是什么呢？是scoreLabelsWeight.combineByKey的结果，而scoreLabelsWeight是scoreAndLabels映射来的，scoreAndLabels不就是我们的预测概率和样本标签吗？

```scala
  val scoreLabelsWeight: RDD[(Double, (Double, Double))] = scoreAndLabels.map {
    case (prediction: Double, label: Double, weight: Double) =>
      require(weight >= 0.0, s"instance weight, $weight has to be >= 0.0")
      (prediction, (label, weight))
    case (prediction: Double, label: Double) =>
      (prediction, (label, 1.0))
    case other =>
      throw new IllegalArgumentException(s"Expected tuples, got $other")
  }
```

所以， 产出结果很大，就是因为numBins采用了默认值0。  解决办法就是传入numBins参数：

```scala
val metrics = new BinaryClassificationMetrics(predictionAndLabel,100)
```









这个类输出的threashold其实是不连续的，但想计算指定threashold的指标怎么办呢？其实它的准召指标和这个评估类输出的某个threashold的指标是一样的，就是比你指定的threshold大的、最小的那个threashold,因为它们的混淆矩阵是一样的
