# 引子

rapids在应用GpuOverrides()规则时，如果一个plan可以被替换，那么首先进行cost模型优化（其实就是根据cost再次进行tag），再进行算子替换

```scala
  private def applyOverrides(plan: SparkPlan, conf: RapidsConf): SparkPlan = {
    val wrap = GpuOverrides.wrapAndTagPlan(plan, conf)
    val reasonsToNotReplaceEntirePlan = wrap.getReasonsNotToReplaceEntirePlan
    if (conf.allowDisableEntirePlan && reasonsToNotReplaceEntirePlan.nonEmpty) {
      if (conf.shouldExplain) {
        logWarning("Can't replace any part of this plan due to: " +
            s"${reasonsToNotReplaceEntirePlan.mkString(",")}")
      }
      plan
    } else {
      //这里执行优化，会加载CostBasedOptimizer类
      val optimizations = GpuOverrides.getOptimizations(wrap, conf)
      wrap.runAfterTagRules()
      if (conf.shouldExplain) {
        wrap.tagForExplain()
        val explain = wrap.explain(conf.shouldExplainAll)
        if (explain.nonEmpty) {
          logWarning(s"\n$explain")
          if (conf.optimizerShouldExplainAll && optimizations.nonEmpty) {
            logWarning(s"Cost-based optimizations applied:\n${optimizations.mkString("\n")}")
          }
        }
      }
      GpuOverrides.doConvertPlan(wrap, conf, optimizations)
    }
  }
```





# CostBasedOptimizer

基于代价的物理计划优化器，避免将更适合CPU执行的plan卸载到GPU，例如如果**下一步**需要在CPU上执行，则没有必要卸载简单的Project。

> 所以算子是否卸载，要关心父节点(也就是“下一步”)是否卸载

注意，它的入参是SparkPlanMeta[SparkPlan]，即wrap后的物理计划

```scala
  def optimize(conf: RapidsConf, plan: SparkPlanMeta[SparkPlan]): Seq[Optimization] = {
    logTrace("CBO optimizing plan")
    val cpuCostModel = new CpuCostModel(conf) // CPU代价模型
    val gpuCostModel = new GpuCostModel(conf) // DPU代价模型
    val optimizations = new ListBuffer[Optimization]() //记录了所用到的优化
    recursivelyOptimize(conf, cpuCostModel, gpuCostModel, plan, optimizations, finalOperator = true)
    optimizations
  }
```

## 优化的基础组件

### QueryStageExec

query stage是一个query plan的子图，query stage在处理其他算子之前会把输出进行物化。可以利用输出的统计数据优化后续的query stage。 共有两种query stage:

1. ShuffleQueryStageExec 
   物化输出到文件，spark启动其他的job来执行后续算子
2. BroadcastQueryStageExec 
   物化输出到driver端的array，spark会先广播array之后再执行算子



### RowCountPlanVisitor

预估每个算子输出行数，聚合所有分区的。

ShuffleExchangeExec会收集dataSize/rowCount 两个metrics， rapids通过ShuffleQueryStageExec拿到它

```scala
  def visit(plan: SparkPlanMeta[_]): Option[BigInt] = plan.wrapped match {
    //如果是QueryStageExec算子，则获取它的行数
    //为啥可以获取到行数？ querystage
    case p: QueryStageExec =>
      p.getRuntimeStatistics.rowCount
    case GlobalLimitExec(limit, _) =>
      visit(plan.childPlans.head).map(_.min(limit)).orElse(Some(limit))
    case LocalLimitExec(limit, _) =>
      // LocalLimit applies the same limit for each partition
      val n = limit * plan.wrapped.asInstanceOf[SparkPlan]
          .outputPartitioning.numPartitions
      visit(plan.childPlans.head).map(_.min(n)).orElse(Some(n))
    case p: TakeOrderedAndProjectExec =>
      visit(plan.childPlans.head).map(_.min(p.limit)).orElse(Some(p.limit))
    case p: HashAggregateExec if p.groupingExpressions.isEmpty =>
      Some(1)
    case p: SortMergeJoinExec =>
      estimateJoin(plan, p.joinType)
    case p: ShuffledHashJoinExec =>
      estimateJoin(plan, p.joinType)
    case p: BroadcastHashJoinExec =>
      estimateJoin(plan, p.joinType)
    case _: UnionExec =>
      Some(plan.childPlans.flatMap(visit).sum)
    case _ =>
    //其他plan输出行数默认是子节点行数的乘积
      default(plan)
  }
```



