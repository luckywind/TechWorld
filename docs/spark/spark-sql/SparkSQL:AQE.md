[参考](https://zhuanlan.zhihu.com/p/608830887)

[可参考百度的优化总结](https://blog.csdn.net/fl63zv9zou86950w/article/details/79049280?utm_source=copy), [英文版](https://www.intel.com/content/www/us/en/developer/articles/technical/spark-sql-adaptive-execution-at-100-tb.html)

[还可参考](https://zhuanlan.zhihu.com/p/535174818)

[深入研究Apache Spark 3.0的新功能](https://blog.csdn.net/lucklilili/article/details/125135291)直播回放：https://developer.aliyun.com/live/2894，http://www.dawuzhe.cn/112197.html

[inter自适应查询执行AQE简介](https://www.pianshen.com/article/39801815777/)

[CBO](https://blog.csdn.net/zyzzxycj/article/details/106469572)，[CBO](https://blog.csdn.net/zyzzxycj/article/details/85839606?ops_request_misc=%257B%2522request%255Fid%2522%253A%2522159098356319725247658642%2522%252C%2522scm%2522%253A%252220140713.130102334.pc%255Fblog.%2522%257D&request_id=159098356319725247658642&biz_id=0&utm_medium=distribute.pc_search_result.none-task-blog-2~blog~first_rank_v2~rank_blog_default-1-85839606.pc_v2_rank_blog_default&utm_term=CBO)

[使用](https://blog.csdn.net/qq_32907491/article/details/124547060)

# SparkSQL Adaptive Execution简介

在Spark1.0中所有的Catalyst Optimizer都是基于规则 (rule) 优化的。为了产生比较好的查询规则，优化器需要理解数据的特性，于是在Spark2.0中引入了基于代价的优化器 （cost-based optimizer），也就是所谓的CBO。然而，CBO也无法解决很多问题，比如：

- 数据统计信息普遍缺失，统计信息的收集代价较高；
- 储存计算分离的架构使得收query集到的统计信息可能不再准确；
- Spark部署在某一单一的硬件架构上，cost很难被估计；
- Spark的UDF（User-defined Function）简单易用，种类繁多，但是对于CBO来说是个黑盒子，无法估计其cost。
- 手动指定执行hint跟不上数据变化。

> CBO默认是开启的，spark.conf.set("spark.sql.cbo.enabled", false)

而在Spark 3.0时代，AQE完全基于精确的运行时统计信息进行优化，引入了一个基本的概念Query Stages，并且以Query Stage为粒度，进行运行时的优化, 原理如下图

![image-20230718153928377](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230718153928377.png)

![image-20230718152515266](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230718152515266.png)

SparkSQL Adaptive Execution 是 Spark SQL 针对 SQL 查询计划的一种自适应执行优化技术。该技术的主要目标是在执行过程中自适应地调整执行计划，以提高查询效率。

![image-20230616165125482](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230616165125482.png)

首先以Exchange节点作为分界将执行计划这棵树划分成多个QueryStage（Exchange节点在Spark SQL中代表shuffle）。每一个QueryStage都是一棵独立的子树，也是一个独立的执行单元。在加入QueryStage的同时，我们也加入一个QueryStageInput的叶子节点，作为父亲QueryStage的输入。例如对于图中两表join的执行计划来说我们会创建3个QueryStage。最后一个QueryStage中的执行计划是join本身，它有2个QueryStageInput代表它的输入，分别指向2个孩子的QueryStage。在执行QueryStage时，我们首先提交它的孩子stage，并且收集这些stage运行时的信息。当这些孩子stage运行完毕后，我们可以知道它们的大小等信息，以此来判断QueryStage中的计划是否可以优化更新。例如当我们获知某一张表的大小是5M，它小于broadcast的阈值时，我们可以将SortMergeJoin转化成BroadcastHashJoin来优化当前的执行计划。我们也可以根据孩子stage产生的shuffle数据量，来动态地调整该stage的reducer个数。在完成一系列的优化处理后，最终我们为该QueryStage生成RDD的DAG图，并且提交给DAG Scheduler来执行。

<font color=red>总之，在非AQE的情况下，SparkSQL会转换为DAG图，然后DAGScheduler基于shuffle将其划分为多个stage, 然后再执行stage。在AQE的情况下，首先会将plan树拆分为多个QueryStages, 在执行时先将它的子 QueryStages 被提交。在所有子节点完成后，收集 shuffle 数据大小。根据收集到的 shuffle 数据统计信息，将当前 QueryStage 的执行计划优化为更好的执行计划。然后转换为DAG图再执行Stage。</font>

**目前AQE主要有三大特性**：

1. **自动分区合并**：在 Shuffle 过后，Reduce Task 数据分布参差不齐，AQE 将自动合并过小的数据分区。**即用一个Reduce Task 处理上游多个分区**
2. **Join 策略调整**：如果某张表在过滤之后，尺寸小于广播变量阈值，这张表参与的数据关联就会从 Shuffle Sort Merge Join 降级（Demote）为执行效率更高的 Broadcast Hash Join。
3. **自动倾斜处理**：结合配置项，AQE 自动拆分 Reduce 阶段过大的数据分区，用多个Reduce 来处理(join 另一侧非倾斜的分区直接复制)，降低单个 Reduce Task 的工作负载。



![image-20250731103023436](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250731103023436.png)



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

### AQE规则插入

QueryExecution在prepareForexecution时构建了一批规则,让物理计划可以执行。

1. 保证子查询planed
2. ER规则：数据分区和排序正确
3. 插入全阶段代码生成
4. 重用exchange和子查询



#### preparations

QueryExecution首次获取可执行物理计划，会先构造一个规则序列，构造时传入InsertAdaptiveSparkPlan

```scala
  lazy val executedPlan: SparkPlan = {
    assertOptimized()
    executePhase(QueryPlanningTracker.PLANNING) {
      QueryExecution.prepareForExecution(preparations, sparkPlan.clone())
    }
  }

//这里在获取规则序列时传入了InsertAdaptiveSparkPlan
protected def preparations: Seq[Rule[SparkPlan]] = {
    QueryExecution.preparations(sparkSession,
      Option(InsertAdaptiveSparkPlan(AdaptiveExecutionContext(sparkSession, this))), false)
  }
```

后续QueryStage的优化也会用这个方法构造规则序列，只是不会再传入InsertAdaptiveSparkPlan了。

方法内规则序列的运行细节：如果传入了adaptiveExecutionRule，则会插入AdaptiveSparkPlanExec并把原始plan（原始plan也就是所谓的Initial Plan）隐藏，而其余规则不认识AdaptiveSparkPlanExec，从而其余规则无效。但是ApplyColumnarRulesAndInsertTransitions这个规则被插件覆盖了，我们可以去识别AdaptiveSparkPlanExec来识别一条query。



后续每个queryStage会再次调用这个接口进行优化，只是不会传入adaptiveExecutionRule，从而其余规则会有效：

1. PlanDynamicPruningFilters规则会调preparations

2. 每个querystage创建前会先对物理计划进行优化，在

   queryStageOptimizerRules的PlanAdaptiveDynamicPruningFilters(this)规则里调用一次

```scala
  private[execution] def preparations(
      sparkSession: SparkSession,
    //这个规则默认是None，只有首次获取可执行物理计划时才会传入InsertAdaptiveSparkPlan规则，其他的调用都没传
      adaptiveExecutionRule: Option[InsertAdaptiveSparkPlan] = None,
      subquery: Boolean): Seq[Rule[SparkPlan]] = {
    // `AdaptiveSparkPlanExec` is a leaf node. If inserted, all the following rules will be no-op
    // as the original plan is hidden behind `AdaptiveSparkPlanExec`.
    // adaptiveExecutionRule规则一旦应用，后续规则就全部失效了，因为原始plan是AdaptiveSparkPlanExec的inputPlan而不是child
    adaptiveExecutionRule.toSeq ++
    Seq(
      CoalesceBucketsInJoin,
      //这里匹配DynamicPruningSubquery，还会调一次preparations这个方法
      PlanDynamicPruningFilters(sparkSession),
      PlanSubqueries(sparkSession),
      RemoveRedundantProjects,
      EnsureRequirements(),
      // `ReplaceHashWithSortAgg` needs to be added after `EnsureRequirements` to guarantee the
      // sort order of each node is checked to be valid.
      ReplaceHashWithSortAgg,
      // `RemoveRedundantSorts` needs to be added after `EnsureRequirements` to guarantee the same
      // number of partitions when instantiating PartitioningCollection.
      RemoveRedundantSorts,
      DisableUnnecessaryBucketedScan,
      ApplyColumnarRulesAndInsertTransitions(
        sparkSession.sessionState.columnarRules, outputsColumnar = false),
      CollapseCodegenStages()) ++
      (if (subquery) {
        Nil
      } else {
        Seq(ReuseExchangeAndSubquery)
      })
  }
```



### AQE规则：InsertAdaptiveSparkPlan

 extends Rule[SparkPlan] 

  AQE只在有exchange或者子查询的qeury中有用，满足以下条件之一：
  1. ADAPTIVE_EXECUTION_FORCE_APPLY=true
  2. 输入是一个子查询，则说明已经开始AQE了，必须继续执行
  3. query包含exchange
  4. query需要添加exchange
  5. query包含子查询

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
          // 2. 对子查询应用自适应规则
          val planSubqueriesRule = PlanAdaptiveSubqueries(subqueryMap)
             //预处理规则
          val preprocessingRules = Seq(
            planSubqueriesRule)
          // 3. 执行预处理规则
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
  4. query需要添加exchange,即requiredChildDistribution
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



<font color=red>所谓的subquery都是SubqueryExpression表达式，SubqueryExpression表达式是一个PlanExpression，它包含一个QueryPlan，这个QueryPlan可以是逻辑计划也可以是物理计划。</font>

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
    // 所有plan型表达式
    plan.foreach(_.expressions.filter(_.containsPattern(PLAN_EXPRESSION)).foreach(_.foreach {
      case expressions.ScalarSubquery(p, _, exprId, _)
          if !subqueryMap.contains(exprId.id) => //如果不存在
        val executedPlan = compileSubquery(p)//子查询转为物理计划
        verifyAdaptivePlan(executedPlan, p)//确保是AdaptiveSparkPlanExec的子类
        val subquery = SubqueryExec.createForScalarSubquery( //子查询的可执行物理计划SubqueryExec
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
    
    // 把sub-query的逻辑计划转为物理计划后，  再次应用这个规则的同一个实例，使得所有子查询共享stageCache用于Exchange重用
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
      case expressions.ScalarSubquery(_, _, exprId, _) =>
 				      //重用已有的？
        execution.ScalarSubquery(subqueryMap(exprId.id), exprId)
      //如果是InSubquery，则转为物理计划InSubqueryExec
      case expressions.InSubquery(values, ListQuery(_, _, exprId, _, _)) =>
        val expr = if (values.length == 1) {
          //in字句只有一个，则替换为值
          values.head
        } else {
          //否则，表达式替换为一个namedStruct
          CreateNamedStruct(
            values.zipWithIndex.flatMap { case (v, index) =>
              Seq(Literal(s"col_$index"), v)
            }
          )
        }
       //用替换后的表达式构造一个物理算子
        InSubqueryExec(expr, subqueryMap(exprId.id), exprId, shouldBroadcast = true)
      //如果是DynamicPruningSubquery表达式，改为in并重用子查询
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

#### 插入AdaptiveSparkPlanExec算子

AdaptiveSparkPlanExec(newPlan, adaptiveExecutionContext, preprocessingRules, isSubquery)



## QueryStageExec

在介绍AQE之前，先了解QueryStageExec

是query plan的独立子图，query stage在继续处理query plan的其他算子之前物化输出。输出的数据统计可用于优化后续query stage。

有两种query stage:

1. Shuffle query stage
   对应ShuffleQueryStageExec算子，这个stage物化输出到shuffle 文件，spark发起其他job执行其他算子
2. Broadcast query stage
   对应BroadcastQueryStageExec算子，物化输出到driver端的array中，spark在执行其他算子前广播array

### 核心接口

QueryStageExec本身也是一个plan叶子节点,它的子节点已经完成物化，所以它不需要维护子节点

```scala
abstract class QueryStageExec extends LeafExecNode { 
  //子类需要实现的接口  
   def getRuntimeStatistics: Statistics

  //计算统计信息
  def computeStats(): Option[Statistics] = if (isMaterialized) {
    val runtimeStats = getRuntimeStatistics
    val dataSize = runtimeStats.sizeInBytes.max(0)
    val numOutputRows = runtimeStats.rowCount.map(_.max(0))
    Some(Statistics(dataSize, numOutputRows, isRuntime = true))
  } else {
    None
  }
  


  /**
   * Cancel the stage materialization if in progress; otherwise do nothing.
   */
  def cancel(): Unit

  /**
    物化当前stage
   * Materialize this query stage, to prepare for the execution, like submitting map stages,
   * broadcasting data, etc. The caller side can use the returned [[Future]] to wait until this
   * stage is ready.
   */
  final def materialize(): Future[Any] = {
    logDebug(s"Materialize query stage ${this.getClass.getSimpleName}: $id")
    doMaterialize()
  }
 def doMaterialize(): Future[Any]
  
}  
```

下面看两个子类如何实现getRuntimeStatistics

### ShuffleQueryStageExec

#### 统计信息获取

我们看它如何实现getRuntimeStatistics，它交给ShuffleExchangeLike来实现了，而ShuffleExchangeLike的实现类是ShuffleExchangeExec算子

```scala
  override def getRuntimeStatistics: Statistics = shuffle.runtimeStatistics  
@transient val shuffle = plan match {
    case s: ShuffleExchangeLike => s
    case ReusedExchangeExec(_, s: ShuffleExchangeLike) => s
    case _ =>
      throw new IllegalStateException(s"wrong plan for shuffle stage:\n ${plan.treeString}")
  }
```

ShuffleExchangeExec通过metrics获取统计信息, 注意，它的rowCount的计算是shuffle写的数据量

```scala
// ShuffleExchangeExec.scala 
 //writeMetrics包含了shuffle bytes writen 和 输出行数
private lazy val writeMetrics =
    SQLShuffleWriteMetricsReporter.createShuffleWriteMetrics(sparkContext)

  private[sql] lazy val readMetrics =
    SQLShuffleReadMetricsReporter.createShuffleReadMetrics(sparkContext)
  override lazy val metrics = Map(
    "dataSize" -> SQLMetrics.createSizeMetric(sparkContext, "data size"),
    "numPartitions" -> SQLMetrics.createMetric(sparkContext, "number of partitions")
  ) ++ readMetrics ++ writeMetrics
override def runtimeStatistics: Statistics = {
    val dataSize = metrics("dataSize").value
    val rowCount = metrics(SQLShuffleWriteMetricsReporter.SHUFFLE_RECORDS_WRITTEN).value
    Statistics(dataSize, Some(rowCount))
  }
```

那么ShuffleExchangeExec算子如何计算metrics呢？ 

会用到ShuffleWriteMetricsReporter和ShuffleWriterProcessor，driver端会创建ShuffleWriterProcessor并放入ShuffleDependency，executor在每个ShuffleMapTask中会使用它：

```scala
  private lazy val serializer: Serializer =
    new UnsafeRowSerializer(child.output.size, longMetric("dataSize"))


lazy val shuffleDependency : ShuffleDependency[Int, InternalRow, InternalRow] = {
  // 创建一个ShuffleDependency
    val dep = ShuffleExchangeExec.prepareShuffleDependency(
      inputRDD,
      child.output,
      outputPartitioning,
      serializer,//metrics传入
      writeMetrics)
    metrics("numPartitions").set(dep.partitioner.numPartitions)
    val executionId = sparkContext.getLocalProperty(SQLExecution.EXECUTION_ID_KEY)
  // driver端更新了numPartitions,推送给executor
    SQLMetrics.postDriverMetricUpdates(
      sparkContext, executionId, metrics("numPartitions") :: Nil)
    dep
  }

  def prepareShuffleDependency(
      rdd: RDD[InternalRow],
      outputAttributes: Seq[Attribute],
      newPartitioning: Partitioning,
      serializer: Serializer,
      writeMetrics: Map[String, SQLMetric]){
  //    ... ...
    //创建ShuffleDependency时，会创建一个SQL专用的ShuffleWriterProcessor，它使用SQLShuffleWriteMetricsReporter包装了默认的metrics reporter
        val dependency =
      new ShuffleDependency[Int, InternalRow, InternalRow](
        rddWithPartitionIds,
        new PartitionIdPassthrough(part.numPartitions),
        serializer,
        shuffleWriterProcessor = createShuffleWriteProcessor(writeMetrics))//创建ShuffleWriteProcessor

    dependency
  }


  def createShuffleWriteProcessor(metrics: Map[String, SQLMetric]): ShuffleWriteProcessor = {
    new ShuffleWriteProcessor {
      override protected def createMetricsReporter(
          context: TaskContext): ShuffleWriteMetricsReporter = {
        //这个createShuffleWriteProcessor使用SQLShuffleWriteMetricsReporter
        new SQLShuffleWriteMetricsReporter(context.taskMetrics().shuffleWriteMetrics, metrics)
      }
    }
  }
```

创建ShuffleDependency时，会创建一个SQL专用的ShuffleWriterProcessor，它使用SQLShuffleWriteMetricsReporter包装了默认的metrics reporter。 

##### ShuffleWriteMetricsReporter

ShuffleWriteMetricsReporter特质是shuffle write metrics报告器接口， 有两个实现

1. ShuffleWriteMetrics：用累加器实现的metrics
2. SQLShuffleWriteMetricsReporter：SQL exchange 操作的shuffle write metrics。 它的构造包含了一个它需要更新的其他的ShuffleWriteMetricsReporter，它更新自身metrics的同时还会更新一个附属的metricsReporter

ShuffleWriterProcessor从shuffleManager获取一个writer时会用到reporter，会传给UnsafeShuffleWriter和BypassMergeSortShuffleWriter

##### ShuffleWriterProcessor(获取MapStatus)

driver端创建ShuffleWriterProcessor并放入了*ShuffleDependency*，executor在每个ShuffleMapTask中会使用它。

它的核心就是writer接口, 对应某个特定分区的write进程，控制从shuffleManager获取的shuffleWriter的生命周期、以及触发rdd compute、最终返回一个MapStatus（调用writer的stop方法）

```scala
  def write(
      rdd: RDD[_],
      dep: ShuffleDependency[_, _, _],
      mapId: Long,
      context: TaskContext,
      partition: Partition): MapStatus = {
    var writer: ShuffleWriter[Any, Any] = null
    try {
      val manager = SparkEnv.get.shuffleManager
      //从shuffleManager获取一个writer， 注意，这里会使用reporter
      /***
       只有UnsafeShuffleWriter和BypassMergeSortShuffleWriter才会使用这个reporter
      */
      writer = manager.getWriter[Any, Any](
        dep.shuffleHandle,
        mapId,
        context,
        createMetricsReporter(context))//这个参数就是上面提到的SQLShuffleWriteMetricsReporter
      //执行写操作
      writer.write(
        rdd.iterator(partition, context).asInstanceOf[Iterator[_ <: Product2[Any, Any]]])
      //再执行stop获取mapStatus对象，调用writer的stop方法，success表示map是否完成
      val mapStatus = writer.stop(success = true)
      if (mapStatus.isDefined) {
        // Check if sufficient shuffle mergers are available now for the ShuffleMapTask to push
        if (dep.shuffleMergeAllowed && dep.getMergerLocs.isEmpty) {
          val mapOutputTracker = SparkEnv.get.mapOutputTracker
          val mergerLocs =
            mapOutputTracker.getShufflePushMergerLocations(dep.shuffleId)
          if (mergerLocs.nonEmpty) {
            dep.setMergerLocs(mergerLocs)
          }
        }
        // Initiate shuffle push process if push based shuffle is enabled
        // The map task only takes care of converting the shuffle data file into multiple
        // block push requests. It delegates pushing the blocks to a different thread-pool -
        // ShuffleBlockPusher.BLOCK_PUSHER_POOL.
        if (!dep.shuffleMergeFinalized) {
          manager.shuffleBlockResolver match {
            case resolver: IndexShuffleBlockResolver =>
              logInfo(s"Shuffle merge enabled with ${dep.getMergerLocs.size} merger locations " +
                s" for stage ${context.stageId()} with shuffle ID ${dep.shuffleId}")
              logDebug(s"Starting pushing blocks for the task ${context.taskAttemptId()}")
              val dataFile = resolver.getDataFile(dep.shuffleId, mapId)
              new ShuffleBlockPusher(SparkEnv.get.conf)
                .initiateBlockPush(dataFile, writer.getPartitionLengths(), dep, partition.index)
            case _ =>
          }
        }
      }
      mapStatus.get
    } catch {
      case e: Exception =>
        try {
          if (writer != null) {
            writer.stop(success = false)
          }
        } catch {
          case e: Exception =>
            log.debug("Could not stop writer", e)
        }
        throw e
    }
  }
```

##### writer如何创建MapStatus?

###### UnsafeShuffleWriter

UnsafeShuffleWriter的构造就需要传入上面介绍的SQLShuffleWriteMetricsReporter

例如，它在merge两个spill文件时更新bytes

```scala
      for (int partition = 0; partition < numPartitions; partition++) {
        boolean copyThrewException = true;
        ShufflePartitionWriter writer = mapWriter.getPartitionWriter(partition);
        WritableByteChannelWrapper resolvedChannel = writer.openChannelWrapper()
            .orElseGet(() -> new StreamFallbackChannelWrapper(openStreamUnchecked(writer)));
        try {
          for (int i = 0; i < spills.length; i++) {
            long partitionLengthInSpill = spills[i].partitionLengths[partition];
            final FileChannel spillInputChannel = spillInputChannels[i];
            final long writeStartTime = System.nanoTime();
            Utils.copyFileStreamNIO(
                spillInputChannel,
                resolvedChannel.channel(),
                spillInputChannelPositions[i],
                partitionLengthInSpill);
            copyThrewException = false;
            spillInputChannelPositions[i] += partitionLengthInSpill;
            writeMetrics.incWriteTime(System.nanoTime() - writeStartTime);
          }
        } finally {
          Closeables.close(resolvedChannel, copyThrewException);
        }
        //获取并累加写的bytes数
        long numBytes = writer.getNumBytesWritten();
        writeMetrics.incBytesWritten(numBytes);
      }
```



###### SortShuffleWriter

```scala
private[spark] class SortShuffleWriter[K, V, C](
    handle: BaseShuffleHandle[K, V, C],
    mapId: Long,
    context: TaskContext,
    shuffleExecutorComponents: ShuffleExecutorComponents) 
  extends ShuffleWriter[K, V] with Logging {
 //    ... ...
  override def write(records: Iterator[Product2[K, V]]): Unit = {
    sorter = if (dep.mapSideCombine) {
      new ExternalSorter[K, V, C](
        context, dep.aggregator, Some(dep.partitioner), dep.keyOrdering, dep.serializer)
    } else {
      new ExternalSorter[K, V, V](
        context, aggregator = None, Some(dep.partitioner), ordering = None, dep.serializer)
    }
    //分区数据插入ExternalSorter
    sorter.insertAll(records)

    //创建一个writer，当前只有LocalDiskShuffleMapOutputWriter
    val mapOutputWriter = shuffleExecutorComponents.createMapOutputWriter(
      dep.shuffleId, mapId, dep.partitioner.numPartitions)
    //分区数据写入存储，这里会更新metrics
    sorter.writePartitionedMapOutput(dep.shuffleId, mapId, mapOutputWriter)
    //获取分区大小
    partitionLengths = mapOutputWriter.commitAllPartitions(sorter.getChecksums).getPartitionLengths
    //创建MapStatus
    mapStatus = MapStatus(blockManager.shuffleServerId, partitionLengths, mapId)
  }

//ExternalSorter.scala
  def writePartitionedMapOutput(
      shuffleId: Int,
      mapId: Long,
      mapOutputWriter: ShuffleMapOutputWriter): Unit = {
    ... ...

    context.taskMetrics().incMemoryBytesSpilled(memoryBytesSpilled)
    context.taskMetrics().incDiskBytesSpilled(diskBytesSpilled)
    context.taskMetrics().incPeakExecutionMemory(peakMemoryUsedBytes)
  }
```









#### 物化

会提交一个ShuffleJob

```scala
  @transient val shuffle = plan match {
    case s: ShuffleExchangeLike => s
    case ReusedExchangeExec(_, s: ShuffleExchangeLike) => s
    case _ =>
      throw new IllegalStateException(s"wrong plan for shuffle stage:\n ${plan.treeString}")
  }
 //提交一个shuffleJob
  @transient private lazy val shuffleFuture = shuffle.submitShuffleJob

  override def doMaterialize(): Future[Any] = shuffleFuture
```



```scala
//ShuffleExchangeLike  
final def submitShuffleJob: Future[MapOutputStatistics] = executeQuery {
    mapOutputStatisticsFuture
  }

  override lazy val mapOutputStatisticsFuture: Future[MapOutputStatistics] = {
    if (inputRDD.getNumPartitions == 0) {
      Future.successful(null)
    } else {
      //提交一个MapStage
      sparkContext.submitMapStage(shuffleDependency)
    }
  }


  private[spark] def submitMapStage[K, V, C](dependency: ShuffleDependency[K, V, C])
      : SimpleFutureAction[MapOutputStatistics] = {
    assertNotStopped()
    val callSite = getCallSite()
    var result: MapOutputStatistics = null
   //这个方法是用于adaptive query planning 执行map stage并收集输出统计数据
    val waiter = dagScheduler.submitMapStage(
      dependency,
      (r: MapOutputStatistics) => { result = r },
      callSite,
      localProperties.get)
    new SimpleFutureAction[MapOutputStatistics](waiter, result)
  }
```



```scala
ShuffleMapTask.runTask
shuffleWriterProcessor.write
```



### BroadcastQueryStageExec

#### 统计信息获取

```scala
 override def getRuntimeStatistics: Statistics = broadcast.runtimeStatistics
  @transient val broadcast = plan match {
    case b: BroadcastExchangeLike => b
    case ReusedExchangeExec(_, b: BroadcastExchangeLike) => b
    case _ =>
      throw new IllegalStateException(s"wrong plan for broadcast stage:\n ${plan.treeString}")
  }
```

它把统计任务交给了BroadcastExchangeLike特质，实现类是BroadcastExchangeExec算子，它仍然是使用metrics实现

```scala 
  override def runtimeStatistics: Statistics = {
    val dataSize = metrics("dataSize").value
    val rowCount = metrics("numOutputRows").value
    Statistics(dataSize, Some(rowCount))
  }
```

它的metrics计算是在relationFuture里实现的

```scala
  override lazy val relationFuture: Future[broadcast.Broadcast[Any]] = {
            // Setup a job group here so later it may get cancelled by groupId if necessary.
            sparkContext.setJobGroup(runId.toString, s"broadcast exchange (runId $runId)",
              interruptOnCancel = true)
            val beforeCollect = System.nanoTime()
            // Use executeCollect/executeCollectIterator to avoid conversion to Scala types
            //累加行数
            val (numRows, input) = child.executeCollectIterator()
            longMetric("numOutputRows") += numRows
//构建表               
            val relation = mode.transform(input, Some(numRows))
            val dataSize = relation match {
              case map: HashedRelation =>
                map.estimatedSize
              case arr: Array[InternalRow] =>
                arr.map(_.asInstanceOf[UnsafeRow].getSizeInBytes.toLong).sum
              case _ =>
                throw new SparkException("[BUG] BroadcastMode.transform returned unexpected " +
                  s"type: ${relation.getClass.getName}")
            }
            //累加dataSize
            longMetric("dataSize") += dataSize
```







#### 物化

同ShuffleQueryStageExec

## AdaptiveSparkPlanExec

```scala
case class AdaptiveSparkPlanExec(
    inputPlan: SparkPlan, // 输入plan,这个很重要
    @transient context: AdaptiveExecutionContext,
    @transient preprocessingRules: Seq[Rule[SparkPlan]],
    @transient isSubquery: Boolean,
    @transient override val supportsColumnar: Boolean = false)
  extends LeafExecNode
```

一个用于自适应执行查询计划的根节点。它将查询计划分割成独立的阶段，并根据它们的依赖顺序执行。查询阶段在末尾实现其输出结果。当一个阶段完成时，实例化输出结果的数据统计信息将用于优化剩余的查询。

为了创建查询阶段，我们从底向上遍历查询树。当我们遇到一个 Exchange 节点时，如果这个 Exchange 节点的所有子查询阶段都被实例化，我们就为这个 Exchange 节点创建一个新的查询阶段。一旦创建，新阶段就会异步地进行实例化。

当一个查询阶段完成实例化时，剩余的查询将基于所有实例化阶段提供的最新统计信息进行重新优化和计划。然后我们再遍历查询计划并在可能的情况下创建更多的阶段。当所有阶段都被实例化后，我们执行计划的其余部分。

### 主要成员

主要成员

**optimizer**：对运行时逻辑计划进行重新优化的优化器，比如DynamicJoinSelection规则进行动态join调整，EliminateLimits规则剔除掉不必要的GlobalLimit计划。 它继承了RuleExecutor，所以其逻辑和catalyst中的常规优化器是一样的

**costEvaluator**：物理计划开销计算器，如果经过重新优化的物理计划比原计划开销小，那么将会替换掉原物理计划，默认是SimpleCostEvaluator

以下三个规则序列是针对物理执行计划的，它们的执行逻辑都是applyPhysicalRules方法

**queryStagePreparationRules**：对AdaptiveSparkPlanExec持有的原物理计划做初始化的应用规则，这里面核心的便是EnsureRequirements，它的作用就是在原物理计划链上插入shuffle物理计划

**initialPlan**：初始化后的物理计划，也就是应用完queryStagePreparationRules规则后的物理计划，这时计划链中已经添加到shuffle物理计划

**queryStageOptimizerRules**：在创建QueryStage前应用的一些规则，比如应用CoalesceShufflePartitions规则进行shuffle后的分区缩减，QueryStage的概念类似于rdd中的stage，以shuffle进行划分

> PlanAdaptiveDynamicPruningFilters(this), 插入动态裁剪重用broadcast结果
> ReuseAdaptiveSubquery(context.*subqueryCache*),重用子查询
> OptimizeSkewInRebalancePartitions, 优化倾斜分区
> CoalesceShufflePartitions(context.session),基于map输出统计缩减shuffle分区避免大量小reduce任务
> OptimizeShuffleWithLocalRead：优化shuffle读为本地读

**postStageCreationRules**：在创建完QueryStage后应用的一些规则，比如应用CollapseCodegenStages规则进行全阶段代码生成，插入一个WholeStageCodegenExec计划





### optimizer(重新优化逻辑计划)

<font color=red>逻辑计划</font>优化器，可用于重新优化当前逻辑计划。

**AQEOptimizer的输入只能是逻辑计划，所以我们需要维护逻辑计划，AQEOptimizer拿到逻辑计划后先进行逻辑计划的优化，然后生成物理计划，最后优化物理计划；返回新的逻辑计划和物理计划。**

对运行时物理计划进行重新优化的优化器，比如DynamicJoinSelection规则进行动态join调整，EliminateLimits规则剔除掉不必要的GlobalLimit计划。 这里的规则仍然是可插拔的，用户可以通过配置spark.sql.adaptive.optimizer.excludedRules排除某个规则。

```scala
  @transient private val optimizer = new AQEOptimizer(conf,
    session.sessionState.adaptiveRulesHolder.runtimeOptimizerRules)
```

从继承关系看，AQEOptimizer仍然继承了RuleExecutor，所以AQEOptimizer的执行和catalyst的优化器执行过程是一样的。

```scala
class AQEOptimizer(conf: SQLConf, extendedRuntimeOptimizerRules: Seq[Rule[LogicalPlan]])
  extends RuleExecutor[LogicalPlan] {

  private def fixedPoint =
    FixedPoint(
      conf.optimizerMaxIterations,
      maxIterationsSetting = SQLConf.OPTIMIZER_MAX_ITERATIONS.key)

  private val defaultBatches = Seq(
    Batch("Propagate Empty Relations", fixedPoint,
      AQEPropagateEmptyRelation,
      ConvertToLocalRelation,
      UpdateAttributeNullability),
    Batch("Dynamic Join Selection", Once, DynamicJoinSelection),
    Batch("Eliminate Limits", fixedPoint, EliminateLimits),
    Batch("Optimize One Row Plan", fixedPoint, OptimizeOneRowPlan)) :+
    Batch("User Provided Runtime Optimizers", fixedPoint, extendedRuntimeOptimizerRules: _*)

  final override protected def batches: Seq[Batch] = {
    val excludedRules = conf.getConf(SQLConf.ADAPTIVE_OPTIMIZER_EXCLUDED_RULES)
      .toSeq.flatMap(Utils.stringToSeq)
    defaultBatches.flatMap { batch =>
      val filteredRules = batch.rules.filter { rule =>
        val exclude = excludedRules.contains(rule.ruleName)
        if (exclude) {
          logInfo(s"Optimization rule '${rule.ruleName}' is excluded from the optimizer.")
        }
        !exclude
      }
      if (batch.rules == filteredRules) {
        Some(batch)
      } else if (filteredRules.nonEmpty) {
        Some(Batch(batch.name, batch.strategy, filteredRules: _*))
      } else {
        logInfo(s"Optimization batch '${batch.name}' is excluded from the optimizer " +
          s"as all enclosed rules have been excluded.")
        None
      }
    }
  }

  override protected def isPlanIntegral(
      previousPlan: LogicalPlan,
      currentPlan: LogicalPlan): Boolean = {
    !Utils.isTesting || (currentPlan.resolved &&
      !currentPlan.exists(PlanHelper.specialExpressionsInUnsupportedOperator(_).nonEmpty) &&
      LogicalPlanIntegrity.checkIfExprIdsAreGloballyUnique(currentPlan) &&
      DataType.equalsIgnoreNullability(previousPlan.schema, currentPlan.schema))
  }
}
```

#### Propagate Empty Relations

该批规则包含了三个规则

1. AQEPropagateEmptyRelation
   继承了PropagateEmptyRelationBase规则，比PropagateEmptyRelationBase多优化一种case即单列NAAJ,识别空join
2. ConvertToLocalRelation
   把LocalRelation上的本地操作转为另一个LocalRelation。 
   例如，对于Project操作，如果表达式列表都是AttributeReference则可以直接在逻辑计划阶段就完成计算而用一个结果LocalRelation代替。
   limit操作也可对LocalRelation做一个take(limit)后封装为一个新的LocalRelation。
   filter操作，如果都可计算，则直接计算完
3. UpdateAttributeNullability

#### DynamicJoinSelection

> 这块代码需要了解Hint的实现

这算一个重磅优化了，包含了三个join选择策略(等值join)：

1. 识别一边有大量空分区的join,增加*NO_BROADCAST_HASH hint*避免广播，因为这种情况下shuffle join反而更快一些(join的一边为空)

   > 默认非空分区比例spark.sql.adaptive.nonEmptyPartitionRatioForBroadcastJoin=0.2
   >
   > 这部分代码参考hasManyEmptyPartitions从MapOutputStatistics获取

2. 识别所有分区都可构建本地map的join，增加*PREFER_SHUFFLE_HASH hint*以鼓励使用shuffle hash join 代替sort merge join

3. 如果一个join满足NO_BROADCAST_HASH 和 PREFER_SHUFFLE_HASH那么增加SHUFFLE_HASH hint

#### OptimizeOneRowPlan

这个优化规则在catlyst里也有，使用最大行来优化plan

1. 如果sort的输入不多于一行，则省略sort
2. 如果分区local sort的输入不多于一行，省略本地sort
3. 聚合的输入少于一行则转为project
4. 聚合的输入行不多于一行，则所有的聚合表达式的distinct都设置为false

```scala
object OptimizeOneRowPlan extends Rule[LogicalPlan] {
  override def apply(plan: LogicalPlan): LogicalPlan = {
    plan.transformUpWithPruning(_.containsAnyPattern(SORT, AGGREGATE), ruleId) {
      case Sort(_, _, child) if child.maxRows.exists(_ <= 1L) => child
      case Sort(_, false, child) if child.maxRowsPerPartition.exists(_ <= 1L) => child
      case agg @ Aggregate(_, _, child) if agg.groupOnly && child.maxRows.exists(_ <= 1L) =>
        Project(agg.aggregateExpressions, child)
      case agg: Aggregate if agg.child.maxRows.exists(_ <= 1L) =>
        agg.transformExpressions {
          case aggExpr: AggregateExpression if aggExpr.isDistinct =>
            aggExpr.copy(isDistinct = false)
        }
    }
  }
}
```



#### EliminateLimits

1. 如果输入数据行数小于limit数，则省略limit
2. 合并相邻的limit

extendedRuntimeOptimizerRules是用户指定的扩展的优化器

### doExecute 入口

它是物理计划执行算子，那么它的核心就在于Executor，即方法——doExecute

```scala
override def doExecute(): RDD[InternalRow] = {
  withFinalPlanUpdate(_.execute())
}

  private def withFinalPlanUpdate[T](fun: SparkPlan => T): T = {
    //核心代码，获取最终物理计划
    val plan = getFinalPhysicalPlan()
    //执行最终计划，即调用plan.execute()
    val result = fun(plan)
    //触发lazy exe
    finalPlanUpdate
    result
  }
```

### getFinalPhysicalPlan源码

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

cost计算器，这个是默认实现，用户可以提供自定义实现，这里就是简单的统计shuffleExchange算子个数。

```scala
case class SimpleCostEvaluator(forceOptimizeSkewedJoin: Boolean) extends CostEvaluator {
  override def evaluateCost(plan: SparkPlan): Cost = {
    val numShuffles = plan.collect {
      case s: ShuffleExchangeLike => s
    }.size
  //默认不开启
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

### queryStagePreparationRules(创建初始计划)

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

#### ensureRequirements(核心)

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
			//如果和child分区方式语义上是相同的，则丢掉这个shuffle
      if (hasSemanticEqualPartitioning(child.outputPartitioning)) {
          child
        } else {
        //否则，保留这个shuffle
          operator
        }

      case operator: SparkPlan =>
    //对join算子的joinKey进行排序，使得match子节点的分区，避免引入额外的sort/shuffle
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



##### ensureDistributionAndOrdering

```scala
  private def ensureDistributionAndOrdering(
      originalChildren: Seq[SparkPlan],
      requiredChildDistributions: Seq[Distribution],
      requiredChildOrderings: Seq[Seq[SortOrder]],
      shuffleOrigin: ShuffleOrigin): Seq[SparkPlan] = {
    assert(requiredChildDistributions.length == originalChildren.length)
    assert(requiredChildOrderings.length == originalChildren.length)
     //当子节点和数据分布和当前节点的数据分布类型不一致时，说明需要进行数据shuffle，就添加一个ShuffleExchangeExec
    //计算新的children
    var children = originalChildren.zip(requiredChildDistributions).map {
      // case 1 子节点分区满足，无需处理
      case (child, distribution) if child.outputPartitioning.satisfies(distribution) =>
        child
      // case 2 需要子节点进行广播，则插入BroadcastExchangeExec算子
      case (child, BroadcastDistribution(mode)) =>
        BroadcastExchangeExec(mode, child)
      // case 3 需要子节点按指定分区数重分区，则插入ShuffleExchangeExec算子
      case (child, distribution) =>
        val numPartitions = distribution.requiredNumPartitions
          .getOrElse(conf.numShufflePartitions)
        ShuffleExchangeExec(distribution.createPartitioning(numPartitions), child, shuffleOrigin)
    }
    ...
    children
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

把倾斜的分区切分为小分区(需要spark.sql.adaptive.skewJoin.enabled=true(默认值))，join的另一边对应的分区膨胀多份，从而并行执行。 注意，如果join的另一边也倾斜了，将变成笛卡尔积膨胀。

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





### queryStageOptimizerRules

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

#### PlanAdaptiveDynamicPruningFilters(this)

 插入动态裁剪重用broadcast结果

#### ReuseAdaptiveSubquery(context.*subqueryCache*)

重用子查询

#### OptimizeSkewInRebalancePartitions

 优化倾斜分区

```scala
  private def tryOptimizeSkewedPartitions(shuffle: ShuffleQueryStageExec): SparkPlan = {
    val advisorySize = conf.getConf(SQLConf.ADVISORY_PARTITION_SIZE_IN_BYTES)
    //从ShuffleQueryStageExec算子获取map输出统计，mapStats.get.bytesByPartitionId返回每个分区的大小
    val mapStats = shuffle.mapStats
    if (mapStats.isEmpty ||
      mapStats.get.bytesByPartitionId.forall(_ <= advisorySize)) {
      return shuffle
    }
    
    val newPartitionsSpec = optimizeSkewedPartitions(
      mapStats.get.shuffleId, mapStats.get.bytesByPartitionId, advisorySize)
    // return origin plan if we can not optimize partitions
    if (newPartitionsSpec.length == mapStats.get.bytesByPartitionId.length) {
      shuffle
    } else {
      AQEShuffleReadExec(shuffle, newPartitionsSpec)
    }
  }
```



#### CoalesceShufflePartitions(context.session)

基于map输出统计缩减shuffle分区避免大量小reduce任务

#### OptimizeShuffleWithLocalRead

优化shuffle读为本地读



### getFinalPhysicalPlan(创建最终计划)

1、创建首个QueryStage，QueryStage的概念就是将整个物理计划链划分为若干个stage进行执行，这和rdd中的stage概念是类似的，基本都是基于shuffle进行划分

2、物化stage，也就是进行stage提交，以job的方式进行执行，然后优化物理计划，创建下个stage，直到所有的子stage都被物化，以rdd的概念来说，就是直到所有的ShuffleMapStage都被执行后，将不会再有stage被创建，这时会得到一个接近最终状态物理计划

3、对这个物理继续应用queryStageOptimizerRules和postStageCreationRules规则，这相当于对最终的ResultStage进行的优化，然后得到一个最终的物理计划



1. 首先提交Broadcast对应的task，防止broadcast等待超时
2. 等待stage完成，意味着有新的统计信息可用，从而可正确创建新的stage，如果此时有多个stage完成，则一起处理。
3. 新plan和原plan取cost最小的

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
      // logicLink是用来获取对应的逻辑计划的
      var currentLogicalPlan = inputPlan.logicalLink.get
      //首次创建queryStage，第一个需要shuffle的stage
      var result = createQueryStages(currentPhysicalPlan)
      
      //stage运行结果队列，是一个阻塞队列，实现等待上一个stage的完成,队列里放的是stage运行结果状态:是否成功，MapOutStat
      val events = new LinkedBlockingQueue[StageMaterializationEvent]()
      val errors = new mutable.ArrayBuffer[Throwable]()
      //一个stage容器，新建的stage不断的添加进来，然后完成物化，填充MapOut信息。
      var stagesToReplace = Seq.empty[QueryStageExec]
      
      // 只要创建结果存在未物化的子stage, 就进入循环
      while (!result.allChildStagesMaterialized) {
        //替换物理计划为创建stage之后的物理计划
        currentPhysicalPlan = result.newPlan
        if (result.newStages.nonEmpty) {
          stagesToReplace = result.newStages ++ stagesToReplace
          executionId.foreach(onUpdatePlan(_, result.newStages.map(_.plan)))

          // SPARK-33933: we should submit tasks of broadcast stages first, to avoid waiting
          // for tasks to be scheduled and leading to broadcast timeout.
          // This partial fix only guarantees the start of materialization for BroadcastQueryStage
          // is prior to others, but because the submission of collect job for broadcasting is
          // running in another thread, the issue is not completely resolved.
          // 对新建的Query Stage进行排序：首先提交broadcast stage避免等待调度导致广播超时。
          val reorderedNewStages = result.newStages //获取未物化的QueryStage
            .sortWith {
              case (_: BroadcastQueryStageExec, _: BroadcastQueryStageExec) => false
              case (_: BroadcastQueryStageExec, _) => true
              case _ => false
            }

          //物化stage，即实际执行stage,以Job的方式提交执行
          reorderedNewStages.foreach { stage =>
            try {
              //materialize()方法最终会调用ShuffleExchangeExec算子的submitShuffleJob方法
              stage.materialize().onComplete { res =>
                if (res.isSuccess) {
                  //res是新提交的stage的物化结果，这里封装为StageSuccess放入阻塞队列events中
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
        /**
        从阻塞队列获取消息
         一旦一个stage完成，意味着新的统计数据可用，从而可以新建stage
        */
        val nextMsg = events.take()
        val rem = new util.ArrayList[StageMaterializationEvent]()
        events.drainTo(rem)
        (Seq(nextMsg) ++ rem.asScala).foreach {
          case StageSuccess(stage, res) =>
          // 阻塞队列中拉取到的stage已经完成物化，填充输出统计信息，后续reOptimize会用到
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
        /**
        重新优化，如果新计划更便宜，则采用新计划，否则， 让当前物理计划和当前逻辑计划在一起，因为物理计划的logic link指向了原始逻辑计划。
        期间，我们保存自从上次plan更新以来，创建的一列query stage，它们代表了当前逻辑计划和物理计划之间的语义差距。
        在每次重新计划之前，用逻辑query stage替换当前逻辑计划中的相应节点，使其与当前物理计划语义上一致。
        一旦心计划被采用并且更新了逻辑计划和物理计划，就可以清空query stage列表了，因为此时物理计划和逻辑计划在语义上和物理上再次同步。
        */
        //重新优化都是对逻辑计划进行重新优化，但此时逻辑计划已经和现在的物理计划不同步了 
        //逻辑计划中的某些节点被替换成了带有MapOutStat的LogicalQueryPlan，从而可用于reOptimize
        val logicalPlan = replaceWithQueryStagesInLogicalPlan(currentLogicalPlan, stagesToReplace)
        //重新优化逻辑计划，会先清空当前逻辑计划的stat缓存。
        //AQEOptimizer的输入只能是逻辑计划，所以我们需要维护逻辑计划，AQEOptimizer拿到逻辑计划后先进行逻辑计划的优化，然后生成物理计划，最后优化物理计划；返回新的逻辑计划和物理计划。
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
        // 对更新后的物理计划创建queryStages, 更新创建结果
        result = createQueryStages(currentPhysicalPlan)
      }

      // Run the final plan when there's no more unfinished stages.
      //当所有stage都完成时，执行最终的物理计划
      currentPhysicalPlan = applyPhysicalRules(
        optimizeQueryStage(result.newPlan, isFinalStage = true),
        postStageCreationRules(supportsColumnar),
        Some((planChangeLogger, "AQE Post Stage Creation")))
      isFinalPlan = true
      executionId.foreach(onUpdatePlan(_, Seq(currentPhysicalPlan)))
      currentPhysicalPlan
    }
  }
```



接下来分析每个具体的过程





#### createQueryStages

**QueryStage的创建依赖于shuffle，也就是Exchange，在遇到Exchange计划节点时才会决定创建QueryStage，插入一个QueryStageExec计划，一般为ShuffleQueryStageExec。这个过程是递归自下而上的，也就是后续遍历的过程。创建QueryStage时需要保证这个stage的子stage，也就是上一个stage已经被物化**，若未被物化，则该Exchange节点不会创建新的QueryStage，直接返回上一个stage，这就保证每次创建的stage都是最底层的那个未被物化的新stage。  这个过程有点像spark rdd stage的创建过程，从根节点向下递归，这个过程不创建stage,只有递归到所有子节点都完成物化时才开始创建stage，然后再从下到上递归创建stage。

自底向上递归遍历plan树并调用这个函数，创建query stage或者重用query stage(如果当前节点是Exchange节点且所有子stage完成物化)

每次调用，返回CreateStageResult，表示：

1.  带有新建QueryStageExec节点的plan
2. 是否当前节点的所有子stage完成物化
3. 新建的query stage列表,未物化

对于不同类型的节点处理逻辑：

1. 对于Exchange类型的节点，exchange会被QueryStageExec节点替代。如果开启了stageCache，同时exchange节点是存在的stage, 则直接重用stage作为QueryStage, 并封装返回CreateStageResult。否则，从下向上迭代，如果孩子节点都迭代完成，将基于broadcast转换为BroadcastQueryStageExec，shuffle作为huffleQueryStageExec，并将其依次封装为CreateStageResult。
2. 对于***QueryStageExec\***节点类型，直接封装为CreateStageResult *返回。*
3. 对于其余类型，createQueryStages 函数应用于节点的直接子节点，这当然会导致再次调用 createQueryStages 并创建其他 QueryStageExec。

```scala
  private def createQueryStages(plan: SparkPlan): CreateStageResult = plan match {
    case e: Exchange =>
      // 如果是Exchange节点，判断是否应该创建query stage, 首先快速check stageCache从而避免不必要遍历这个子树
      //  e.canonicalized:查找前先对e进行规范化
      context.stageCache.get(e.canonicalized) match {
        case Some(existingStage) if conf.exchangeReuseEnabled =>//1. 说明已经存在，可重用
          val stage = reuseQueryStage(existingStage, e)
          //stage是否物化
          val isMaterialized = stage.isMaterialized
          CreateStageResult(
            newPlan = stage,
            allChildStagesMaterialized = isMaterialized,
            //如果未物化则不创建新的stage,否则返回重用的stage, 从这里猜想newStages是未物化的queryStage
            newStages = if (isMaterialized) Seq.empty else Seq(stage))

        case _ =>    //2. 说明不存在，  向下递归查找Exchange
          val result = createQueryStages(e.child)
          val newPlan = e.withNewChildren(Seq(result.newPlan)).asInstanceOf[Exchange]
          // 如果所有子stage都已物化，则新建一个query stage
          if (result.allChildStagesMaterialized) {
            
            //真正新建queryStage
            var newStage = newQueryStage(newPlan)
            if (conf.exchangeReuseEnabled) {
              // Check the `stageCache` again for reuse. If a match is found, ditch the new stage
              // and reuse the existing stage found in the `stageCache`, otherwise update the
              // `stageCache` with the new stage.
              //再次检查stageCache，
              val queryStage = context.stageCache.getOrElseUpdate(
                newStage.plan.canonicalized, newStage) //查找，不存在则更新
              if (queryStage.ne(newStage)) {//未返回刚创建的，说明在缓存中找到已存在的了，从而重用即可
                newStage = reuseQueryStage(queryStage, e)
              }
            }
            val isMaterialized = newStage.isMaterialized
            CreateStageResult(
              newPlan = newStage,
              allChildStagesMaterialized = isMaterialized,
              newStages = if (isMaterialized) Seq.empty else Seq(newStage))
          } else {
            CreateStageResult(newPlan = newPlan,
              allChildStagesMaterialized = false, newStages = result.newStages)
          }
      }

    case q: QueryStageExec =>
      CreateStageResult(newPlan = q,
        allChildStagesMaterialized = q.isMaterialized, newStages = Seq.empty)

    //常规节点，递归向下遍历，直到叶子节点，返回CreateStageResult
    case _ =>
      //叶子节点直接创建CreateStageResult
      if (plan.children.isEmpty) {
              //newPlan仍然是原来的plan,没有创建queryStage, newStages为空
        CreateStageResult(newPlan = plan, allChildStagesMaterialized = true, newStages = Seq.empty)
      } else {
        //否则递归对子节点创建queryStage
        val results = plan.children.map(createQueryStages)
        
        CreateStageResult(
          //注意withNewChildren, newPlan为原plan，只不过所有子节点完成了替换，替换为创建完queryStage后的newPlan
          newPlan = plan.withNewChildren(results.map(_.newPlan)),
          allChildStagesMaterialized = results.forall(_.allChildStagesMaterialized),
          newStages = results.flatMap(_.newStages))
      }
  }
```



##### newQueryStage

有两种类型的QueryStageExec可以物化统计数据，用于AQE的后序优化。

1. Shuffle 查询阶段：这个阶段将其输出具体化为 Shuffle 文件，Spark 启动另一个作业来执行进一步的算子。
2. 广播查询阶段：这个阶段将其输出具体化为 Driver JVM 中的一个数组。Spark 在执行进一步的算子之前先广播数组。

```scala
  private def newQueryStage(e: Exchange): QueryStageExec = {
    // 优化后的plan，应用queryStageOptimizerRules进行物理计划优化，比如缩减shuffle分区
    val optimizedPlan = optimizeQueryStage(e.child, isFinalStage = false)
    val queryStage = e match {
      case s: ShuffleExchangeLike =>
        //应用postStageCreationRules进行优化，比如全阶段代码生成
        val newShuffle = applyPhysicalRules(
          s.withNewChildren(Seq(optimizedPlan)),
          postStageCreationRules(outputsColumnar = s.supportsColumnar),
          Some((planChangeLogger, "AQE Post Stage Creation")))
        if (!newShuffle.isInstanceOf[ShuffleExchangeLike]) {
          throw new IllegalStateException(
            "Custom columnar rules cannot transform shuffle node to something else.")
        }
        //新建ShuffleQueryStageExec算子,意味着QueryStage物理计划完成创建
        ShuffleQueryStageExec(currentStageId, newShuffle, s.canonicalized)
      case b: BroadcastExchangeLike =>
        val newBroadcast = applyPhysicalRules(
          b.withNewChildren(Seq(optimizedPlan)),
          postStageCreationRules(outputsColumnar = b.supportsColumnar),
          Some((planChangeLogger, "AQE Post Stage Creation")))
        if (!newBroadcast.isInstanceOf[BroadcastExchangeLike]) {
          throw new IllegalStateException(
            "Custom columnar rules cannot transform broadcast node to something else.")
        }
       //新建BroadcastQueryStageExec算子
        BroadcastQueryStageExec(currentStageId, newBroadcast, b.canonicalized)
    }
    currentStageId += 1
    setLogicalLinkForNewQueryStage(queryStage, e)
    queryStage
  }
```

###### postStageCreationRules

在query stage完成创建后，会应用一系列物理优化规则，注意，**每个输入plan的根节点都是exchange**

比如在创建stage前应用CoalesceShufflePartitions规则进行shuffle后的分区缩减，这会在上一个shuffle/ShuffleQueryStageExec计划之后、这个stage的起点处增加一个AQEShuffleReadExec计划；在创建stage后应用CollapseCodegenStages规则进行全阶段代码生成，插入一个WholeStageCodegenExec计划。QueryStageExec属于整个物理计划的一部分，那么整个物理计划也随之改变了

```scala
  private def postStageCreationRules(outputsColumnar: Boolean) = Seq(
    ApplyColumnarRulesAndInsertTransitions(
      context.session.sessionState.columnarRules, outputsColumnar),
    collapseCodegenStagesRule
  )
```



##### optimizeQueryStage

这一步主要是调用创建QueryStage时用到的规则

```scala
  private def optimizeQueryStage(plan: SparkPlan, isFinalStage: Boolean): SparkPlan = {
    val optimized = queryStageOptimizerRules.foldLeft(plan) { case (latestPlan, rule) =>
      val applied = rule.apply(latestPlan)
      val result = rule match {
        case _: AQEShuffleReadRule if !applied.fastEquals(latestPlan) =>
          val distribution = if (isFinalStage) {
            // If `requiredDistribution` is None, it means `EnsureRequirements` will not optimize
            // out the user-specified repartition, thus we don't have a distribution requirement
            // for the final plan.
            requiredDistribution.getOrElse(UnspecifiedDistribution)
          } else {
            UnspecifiedDistribution
          }
          if (ValidateRequirements.validate(applied, distribution)) {
            applied
          } else {
            logDebug(s"Rule ${rule.ruleName} is not applied as it breaks the " +
              "distribution requirement of the query plan.")
            latestPlan
          }
        case _ => applied
      }
      planChangeLogger.logRule(rule.ruleName, latestPlan, result)
      result
    }
    planChangeLogger.logBatch("AQE Query Stage Optimization", plan, optimized)
    optimized
  }
```
#### replaceWithQueryStagesInLogicalPlan

对stagesToReplace中的每个QueryStageExec物理算子，找到它们在LogicalPlan中对应的节点并将它们替换为*LogicalQueryStage*节点。

> LogicalQueryStage所包含的queryStage算子物化后会包含输出统计信息，在reOptimize阶段会用到

1. 如果query stage可以映射为内部逻辑子树，替换对应的逻辑子树为引用该query stage的叶子节点*LogicalQueryStage*
   例如(Xchg1，Xchg2代表Exchange节点)

   ```java
       Join                   SMJ                      SMJ
     /     \                /    \                   /    \
   r1      r2    =>    Xchg1     Xchg2    =>    Stage1     Stage2
                         |        |
                         r1       r2
   更新后的节点:
                              Join
                            /     \
   LogicalQueryStage1(Stage1)     LogicalQueryStage2(Stage2)
                     
   ```

2. 否则(意味着query stage只能映射到逻辑子树的一部分)，替换对应的逻辑子树为叶子节点LogicalQueryStage，它引用该节点产生的物理计划的顶层节点，例如agg节点产生物理计划时会产生partial_agg->exchange->full_agg三个物理节点，而query stage只能映射为它的一部分，那么再替换这个子树时需要把顶层节点full_agg也包进来

   ```shell
    Agg           HashAgg          HashAgg
     |               |                |
   child    =>     Xchg      =>     Stage1
                     |
                  HashAgg
                     |
                   child
   更新后的节点：
   LogicalQueryStage(HashAgg - Stage1)
   ```

   

```scala
  private def replaceWithQueryStagesInLogicalPlan(
      plan: LogicalPlan,
      stagesToReplace: Seq[QueryStageExec]): LogicalPlan = {
    var logicalPlan = plan
    stagesToReplace.foreach {
      //注意这个exists:前序遍历树
      case stage if currentPhysicalPlan.exists(_.eq(stage)) =>
      // 找到stage对应的逻辑节点
        val logicalNodeOpt = stage.getTagValue(TEMP_LOGICAL_PLAN_TAG).orElse(stage.logicalLink)
        assert(logicalNodeOpt.isDefined)
        val logicalNode = logicalNodeOpt.get
      // 找到这个逻辑计划在当前物理计划中对应的子树的根节点
        val physicalNode = currentPhysicalPlan.collectFirst {
          case p if p.eq(stage) ||
            p.getTagValue(TEMP_LOGICAL_PLAN_TAG).exists(logicalNode.eq) ||
            p.logicalLink.exists(logicalNode.eq) => p
        }
        assert(physicalNode.isDefined)
        // Set the temp link for those nodes that are wrapped inside a `LogicalQueryStage` node for
        // they will be shared and reused by different physical plans and their usual logical links
        // can be overwritten through re-planning processes.
        setTempTagRecursive(physicalNode.get, logicalNode)
        // 替换对应的logical node 为LogicalQueryStage
        val newLogicalNode = LogicalQueryStage(logicalNode, physicalNode.get)
        val newLogicalPlan = logicalPlan.transformDown {
          case p if p.eq(logicalNode) => newLogicalNode
        }
        logicalPlan = newLogicalPlan

      case _ => // Ignore those earlier stages that have been wrapped in later stages.
    }
    logicalPlan
  }

```
#### final PhysicalPlan

等到前面的QueryStage（RDD层面来说就是ShuffleMapStage）都物化完了，这时才会确定下来最终的物理计划，这时也就剩下最后一个stage了，也就是ResultStage，ResultStage无需再进行专门创建，因为从最后一个物理计划到上一个ShuffleQueryStageExec这个过程便是ResultStage了，只需要去应用一些规则进行优化即可，同样也会应用之前创建stage时应用的规则，比如CoalesceShufflePartitions和CollapseCodegenStages

```scala
      //当所有stage都完成时，执行最终的物理计划
      currentPhysicalPlan = applyPhysicalRules(
        optimizeQueryStage(result.newPlan, isFinalStage = true),
        postStageCreationRules(supportsColumnar),
        Some((planChangeLogger, "AQE Post Stage Creation")))
      isFinalPlan = true
      executionId.foreach(onUpdatePlan(_, Seq(currentPhysicalPlan)))
      currentPhysicalPlan
    }
```



# 规则应用顺序



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



## sql

select ss_sold_date_sk, avg(ss_sales_price) from  ds1g.store_sales group by ss_sold_date_sk order by avg(ss_sales_price)

```shell
== Parsed Logical Plan ==
'Sort ['avg('ss_sales_price) ASC NULLS FIRST], true
+- 'Aggregate ['ss_sold_date_sk], ['ss_sold_date_sk, unresolvedalias('avg('ss_sales_price), None)]
   +- 'UnresolvedRelation [ds1g, store_sales], [], false

== Analyzed Logical Plan ==
ss_sold_date_sk: int, avg(ss_sales_price): decimal(11,6)
Sort [avg(ss_sales_price)#24 ASC NULLS FIRST], true
+- Aggregate [ss_sold_date_sk#0], [ss_sold_date_sk#0, avg(ss_sales_price#13) AS avg(ss_sales_price)#24]
   +- SubqueryAlias spark_catalog.ds1g.store_sales
      +- Relation ds1g.store_sales[ss_sold_date_sk#0,ss_sold_time_sk#1,ss_item_sk#2,ss_customer_sk#3,ss_cdemo_sk#4,ss_hdemo_sk#5,ss_addr_sk#6,ss_store_sk#7,ss_promo_sk#8,ss_ticket_number#9,ss_quantity#10,ss_wholesale_cost#11,ss_list_price#12,ss_sales_price#13,ss_ext_discount_amt#14,ss_ext_sales_price#15,ss_ext_wholesale_cost#16,ss_ext_list_price#17,ss_ext_tax#18,ss_coupon_amt#19,ss_net_paid#20,ss_net_paid_inc_tax#21,ss_net_profit#22] parquet

== Optimized Logical Plan ==
Sort [avg(ss_sales_price)#24 ASC NULLS FIRST], true
+- Aggregate [ss_sold_date_sk#0], [ss_sold_date_sk#0, cast((avg(UnscaledValue(ss_sales_price#13)) / 100.0) as decimal(11,6)) AS avg(ss_sales_price)#24]
   +- Project [ss_sold_date_sk#0, ss_sales_price#13]
      +- Relation ds1g.store_sales[ss_sold_date_sk#0,ss_sold_time_sk#1,ss_item_sk#2,ss_customer_sk#3,ss_cdemo_sk#4,ss_hdemo_sk#5,ss_addr_sk#6,ss_store_sk#7,ss_promo_sk#8,ss_ticket_number#9,ss_quantity#10,ss_wholesale_cost#11,ss_list_price#12,ss_sales_price#13,ss_ext_discount_amt#14,ss_ext_sales_price#15,ss_ext_wholesale_cost#16,ss_ext_list_price#17,ss_ext_tax#18,ss_coupon_amt#19,ss_net_paid#20,ss_net_paid_inc_tax#21,ss_net_profit#22] parquet

== Physical Plan ==
AdaptiveSparkPlan isFinalPlan=true
+- == Final Plan ==
   *(3) Sort [avg(ss_sales_price)#24 ASC NULLS FIRST], true, 0
   +- AQEShuffleRead coalesced
      +- ShuffleQueryStage 1
         +- Exchange rangepartitioning(avg(ss_sales_price)#24 ASC NULLS FIRST, 200), ENSURE_REQUIREMENTS, [id=#64]
            +- *(2) HashAggregate(keys=[ss_sold_date_sk#0], functions=[avg(UnscaledValue(ss_sales_price#13))], output=[ss_sold_date_sk#0, avg(ss_sales_price)#24])
               +- AQEShuffleRead coalesced
                  +- ShuffleQueryStage 0
                     +- Exchange hashpartitioning(ss_sold_date_sk#0, 200), ENSURE_REQUIREMENTS, [id=#36]
                        +- *(1) HashAggregate(keys=[ss_sold_date_sk#0], functions=[partial_avg(UnscaledValue(ss_sales_price#13))], output=[ss_sold_date_sk#0, sum#53, count#54L])
                           +- *(1) ColumnarToRow
                              +- FileScan parquet ds1g.store_sales[ss_sold_date_sk#0,ss_sales_price#13] Batched: true, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/store_sales], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<ss_sold_date_sk:int,ss_sales_price:decimal(7,2)>
+- == Initial Plan ==
   Sort [avg(ss_sales_price)#24 ASC NULLS FIRST], true, 0
   +- Exchange rangepartitioning(avg(ss_sales_price)#24 ASC NULLS FIRST, 200), ENSURE_REQUIREMENTS, [id=#18]
      +- HashAggregate(keys=[ss_sold_date_sk#0], functions=[avg(UnscaledValue(ss_sales_price#13))], output=[ss_sold_date_sk#0, avg(ss_sales_price)#24])
         +- Exchange hashpartitioning(ss_sold_date_sk#0, 200), ENSURE_REQUIREMENTS, [id=#15]
            +- HashAggregate(keys=[ss_sold_date_sk#0], functions=[partial_avg(UnscaledValue(ss_sales_price#13))], output=[ss_sold_date_sk#0, sum#53, count#54L])
               +- FileScan parquet ds1g.store_sales[ss_sold_date_sk#0,ss_sales_price#13] Batched: true, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/store_sales], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<ss_sold_date_sk:int,ss_sales_price:decimal(7,2)>

```



