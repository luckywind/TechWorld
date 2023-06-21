# Aggregation实现

## 概述

### 逻辑算子树到物理算子树

从 Unresolved LogicalPlan 到 Analyzed LogicalPlan 经过了 4条规则的处理 。 对于聚合查询来 说，比较重要的是其中的 ResolveFunctions规则，用来分析聚合函数。对于 UnresolvedFunction 表达式， Analyzer 会根据函数名和函数参数去 SessionCatalog 中查找。 

对于聚合查询，逻辑算子树转换为物理算子树，必不可少的是 Aggregation 转换策略 。 实 际上， Aggregation策略是基于PhysicalAggregation的。与PhysicalOperation类似， PhysicalAggregation也是一种逻辑算子树的模式，用来匹配逻辑算子树中的 Aggregate节点并提 取该节点中的相关信息 。 PhysicalAggregation在提取信息时会进行以下转换 

![image-20230612112723310](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230612112723310.png)

- **去重**:对 Aggregate逻辑算子节点中多次重复出现的聚合操作进行去重， 参见 Physical­ Aggregation中aggregateExpressions表达式的逻辑，收集resultExpressions中的聚合函数表 达式，然后执行 distinct操作 。

- **命名**: 参见上述代码中 namedGroupingExpressions 的操作，对未命名的分组表达式( Group­ ing expressions)进行命名(套上一个 Alias表达式)，这样方便在后续聚合过程中进行引 用。

- **分离**:对应 rewrittenResultExpressions 中的操作逻辑，从最后结果中分离出聚合计算本身 的值，例如“count+l”会被拆分为 count (AggregateExpression)和“count.resultAttribute + 1”的最终计算。

Aggregation策略会根据这些信息生成相应的物理计划：

![image-20230612112853877](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230612112853877.png)

例如planAggregateWithoutDistinct方法生成两个HashAggregate物理算子树节点，分别进行局部聚合与最终聚合，最后再生成的SparkPlan中添加Exchange节点，统一排序与分区信息，生成物理计划。

## 聚合函数AggregateFunciton

聚合函数( AggregateFunction) 是聚合查询中 非常重要 的元素 。在实现上，聚合函数是表达 式中的一种，和 Catalyste 中定义的聚合表达式( AggregationExpression)紧密关联。无论是在逻 辑算子树还是物理算子树中，聚合函数都是以聚合表达式的形式进行封装的，同时聚合函数表 达式中也定义了直接生成聚合表达式的方法 。

是以下两个聚合函数接口的超类：

- ImperativeAggregate：显示的initialize(), update(),  merge()函数，操作基于行的agg buffer
- DeclarativeAggregate：使用catalyst表达式的聚合函数

都必须定义agg buffer的aggBufferSchema和aggBufferAttributes来保存局部聚合结果。运行时，多个聚合函数使用同一个算子计算，使用了一个合并的agg buffer。

```scala
abstract class AggregateFunction extends Expression {

  /** An aggregate function is not foldable. */
  final override def foldable: Boolean = false

  /** The schema of the aggregation buffer. */
  def aggBufferSchema: StructType

  /** Attributes of fields in aggBufferSchema. */
  def aggBufferAttributes: Seq[AttributeReference]

  /**
   * Attributes of fields in input aggregation buffers (immutable aggregation buffers that are
   * merged with mutable aggregation buffers in the merge() function or merge expressions).
   * These attributes are created automatically by cloning the [[aggBufferAttributes]].
   */
  def inputAggBufferAttributes: Seq[AttributeReference]

  /**
   * Result of the aggregate function when the input is empty.
   */
  def defaultResult: Option[Literal] = None

  /**
   * Creates [[AggregateExpression]] with `isDistinct` flag disabled.
   *
   * @see `toAggregateExpression(isDistinct: Boolean)` for detailed description
   */
  def toAggregateExpression(): AggregateExpression = toAggregateExpression(isDistinct = false)

  /**
    ⚠️：AggregateExpression是AggregateFunction的封装，AggregateFunction不应该单独使用
 
   * Wraps this [[AggregateFunction]] in an [[AggregateExpression]] and sets `isDistinct`
   * flag of the [[AggregateExpression]] to the given value because
   * [[AggregateExpression]] is the container of an [[AggregateFunction]], aggregation mode,
   * and the flag indicating if this aggregation is distinct aggregation or not.
   * An [[AggregateFunction]] should not be used without being wrapped in
   * an [[AggregateExpression]].
   */
  def toAggregateExpression(
      isDistinct: Boolean,
      filter: Option[Expression] = None): AggregateExpression = {
    AggregateExpression(
      aggregateFunction = this,
      mode = Complete,
      isDistinct = isDistinct,
      filter = filter)
  }

}
```