## CostModel

算子Cost是通过配置获取的，例如Cpu的Union代价，spark.rapids.sql.optimizer.cpu.exec.UnionExec->0

### CpuCostModel

算子Cost=算子cost(配置)*行数(metrics) + 表达式Cost

表达式Cost=exprEvalCost + memoryReadCost + memoryWriteCost

exprEvalCost=expressionCost(配置)*行数

memoryReadCost/memoryWriteCost： 估计内存大小/读写速度

⚠️：行数通过metrics获取，但是数据大小是通过schema和行数确定的，没有采用metrics的datasize

#### getCost

算子Cost=算子cost*行数 + 表达式Cost

```scala
  def getCost(plan: SparkPlanMeta[_]): Double = {

    //行数： 从metrics获取
    val rowCount = RowCountPlanVisitor.visit(plan).map(_.toDouble)
      .getOrElse(conf.defaultRowCount.toDouble)

    //算子代价
    val operatorCost = plan.conf
        .getCpuOperatorCost(plan.wrapped.getClass.getSimpleName)
        .getOrElse(conf.defaultCpuOperatorCost) * rowCount

    //表达式代价
    val exprEvalCost = plan.childExprs
      .map(expr => exprCost(expr.asInstanceOf[BaseExprMeta[Expression]], rowCount))
      .sum

    operatorCost + exprEvalCost
  }
```

算子代价目前是通过配置获取的

```scala
    "spark.rapids.sql.optimizer.cpu.exec.ProjectExec" -> "0",
    // The cost of a GPU projection is mostly the cost of evaluating the expressions
    // to produce the projected columns
    "spark.rapids.sql.optimizer.gpu.exec.ProjectExec" -> "0",
    // union does not further process data produced by its children
    "spark.rapids.sql.optimizer.cpu.exec.UnionExec" -> "0",
    "spark.rapids.sql.optimizer.gpu.exec.UnionExec" -> "0"
```



#### exprCost

表达式的cost需要依赖rowCount

 表达式开销=exprEvalCost + memoryReadCost + memoryWriteCost

```scala
  private def exprCost[INPUT <: Expression](expr: BaseExprMeta[INPUT], rowCount: Double): Double = {
    //这里排除一些不能获取开销的表达式，其开销直接是0
    if (MemoryCostHelper.isExcludedFromCost(expr)) {
      return 0d
    }
  
    //内存读取开销
    val memoryReadCost = expr.wrapped match {
      case _: Alias =>
        // 别名没有cost，所以计算子节点的cost
        exprCost(expr.childExprs.head.asInstanceOf[BaseExprMeta[Expression]], rowCount)

       // 列裁剪
      case _: AttributeReference | _: GetStructField =>
        MemoryCostHelper.calculateCost(MemoryCostHelper.estimateGpuMemory(
          expr.typeMeta.dataType, nullable = false, rowCount), conf.cpuReadMemorySpeed)

      case _ =>
        expr.childExprs
          .map(e => exprCost(e.asInstanceOf[BaseExprMeta[Expression]], rowCount)).sum
    }

    //内存写入开销
    val memoryWriteCost = MemoryCostHelper.calculateCost(MemoryCostHelper.estimateGpuMemory(
      expr.typeMeta.dataType, nullable = false, rowCount), conf.cpuWriteMemorySpeed)

    // 计算开销
    val exprEvalCost = rowCount *
      expr.conf.getCpuExpressionCost(expr.getClass.getSimpleName)
        .getOrElse(conf.defaultCpuExpressionCost)

    exprEvalCost + memoryReadCost + memoryWriteCost
  }
}
```

##### 内存读写开销的计算

内存开销=数据总大小/cpu内存读取速度

内存大小估计：dataSize + validityBufferSize 与数据类型、是否可空、rowCount有关

cpu内存读取速度：通过配置(默认30G/s)

