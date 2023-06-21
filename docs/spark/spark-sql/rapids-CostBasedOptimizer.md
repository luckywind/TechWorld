# CostBasedOptimizer

```scala
  def optimize(conf: RapidsConf, plan: SparkPlanMeta[SparkPlan]): Seq[Optimization] = {
    logTrace("CBO optimizing plan")
    val cpuCostModel = new CpuCostModel(conf) // CPU代价模型
    val gpuCostModel = new GpuCostModel(conf) // DPU代价模型
    val optimizations = new ListBuffer[Optimization]()
    recursivelyOptimize(conf, cpuCostModel, gpuCostModel, plan, optimizations, finalOperator = true)
    optimizations
  }
```

## 优化的基础QueryStageExec

query stage是一个query plan的子图，query stage在处理其他算子之前会把输出进行物化。可以利用输出的统计数据优化后续的query stage。 共有两种query stage:

1. ShuffleQueryStageExec 
   物化输出到文件，spark启动其他的job来执行后续算子
2. BroadcastQueryStageExec 
   物化输出到driver端的array，spark会先广播array之后再执行算子







## RowCountPlanVisitor

预估每个算子输出行数，聚合所有分区的。

ShuffleExchangeExec会收集dataSize/rowCount 两个metrics， rapids通过ShuffleQueryStageExec拿到它

## CostModel

算子Cost是通过配置获取的，例如Cpu的Union代价，spark.rapids.sql.optimizer.cpu.exec.UnionExec->0

### CpuCostModel

算子Cost=算子单行cost*行数 + 表达式Cost

```scala
  def getCost(plan: SparkPlanMeta[_]): Double = {

    //stage行数
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



### GpuCostModel



## 递归优化

遍历所有算子，并决定每个算子运行在CPU或者GPU

```scala
  private def recursivelyOptimize(
      conf: RapidsConf,
      cpuCostModel: CostModel,
      gpuCostModel: CostModel,
      plan: SparkPlanMeta[SparkPlan],
      optimizations: ListBuffer[Optimization],// 收集结果
      finalOperator: Boolean): (Double, Double) = {

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

    // calculate total (this operator + children)
    val totalCpuCost = operatorCpuCost + childCpuCosts.sum
    var totalGpuCost = operatorGpuCost + childGpuCosts.sum
    logCosts(plan, "Operator costs", operatorCpuCost, operatorGpuCost)
    logCosts(plan, "Operator + child costs", totalCpuCost, totalGpuCost)

    plan.estimatedOutputRows = RowCountPlanVisitor.visit(plan)

    // determine how many transitions between CPU and GPU are taking place between
    // the child operators and this operator
    val numTransitions = plan.childPlans
      .count(canRunOnGpu(_) != canRunOnGpu(plan))
    logCosts(plan, s"numTransitions=$numTransitions", totalCpuCost, totalGpuCost)

    if (numTransitions > 0) {
      // there are transitions between CPU and GPU so we need to calculate the transition costs
      // and also make decisions based on those costs to see whether any parts of the plan would
      // have been better off just staying on the CPU

      // is this operator on the GPU?
      if (canRunOnGpu(plan)) {
        // at least one child is transitioning from CPU to GPU so we calculate the
        // transition costs
        val transitionCost = plan.childPlans
          .filterNot(canRunOnGpu)
          .map(transitionToGpuCost(conf, _)).sum

        // if the GPU cost including transition is more than the CPU cost then avoid this
        // transition and reset the GPU cost
        if (operatorGpuCost + transitionCost > operatorCpuCost && !isExchangeOp(plan)) {
          // avoid transition and keep this operator on CPU
          optimizations.append(AvoidTransition(plan))
          plan.costPreventsRunningOnGpu()
          // reset GPU cost
          totalGpuCost = totalCpuCost
          logCosts(plan, s"Avoid transition to GPU", totalCpuCost, totalGpuCost)
        } else {
          // add transition cost to total GPU cost
          totalGpuCost += transitionCost
          logCosts(plan, s"transitionFromCpuCost=$transitionCost", totalCpuCost, totalGpuCost)
        }
      } else {
        // at least one child is transitioning from GPU to CPU so we evaluate each of this
        // child plans to see if it was worth running on GPU now that we have the cost of
        // transitioning back to CPU
        plan.childPlans.zip(childCosts).foreach {
          case (child, childCosts) =>
            val (childCpuCost, childGpuCost) = childCosts
            val transitionCost = transitionToCpuCost(conf, child)
            val childGpuTotal = childGpuCost + transitionCost
            if (canRunOnGpu(child) && !isExchangeOp(child)
                && childGpuTotal > childCpuCost) {
              // force this child plan back onto CPU
              optimizations.append(ReplaceSection(
                child, totalCpuCost, totalGpuCost))
              child.recursiveCostPreventsRunningOnGpu()
            }
        }

        // recalculate the transition costs because child plans may have changed
        val transitionCost = plan.childPlans
          .filter(canRunOnGpu)
          .map(transitionToCpuCost(conf, _)).sum
        totalGpuCost += transitionCost
        logCosts(plan, s"transitionFromGpuCost=$transitionCost", totalCpuCost, totalGpuCost)
      }
    }

    // special behavior if this is the final operator in the plan because we always have the
    // cost of going back to CPU at the end
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