### DeclarativeAggregate聚合函数

DeclarativeAggreg由聚合函数是一类直接由 Catalyst中的表达式(Expressions)构建的聚合 函数，主要逻辑通过调用 4 个表达式完成，分别是 

- initialValues (聚合缓冲区初始化表达式)、 

- updateExpressions (聚合缓冲区更新表达式)、 
- mergeExpressions (聚合缓冲区合并表达式)和 
- evaluateExpression (最终结果生成表达式) 

首先需要实现bufferAttributes定义agg buffer字段， 接下来用这些attribte定义updateExpressions、mergeExpressions和evaluateExpressions.

```scala
abstract class DeclarativeAggregate
  extends AggregateFunction
  with Serializable {

  val initialValues: Seq[Expression]
   // 基于输入行更新agg buffer
  val updateExpressions: Seq[Expression]

  /**聚合两个agg buffer
   * A sequence of expressions for merging two aggregation buffers together. When defining these
   * expressions, you can use the syntax `attributeName.left` and `attributeName.right` to refer
   * to the attributes corresponding to each of the buffers being merged (this magic is enabled
   * by the [[RichAttribute]] implicit class).
   */
  val mergeExpressions: Seq[Expression]

  /**
    一个返回聚合函数最终结果的表达式
   * An expression which returns the final value for this aggregate function. Its data type should
   * match this expression's [[dataType]].
   */
  val evaluateExpression: Expression

  /** An expression-based aggregate's bufferSchema is derived from bufferAttributes. */
  final override def aggBufferSchema: StructType = StructType.fromAttributes(aggBufferAttributes)

  final lazy val inputAggBufferAttributes: Seq[AttributeReference] =
    aggBufferAttributes.map(_.newInstance())

  /**
   * A helper class for representing an attribute used in merging two
   * aggregation buffers. When merging two buffers, `bufferLeft` and `bufferRight`,
   * we merge buffer values and then update bufferLeft. A [[RichAttribute]]
   * of an [[AttributeReference]] `a` has two functions `left` and `right`,
   * which represent `a` in `bufferLeft` and `bufferRight`, respectively.
   */
  implicit class RichAttribute(a: AttributeReference) {
    /** Represents this attribute at the mutable buffer side. */
    def left: AttributeReference = a

    /** Represents this attribute at the input buffer side (the data value is read-only). */
    def right: AttributeReference = inputAggBufferAttributes(aggBufferAttributes.indexOf(a))
  }

  final override def eval(input: InternalRow = null): Any =
    throw QueryExecutionErrors.cannotEvaluateExpressionError(this)

  final override protected def doGenCode(ctx: CodegenContext, ev: ExprCode): ExprCode =
    throw QueryExecutionErrors.cannotGenerateCodeForExpressionError(this)
}

```



### lmperativeAggregate聚合函数

需要<font color=red>**显式**</font>地实现 initialize、 update和 merge 方法来操作聚合缓冲区中的数据 。一个比较 显著的不同是， ImperativeAggregate聚合函数所处理的聚合缓冲区本质上是基于行(InternalRow 类型)的 。

```scala
abstract class ImperativeAggregate extends AggregateFunction with CodegenFallback {

  protected val mutableAggBufferOffset: Int

  def withNewMutableAggBufferOffset(newMutableAggBufferOffset: Int): ImperativeAggregate

  protected val inputAggBufferOffset: Int

  def withNewInputAggBufferOffset(newInputAggBufferOffset: Int): ImperativeAggregate

  def initialize(mutableAggBuffer: InternalRow): Unit

  def update(mutableAggBuffer: InternalRow, inputRow: InternalRow): Unit

  def merge(mutableAggBuffer: InternalRow, inputAggBuffer: InternalRow): Unit
}
```





### TypedlmperativeAggregate聚合函数

TypedimperativeAggregate[T) 聚合函数 允许使用用 户自定义的 Java 对象 T 作为内部的聚合 缓 冲 区 ，因此这种类型的 聚合函数是最灵活 的 。 