```scala
  def estimateGpuMemory(dataType: Option[DataType], nullable: Boolean, rowCount: Double): Long = {
    dataType match {
      case Some(dt) =>
        // cardinality estimates tend to grow to very large numbers with nested joins so
        // we apply a maximum to the row count that we use when estimating data sizes in
        // order to avoid integer overflow
        val safeRowCount = rowCount.min(Int.MaxValue).toLong
        GpuBatchUtils.estimateGpuMemory(dt, nullable, safeRowCount)
      case None =>
        throw new UnsupportedOperationException("Data type is None")
    }
  }


def estimateGpuMemory(dataType: DataType, nullable: Boolean, rowCount: Long): Long = {
  //如果可空，则有validityBufferSize开销，否则没有
    val validityBufferSize = if (nullable) {
      calculateValidityBufferSize(rowCount)
    } else {
      0
    }
    val dataSize = dataType match {
      case dt@DataTypes.BinaryType =>
        val offsetBufferSize = calculateOffsetBufferSize(rowCount)
        val dataSize = dt.defaultSize * rowCount
        dataSize + offsetBufferSize
      case dt@DataTypes.StringType =>
        //offset开销
        val offsetBufferSize = calculateOffsetBufferSize(rowCount)
        //数据类型默认大小 * 行数
        val dataSize = dt.defaultSize * rowCount
        dataSize + offsetBufferSize
      case dt: MapType =>
        // The Spark default map size assumes one entry for good or bad
        calculateOffsetBufferSize(rowCount) +
            estimateGpuMemory(dt.keyType, false, rowCount) +
            estimateGpuMemory(dt.valueType, dt.valueContainsNull, rowCount)
      case dt: ArrayType =>
        // The Spark default array size assumes one entry for good or bad
        calculateOffsetBufferSize(rowCount) +
            estimateGpuMemory(dt.elementType, dt.containsNull, rowCount)
      case dt: StructType =>
        dt.fields.map { f =>
          estimateGpuMemory(f.dataType, f.nullable, rowCount)
        }.sum
      case dt =>
         //其他数据类型，直接是大小 * 行数
        dt.defaultSize * rowCount
    }
    dataSize + validityBufferSize
  }
```



##### 表达式计算的开销

纯配置

### GpuCostModel

#### getCost

逻辑同cpu的，算子开销读取配置文件，只是配置前缀不同

#### exprCost

GPU上列的读取开销为0，因为仅仅是加了一个引用数

写的开销计算方式和cpu一样

表达式计算开销通过配置完成

## CostBasedOptimizer

```scala
  def optimize(conf: RapidsConf, plan: SparkPlanMeta[SparkPlan]): Seq[Optimization] = {
    logTrace("CBO optimizing plan")
    val cpuCostModel = new CpuCostModel(conf)
    val gpuCostModel = new GpuCostModel(conf)
    val optimizations = new ListBuffer[Optimization]()
    recursivelyOptimize(conf, cpuCostModel, gpuCostModel, plan, optimizations, finalOperator = true)
    if (optimizations.isEmpty) {
      logTrace(s"CBO finished optimizing plan. No optimizations applied.")
    } else {
      logTrace(s"CBO finished optimizing plan. " +
        s"${optimizations.length} optimizations applied:\n\t${optimizations.mkString("\n\t")}")
    }
    optimizations
  }
```



## 递归优化recursivelyOptimize

遍历所有算子，并决定每个算子运行在CPU或者GPU

![image-20230726171120502](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230726171120502.png)

