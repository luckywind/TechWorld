# 源码

```scala
case class DpuHashAggregateExec(
                                 requiredChildDistributionExpressions: Option[Seq[Expression]],
                                 groupingExpressions: Seq[NamedExpression],
                                 aggregateExpressions: Seq[DpuAggregateExpression],
                                 aggregateAttributes: Seq[Attribute],
                                 resultExpressions: Seq[NamedExpression],
                                 child: SparkPlan
                               ) extends UnaryExecNode with DpuExec with Arm {
  override lazy val metrics = Map(
    "numOutputRows" -> SQLMetrics.createMetric(sparkContext, "number of output rows"),
    "spillSize" -> SQLMetrics.createSizeMetric(sparkContext, "spill size"),
    "aggTime" -> SQLMetrics.createTimingMetric(sparkContext, "time in aggregation build"))

  override protected def doExecute(): RDD[InternalRow] = throw new IllegalStateException(
    "Row-based execution should not occur for this class")

  override def output: Seq[Attribute] = resultExpressions.map(_.toAttribute)

  override protected def withNewChildInternal(newChild: SparkPlan): SparkPlan = {
    copy(child = newChild)
  }

  // lifted directly from `BaseAggregateExec.inputAttributes`, edited comment.
  def inputAttributes: Seq[Attribute] = {
    val modes = aggregateExpressions.map(_.mode).distinct
    if (modes.contains(Final) || modes.contains(PartialMerge)) {
      // SPARK-31620: when planning aggregates, the partial aggregate uses aggregate function's
      // `inputAggBufferAttributes` as its output. And Final and PartialMerge aggregate rely on the
      // output to bind references used by `mergeAggregates`. But if we copy the
      // aggregate function somehow after aggregate planning, the `DeclarativeAggregate` will
      // be replaced by a new instance with new `inputAggBufferAttributes`. Then Final and
      // PartialMerge aggregate can't bind the references used by `mergeAggregates` with the output
      // of the partial aggregate, as they use the `inputAggBufferAttributes` of the
      // original `DeclarativeAggregate` before copy. Instead, we shall use
      // `inputAggBufferAttributes` after copy to match the new `mergeExpressions`.
      对于局部聚合，使用聚合函数的inputAggBufferAttributes作为output. Final/PartialMerge聚合依赖output绑定mergeAggregates使用的reference.
      val aggAttrs = inputAggBufferAttributes
      child.output.dropRight(aggAttrs.length) ++ aggAttrs
    } else {
      child.output
    }
  }

  private val inputAggBufferAttributes: Seq[Attribute] = {
    aggregateExpressions
      // there're exactly four cases needs `inputAggBufferAttributes` from child according to the
      // agg planning in `AggUtils`: Partial -> Final, PartialMerge -> Final,
      // Partial -> PartialMerge, PartialMerge -> PartialMerge.
      .filter(a => a.mode == Final || a.mode == PartialMerge)
      .flatMap(_.aggregateFunction.inputAggBufferAttributes)
  }

  private lazy val uniqueModes: Seq[AggregateMode] = aggregateExpressions.map(_.mode).distinct

  override protected def doExecuteColumnar(): RDD[ColumnarBatch] = {
    val startTime = System.currentTimeMillis()
    logInfo("prof: start HashAggregate, current time: " + startTime)
    val numOutputRows = longMetric("numOutputRows")
    val spillSize = longMetric("spillSize")
    val aggTime = longMetric("aggTime")

    val modeInfo = AggregateModeInfo(uniqueModes)

    val rdd = child.executeColumnar()
    val value: RDD[ColumnarBatch] = rdd.mapPartitions { cbIter =>
      new DpuAggregateIterator(cbIter, inputAttributes, groupingExpressions, aggregateExpressions, aggregateAttributes, resultExpressions, modeInfo, numOutputRows, spillSize, aggTime)
    }

    logInfo(s"prof: end HashAggregate, use time: ${System.currentTimeMillis() - startTime}")
    value
  }
}
```

