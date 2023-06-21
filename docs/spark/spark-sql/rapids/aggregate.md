

# trait GpuAggregateFunction



## GpuAverage

```scala
case class GpuAverage(child: Expression) extends GpuAggregateFunction
    with GpuReplaceWindowFunction {
    }
```



## GpuTypedImperativeSupportedAggregateExecMeta

SortAggregateExec 和 ObjectHashAggregateExec的基类，可能包含TypedImperativeAggregate函数

### overrideAggBufTypes

覆盖TypedImperativeAggregate函数创建的aggregation buffer的数据类型

1. 首先，遍历所有aggregateFunctions查找这样的 attributes：引用TypedImperativeAggregate函数所创建的aggregation buffer
2. 提取它们在GPU运行时的真实的数据类型，并映射到对应的表达式ID上
3. 最后，遍历aggregateAttributes 和 resultExpressions，在Meta中覆盖数据类型，从而确保TypeCheck  tag运行时真正的类型

```scala
  private val mayNeedAggBufferConversion: Boolean =
    agg.aggregateExpressions.exists { expr =>
      expr.aggregateFunction.isInstanceOf[TypedImperativeAggregate[_]] &&
          (expr.mode == Partial || expr.mode == PartialMerge)//只有这两种mode需要aggBuffer?
    }

  // overriding data types of Aggregation Buffers if necessary
  if (mayNeedAggBufferConversion) overrideAggBufTypes()
```

具体实现

```scala
  private def overrideAggBufTypes(): Unit = {
    val desiredAggBufTypes = mutable.HashMap.empty[ExprId, DataType]
    val desiredInputAggBufTypes = mutable.HashMap.empty[ExprId, DataType]
    // Collects exprId from TypedImperativeAggBufferAttributes, and maps them to the data type
    // of `TypedImperativeAggExprMeta.aggBufferAttribute`.
    aggregateExpressions.map(_.childExprs.head).foreach {
      case aggMeta: TypedImperativeAggExprMeta[_] =>
        val aggFn = aggMeta.wrapped.asInstanceOf[TypedImperativeAggregate[_]]
        val desiredType = aggMeta.aggBufferAttribute.dataType

        desiredAggBufTypes(aggFn.aggBufferAttributes.head.exprId) = desiredType
        desiredInputAggBufTypes(aggFn.inputAggBufferAttributes.head.exprId) = desiredType

      case _ =>
    }

    // Overrides the data types of typed imperative aggregation buffers for type checking
    aggregateAttributes.foreach { attrMeta =>
      attrMeta.wrapped match {
        case ar: AttributeReference if desiredAggBufTypes.contains(ar.exprId) =>
          attrMeta.overrideDataType(desiredAggBufTypes(ar.exprId))
        case _ =>
      }
    }
    resultExpressions.foreach { retMeta =>
      retMeta.wrapped match {
        case ar: AttributeReference if desiredInputAggBufTypes.contains(ar.exprId) =>
          retMeta.overrideDataType(desiredInputAggBufTypes(ar.exprId))
        case _ =>
      }
    }
  }
```

### outputTypeMetas

输出类型也有一个meta, 注意，这个meta是resultExpressions的typeMeta, 不需要类型转换时为空

```scala
  override protected lazy val outputTypeMetas: Option[Seq[DataTypeMeta]] =
    if (mayNeedAggBufferConversion) {
      Some(resultExpressions.map(_.typeMeta))
    } else {
      None
    }
```

### tagPlanForGpu

```scala
  override def tagPlanForGpu(): Unit = {
    super.tagPlanForGpu()
     构建类型转换器并绑定到plan上
    // If a typedImperativeAggregate function run across CPU and GPU (ex: Partial mode on CPU,
    // Merge mode on GPU), it will lead to a runtime crash. Because aggregation buffers produced
    // by the previous stage of function are NOT in the same format as the later stage consuming.
    // If buffer converters are available for all incompatible buffers, we will build these buffer
    // converters and bind them to certain plans, in order to integrate these converters into
    // R2C/C2R Transitions during GpuTransitionOverrides to fix the gap between GPU and CPU.
    // Otherwise, we have to fall back all Aggregate stages to CPU once any of them did fallback.
    //
    // The binding also works when AQE is on, since it leverages the TreeNodeTag to cache buffer
    // converters.
    GpuTypedImperativeSupportedAggregateExecMeta.handleAggregationBuffer(this)
  }
```