```scala
  private def recursivelyOptimize(
      conf: RapidsConf,
      cpuCostModel: CostModel,
      gpuCostModel: CostModel,
      plan: SparkPlanMeta[SparkPlan],
    // 收集所应用的优化，记录plan的cpuCost和gpuCost
      optimizations: ListBuffer[Optimization],
    // 是否是根节点，根节点处理比较特殊，因为需要以行格式输出必须运行在CPU上
      finalOperator: Boolean): 
     //返回(cpuCost ,gpuCost)
              (Double, Double) = {  

    // get the CPU and GPU cost of this operator (excluding cost of children)
    val operatorCpuCost = cpuCostModel.getCost(plan)
    val operatorGpuCost = gpuCostModel.getCost(plan)

    // get the CPU and GPU cost of the child plan(s)
    val childCosts = plan.childPlans
      .map(child => recursivelyOptimize(
        conf,
        cpuCostModel,
        gpuCostModel,
        child,
        optimizations,
        finalOperator = false))
    val (childCpuCosts, childGpuCosts) = childCosts.unzip

    // calculate total (this operator + children)  当前算子+所有子算子 的cpu/GPU开销
    val totalCpuCost = operatorCpuCost + childCpuCosts.sum
    var totalGpuCost = operatorGpuCost + childGpuCosts.sum
    logCosts(plan, "Operator costs", operatorCpuCost, operatorGpuCost)
    logCosts(plan, "Operator + child costs", totalCpuCost, totalGpuCost)
            
    //获取当前plan的输出行数
    plan.estimatedOutputRows = RowCountPlanVisitor.visit(plan)

    // determine how many transitions between CPU and GPU are taking place between
    // the child operators and this operator
    // 看起来是所有与当前节点运行模式不同的子节点数量？transitions指得是行列转换？
    val numTransitions = plan.childPlans
      .count(canRunOnGpu(_) != canRunOnGpu(plan))
    logCosts(plan, s"numTransitions=$numTransitions", totalCpuCost, totalGpuCost)

    if (numTransitions > 0) {
      // there are transitions between CPU and GPU so we need to calculate the transition costs
      // and also make decisions based on those costs to see whether any parts of the plan would
      // have been better off just staying on the CPU
      // 有行列转换，因此需要计算行列转换的开销，并基于此决定当前算子是否就在CPU上运行
      // is this operator on the GPU?
      if (canRunOnGpu(plan)) {
        //如果当前plan运行在GPU上，则子节点存在行列转换CPU2GPU
        // at least one child is transitioning from CPU to GPU so we calculate the
        // transition costs
        val transitionCost = plan.childPlans
          .filterNot(canRunOnGpu)
           //计算行转列开销
          .map(transitionToGpuCost(conf, _)).sum

        // if the GPU cost including transition is more than the CPU cost then avoid this
        // transition and reset the GPU cost
        // 如果GPU cost+行转列cost超过了CPU cost，且非exchange算子(包括从excange读，这种情况不能打回)，则当前plan打回CPU
        if (operatorGpuCost + transitionCost > operatorCpuCost && !isExchangeOp(plan)) {
          // avoid transition and keep this operator on CPU
          optimizations.append(AvoidTransition(plan))
          //连续打回CPU
          plan.costPreventsRunningOnGpu()
          // reset GPU cost 保持和CPU一样
          totalGpuCost = totalCpuCost
          logCosts(plan, s"Avoid transition to GPU", totalCpuCost, totalGpuCost)
        } else {
          //否则，说明值得卸载到GPU
          // add transition cost to total GPU cost
          totalGpuCost += transitionCost
          logCosts(plan, s"transitionFromCpuCost=$transitionCost", totalCpuCost, totalGpuCost)
        }
      } else {
        //当前plan 运行在CPU, 说明必有子节点运行在GPU
        // at least one child is transitioning from GPU to CPU so we evaluate each of this
        // child plans to see if it was worth running on GPU now that we have the cost of
        // transitioning back to CPU
        plan.childPlans.zip(childCosts).foreach {
          case (child, childCosts) =>
            val (childCpuCost, childGpuCost) = childCosts
               //列转行开销
            val transitionCost = transitionToCpuCost(conf, child)
            val childGpuTotal = childGpuCost + transitionCost
               //如果子节点运行在GPU上，且非exchange节点，且不值得运行在GPU上，则子节点打回CPU
            if (canRunOnGpu(child) && !isExchangeOp(child)
                && childGpuTotal > childCpuCost) {
              // force this child plan back onto CPU
              optimizations.append(ReplaceSection(
                child, totalCpuCost, totalGpuCost))
              child.recursiveCostPreventsRunningOnGpu()
            }
        }

        // recalculate the transition costs because child plans may have changed
        // 再次计算当前运行在GPU上的子节点列转行的开销
        val transitionCost = plan.childPlans
          .filter(canRunOnGpu)
          .map(transitionToCpuCost(conf, _)).sum
        // 更新GPU cost： 当前plan运行在cpu上，子节点运行在gpu上，列转行的开销应该记在gpu头上
        totalGpuCost += transitionCost
        logCosts(plan, s"transitionFromGpuCost=$transitionCost", totalCpuCost, totalGpuCost)
      }
    }

    // special behavior if this is the final operator in the plan because we always have the
    // cost of going back to CPU at the end
    // 根节点特殊处理，如果根节点运行在gpu上，那么列转行的开销应该记在gpu头上
    if (finalOperator && canRunOnGpu(plan)) {
      val transitionCost = transitionToCpuCost(conf, plan)
      totalGpuCost += transitionCost
      logCosts(plan, s"final operator, transitionFromGpuCost=$transitionCost",
        totalCpuCost, totalGpuCost)
    }

    if (totalGpuCost > totalCpuCost) {
      // we have reached a point where we have transitioned onto GPU for part of this
      // plan but with no benefit from doing so, so we want to undo this and go back to CPU
      if (canRunOnGpu(plan) && !isExchangeOp(plan)) {
        // this plan would have been on GPU so we move it and onto CPU and recurse down
        // until we reach a part of the plan that is already on CPU and then stop
        optimizations.append(ReplaceSection(plan, totalCpuCost, totalGpuCost))
        // gpu不值得，打回CPU
        plan.recursiveCostPreventsRunningOnGpu()
        // reset the costs because this section of the plan was not moved to GPU
        totalGpuCost = totalCpuCost
        logCosts(plan, s"ReplaceSection: ${plan}", totalCpuCost, totalGpuCost)
      }
    }

    if (!canRunOnGpu(plan) || isExchangeOp(plan)) {
      // reset the costs because this section of the plan was not moved to GPU
      totalGpuCost = totalCpuCost
      logCosts(plan, s"Reset costs (not on GPU / exchange)", totalCpuCost, totalGpuCost)
    }

    logCosts(plan, "END", totalCpuCost, totalGpuCost)
    (totalCpuCost, totalGpuCost)
  }
```

