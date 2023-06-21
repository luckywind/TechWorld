

[可参考百度的优化总结](https://blog.csdn.net/fl63zv9zou86950w/article/details/79049280?utm_source=copy), [英文版](https://www.intel.com/content/www/us/en/developer/articles/technical/spark-sql-adaptive-execution-at-100-tb.html)

[还可参考](https://zhuanlan.zhihu.com/p/535174818)

# SparkSQL Adaptive Execution简介

SparkSQL Adaptive Execution 是 Spark SQL 针对 SQL 查询计划的一种自适应执行优化技术。该技术的主要目标是在执行过程中自适应地调整执行计划，以提高查询效率。了解源码实现可以更好地理解 Adaptive Execution 的工作原理和优化策略。

![image-20230616165125482](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230616165125482.png)

首先以Exchange节点作为分界将执行计划这棵树划分成多个QueryStage（Exchange节点在Spark SQL中代表shuffle）。每一个QueryStage都是一棵独立的子树，也是一个独立的执行单元。在加入QueryStage的同时，我们也加入一个QueryStageInput的叶子节点，作为父亲QueryStage的输入。例如对于图中两表join的执行计划来说我们会创建3个QueryStage。最后一个QueryStage中的执行计划是join本身，它有2个QueryStageInput代表它的输入，分别指向2个孩子的QueryStage。在执行QueryStage时，我们首先提交它的孩子stage，并且收集这些stage运行时的信息。当这些孩子stage运行完毕后，我们可以知道它们的大小等信息，以此来判断QueryStage中的计划是否可以优化更新。例如当我们获知某一张表的大小是5M，它小于broadcast的阈值时，我们可以将SortMergeJoin转化成BroadcastHashJoin来优化当前的执行计划。我们也可以根据孩子stage产生的shuffle数据量，来动态地调整该stage的reducer个数。在完成一系列的优化处理后，最终我们为该QueryStage生成RDD的DAG图，并且提交给DAG Scheduler来执行。

总之，在非AQE的情况下，SparkSQL会转换为DAG图，然后DAGScheduler基于shuffle将其划分为多个stage, 然后再执行stage。

在AQE的情况下，首先会将plan树拆分为多个QueryStages, 在执行时先将它的子 QueryStages 被提交。在所有子节点完成后，收集 shuffle 数据大小。根据收集到的 shuffle 数据统计信息，将当前 QueryStage 的执行计划优化为更好的执行计划。然后转换为DAG图再执行Stage。

目前AQE主要有三大特性：

1. 自动分区合并：在 Shuffle 过后，Reduce Task 数据分布参差不齐，AQE 将自动合并过小的数据分区。
2. Join 策略调整：如果某张表在过滤之后，尺寸小于广播变量阈值，这张表参与的数据关联就会从 Shuffle Sort Merge Join 降级（Demote）为执行效率更高的 Broadcast Hash Join。
3. 自动倾斜处理：结合配置项，AQE 自动拆分 Reduce 阶段过大的数据分区，降低单个 Reduce Task 的工作负载。





1. TaskSchedulerImpl

TaskSchedulerImpl是 Spark 任务调度的核心组件。它负责任务的分配和调度。为了实现自适应执行，TaskSchedulerImpl 维护了一些关键的信息，如Executor的负载状况、Task执行时长、数据本地性等。这些信息可以帮助 Adaptive Execution 识别不同任务的特点，并根据特点自适应地调整任务调度策略。

2. QueryExecution

QueryExecution是 Spark SQL 完整查询的执行引擎。它负责将 SQL 查询转换成 Spark 执行计划，并通过 Spark 的执行引擎执行计划。为了支持自适应执行，QueryExecution 添加了 AdaptiveSparkPlan类。该类可以通过 costEvaluator和planOptimizer 实现动态计划优化和重建。

3. CostEvaluator

CostEvaluator用于估计执行计划的开销，Adaptive Execution 通过维护每个阶段的实际执行时间、数据大小、潜在并行度等信息，可以更准确地估计执行计划的开销。基于开销估算，Adaptive Execution 可以根据时间、资源等因素自适应地调整执行计划。

4. PlanOptimizer

PlanOptimizer 是通过 costEvaluator 计算出执行计划的开销，然后根据开销重新生成优化的执行计划。PlanOptimizer 通过 Spark SQL 查询计划的物化视图来实现重建执行计划：根据视图信息，判断是否存在优化的查询计划，若存在，将现有的执行计划替换成新的执行计划。这样就可以在不停止当前查询的情况下自适应地优化执行计划。

5. ShuffleManager

ShuffleManager是 Spark Shuffle的核心组件。Adaptive Execution 可以通过 ShuffleManager 动态调整Shuffle操作的并行度和数据分布，以避免数据倾斜和资源浪费，并提高执行效率。

总的来说，Adaptive Execution的实现涉及多个组件，如TaskSchedulerImpl、QueryExecution、CostEvaluator、PlanOptimizer、ShuffleManager等。这些组件协同工作，实现了 Adaptive Execution 的自适应执行和优化策略。了解它们的工作原理和实现细节，可以帮助开发者更好地理解 Spark SQL 的执行过程和自适应执行优化技术。

# 实现

## AdaptiveSparkPlanExec的插入



### AdaptiveExecutionContext

维护主query与所有子query的映射

```scala
case class AdaptiveExecutionContext(session: SparkSession, qe: QueryExecution) {

  /**
   * The subquery-reuse map shared across the entire query.
   */
  val subqueryCache: TrieMap[SparkPlan, BaseSubqueryExec] =
    new TrieMap[SparkPlan, BaseSubqueryExec]()

  /**
   * The exchange-reuse map shared across the entire query, including sub-queries.
   */
  val stageCache: TrieMap[SparkPlan, QueryStageExec] =
    new TrieMap[SparkPlan, QueryStageExec]()
}
```

### AE插入规则：InsertAdaptiveSparkPlan extends Rule[SparkPlan] 

```scala
{

  override def apply(plan: SparkPlan): SparkPlan = applyInternal(plan, false)

  private def applyInternal(plan: SparkPlan, isSubquery: Boolean): SparkPlan = plan match {
    // 未启用AQE则不处理
    case _ if !conf.adaptiveExecutionEnabled => plan
    case _: ExecutedCommandExec => plan
    case _: CommandResultExec => plan
    case c: DataWritingCommandExec => c.copy(child = apply(c.child))
    case c: V2CommandExec => c.withNewChildren(c.children.map(apply))
    case _ if shouldApplyAQE(plan, isSubquery) =>
      if (supportAdaptive(plan)) {
        try {
          // Plan sub-queries recursively and pass in the shared stage cache for exchange reuse.
          // 1. 递归构建所有子查询的 表达式id->执行计划的map
          val subqueryMap = buildSubqueryMap(plan)
          // 2. 构建规则
          val planSubqueriesRule = PlanAdaptiveSubqueries(subqueryMap)
          val preprocessingRules = Seq(
            planSubqueriesRule)
          // 3. 执行规则
          val newPlan = AdaptiveSparkPlanExec.applyPhysicalRules(plan, preprocessingRules)
          logDebug(s"Adaptive execution enabled for plan: $plan")
          // 4. 插入AdaptiveSparkPlanExec算子
          AdaptiveSparkPlanExec(newPlan, adaptiveExecutionContext, preprocessingRules, isSubquery)
        } catch {//失败则退回原计划
          case SubqueryAdaptiveNotSupportedException(subquery) =>
            logWarning(s"${SQLConf.ADAPTIVE_EXECUTION_ENABLED.key} is enabled " +
              s"but is not supported for sub-query: $subquery.")
            plan
        }
      } else {
        logDebug(s"${SQLConf.ADAPTIVE_EXECUTION_ENABLED.key} is enabled " +
          s"but is not supported for query: $plan.")
        plan
      }

    case _ => plan
  }


  //AQE只在有exchange或者自查询的qeury中有用，满足以下条件之一：
  1. ADAPTIVE_EXECUTION_FORCE_APPLY=true
  2. 输入是一个自查询，则说明已经开始AQE了，必须继续执行
  3. query包含exchange
  4. query需要添加exchange
  5. query包含子查询
  private def shouldApplyAQE(plan: SparkPlan, isSubquery: Boolean): Boolean = {
    conf.getConf(SQLConf.ADAPTIVE_EXECUTION_FORCE_APPLY) || isSubquery || {
      plan.exists {
        case _: Exchange => true
        case p if !p.requiredChildDistribution.forall(_ == UnspecifiedDistribution) => true
        case p => p.expressions.exists(_.exists {
          case _: SubqueryExpression => true
          case _ => false
        })
      }
    }
  }

  private def supportAdaptive(plan: SparkPlan): Boolean = {
    sanityCheck(plan) &&
      !plan.logicalLink.exists(_.isStreaming) &&
    plan.children.forall(supportAdaptive)
  }

  private def sanityCheck(plan: SparkPlan): Boolean =
    plan.logicalLink.isDefined

  
 

  def compileSubquery(plan: LogicalPlan): SparkPlan = {
    // Apply the same instance of this rule to sub-queries so that sub-queries all share the
    // same `stageCache` for Exchange reuse.
    this.applyInternal(
      QueryExecution.createSparkPlan(adaptiveExecutionContext.session,
        adaptiveExecutionContext.session.sessionState.planner, plan.clone()), true)
  }

  private def verifyAdaptivePlan(plan: SparkPlan, logicalPlan: LogicalPlan): Unit = {
    if (!plan.isInstanceOf[AdaptiveSparkPlanExec]) {
      throw SubqueryAdaptiveNotSupportedException(logicalPlan)
    }
  }
}
```



#### 子查询计划buildSubqueryMap

递归构建所有子查询的 表达式id->执行计划的map

对于每个sub query, 应用当前规则生成一个AE计划，或者重用已有的等价的执行计划



所谓的subquery都是表达式

1. ScalarSubquery:只返回一个行一列的子查询
2. InSubquery(values: Seq[Expression], query: ListQuery)： 如果“查询”的结果集中返回“值”，则计算结果为“true”。
3. DynamicPruningSubquery：使用join一边的filter过滤另一边的表，在应用分区裁剪时插入的

```scala
 // 返回一个子查询的 表达式ID->执行计划的映射，对于每个子查询，使用这个规则生成一个自适应执行计划，也可以重用已有的。
  private def buildSubqueryMap(plan: SparkPlan): Map[Long, BaseSubqueryExec] = {
    val subqueryMap = mutable.HashMap.empty[Long, BaseSubqueryExec]
    if (!plan.containsAnyPattern(SCALAR_SUBQUERY, IN_SUBQUERY, DYNAMIC_PRUNING_SUBQUERY)) {
      return subqueryMap.toMap
    }
    plan.foreach(_.expressions.filter(_.containsPattern(PLAN_EXPRESSION)).foreach(_.foreach {
      case expressions.ScalarSubquery(p, _, exprId, _)
          if !subqueryMap.contains(exprId.id) =>
        val executedPlan = compileSubquery(p)
        verifyAdaptivePlan(executedPlan, p)
        val subquery = SubqueryExec.createForScalarSubquery(
          s"subquery#${exprId.id}", executedPlan)
        subqueryMap.put(exprId.id, subquery)
      case expressions.InSubquery(_, ListQuery(query, _, exprId, _, _))
          if !subqueryMap.contains(exprId.id) =>
        val executedPlan = compileSubquery(query)
        verifyAdaptivePlan(executedPlan, query)
        val subquery = SubqueryExec(s"subquery#${exprId.id}", executedPlan)
        subqueryMap.put(exprId.id, subquery)
      case expressions.DynamicPruningSubquery(value, buildPlan,
          buildKeys, broadcastKeyIndex, onlyInBroadcast, exprId)
          if !subqueryMap.contains(exprId.id) =>
        val executedPlan = compileSubquery(buildPlan)
        verifyAdaptivePlan(executedPlan, buildPlan)

        val name = s"dynamicpruning#${exprId.id}"
        val subquery = SubqueryAdaptiveBroadcastExec(
          name, broadcastKeyIndex, onlyInBroadcast,
          buildPlan, buildKeys, executedPlan)
        subqueryMap.put(exprId.id, subquery)
      case _ =>
    }))

    subqueryMap.toMap
  }


  def compileSubquery(plan: LogicalPlan): SparkPlan = {
    // Apply the same instance of this rule to sub-queries so that sub-queries all share the
    // same `stageCache` for Exchange reuse.
    
    // 对sub-query再次使用这个规则
    this.applyInternal(
      QueryExecution.createSparkPlan(adaptiveExecutionContext.session,
        adaptiveExecutionContext.session.sessionState.planner, plan.clone()), true)
  }
```



#### 预处理规则PlanAdaptiveSubqueries

所有子查询的 表达式id->执行计划的map 构造一个规则

```scala
case class PlanAdaptiveSubqueries(
    subqueryMap: Map[Long, BaseSubqueryExec]) extends Rule[SparkPlan] {

  def apply(plan: SparkPlan): SparkPlan = {
    plan.transformAllExpressionsWithPruning(
      _.containsAnyPattern(SCALAR_SUBQUERY, IN_SUBQUERY, DYNAMIC_PRUNING_SUBQUERY)) {
      //如果是只返回一行一列的subquery，将会转为物理scalar subquery
      case expressions.ScalarSubquery(_, _, exprId, _) =>
 				//ScalarSubquery转为对应的物理计划
        execution.ScalarSubquery(subqueryMap(exprId.id), exprId)
      //如果是InSubquery，则转为物理计划InSubqueryExec
      case expressions.InSubquery(values, ListQuery(_, _, exprId, _, _)) =>
        val expr = if (values.length == 1) {
          values.head
        } else {
          CreateNamedStruct(
            values.zipWithIndex.flatMap { case (v, index) =>
              Seq(Literal(s"col_$index"), v)
            }
          )
        }
        InSubqueryExec(expr, subqueryMap(exprId.id), exprId, shouldBroadcast = true)
      //如果是DynamicPruningSubquery表达式，转为表达式DynamicPruningSubquery
      case expressions.DynamicPruningSubquery(value, _, _, _, _, exprId) =>
        DynamicPruningExpression(InSubqueryExec(value, subqueryMap(exprId.id), exprId))
    }
  }
}
```



#### 执行AQE的物理规则AdaptiveSparkPlanExec.applyPhysicalRules

```scala
  def applyPhysicalRules(
      plan: SparkPlan,
      rules: Seq[Rule[SparkPlan]],
      loggerAndBatchName: Option[(PlanChangeLogger[SparkPlan], String)] = None): SparkPlan = {
    if (loggerAndBatchName.isEmpty) {
      rules.foldLeft(plan) { case (sp, rule) => rule.apply(sp) }
    } else {
      val (logger, batchName) = loggerAndBatchName.get
      val newPlan = rules.foldLeft(plan) { case (sp, rule) =>
        val result = rule.apply(sp)
        logger.logRule(rule.ruleName, sp, result)
        result
      }
      logger.logBatch(batchName, plan, newPlan)
      newPlan
    }
  }
```



## AdaptiveSparkPlanExec

一个用于自适应执行查询计划的根节点。它将查询计划分割成独立的阶段，并根据它们的依赖顺序执行。查询阶段在末尾实现其输出结果。当一个阶段完成时，实例化输出结果的数据统计信息将用于优化剩余的查询。

为了创建查询阶段，我们从底向上遍历查询树。当我们遇到一个 Exchange 节点时，如果这个 Exchange 节点的所有子查询阶段都被实例化，我们就为这个 Exchange 节点创建一个新的查询阶段。一旦创建，新阶段就会异步地进行实例化。

当一个查询阶段完成实例化时，剩余的查询将基于所有实例化阶段提供的最新统计信息进行重新优化和计划。然后我们再遍历查询计划并在可能的情况下创建更多的阶段。当所有阶段都被实例化后，我们执行计划的其余部分。

它是物理计划执行算子，那么它的核心就在于Executor，即方法——doExecute

```scala
override def doExecute(): RDD[InternalRow] = {
  withFinalPlanUpdate(_.execute())
}

  private def withFinalPlanUpdate[T](fun: SparkPlan => T): T = {
    val plan = getFinalPhysicalPlan()
    val result = fun(plan)
    //触发lazy exe
    finalPlanUpdate
    result
  }
```

### getFinalPhysicalPlan

```scala
  private def getFinalPhysicalPlan(): SparkPlan = lock.synchronized {
    if (isFinalPlan) return currentPhysicalPlan

    // In case of this adaptive plan being executed out of `withActive` scoped functions, e.g.,
    // `plan.queryExecution.rdd`, we need to set active session here as new plan nodes can be
    // created in the middle of the execution.
    context.session.withActive {
      val executionId = getExecutionId
      // Use inputPlan logicalLink here in case some top level physical nodes may be removed
      // during `initialPlan`
      var currentLogicalPlan = inputPlan.logicalLink.get
      var result = createQueryStages(currentPhysicalPlan)
      val events = new LinkedBlockingQueue[StageMaterializationEvent]()
      val errors = new mutable.ArrayBuffer[Throwable]()
      var stagesToReplace = Seq.empty[QueryStageExec]
      while (!result.allChildStagesMaterialized) {
        currentPhysicalPlan = result.newPlan
        if (result.newStages.nonEmpty) {
          stagesToReplace = result.newStages ++ stagesToReplace
          executionId.foreach(onUpdatePlan(_, result.newStages.map(_.plan)))

          // SPARK-33933: we should submit tasks of broadcast stages first, to avoid waiting
          // for tasks to be scheduled and leading to broadcast timeout.
          // This partial fix only guarantees the start of materialization for BroadcastQueryStage
          // is prior to others, but because the submission of collect job for broadcasting is
          // running in another thread, the issue is not completely resolved.
          val reorderedNewStages = result.newStages
            .sortWith {
              case (_: BroadcastQueryStageExec, _: BroadcastQueryStageExec) => false
              case (_: BroadcastQueryStageExec, _) => true
              case _ => false
            }

          // Start materialization of all new stages and fail fast if any stages failed eagerly
          reorderedNewStages.foreach { stage =>
            try {
              stage.materialize().onComplete { res =>
                if (res.isSuccess) {
                  events.offer(StageSuccess(stage, res.get))
                } else {
                  events.offer(StageFailure(stage, res.failed.get))
                }
              }(AdaptiveSparkPlanExec.executionContext)
            } catch {
              case e: Throwable =>
                cleanUpAndThrowException(Seq(e), Some(stage.id))
            }
          }
        }

        // Wait on the next completed stage, which indicates new stats are available and probably
        // new stages can be created. There might be other stages that finish at around the same
        // time, so we process those stages too in order to reduce re-planning.
        val nextMsg = events.take()
        val rem = new util.ArrayList[StageMaterializationEvent]()
        events.drainTo(rem)
        (Seq(nextMsg) ++ rem.asScala).foreach {
          case StageSuccess(stage, res) =>
            stage.resultOption.set(Some(res))
          case StageFailure(stage, ex) =>
            errors.append(ex)
        }

        // In case of errors, we cancel all running stages and throw exception.
        if (errors.nonEmpty) {
          cleanUpAndThrowException(errors.toSeq, None)
        }

        // Try re-optimizing and re-planning. Adopt the new plan if its cost is equal to or less
        // than that of the current plan; otherwise keep the current physical plan together with
        // the current logical plan since the physical plan's logical links point to the logical
        // plan it has originated from.
        // Meanwhile, we keep a list of the query stages that have been created since last plan
        // update, which stands for the "semantic gap" between the current logical and physical
        // plans. And each time before re-planning, we replace the corresponding nodes in the
        // current logical plan with logical query stages to make it semantically in sync with
        // the current physical plan. Once a new plan is adopted and both logical and physical
        // plans are updated, we can clear the query stage list because at this point the two plans
        // are semantically and physically in sync again.
        val logicalPlan = replaceWithQueryStagesInLogicalPlan(currentLogicalPlan, stagesToReplace)
        val (newPhysicalPlan, newLogicalPlan) = reOptimize(logicalPlan)
        val origCost = costEvaluator.evaluateCost(currentPhysicalPlan)
        val newCost = costEvaluator.evaluateCost(newPhysicalPlan)
        if (newCost < origCost ||
            (newCost == origCost && currentPhysicalPlan != newPhysicalPlan)) {
          logOnLevel("Plan changed:\n" +
            sideBySide(currentPhysicalPlan.treeString, newPhysicalPlan.treeString).mkString("\n"))
          cleanUpTempTags(newPhysicalPlan)
          currentPhysicalPlan = newPhysicalPlan
          currentLogicalPlan = newLogicalPlan
          stagesToReplace = Seq.empty[QueryStageExec]
        }
        // Now that some stages have finished, we can try creating new stages.
        result = createQueryStages(currentPhysicalPlan)
      }

      // Run the final plan when there's no more unfinished stages.
      currentPhysicalPlan = applyPhysicalRules(
        optimizeQueryStage(result.newPlan, isFinalStage = true),
        postStageCreationRules(supportsColumnar),
        Some((planChangeLogger, "AQE Post Stage Creation")))
      isFinalPlan = true
      executionId.foreach(onUpdatePlan(_, Seq(currentPhysicalPlan)))
      currentPhysicalPlan
    }
```





### requiredDistribution

```scala
  @transient private val requiredDistribution: Option[Distribution] = if (isSubquery) {
    // Subquery output does not need a specific output partitioning.
    // 子查询的输出不需要指定分区
    Some(UnspecifiedDistribution)
  } else {
    // 分析给定的plan并计算需要的分布，例如用户调用repartition
    AQEUtils.getRequiredDistribution(inputPlan)
  }



  def getRequiredDistribution(p: SparkPlan): Option[Distribution] = p match {
    // User-specified repartition is only effective when it's the root node, or under
    // Project/Filter/LocalSort/CollectMetrics.
    // Note: we only care about `HashPartitioning` as `EnsureRequirements` can only optimize out
    // user-specified repartition with `HashPartitioning`.
    case ShuffleExchangeExec(h: HashPartitioning, _, shuffleOrigin)
        if shuffleOrigin == REPARTITION_BY_COL || shuffleOrigin == REPARTITION_BY_NUM =>
      val numPartitions = if (shuffleOrigin == REPARTITION_BY_NUM) {
        Some(h.numPartitions)
      } else {
        None
      }
      Some(ClusteredDistribution(h.expressions, requiredNumPartitions = numPartitions))
    case f: FilterExec => getRequiredDistribution(f.child)
    case s: SortExec if !s.global => getRequiredDistribution(s.child)
    case c: CollectMetricsExec => getRequiredDistribution(c.child)
    case p: ProjectExec =>
      getRequiredDistribution(p.child).flatMap {
        case h: ClusteredDistribution =>
          if (h.clustering.forall(e => p.projectList.exists(_.semanticEquals(e)))) {
            Some(h)
          } else {
            // It's possible that the user-specified repartition is effective but the output
            // partitioning is not retained, e.g. `df.repartition(a, b).select(c)`. We can't
            // handle this case with required distribution. Here we return None and later on
            // `EnsureRequirements` will skip optimizing out the user-specified repartition.
            None
          }
        case other => Some(other)
      }
    case _ => Some(UnspecifiedDistribution)
  }
```



### *costEvaluator*

```scala
case class SimpleCostEvaluator(forceOptimizeSkewedJoin: Boolean) extends CostEvaluator {
  override def evaluateCost(plan: SparkPlan): Cost = {
    val numShuffles = plan.collect {
      case s: ShuffleExchangeLike => s
    }.size

    if (forceOptimizeSkewedJoin) {
      val numSkewJoins = plan.collect {
        case j: ShuffledJoin if j.isSkewJoin => j
      }.size
      // We put `-numSkewJoins` in the first 32 bits of the long value, so that it's compared first
      // when comparing the cost, and larger `numSkewJoins` means lower cost.
      SimpleCost(-numSkewJoins.toLong << 32 | numShuffles)
    } else {
      SimpleCost(numShuffles)
    }
  }
}
```

### *queryStagePreparationRules*

在创建query  stage之前执行的一些规则，这些规则执行完后，物理计划的Exchange节点数应该是确定的。

```scala
  @transient private val queryStagePreparationRules: Seq[Rule[SparkPlan]] = {
    // For cases like `df.repartition(a, b).select(c)`, there is no distribution requirement for
    // the final plan, but we do need to respect the user-specified repartition. Here we ask
    // `EnsureRequirements` to not optimize out the user-specified repartition-by-col to work
    // around this case.
    // df.repartition(a, b).select(c) 这种情况不需要重分区，但为了尊重用户，这里让EnsureRequirements不要优化掉用户指定的重分区
    val ensureRequirements =
      EnsureRequirements(requiredDistribution.isDefined, requiredDistribution)
    Seq(
      RemoveRedundantProjects,
      ensureRequirements,
      ReplaceHashWithSortAgg,
      RemoveRedundantSorts,
      DisableUnnecessaryBucketedScan,
      OptimizeSkewedJoin(ensureRequirements)
    ) ++ context.session.sessionState.adaptiveRulesHolder.queryStagePrepRules
  }
```

#### RemoveRedundantProjects

以下情况会删除Project： output和子节点完全一样包括顺序，或者不要求顺序时只要output一样

#### ensureRequirements

通过插入*ShuffleExchangeExec*算子确保输入数据的*Partitioning*和每个算子需要的*Distribution*一致，同时确保输入分区排序需求一致。

```scala
case class EnsureRequirements(
   //是否优化掉用户的repartition，大多数情况是true,但在AQE里可以是false,因为AQE可能修改计划输出分区需要保留用户的repartition shuffle
    optimizeOutRepartition: Boolean = true,
    requiredDistribution: Option[Distribution] = None)
  extends Rule[SparkPlan]


def apply(plan: SparkPlan): SparkPlan = {
    val newPlan = plan.transformUp {
      case operator @ ShuffleExchangeExec(upper: HashPartitioning, child, shuffleOrigin)
          if optimizeOutRepartition &&
            (shuffleOrigin == REPARTITION_BY_COL || shuffleOrigin == REPARTITION_BY_NUM) =>
        def hasSemanticEqualPartitioning(partitioning: Partitioning): Boolean = {
          partitioning match {
            case lower: HashPartitioning if upper.semanticEquals(lower) => true
            case lower: PartitioningCollection =>
              lower.partitionings.exists(hasSemanticEqualPartitioning)
            case _ => false
          }
        }
        if (hasSemanticEqualPartitioning(child.outputPartitioning)) {
          child
        } else {
          operator
        }

      case operator: SparkPlan =>
        val reordered = reorderJoinPredicates(operator)
        val newChildren = ensureDistributionAndOrdering(
          reordered.children,
          reordered.requiredChildDistribution,
          reordered.requiredChildOrdering,
          ENSURE_REQUIREMENTS)
        reordered.withNewChildren(newChildren)
    }

    if (requiredDistribution.isDefined) {
      val shuffleOrigin = if (requiredDistribution.get.requiredNumPartitions.isDefined) {
        REPARTITION_BY_NUM
      } else {
        REPARTITION_BY_COL
      }
      val finalPlan = ensureDistributionAndOrdering(
        newPlan :: Nil,
        requiredDistribution.get :: Nil,
        Seq(Nil),
        shuffleOrigin)
      assert(finalPlan.size == 1)
      finalPlan.head
    } else {
      newPlan
    }
  }
```

reorderJoinPredicates:
当Join创建的物理算子，join key的顺序是根据sql中的顺序来的，可能和join节点的输出不一致，从而导致额外的排序/shuffle。

这里会改变join key的顺序：

```scala
  private def reorderJoinPredicates(plan: SparkPlan): SparkPlan = {
    plan match {
      case ShuffledHashJoinExec(
        leftKeys, rightKeys, joinType, buildSide, condition, left, right, isSkew) =>
        val (reorderedLeftKeys, reorderedRightKeys) =
          reorderJoinKeys(leftKeys, rightKeys, left.outputPartitioning, right.outputPartitioning)
        ShuffledHashJoinExec(reorderedLeftKeys, reorderedRightKeys, joinType, buildSide, condition,
          left, right, isSkew)

      case SortMergeJoinExec(leftKeys, rightKeys, joinType, condition, left, right, isSkew) =>
        val (reorderedLeftKeys, reorderedRightKeys) =
          reorderJoinKeys(leftKeys, rightKeys, left.outputPartitioning, right.outputPartitioning)
        SortMergeJoinExec(reorderedLeftKeys, reorderedRightKeys, joinType, condition,
          left, right, isSkew)

      case other => other
    }
  }
```



#### ReplaceHashWithSortAgg

sort聚合在无需排序的情况下，比hash 聚合少了hash开销。

替换hash聚合为sort聚合，只要符合以下条件：

1. *HashAggregateExec*/*ObjectHashAggregateExec*的partial聚合和final聚合对儿，partial聚合的子节点满足*SortAggregateExec*的排序
2. *HashAggregateExec*/*ObjectHashAggregateExec*，且子节点满足*SortAggregateExec*的排序

例如：

```scala
1. join后的聚合:
 HashAggregate(t1.i, SUM, final)
              |                         SortAggregate(t1.i, SUM, complete)
HashAggregate(t1.i, SUM, partial)   =>                |
              |                            SortMergeJoin(t1.i = t2.j)
   SortMergeJoin(t1.i = t2.j)
2. 排序后的聚合:
HashAggregate(t1.i, SUM, partial)         SortAggregate(t1.i, SUM, partial)
              |                     =>                  |
          Sort(t1.i)                                Sort(t1.i)
```



以下可忽略,纯记录下技巧

```scala
  private def replaceHashAgg(plan: SparkPlan): SparkPlan = {
    plan.transformDown {
      case hashAgg: BaseAggregateExec if isHashBasedAggWithKeys(hashAgg) =>
        val sortAgg = hashAgg.toSortAggregate
        hashAgg.child match {
          case partialAgg: BaseAggregateExec
            if isHashBasedAggWithKeys(partialAgg) && isPartialAgg(partialAgg, hashAgg) =>
            if (SortOrder.orderingSatisfies(
                partialAgg.child.outputOrdering, sortAgg.requiredChildOrdering.head)) {
              // case 类的copy方法，可修改其构造属性
              sortAgg.copy(
                aggregateExpressions = sortAgg.aggregateExpressions.map(_.copy(mode = Complete)),
                child = partialAgg.child)
            } else {
              hashAgg
            }
          case other =>
            if (SortOrder.orderingSatisfies(
                other.outputOrdering, sortAgg.requiredChildOrdering.head)) {
              sortAgg
            } else {
              hashAgg
            }
        }
      case other => other
    }
  }
```



```scala
  private def isPartialAgg(partialAgg: BaseAggregateExec, finalAgg: BaseAggregateExec): Boolean = {
    if (partialAgg.aggregateExpressions.forall(_.mode == Partial) &&
        finalAgg.aggregateExpressions.forall(_.mode == Final)) {
      //logicalLink用于判断是否属于同一个逻辑计划
      (finalAgg.logicalLink, partialAgg.logicalLink) match {
        					//sameResult用于判断产生同样结果的计划
        case (Some(agg1), Some(agg2)) => agg1.sameResult(agg2)
        case _ => false
      }
    } else {
      false
    }
  }
```

模式匹配多个类型

```scala
  private def isHashBasedAggWithKeys(agg: BaseAggregateExec): Boolean = {
    val isHashBasedAgg = agg match {
      case _: HashAggregateExec | _: ObjectHashAggregateExec => true
      case _ => false
    }
    isHashBasedAgg && agg.groupingExpressions.nonEmpty
  }
```



#### RemoveRedundantSorts

删除多余的排序算子，多余：它的子节点的sort顺序和distribution都满足该Sort。

```scala
  private def removeSorts(plan: SparkPlan): SparkPlan = plan transform {
    case s @ SortExec(orders, _, child, _)
        if SortOrder.orderingSatisfies(child.outputOrdering, orders) &&
          child.outputPartitioning.satisfies(s.requiredChildDistribution.head) =>
      child
  }
```



#### DisableUnnecessaryBucketedScan

#### OptimizeSkewedJoin(ensureRequirements)

把倾斜的分区切分为小分区，join的另一边对应的分区膨胀多份，从而并行执行。 注意，如果join的另一边也倾斜了，将变成笛卡尔积膨胀。

left:  [L1, L2, L3, L4]
right: [R1, R2, R3, R4]

假如，L2,L4和R3,R4倾斜，且每个被分为两个子分区，那么开始的4个task将被分为9个task:
(L1, R1),  未倾斜的不处理
(L2-1, R2), (L2-2, R2),右边膨胀
(L3, R3-1), (L3, R3-2),左边膨胀
(L4-1, R4-1), (L4-2, R4-1), (L4-1, R4-2), (L4-2, R4-2) 两边都膨胀

```scala
  override def apply(plan: SparkPlan): SparkPlan = {
    if (!conf.getConf(SQLConf.SKEW_JOIN_ENABLED)) {
      return plan
    }

   // 如果引入了额外的shuffle,则放弃这个优化，除非配置为强制apply这个优化
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

具体的实现后续查看源码





### *queryStageOptimizerRules*

在一个新query stage执行前应用的一些优化规则，

```scala 
  @transient private val queryStageOptimizerRules: Seq[Rule[SparkPlan]] = Seq(
    PlanAdaptiveDynamicPruningFilters(this),
    ReuseAdaptiveSubquery(context.subqueryCache),
    OptimizeSkewInRebalancePartitions,
    CoalesceShufflePartitions(context.session),
    // `OptimizeShuffleWithLocalRead` needs to make use of 'AQEShuffleReadExec.partitionSpecs'
    // added by `CoalesceShufflePartitions`, and must be executed after it.
    OptimizeShuffleWithLocalRead
  )
```





# 示例

select avg(w_warehouse_sq_ft) from tpcds1gv.warehouse

计划树

```java
== Parsed Logical Plan ==
'Project [unresolvedalias('avg('w_warehouse_sq_ft), None)]
+- 'UnresolvedRelation [tpcds1gv, warehouse], [], false

// 生成逻辑计划时插入了Aggregate算子，丢掉了Project算子
== Analyzed Logical Plan ==
avg(w_warehouse_sq_ft): double
Aggregate [avg(w_warehouse_sq_ft#3) AS avg(w_warehouse_sq_ft)#15]
+- SubqueryAlias spark_catalog.tpcds1gv.warehouse
   +- Relation tpcds1gv.warehouse[w_warehouse_sk#0,w_warehouse_id#1,w_warehouse_name#2,w_warehouse_sq_ft#3,w_street_number#4,w_street_name#5,w_street_type#6,w_suite_number#7,w_city#8,w_county#9,w_state#10,w_zip#11,w_country#12,w_gmt_offset#13] parquet

// 优化后的逻辑计划，在聚合前插入了一个Project                               
== Optimized Logical Plan ==
Aggregate [avg(w_warehouse_sq_ft#3) AS avg(w_warehouse_sq_ft)#15]
+- Project [w_warehouse_sq_ft#3]
   +- Relation tpcds1gv.warehouse[w_warehouse_sk#0,w_warehouse_id#1,w_warehouse_name#2,w_warehouse_sq_ft#3,w_street_number#4,w_street_name#5,w_street_type#6,w_suite_number#7,w_city#8,w_county#9,w_state#10,w_zip#11,w_country#12,w_gmt_offset#13] parquet

// 物理计划，插入了Exchange算子和AdaptiveSparkPlan算子                               
== Physical Plan ==
AdaptiveSparkPlan isFinalPlan=false
+- HashAggregate(keys=[], functions=[avg(w_warehouse_sq_ft#3)], output=[avg(w_warehouse_sq_ft)#15])
   +- Exchange SinglePartition, ENSURE_REQUIREMENTS, [id=#11]
      +- HashAggregate(keys=[], functions=[partial_avg(w_warehouse_sq_ft#3)], output=[sum#33, count#34L])
         +- FileScan parquet tpcds1gv.warehouse[w_warehouse_sq_ft#3] Batched: true, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/tpcds1gv.db/warehouse], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<w_warehouse_sq_ft:int>
```





## prepareForExecution

该函数拿到的plan如下：

```shell
HashAggregate(keys=[], functions=[avg(w_warehouse_sq_ft#3)], output=[avg(w_warehouse_sq_ft)#15])
+- HashAggregate(keys=[], functions=[partial_avg(w_warehouse_sq_ft#3)], output=[sum#33, count#34L])
   +- FileScan parquet tpcds1gv.warehouse[w_warehouse_sq_ft#3] Batched: true, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/tpcds1gv.db/warehouse], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<w_warehouse_sq_ft:int>
```

应用完InsertAdaptiveSparkPlan规则，具体是执行AdaptiveSparkPlanExec(newPlan, adaptiveExecutionContext, preprocessingRules, isSubquery)得到的物理计划中，插入了Exchange算子和AdaptiveSparkPlan算子            



