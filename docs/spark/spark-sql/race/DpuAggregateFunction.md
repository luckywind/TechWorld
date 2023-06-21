# DpuAggregateFunction

```scala
trait DpuAggregateFunction extends DpuExpression
  with DpuUnevaluable {
  /**
   * These are values that spark calls initial because it uses
   * them to initialize the aggregation buffer, and returns them in case
   * of an empty aggregate when there are no expressions.
   *
   * In our case they are only used in a very specific case:
   * the empty input reduction case. In this case we don't have input
   * to reduce, but we do have reduction functions, so each reduction function's
   * `initialValues` is invoked to populate a single row of output.
   * */
  val initialValues: Seq[Expression]

  /**
    pre update: 就是把input改成update需要的batch
    
   * Using the child reference, define the shape of input batches sent to
   * the update expressions
   *
   * @note this can be thought of as "pre" update: as update consumes its
   *       output in order
   */
  val inputProjection: Seq[Expression]

  def updateAggregates: Seq[AggregateOperator]

  def mergeAggregates: Seq[AggregateOperator]

  /**
   * This takes the output of `postMerge` computes the final result of the aggregation.
   *
   * @note `evaluateExpression` is bound to `aggBufferAttributes`, so the references used in
   *       `evaluateExpression` must also be used in `aggBufferAttributes`.
   */
  val evaluateExpression: Expression

  /** Attributes of fields in aggBufferSchema. */
  def aggBufferAttributes: Seq[AttributeReference]

  /**
   * Attributes of fields in input aggregation buffers (immutable aggregation buffers that are
   * merged with mutable aggregation buffers in the merge() function or merge expressions).
   * These attributes are created automatically by cloning the [[aggBufferAttributes]].
   */
  final lazy val inputAggBufferAttributes: Seq[AttributeReference] =
    aggBufferAttributes.map(_.newInstance())

  def sql(isDistinct: Boolean): String = {
    val distinct = if (isDistinct) "DISTINCT " else ""
    s"$prettyName($distinct${children.map(_.sql).mkString(", ")})"
  }

  /** String representation used in explain plans. */
  def toAggString(isDistinct: Boolean): String = {
    val start = if (isDistinct) "(distinct " else "("
    prettyName + flatArguments.mkString(start, ", ", ")")
  }

}
```

# DpuAggregateExpression

就是DpuAggregateFunction的一个封装，会把DpuAggregateFunction的AggregateMode、dataType、children拿出来