### transitionToGpuCost

行转列算子开销是通过配置定义的

行转列开销： 行转列算子开销*行数 + cpu读+gpu写

```scala
  private def transitionToGpuCost(conf: RapidsConf, plan: SparkPlanMeta[SparkPlan]): Double = {
    val rowCount = RowCountPlanVisitor.visit(plan).map(_.toDouble)
      .getOrElse(conf.defaultRowCount.toDouble)
    //计算一行的大小
    val dataSize = MemoryCostHelper.estimateGpuMemory(plan.wrapped.schema, rowCount)
    conf.getGpuOperatorCost("GpuRowToColumnarExec").getOrElse(0d) * rowCount +
      MemoryCostHelper.calculateCost(dataSize, conf.cpuReadMemorySpeed) +
      MemoryCostHelper.calculateCost(dataSize, conf.gpuWriteMemorySpeed)
  }
```

### costPreventsRunningOnGpu

把当前GPU算子下**连续出现的**GPU算子打回CPU

```scala
  final def costPreventsRunningOnGpu(): Unit = {
    cannotRunOnGpuBecauseOfCost = true
    //修改tag
    willNotWorkOnGpu("Removed by cost-based optimizer")
    childExprs.foreach(_.recursiveCostPreventsRunningOnGpu())
    childParts.foreach(_.recursiveCostPreventsRunningOnGpu())
    childScans.foreach(_.recursiveCostPreventsRunningOnGpu())
  }

//递归打回CPU，直到遇到一个plan已经在CPU上运行了(由if里的条件canThisBeReplaced && !mustThisBeReplaced决定)
  final def recursiveCostPreventsRunningOnGpu(): Unit = {
    if (canThisBeReplaced && !mustThisBeReplaced) {
      costPreventsRunningOnGpu()
      //这里是为啥对datawritecmd的递归要放到childExprs/childParts/childScans的后面？
      childDataWriteCmds.foreach(_.recursiveCostPreventsRunningOnGpu())
    }
  }
```

## runAfterTagRules

混合

tag完成后运行的一些规则