```scala
abstract class TypedImperativeAggregate[T] extends ImperativeAggregate {

  /**
   * Creates an empty aggregation buffer object. This is called before processing each key group
   * (group by key).
   *
   * @return an aggregation buffer object
   */
  def createAggregationBuffer(): T

  /**
   * Updates the aggregation buffer object with an input row and returns a new buffer object. For
   * performance, the function may do in-place update and return it instead of constructing new
   * buffer object.
   *
   * This is typically called when doing Partial or Complete mode aggregation.
   *
   * @param buffer The aggregation buffer object.
   * @param input an input row
   */
  def update(buffer: T, input: InternalRow): T

  /**
   * Merges an input aggregation object into aggregation buffer object and returns a new buffer
   * object. For performance, the function may do in-place merge and return it instead of
   * constructing new buffer object.
   *
   * This is typically called when doing PartialMerge or Final mode aggregation.
   *
   * @param buffer the aggregation buffer object used to store the aggregation result.
   * @param input an input aggregation object. Input aggregation object can be produced by
   *              de-serializing the partial aggregate's output from Mapper side.
   */
  def merge(buffer: T, input: T): T

  /**
   * Generates the final aggregation result value for current key group with the aggregation buffer
   * object.
   *
   * Developer note: the only return types accepted by Spark are:
   *   - primitive types
   *   - InternalRow and subclasses
   *   - ArrayData
   *   - MapData
   *
   * @param buffer aggregation buffer object.
   * @return The aggregation result of current key group
   */
  def eval(buffer: T): Any

  /** Serializes the aggregation buffer object T to Array[Byte] */
  def serialize(buffer: T): Array[Byte]

  /** De-serializes the serialized format Array[Byte], and produces aggregation buffer object T */
  def deserialize(storageFormat: Array[Byte]): T

  final override def initialize(buffer: InternalRow): Unit = {
    buffer(mutableAggBufferOffset) = createAggregationBuffer()
  }

  final override def update(buffer: InternalRow, input: InternalRow): Unit = {
    buffer(mutableAggBufferOffset) = update(getBufferObject(buffer), input)
  }

  final override def merge(buffer: InternalRow, inputBuffer: InternalRow): Unit = {
    val bufferObject = getBufferObject(buffer)
    // The inputBuffer stores serialized aggregation buffer object produced by partial aggregate
    val inputObject = deserialize(inputBuffer.getBinary(inputAggBufferOffset))
    buffer(mutableAggBufferOffset) = merge(bufferObject, inputObject)
  }

  final override def eval(buffer: InternalRow): Any = {
    eval(getBufferObject(buffer))
  }

  private[this] val anyObjectType = ObjectType(classOf[AnyRef])
  private def getBufferObject(bufferRow: InternalRow): T = {
    getBufferObject(bufferRow, mutableAggBufferOffset)
  }

  final override lazy val aggBufferAttributes: Seq[AttributeReference] = {
    // Underlying storage type for the aggregation buffer object
    Seq(AttributeReference("buf", BinaryType)())
  }

  final override lazy val inputAggBufferAttributes: Seq[AttributeReference] =
    aggBufferAttributes.map(_.newInstance())

  final override def aggBufferSchema: StructType = StructType.fromAttributes(aggBufferAttributes)

  /**
   * In-place replaces the aggregation buffer object stored at buffer's index
   * `mutableAggBufferOffset`, with SparkSQL internally supported underlying storage format
   * (BinaryType).
   *
   * This is only called when doing Partial or PartialMerge mode aggregation, before the framework
   * shuffle out aggregate buffers.
   */
  final def serializeAggregateBufferInPlace(buffer: InternalRow): Unit = {
    buffer(mutableAggBufferOffset) = serialize(getBufferObject(buffer))
  }

  /**
   * Merge an input buffer into the aggregation buffer, where both buffers contain the deserialized
   * java object. This function is used by aggregating accumulators.
   *
   * @param buffer the aggregation buffer that is updated.
   * @param inputBuffer the buffer that is merged into the aggregation buffer.
   */
  final def mergeBuffersObjects(buffer: InternalRow, inputBuffer: InternalRow): Unit = {
    val bufferObject = getBufferObject(buffer)
    val inputObject = getBufferObject(inputBuffer, inputAggBufferOffset)
    buffer(mutableAggBufferOffset) = merge(bufferObject, inputObject)
  }

  private def getBufferObject(buffer: InternalRow, offset: Int): T = {
    buffer.get(offset, anyObjectType).asInstanceOf[T]
  }
}

```











## 聚合执行

聚合查询的最终执行有两种方式:基于排序的聚合执行方式( SortAg­ gregateExec)与基于 Hash 的聚合执行方式( HashAggregateExec) 。 在后续版本中，又加入了 ObjectHashAggreg耐 Exec 的执行方式( SPARK-17949)

