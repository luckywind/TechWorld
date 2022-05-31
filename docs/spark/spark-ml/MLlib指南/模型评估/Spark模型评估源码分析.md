[参考](https://blog.csdn.net/wo334499/article/details/51689609)

```scala
  def fMeasureByThreshold(): RDD[(Double, Double)] = fMeasureByThreshold(1.0)

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

发现三个指标都是调用createCurve，只是参数不同，点进createCurve：

```scala
  private def createCurve(y: BinaryClassificationMetricComputer): RDD[(Double, Double)] = {
    confusions.map { case (s, c) =>
      (s, y(c))
    }
  }
```

发现它接受一个BinaryClassificationMetricComputer类型的参数，实际上上面三个参数类型都是它的子类型:

![image-20220524100513681](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220524100513681.png)

另外，从它的实现上来看，confusions转换一下就的到结果，s就是Threadshold, y(c)就是指标。

confusions就很关键,源码如下：

```scala
 confusions: RDD[(Double, BinaryConfusionMatrix)]) = {
    // Create a bin for each distinct score value, count weighted positives and
    // negatives within each bin, and then sort by score values in descending order.
    val counts = scoreLabelsWeight.combineByKey(
      createCombiner = (labelAndWeight: (Double, Double)) =>
        new BinaryLabelCounter(0.0, 0.0) += (labelAndWeight._1, labelAndWeight._2),
      mergeValue = (c: BinaryLabelCounter, labelAndWeight: (Double, Double)) =>
        c += (labelAndWeight._1, labelAndWeight._2),
      mergeCombiners = (c1: BinaryLabelCounter, c2: BinaryLabelCounter) => c1 += c2
    ).sortByKey(ascending = false)

   // binnedCounts的数量跟numBins有关
    val binnedCounts =
      // Only down-sample if bins is > 0
      if (numBins == 0) {
        // Use original directly
        counts
      } else {
        val countsSize = counts.count()
        // Group the iterator into chunks of about countsSize / numBins points,
        // so that the resulting number of bins is about numBins
        val grouping = countsSize / numBins
        if (grouping < 2) {
          // numBins was more than half of the size; no real point in down-sampling to bins
          logInfo(s"Curve is too small ($countsSize) for $numBins bins to be useful")
          counts
        } else {
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
       val agg = binnedCounts.values.mapPartitions { iter =>
      val agg = new BinaryLabelCounter()
      iter.foreach(agg += _)
      Iterator(agg)
    }.collect()
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