```scala
  def runAfterTagRules(): Unit = {

    InputFileBlockRule.apply(this.asInstanceOf[SparkPlanMeta[SparkPlan]])

 // ShuffledHashJoin and SortMergeJoin 的输入要么全部CPU实现，要么全部GPU实现，因为两者hash实现不一致
    fixUpExchangeOverhead()
  }


  private def fixUpExchangeOverhead(): Unit = {
    childPlans.foreach(_.fixUpExchangeOverhead())
    if (wrapped.isInstanceOf[ShuffleExchangeExec] &&
      !childPlans.exists(_.canThisBeReplaced) &&
        (plan.conf.adaptiveExecutionEnabled ||
        !parent.exists(_.canThisBeReplaced))) {

      willNotWorkOnGpu("Columnar exchange without columnar children is inefficient")

      childPlans.head.wrapped
          .getTagValue(GpuOverrides.preRowToColProjection).foreach { r2c =>
        wrapped.setTagValue(GpuOverrides.preRowToColProjection, r2c)
      }
    }
  }
```

### InputFileBlockRule

如果file scan运行在CPU上，而接下来的算子运行在GPU上，那么将插入GpuRowToColumnar，GpuRowToColumnar会让input_file_xxx失效，从而接下来的GPU算子中的input_file_xxx将会得到空值。InputFileBlockRule规则用于阻止包含首个input_file_xxx表达式的Plan运行在GPU中。

```scala
  def apply(plan: SparkPlanMeta[SparkPlan]) = {
    /**
     * key: the SparkPlanMeta where has the first input_file_xxx expression
     * value: an array of the SparkPlanMeta chain [SparkPlan (with first input_file_xxx), FileScan)
     */
    val resultOps = LinkedHashMap[SparkPlanMeta[SparkPlan], ArrayBuffer[SparkPlanMeta[SparkPlan]]]()
    //递归查找包含首个input_file_xxx表达式的plan(作为parent节点)，及其后对应的SparkPlanMeta链。 开始递归时key为None,需要判断是否
    recursivelyResolve(plan, None, resultOps)

    // If we've found some chains, we should prevent the transition.
    resultOps.foreach { item =>
      //全部tag为不能运行在GPU上
      item._2.foreach(p => p.inputFilePreventsRunningOnGpu())
    }
  } 

  private def recursivelyResolve(
      plan: SparkPlanMeta[SparkPlan],
      key: Option[SparkPlanMeta[SparkPlan]],//有首个input_file_xxx的SparkPlanMeta
      resultOps: LinkedHashMap[SparkPlanMeta[SparkPlan],//发现的SparkPlan链
        ArrayBuffer[SparkPlanMeta[SparkPlan]]]): Unit = {

    plan.wrapped match {
      //Exchange
      case _: ShuffleExchangeExec => // Exchange will invalid the input_file_xxx
        key.map(p => resultOps.remove(p)) // Remove the chain from Map
        plan.childPlans.foreach(p => recursivelyResolve(p, None, resultOps))
      case _: FileSourceScanExec | _: BatchScanExec =>
        if (plan.canThisBeReplaced) { // FileScan can be replaced，无需阻止卸载
          key.map(p => resultOps.remove(p)) // Remove the chain from Map
        }
      case _: LeafExecNode => // We've reached the LeafNode but without any FileScan
        key.map(p => resultOps.remove(p)) // Remove the chain from Map
      case _ =>
        val newKey = if (key.isDefined) {
          // The node is in the middle of chain [SparkPlan with input_file_xxx, FileScan)
          resultOps.getOrElseUpdate(key.get,  new ArrayBuffer[SparkPlanMeta[SparkPlan]]) += plan
          key
        } else { // There is no parent Node who has input_file_xxx
          if (checkHasInputFileExpressions(plan.wrapped)) {
            // Current node has input_file_xxx. Mark it as the first Node with input_file_xxx
            resultOps.getOrElseUpdate(plan, new ArrayBuffer[SparkPlanMeta[SparkPlan]]) += plan
            Some(plan)
          } else {
            None
          }
        }

        plan.childPlans.foreach(p => recursivelyResolve(p, newKey, resultOps))
    }
  }


def checkHasInputFileExpressions(exec: Expression): Boolean = exec match {
    case _: InputFileName => true
    case _: InputFileBlockStart => true
    case _: InputFileBlockLength => true
    case e => e.children.exists(checkHasInputFileExpressions)
  }
```

### fixUpExchangeOverhead

ShuffledHashJoin and SortMergeJoin 的输入要么全部CPU实现，要么全部GPU实现，因为两者hash实现不一致