### 执行框架 Aggregationlterator

聚合执行框架指的是聚合过程中抽象出来的通用功能，包括聚合函数的初始化、聚合缓冲 区更新合并函数和聚合结果生成函数等 。 这些功能都在聚合迭代器( Aggregationlterator)中得 到了实现 。

1. 聚合函数初始化
   - funcWithBoundReference
   - funcWithUpdatedAggBufferOffset
2. 数据处理函数processRow
   其参数类型是 (InternalRow, InternalRow)，分别代表当前的聚合缓冲区 currentBufferRow和输入数据行 row，输出是 Unit类 型。数据处理函数 processRow 的核心操作是获取各 Aggregation 中的 update 函数或 merge 函数。
3. 生成聚合结果
   入类型是( UnsafeRow, Internal- Row)，输出的是 UnsafeRow数据行类型。 对于 Partial或 PartialMerge模式的聚合函数，因为只 是中间结果，所以需要保存 grouping语句与 buffer 中所有的属性;对于 Final和 Complete 聚合 模式，直接对应 resultExpressions 表达式 

## Average

```scala
abstract class AverageBase
  extends DeclarativeAggregate
  with ImplicitCastInputTypes
  with UnaryLike[Expression] {

  // Whether to use ANSI add or not during the execution.
  def useAnsiAdd: Boolean

  override def prettyName: String = getTagValue(FunctionRegistry.FUNC_ALIAS).getOrElse("avg")

  override def inputTypes: Seq[AbstractDataType] =
    Seq(TypeCollection(NumericType, YearMonthIntervalType, DayTimeIntervalType))

  override def checkInputDataTypes(): TypeCheckResult =
    TypeUtils.checkForAnsiIntervalOrNumericType(child.dataType, "average")

  override def nullable: Boolean = true

  // Return data type.
  override def dataType: DataType = resultType

  final override val nodePatterns: Seq[TreePattern] = Seq(AVERAGE)

  protected lazy val resultType = child.dataType match {
    case DecimalType.Fixed(p, s) =>
      DecimalType.bounded(p + 4, s + 4)
    case _: YearMonthIntervalType => YearMonthIntervalType()
    case _: DayTimeIntervalType => DayTimeIntervalType()
    case _ => DoubleType
  }

  lazy val sumDataType = child.dataType match {
    case _ @ DecimalType.Fixed(p, s) => DecimalType.bounded(p + 10, s)
    case _: YearMonthIntervalType => YearMonthIntervalType()
    case _: DayTimeIntervalType => DayTimeIntervalType()
    case _ => DoubleType
  }

  lazy val sum = AttributeReference("sum", sumDataType)()
  lazy val count = AttributeReference("count", LongType)()

  override lazy val aggBufferAttributes = sum :: count :: Nil

  override lazy val initialValues = Seq(
    /* sum = */ Literal.default(sumDataType),
    /* count = */ Literal(0L)
  )

  protected def getMergeExpressions = Seq(
    /* sum = */ Add(sum.left, sum.right, useAnsiAdd),
    /* count = */ count.left + count.right
  )

  // If all input are nulls, count will be 0 and we will get null after the division.
  // We can't directly use `/` as it throws an exception under ansi mode.
  protected def getEvaluateExpression = child.dataType match {
    case _: DecimalType =>
      DecimalPrecision.decimalAndDecimal()(
        Divide(
          CheckOverflowInSum(sum, sumDataType.asInstanceOf[DecimalType], !useAnsiAdd),
          count.cast(DecimalType.LongDecimal), failOnError = false)).cast(resultType)
    case _: YearMonthIntervalType =>
      If(EqualTo(count, Literal(0L)),
        Literal(null, YearMonthIntervalType()), DivideYMInterval(sum, count))
    case _: DayTimeIntervalType =>
      If(EqualTo(count, Literal(0L)),
        Literal(null, DayTimeIntervalType()), DivideDTInterval(sum, count))
    case _ =>
      Divide(sum.cast(resultType), count.cast(resultType), failOnError = false)
  }

  protected def getUpdateExpressions: Seq[Expression] = Seq(
    /* sum = */
    Add(
      sum,
      coalesce(child.cast(sumDataType), Literal.default(sumDataType)),
      failOnError = useAnsiAdd),
    /* count = */ If(child.isNull, count, count + 1L)
  )

  // The flag `useAnsiAdd` won't be shown in the `toString` or `toAggString` methods
  override def flatArguments: Iterator[Any] = Iterator(child)
}

```

