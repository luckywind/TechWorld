# SparkSQL:执行全流程



![image-20230413160044857](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230413160044857.png)

一条sql执行的核心流程

![image-20230413160139589](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230413160139589.png)

```
select substring('name',0,2) from p 
```


protected case class Batch*(*name: String, strategy: Strategy, rules: Rule*[*TreeType*]***)*

## catalyst物理计划生成过程

一共四个阶段：

- parsing:   解析获得语法树
- analysis:   产生逻辑计划
- optimization：优化逻辑计划
- planning ：包含了两步

1. QueryExection.createSparkPlan  产生物理计划
2. QueryExection.prepareForExecution 对物理计划插入shuffle算子和行列转换算子、AQE算子、全代码生成算子

## RuleExecutor与规则执行策略

表示执行最大次数，如果达到fix point(收敛)，则停止执行。

```scala
  abstract class Strategy {

    /** 执行最大次数 */
    def maxIterations: Int

    /** 超过最大次数是否抛异常 */
    def errorOnExceed: Boolean = false

    /** The key of SQLConf setting to tune maxIterations */
    def maxIterationsSetting: String = null
  }
```

### FixedPoint

```scala
  case class FixedPoint(
    override val maxIterations: Int,
    override val errorOnExceed: Boolean = false,
    override val maxIterationsSetting: String = null) extends Strategy
```

执行到固定点或者到达最大迭代次数

### Once

```scala
case object Once extends Strategy { val maxIterations = 1 }
```

### execute()

执行子类定义的batch，batch是顺序执行的，batch里的rule也是顺序执行的。

```scala
  def execute(plan: TreeType): TreeType = {
    var curPlan = plan
    val queryExecutionMetrics = RuleExecutor.queryExecutionMeter
    val planChangeLogger = new PlanChangeLogger[TreeType]()
    val tracker: Option[QueryPlanningTracker] = QueryPlanningTracker.get
    val beforeMetrics = RuleExecutor.getCurrentMetrics()

    // 结构检查，确保plan正确，目前啥也没做
    if (!isPlanIntegral(plan, plan)) {
      throw QueryExecutionErrors.structuralIntegrityOfInputPlanIsBrokenInClassError(
        this.getClass.getName.stripSuffix("$"))
    }

    batches.foreach { batch =>
      val batchStartPlan = curPlan
      var iteration = 1
      var lastPlan = curPlan
      var continue = true

      // Run until fix point (or the max number of iterations as specified in the strategy.
      while (continue) {
        curPlan = batch.rules.foldLeft(curPlan) {
          case (plan, rule) =>
            val startTime = System.nanoTime()
            // 应用规则
            val result = rule(plan)
            val runTime = System.nanoTime() - startTime
            val effective = !result.fastEquals(plan)

            if (effective) { //查询计划被替换
              queryExecutionMetrics.incNumEffectiveExecution(rule.ruleName)
              queryExecutionMetrics.incTimeEffectiveExecutionBy(rule.ruleName, runTime)
              planChangeLogger.logRule(rule.ruleName, plan, result)
            }
            queryExecutionMetrics.incExecutionTimeBy(rule.ruleName, runTime)
            queryExecutionMetrics.incNumExecution(rule.ruleName)

            // Record timing information using QueryPlanningTracker
            tracker.foreach(_.recordRuleInvocation(rule.ruleName, runTime, effective))
            result
        }
        iteration += 1
        if (iteration > batch.strategy.maxIterations) {
          // Only log if this is a rule that is supposed to run more than once.
          if (iteration != 2) {
            val endingMsg = if (batch.strategy.maxIterationsSetting == null) {
              "."
            } else {
              s", please set '${batch.strategy.maxIterationsSetting}' to a larger value."
            }
            val message = s"Max iterations (${iteration - 1}) reached for batch ${batch.name}" +
              s"$endingMsg"
            if (Utils.isTesting || batch.strategy.errorOnExceed) {
              throw new RuntimeException(message)
            } else {
              logWarning(message)
            }
          }
          // Check idempotence for Once batches.
          if (batch.strategy == Once &&
            Utils.isTesting && !excludedOnceBatches.contains(batch.name)) {
            checkBatchIdempotence(batch, curPlan)
          }
          continue = false
        }

        if (curPlan.fastEquals(lastPlan)) {
          logTrace(
            s"Fixed point reached for batch ${batch.name} after ${iteration - 1} iterations.")
          continue = false
        }
        lastPlan = curPlan
      }

      planChangeLogger.logBatch(batch.name, batchStartPlan, curPlan)
    }
    planChangeLogger.logMetrics(RuleExecutor.getCurrentMetrics() - beforeMetrics)

    curPlan
  }
```





## Analyzer extends RuleExecutor

利用SessionCatalog中的信息把未解析的属性、关系转为一个有类型的对象

### batches

fixedPoint是默认执行100次的策略

```scala
    Batch("Substitution", fixedPoint,
      OptimizeUpdateFields,
      CTESubstitution,
      WindowsSubstitution,
      EliminateUnions,
      SubstituteUnresolvedOrdinals),
    Batch("Disable Hints", Once,
      new ResolveHints.DisableHints),
    Batch("Hints", fixedPoint,
      ResolveHints.ResolveJoinStrategyHints,
      ResolveHints.ResolveCoalesceHints),
    Batch("Simple Sanity Check", Once,
      LookupFunctions),
    Batch("Keep Legacy Outputs", Once,
      KeepLegacyOutputs),
    Batch("Resolution", fixedPoint,
      ResolveTableValuedFunctions(v1SessionCatalog) ::
      ResolveNamespace(catalogManager) ::
      new ResolveCatalogs(catalogManager) ::
      ResolveUserSpecifiedColumns ::
      ResolveInsertInto ::
      ResolveRelations ::
      ResolvePartitionSpec ::
      ResolveFieldNameAndPosition ::
      AddMetadataColumns ::
      DeduplicateRelations ::
      ResolveReferences ::
      ResolveExpressionsWithNamePlaceholders ::
      ResolveDeserializer ::
      ResolveNewInstance ::
      ResolveUpCast ::   //把UpCast替换为Cast
      ResolveGroupingAnalytics ::
      ResolvePivot ::
      ResolveOrdinalInOrderByAndGroupBy ::
      ResolveAggAliasInGroupBy ::
      ResolveMissingReferences ::
      ExtractGenerator ::
      ResolveGenerate ::
      ResolveFunctions ::
      ResolveAliases ::
      ResolveSubquery ::
      ResolveSubqueryColumnAliases ::
      ResolveWindowOrder ::
      ResolveWindowFrame ::
      ResolveNaturalAndUsingJoin ::
      ResolveOutputRelation ::
      ExtractWindowExpressions ::
      GlobalAggregates ::
      ResolveAggregateFunctions ::
      TimeWindowing ::
      SessionWindowing ::
      ResolveDefaultColumns(this, v1SessionCatalog) ::
      ResolveInlineTables ::
      ResolveLambdaVariables ::
      ResolveTimeZone ::
      ResolveRandomSeed ::
      ResolveBinaryArithmetic ::
      ResolveUnion ::
      RewriteDeleteFromTable ::
      //用于类型强转的规则集合，例如参与运算的两个不同类型转到最小公共类型
      typeCoercionRules ++ 
      
      Seq(ResolveWithCTE) ++
      extendedResolutionRules : _*),
    Batch("Remove TempResolvedColumn", Once, RemoveTempResolvedColumn),
    Batch("Apply Char Padding", Once,
      ApplyCharTypePadding),
    Batch("Post-Hoc Resolution", Once,
      Seq(ResolveCommandsWithIfExists) ++
      postHocResolutionRules: _*),
    Batch("Remove Unresolved Hints", Once,
      new ResolveHints.RemoveAllHints),
    Batch("Nondeterministic", Once,
      PullOutNondeterministic),
    Batch("UDF", Once,
      HandleNullInputsForUDF,
      ResolveEncodersInUDF),
    Batch("UpdateNullability", Once,
      UpdateAttributeNullability),
    Batch("Subquery", Once,
      UpdateOuterReferences),
    Batch("Cleanup", fixedPoint,
      CleanupAliases),
    Batch("HandleAnalysisOnlyCommand", Once,
      HandleAnalysisOnlyCommand)
```

#### 实例演示

我们以下面的语句为例说明：

```
    spark.sql("select cast(w_warehouse_sk as long) from warehouse")
.collect()
```

解析前的逻辑计划

```
'Project [unresolvedalias(cast('w_warehouse_sk as bigint), None)]
+- 'UnresolvedRelation [warehouse], [], false
```

我们看cast如何解析
经过一系列规则作用后，逻辑计划变成了

```
'Project [unresolvedalias(cast(w_warehouse_sk#0 as bigint), None)]
+- SubqueryAlias warehouse
   +- View (`warehouse`, [w_warehouse_sk#0,w_warehouse_id#1,w_warehouse_name#2,w_warehouse_sq_ft#3,w_street_number#4,w_street_name#5,w_street_type#6,w_suite_number#7,w_city#8,w_county#9,w_state#10,w_zip#11,w_country#12,w_gmt_offset#13])
      +- Relation [w_warehouse_sk#0,w_warehouse_id#1,w_warehouse_name#2,w_warehouse_sq_ft#3,w_street_number#4,w_street_name#5,w_street_type#6,w_suite_number#7,w_city#8,w_county#9,w_state#10,w_zip#11,w_country#12,w_gmt_offset#13] parquet
      
```

此时，UnresolvedRelation已经被解析

然后ResolveAliases规则首先解析unresolvedalias，把UnresolvedAlias用具体的别名替换，如果是Project算子，且其projectList有未解析的别名，则先解析其projectList中的别名

```scala
//子节点已完成解析，且projectList有未解析的project项
case Project(projectList, child) if child.resolved && hasUnresolvedAlias(projectList) =>
  Project(assignAliases(projectList), child)
  
```

即def assignAliases(exprs: Seq[NamedExpression])方法:

```scala
exprs.map(_.transformUpWithPruning(_.containsPattern(UNRESOLVED_ALIAS)) {
             //如果是未解析的别名，则递归解析其child
          case u @ UnresolvedAlias(child, optGenAliasFunc) =>
          child match {
            case ne: NamedExpression => ne
            case go @ GeneratorOuter(g: Generator) if g.resolved => MultiAlias(go, Nil)
            // 这里会懒执行子节点的resolved
            case e if !e.resolved => u
            case g: Generator => MultiAlias(g, Nil)
            case c @ Cast(ne: NamedExpression, _, _, _) => Alias(c, ne.name)()
            case e: ExtractValue =>
              if (extractOnly(e)) {
                Alias(e, toPrettySQL(e))()
              } else {
                Alias(e, toPrettySQL(e))(explicitMetadata = Some(metaForAutoGeneratedAlias))
              }
            case e if optGenAliasFunc.isDefined =>
              Alias(child, optGenAliasFunc.get.apply(e))()
            case l: Literal => Alias(l, toPrettySQL(l))()
            case e =>
              Alias(e, toPrettySQL(e))(explicitMetadata = Some(metaForAutoGeneratedAlias))
          }
        }
      ).asInstanceOf[Seq[NamedExpression]] //至此，已经全部完成别名解析
```

从最后这个强转看出，解析后，projectList就全是NamedExpression了  。
注意这里会触发子节点的resolved的懒执行，例如本例子中的cast,其resolved逻辑就是子节点解析了且输入类型校验通过

```scala
override lazy val resolved: Boolean =
  childrenResolved && checkInputDataTypes().isSuccess && (!needsTimeZone || timeZoneId.isDefined)
  //checkInputDataTypes则进行输入/输出类型检查,
```

 至此，解析计划变成了：

```
Project [cast(w_warehouse_sk#0 as bigint) AS w_warehouse_sk#29L]
+- SubqueryAlias warehouse
   +- View (`warehouse`, [w_warehouse_sk#0,w_warehouse_id#1,w_warehouse_name#2,w_warehouse_sq_ft#3,w_street_number#4,w_street_name#5,w_street_type#6,w_suite_number#7,w_city#8,w_county#9,w_state#10,w_zip#11,w_country#12,w_gmt_offset#13])
      +- Relation [w_warehouse_sk#0,w_warehouse_id#1,w_warehouse_name#2,w_warehouse_sq_ft#3,w_street_number#4,w_street_name#5,w_street_type#6,w_suite_number#7,w_city#8,w_county#9,w_state#10,w_zip#11,w_country#12,w_gmt_offset#13] parquet
      
```

此时，unresolvedalias已经被解析。



再经过了一系列的规则解析和Optimizer的优化，最终的物理计划

```
*(1) Project [cast(w_warehouse_sk#0 as bigint) AS w_warehouse_sk#28L]
+- *(1) ColumnarToRow
   +- FileScan parquet [w_warehouse_sk#0] Batched: true, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[file:/warehouse/chengxingfu/code/open/spark/sourceCode/spark/examples/src/..., PartitionFilters: [], PushedFilters: [], ReadSchema: struct<w_warehouse_sk:int>
   
```

可见，最终插入了行列转换算子。



整个执行计划的演变过程如下：

```
== Parsed Logical Plan ==
'Project [unresolvedalias(cast('w_warehouse_sk as bigint), None)]
+- 'UnresolvedRelation [warehouse], [], false

== Analyzed Logical Plan ==
w_warehouse_sk: bigint
Project [cast(w_warehouse_sk#0 as bigint) AS w_warehouse_sk#29L]
+- SubqueryAlias warehouse
   +- View (`warehouse`, [w_warehouse_sk#0,w_warehouse_id#1,w_warehouse_name#2,w_warehouse_sq_ft#3,w_street_number#4,w_street_name#5,w_street_type#6,w_suite_number#7,w_city#8,w_county#9,w_state#10,w_zip#11,w_country#12,w_gmt_offset#13])
      +- Relation [w_warehouse_sk#0,w_warehouse_id#1,w_warehouse_name#2,w_warehouse_sq_ft#3,w_street_number#4,w_street_name#5,w_street_type#6,w_suite_number#7,w_city#8,w_county#9,w_state#10,w_zip#11,w_country#12,w_gmt_offset#13] parquet

== Optimized Logical Plan ==
Project [cast(w_warehouse_sk#0 as bigint) AS w_warehouse_sk#28L]
+- Relation [w_warehouse_sk#0,w_warehouse_id#1,w_warehouse_name#2,w_warehouse_sq_ft#3,w_street_number#4,w_street_name#5,w_street_type#6,w_suite_number#7,w_city#8,w_county#9,w_state#10,w_zip#11,w_country#12,w_gmt_offset#13] parquet

== Physical Plan ==
*(1) Project [cast(w_warehouse_sk#0 as bigint) AS w_warehouse_sk#28L]
+- *(1) ColumnarToRow
   +- FileScan parquet [w_warehouse_sk#0] Batched: true, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[file:/warehouse/chengxingfu/..., PartitionFilters: [], PushedFilters: [], ReadSchema: struct<w_warehouse_sk:int>
   
```





## Optimizer extends RuleExecutor

使用已有的规则对逻辑执行计划进行优化，该过程是基于经验/启发式的优化方法，得到优化过的逻辑执行计划。

![image-20230413160357356](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230413160357356.png)

### defaultBatches:

```scala
val batches = (
    Batch("Finish Analysis", Once, FinishAnalysis) ::
    //////////////////////////////////////////////////////////////////////////////////////////
    // Optimizer rules start here
    //////////////////////////////////////////////////////////////////////////////////////////
    Batch("Eliminate Distinct", Once, EliminateDistinct) ::
    // - Do the first call of CombineUnions before starting the major Optimizer rules,
    //   since it can reduce the number of iteration and the other rules could add/move
    //   extra operators between two adjacent Union operators.
    // - Call CombineUnions again in Batch("Operator Optimizations"),
    //   since the other rules might make two separate Unions operators adjacent.
    Batch("Inline CTE", Once,
      InlineCTE()) ::
    Batch("Union", Once,
      RemoveNoopOperators,
      CombineUnions,
      RemoveNoopUnion) ::
    Batch("OptimizeLimitZero", Once,
      OptimizeLimitZero) ::
    // Run this once earlier. This might simplify the plan and reduce cost of optimizer.
    // For example, a query such as Filter(LocalRelation) would go through all the heavy
    // optimizer rules that are triggered when there is a filter
    // (e.g. InferFiltersFromConstraints). If we run this batch earlier, the query becomes just
    // LocalRelation and does not trigger many rules.
    Batch("LocalRelation early", fixedPoint,
      ConvertToLocalRelation,
      PropagateEmptyRelation,
      // PropagateEmptyRelation can change the nullability of an attribute from nullable to
      // non-nullable when an empty relation child of a Union is removed
      UpdateAttributeNullability) ::
    Batch("Pullup Correlated Expressions", Once,
      OptimizeOneRowRelationSubquery,
      PullupCorrelatedPredicates) ::
    // Subquery batch applies the optimizer rules recursively. Therefore, it makes no sense
    // to enforce idempotence on it and we change this batch from Once to FixedPoint(1).
    Batch("Subquery", FixedPoint(1),
      OptimizeSubqueries) ::
    Batch("Replace Operators", fixedPoint,
      RewriteExceptAll,
      RewriteIntersectAll,
      ReplaceIntersectWithSemiJoin,
      ReplaceExceptWithFilter,
      ReplaceExceptWithAntiJoin,
      ReplaceDistinctWithAggregate,
      ReplaceDeduplicateWithAggregate) ::
    Batch("Aggregate", fixedPoint,
      RemoveLiteralFromGroupExpressions,
      RemoveRepetitionFromGroupExpressions) :: Nil ++
    operatorOptimizationBatch) :+
    Batch("Clean Up Temporary CTE Info", Once, CleanUpTempCTEInfo) :+
    // This batch rewrites plans after the operator optimization and
    // before any batches that depend on stats.
    Batch("Pre CBO Rules", Once, preCBORules: _*) :+
    // This batch pushes filters and projections into scan nodes. Before this batch, the logical
    // plan may contain nodes that do not report stats. Anything that uses stats must run after
    // this batch.
    Batch("Early Filter and Projection Push-Down", Once, earlyScanPushDownRules: _*) :+
    Batch("Update CTE Relation Stats", Once, UpdateCTERelationStats) :+
    // Since join costs in AQP can change between multiple runs, there is no reason that we have an
    // idempotence enforcement on this batch. We thus make it FixedPoint(1) instead of Once.
    Batch("Join Reorder", FixedPoint(1),
      CostBasedJoinReorder) :+
    Batch("Eliminate Sorts", Once,
      EliminateSorts) :+
    Batch("Decimal Optimizations", fixedPoint,
      DecimalAggregates) :+
    // This batch must run after "Decimal Optimizations", as that one may change the
    // aggregate distinct column
    Batch("Distinct Aggregate Rewrite", Once,
      RewriteDistinctAggregates) :+
    Batch("Object Expressions Optimization", fixedPoint,
      EliminateMapObjects,
      CombineTypedFilters,
      ObjectSerializerPruning,
      ReassignLambdaVariableID) :+
    Batch("LocalRelation", fixedPoint,
      ConvertToLocalRelation,
      PropagateEmptyRelation,
      // PropagateEmptyRelation can change the nullability of an attribute from nullable to
      // non-nullable when an empty relation child of a Union is removed
      UpdateAttributeNullability) :+
    Batch("Optimize One Row Plan", fixedPoint, OptimizeOneRowPlan) :+
    // The following batch should be executed after batch "Join Reorder" and "LocalRelation".
    Batch("Check Cartesian Products", Once,
      CheckCartesianProducts) :+
    Batch("RewriteSubquery", Once,
      RewritePredicateSubquery,
      ColumnPruning,
      CollapseProject,
      RemoveRedundantAliases,
      RemoveNoopOperators) :+
    // This batch must be executed after the `RewriteSubquery` batch, which creates joins.
    Batch("NormalizeFloatingNumbers", Once, NormalizeFloatingNumbers) :+
    Batch("ReplaceUpdateFieldsExpression", Once, ReplaceUpdateFieldsExpression)
```





### operatorOptimizationRuleSet:

```scala
        PushProjectionThroughUnion,
        ReorderJoin,
        EliminateOuterJoin,
        PushDownPredicates,
        PushDownLeftSemiAntiJoin,
        PushLeftSemiLeftAntiThroughJoin,
        LimitPushDown,//Pushes down [[LocalLimit]] beneath UNION ALL and joins.
        LimitPushDownThroughWindow,
        ColumnPruning,
        GenerateOptimization,
        // Operator combine
        CollapseRepartition,
        CollapseProject,
        OptimizeWindowFunctions,
        CollapseWindow,
        CombineFilters,
        EliminateLimits,
        RewriteOffsets,
        CombineUnions,
        // Constant folding and strength reduction
        OptimizeRepartition,
        TransposeWindow,
        NullPropagation,
        NullDownPropagation,
        ConstantPropagation,
        FoldablePropagation,
        OptimizeIn,
        ConstantFolding,
        EliminateAggregateFilter,
        ReorderAssociativeOperator,
        LikeSimplification,
        BooleanSimplification,
        SimplifyConditionals,
        PushFoldableIntoBranches,
        RemoveDispensableExpressions,
        SimplifyBinaryComparison,
        ReplaceNullWithFalseInPredicate,
        SimplifyConditionalsInPredicate,
        PruneFilters,
        SimplifyCasts,
        SimplifyCaseConversionExpressions,
        RewriteCorrelatedScalarSubquery,
        RewriteLateralSubquery,
        EliminateSerialization,
        RemoveRedundantAliases,
        RemoveRedundantAggregates,
        UnwrapCastInBinaryComparison,
        RemoveNoopOperators,
        OptimizeUpdateFields,
        SimplifyExtractValueOps,
        OptimizeCsvJsonExprs,
        CombineConcats,
        PushdownPredicatesAndPruneColumnsForCTEDef)
```



### bathes

```scala
  final override def batches: Seq[Batch] = {
    val excludedRulesConf =
      SQLConf.get.optimizerExcludedRules.toSeq.flatMap(Utils.stringToSeq)
    //计算要排除的规则
    val excludedRules = excludedRulesConf.filter { ruleName =>
      val nonExcludable = nonExcludableRules.contains(ruleName)
      if (nonExcludable) {
        logWarning(s"Optimization rule '${ruleName}' was not excluded from the optimizer " +
          s"because this rule is a non-excludable rule.")
      }
      !nonExcludable
    }
    if (excludedRules.isEmpty) {
      defaultBatches
    } else {
      defaultBatches.flatMap { batch =>
        //当前batch排除后的规则
        val filteredRules = batch.rules.filter { rule =>
          val exclude = excludedRules.contains(rule.ruleName)
          if (exclude) {
            logInfo(s"Optimization rule '${rule.ruleName}' is excluded from the optimizer.")
          }
          !exclude
        }
        //如果当前batch没排除任何规则，那么直接返回当前batch
        if (batch.rules == filteredRules) {
          Some(batch)
        } else if (filteredRules.nonEmpty) {
          //否则，如果过滤后还存在规则，那么重新构造当前batch
          Some(Batch(batch.name, batch.strategy, filteredRules: _*))
        } else {
          //过滤后，当前batch已经没有规则了
          logInfo(s"Optimization batch '${batch.name}' is excluded from the optimizer " +
            s"as all enclosed rules have been excluded.")
          None
        }
      }
    }
  }
```



## SparkPlanner extends SparkStrategies extends QueryPlanner

把逻辑计划转为物理计划，其父类QueryPlanner的plan方法会把各种策略应用到逻辑计划上

参考: https://issues.apache.org/jira/browse/SPARK-1251
SparkPlanner将逻辑执行计划转换成物理执行计划，即将逻辑执行计划树中的逻辑节点转换成物理节点，如Join转换成HashJoinExec/SortMergeJoinExec...，Filter转成FilterExec等

![666](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/9a5924e9df08733fdc1ec8896eb2736df65f3e16.jpeg)

### 物理计划Strategy

Spark的`Stragety`有8个:

- DataSourceV2Strategy
- FileSourceStrategy
- DataSourceStrategy
- SpecialLimits
- Aggregation
- JoinSelection
- InMemoryScans
- BasicOperators

上述很多Stragety都是基于规则的策略。
JoinSelection用到了相关的统计信息来选择将Join转换为BroadcastHashJoinExec还是ShuffledHashJoinExec还是SortMergeJoinExec，属于CBO基于代价的策略。

在实现上，各种 Strategy会匹配传入的 LogicalPlan节点，根据节点或节点组合的不同情形 实行一对一 的映射或多对一 的映射 。一对一 的映射方式比较直观，以 BasicOperators 为例，该 Strategy实现了各种基本操作的转换， 其中列出了大量的映射关系，包括 Sort对应 SortExec, Union 对应 UnionExec等 。 多对一 的情况涉及对多个 LogicalPlan 节点进行组合转换，这里称为<font color=red>**逻辑算子树的模式匹配** </font>。 目前在 SparkSQL 中，逻辑算子树的节点模式共有 4种

- ExtractEquiJoinKeys:针对具有相等条件的 Join操作的算子集合，提取出其中的 Join条件、左子节点和右子节点等信息 。
- ExtractFiltersAndinnerJoins: 收集 Inner类型 Join操作中的过滤条件，目前仅支持对左子树进行处理 。
- PhysicalAggregation:针对聚合操作，提取出聚合算子中的各个部分，并对一些表达式进行初步的转换 。
- PhysicalOperation:匹配逻辑算子树中 的 Project和 Filter等节点，返回投影列、过滤条件集合和子节点 。

以上策略如何使用逻辑算子树的模式匹配呢？ 

#### FileSourceStrategy

该策略针对的典型模式是: 能够匹配 PhysicalOperation 的节点集合加上 LogicalRelation 节点 。 在这种情况下，该策略会根 据数据文件信息构建 FileSourceScanExec这样的物理执行计划，并在此物理执行计划后添加过 滤( FilterExec)与列剪裁( ProjectExec)物理计划 。

*Note:* 需要注意的是，即使在逻辑算子树 上 LogicalRelation 节点往上 存在多 个过滤算子与投影 算子，经过 PhysicalOperation 模式匹配，也会整合成为一个。

```scala
  def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
    case ScanOperation(projects, filters,
      l @ LogicalRelation(fsRelation: HadoopFsRelation, _, table, _)) =>
      // Filters 分为四类
      //  - partition keys only - 裁剪读取目录
      //  - bucket keys only - optionally 裁剪文件
      //  - keys stored in the data only - optionally used to skip groups of data in files
      //  - filters that need to be evaluated again after the scan
      val filterSet = ExpressionSet(filters)

      val normalizedFilters = DataSourceStrategy.normalizeExprs(
        filters.filter(_.deterministic), l.output)

      val partitionColumns =
        l.resolve(
          fsRelation.partitionSchema, fsRelation.sparkSession.sessionState.analyzer.resolver)
      val partitionSet = AttributeSet(partitionColumns)

      // this partitionKeyFilters should be the same with the ones being executed in
      // PruneFileSourcePartitions
      val partitionKeyFilters = DataSourceStrategy.getPushedDownFilters(partitionColumns,
        normalizedFilters)

      // subquery expressions are filtered out because they can't be used to prune buckets or pushed
      // down as data filters, yet they would be executed
      val normalizedFiltersWithoutSubqueries =
        normalizedFilters.filterNot(SubqueryExpression.hasSubquery)

      val bucketSpec: Option[BucketSpec] = fsRelation.bucketSpec
      val bucketSet = if (shouldPruneBuckets(bucketSpec)) {
        genBucketSet(normalizedFiltersWithoutSubqueries, bucketSpec.get)
      } else {
        None
      }

      val dataColumns =
        l.resolve(fsRelation.dataSchema, fsRelation.sparkSession.sessionState.analyzer.resolver)

      // Partition keys are not available in the statistics of the files.
      // `dataColumns` might have partition columns, we need to filter them out.
      val dataColumnsWithoutPartitionCols = dataColumns.filterNot(partitionColumns.contains)
      val dataFilters = normalizedFiltersWithoutSubqueries.flatMap { f =>
        if (f.references.intersect(partitionSet).nonEmpty) {
          extractPredicatesWithinOutputSet(f, AttributeSet(dataColumnsWithoutPartitionCols))
        } else {
          Some(f)
        }
      }
      val supportNestedPredicatePushdown =
        DataSourceUtils.supportNestedPredicatePushdown(fsRelation)
      val pushedFilters = dataFilters
        .flatMap(DataSourceStrategy.translateFilter(_, supportNestedPredicatePushdown))
      logInfo(s"Pushed Filters: ${pushedFilters.mkString(",")}")

      // Predicates with both partition keys and attributes need to be evaluated after the scan.
      val afterScanFilters = filterSet -- partitionKeyFilters.filter(_.references.nonEmpty)
      logInfo(s"Post-Scan Filters: ${afterScanFilters.mkString(",")}")

      val filterAttributes = AttributeSet(afterScanFilters)
      val requiredExpressions: Seq[NamedExpression] = filterAttributes.toSeq ++ projects
      val requiredAttributes = AttributeSet(requiredExpressions)

      val readDataColumns =
        dataColumns
          .filter(requiredAttributes.contains)
          .filterNot(partitionColumns.contains)
      val outputSchema = readDataColumns.toStructType
      logInfo(s"Output Data Schema: ${outputSchema.simpleString(5)}")

      val metadataStructOpt = l.output.collectFirst {
        case FileSourceMetadataAttribute(attr) => attr
      }

      val metadataColumns = metadataStructOpt.map { metadataStruct =>
        metadataStruct.dataType.asInstanceOf[StructType].fields.map { field =>
          FileSourceMetadataAttribute(field.name, field.dataType)
        }.toSeq
      }.getOrElse(Seq.empty)

      // outputAttributes should also include the metadata columns at the very end
      val outputAttributes = readDataColumns ++ partitionColumns ++ metadataColumns

   //    构造FileSourceScanExec算子
      val scan =
        FileSourceScanExec(
          fsRelation,
          outputAttributes,
          outputSchema,
          partitionKeyFilters.toSeq,
          bucketSet,
          None,
          dataFilters,
          table.map(_.identifier))

      // extra Project node: wrap flat metadata columns to a metadata struct
      val withMetadataProjections = metadataStructOpt.map { metadataStruct =>
        val metadataAlias =
          Alias(CreateStruct(metadataColumns), METADATA_NAME)(exprId = metadataStruct.exprId)
        execution.ProjectExec(
          scan.output.dropRight(metadataColumns.length) :+ metadataAlias, scan)
      }.getOrElse(scan)

      val afterScanFilter = afterScanFilters.toSeq.reduceOption(expressions.And)
      val withFilter = afterScanFilter
        .map(execution.FilterExec(_, withMetadataProjections))
        .getOrElse(withMetadataProjections)
    //最终的Project如果和Filter的输出一样，则省略Project
      val withProjections = if (projects == withFilter.output) {
        withFilter
      } else {
        execution.ProjectExec(projects, withFilter)
      }

      withProjections :: Nil

    case _ => Nil
  }
```

#### JoinSelection

##### 各种类型Join的特点和限制

- Broadcast hash join (BHJ):
    Only supported for equi-joins, while the join keys do not need to be sortable.
    Supported for all join types except full outer joins.
    BHJ usually performs faster than the other join algorithms when the broadcast side is
    small. However, broadcasting tables is a network-intensive operation and it could cause
    OOM or perform badly in some cases, especially when the build/broadcast side is big.
- Shuffle hash join:
    Only supported for equi-joins, while the join keys do not need to be sortable.
    Supported for all join types.
    Building hash map from table is a memory-intensive operation and it could cause OOM
    when the build side is big.
- Shuffle sort merge join (SMJ):
    Only supported for equi-joins and the join keys have to be sortable.
    Supported for all join types.
- Broadcast nested loop join (BNLJ):
    Supports both equi-joins and non-equi-joins.
    Supports all the join types, but the implementation is optimized for:
      1) broadcasting the left side in a right outer join;
      2) broadcasting the right side in a left outer, left semi, left anti or existence join;
      3) broadcasting either side in an inner-like join.
    For other cases, we need to scan the data multiple times, which can be rather slow.
- Shuffle-and-replicate nested loop join (a.k.a. cartesian product join):
    Supports both equi-joins and non-equi-joins.
    Supports only inner like joins.



If it is an equi-join, we first look at the join hints w.r.t. the following order:
  1. broadcast hint: pick broadcast hash join if the join type is supported. If both sides
     have the broadcast hints, choose the smaller side (based on stats) to broadcast.
  2. sort merge hint: pick sort merge join if join keys are sortable.
  3. shuffle hash hint: We pick shuffle hash join if the join type is supported. If both
     sides have the shuffle hash hints, choose the smaller side (based on stats) as the
     build side.
  4. shuffle replicate NL hint: pick cartesian product if join type is inner like.

If there is no hint or the hints are not applicable, we follow these rules one by one:
  1. Pick broadcast hash join if one side is small enough to broadcast, and the join type
     is supported. If both sides are small, choose the smaller side (based on stats)
     to broadcast.
  2. Pick shuffle hash join if one side is small enough to build local hash map, and is
     much smaller than the other side, and `spark.sql.join.preferSortMergeJoin` is false.
  3. Pick sort merge join if the join keys are sortable.
  4. Pick cartesian product if join type is inner like.
  5. Pick broadcast nested loop join as the final solution. It may OOM but we don't have
     other choice.



```scala
        if (hint.isEmpty) {
          createJoinWithoutHint()
        } else {
          createBroadcastHashJoin(true)
            .orElse { if (hintToSortMergeJoin(hint)) createSortMergeJoin() else None }
            .orElse(createShuffleHashJoin(true))
            .orElse { if (hintToShuffleReplicateNL(hint)) createCartesianProduct() else None }
            .getOrElse(createJoinWithoutHint())
        }


        def createJoinWithoutHint() = {
          createBroadcastHashJoin(false)
            .orElse(createShuffleHashJoin(false))
            .orElse(createSortMergeJoin())
            .orElse(createCartesianProduct())
            .getOrElse {
              // This join could be very slow or OOM
              val buildSide = getSmallerSide(left, right)
              Seq(joins.BroadcastNestedLoopJoinExec(
                planLater(left), planLater(right), buildSide, joinType, j.condition))
            }
        }

        def createJoinWithoutHint() = {
          createBroadcastHashJoin(false)
            .orElse(createShuffleHashJoin(false))
            .orElse(createSortMergeJoin())
            .orElse(createCartesianProduct())
            .getOrElse {
              // This join could be very slow or OOM
              val buildSide = getSmallerSide(left, right)
              Seq(joins.BroadcastNestedLoopJoinExec(
                planLater(left), planLater(right), buildSide, joinType, j.condition))
            }
        }
```

这些createJoinXXX方法都是返回一个Option，创建广播表时构建buildSide时，如果两个表都不能广播则返回一个None。



##### canBroadcastBySize

根据表的sizeInBytes决定是否广播表

```scala
  def canBroadcastBySize(plan: LogicalPlan, conf: SQLConf): Boolean = {
    val autoBroadcastJoinThreshold = if (plan.stats.isRuntime) {
      conf.getConf(SQLConf.ADAPTIVE_AUTO_BROADCASTJOIN_THRESHOLD)
        .getOrElse(conf.autoBroadcastJoinThreshold)
    } else {
      conf.autoBroadcastJoinThreshold
    }
    plan.stats.sizeInBytes >= 0 && plan.stats.sizeInBytes <= autoBroadcastJoinThreshold
  }

```



### createSparkPlan

这里会把逻辑计划转为物理计划，需要用到strategies

```scala
  def createSparkPlan(
      sparkSession: SparkSession,
      planner: SparkPlanner,
      plan: LogicalPlan): SparkPlan = {
    // 先插入一个ReturnAnswer节点，用于识别树根
    // 这个next() 就是取第一个plan， 将来会选择最优的
    planner.plan(ReturnAnswer(plan)).next()
  }


  override def plan(plan: LogicalPlan): Iterator[SparkPlan] = {
    super.plan(plan).map { p =>
      val logicalPlan = plan match {
        //如果是ReturnAnswer节点，说明找到了根节点，则丢掉ReturnAnswer并返回树根，作为logicalPlan
        case ReturnAnswer(rootPlan) => rootPlan
        //非ReturnAnswer则不处理
        case _ => plan
      }
      //所有的节点都要链接到同一个logicalPlan
      p.setLogicalLink(logicalPlan)
      p
    }
  }
```



### plan

QueryPlanner.plan

生成物理 计 划的实现代码如下， plan()方法传入 LogicalPlan 作为参数 ， 将 strategies 应用 到 LogicalPlan，生成物理计划候选集合( Candidates) 。 如果该集合中存在 PlanLater 类型的 SparkPlan， 则 通过 placeholder 中间变 量取 出对应的 LogicalPlan 后，递归调用 plan()方法，将 PlanLater 替换为子节点的物理计划 。 最后，对物理计划列表进行过滤，去掉一些不够高效的物 理计划 。

![image-20230714114512998](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230714114512998.png)

```scala
  def plan(plan: LogicalPlan): Iterator[PhysicalPlan] = {
    // Obviously a lot to do here still...

    // Collect physical plan candidates.
    val candidates = strategies.iterator.flatMap(_(plan))

    // The candidates may contain placeholders marked as [[planLater]],
    // so try to replace them by their child plans.
    val plans = candidates.flatMap { candidate =>
      val placeholders = collectPlaceholders(candidate)

      if (placeholders.isEmpty) {
        //不包含placeholders，则可以直接返回
        Iterator(candidate)
      } else {
        // Plan the logical plan marked as [[planLater]] and replace the placeholders.
        placeholders.iterator.foldLeft(Iterator(candidate)) {
          case (candidatesWithPlaceholders, (placeholder, logicalPlan)) =>
            // Plan the logical plan for the placeholder.
            // placeholders对应的逻辑计划转为物理计划，这里递归调用
            val childPlans = this.plan(logicalPlan)

            candidatesWithPlaceholders.flatMap { candidateWithPlaceholders =>
              childPlans.map { childPlan =>
                // Replace the placeholder by the child plan
                candidateWithPlaceholders.transformUp {
                  case p if p.eq(placeholder) => childPlan
                }
              }
            }
        }
      }
    }

    val pruned = prunePlans(plans)
    assert(pruned.hasNext, s"No plan for $plan")
    pruned
  }

```



## QueryExecution.PrepareForExecution

在执行之前，对物理执行计划做一些处理，这些处理都是基于规则的，包括

- PlanSubqueries
- EnsureRequirements
- CollapseCodegenStages
- ReuseExchange
- ReuseSubquery

经过上述步骤之后生成的最终物理执行计划提交到Spark执行。

## collect()

![image-20230413160549843](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230413160549843.png)

## doExecute

![image-20230413160445528](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230413160445528.png)

1. produce: 生成处理inputRDD的Java源代码
   每个算子都是CodegenSupport，所以都有produce接口
   调用算子的doProduce方法生成Java源代码，通常生成框架，produce是一个递归调用

2. consume:消费当前算子生成的列或者行
   ColumnarToRowExec有些特殊，它在produce生成代码时会调用算子的consume，
   来触发parent.doConsume()它同样也是递归调用

具体看下ProjectExec的doConsume实现

```scala
  override def doConsume(ctx: CodegenContext, input: Seq[ExprCode], row: ExprCode): String = {
    val exprs = bindReferences[Expression](projectList, child.output)
    val (subExprsCode, resultVars, localValInputs) = if (conf.subexpressionEliminationEnabled) {
      // subexpression 消除
      val subExprs = ctx.subexpressionEliminationForWholeStageCodegen(exprs)
      val genVars = ctx.withSubExprEliminationExprs(subExprs.states) {
      // 触发表达式genCode, 作为genVars
        exprs.map(_.genCode(ctx))
      }
      (ctx.evaluateSubExprEliminationState(subExprs.states.values), genVars,
        subExprs.exprCodesNeedEvaluate)
    } else {
      ("", exprs.map(_.genCode(ctx)), Seq.empty)
    }

    // Evaluation of non-deterministic expressions can't be deferred.
    val nonDeterministicAttrs = projectList.filterNot(_.deterministic).map(_.toAttribute)
    s"""
       |// common sub-expressions
       |${evaluateVariables(localValInputs)}
       |$subExprsCode
       |${evaluateRequiredVariables(output, resultVars, AttributeSet(nonDeterministicAttrs))}
       |${consume(ctx, resultVars)}
     """.stripMargin
  }
  
```

# 案例

```sql
select  d_date_sk, count(1) cnt  from  ds1g.store_sales join  ds1g.date_dim
on ss_sold_date_sk=d_date_sk
group by d_date_sk
```

计划演变过程

```shell
== Parsed Logical Plan ==
'Aggregate ['d_date_sk], ['d_date_sk, 'count(1) AS cnt#0]
+- 'Join Inner, ('ss_sold_date_sk = 'd_date_sk)
   :- 'UnresolvedRelation [ds1g, store_sales], [], false
   +- 'UnresolvedRelation [ds1g, date_dim], [], false

== Analyzed Logical Plan ==
d_date_sk: int, cnt: bigint
Aggregate [d_date_sk#25], [d_date_sk#25, count(1) AS cnt#0L]
+- Join Inner, (ss_sold_date_sk#2 = d_date_sk#25)
   :- SubqueryAlias spark_catalog.ds1g.store_sales
   :  +- Relation ds1g.store_sales[ss_sold_date_sk#2,ss_sold_time_sk#3,ss_item_sk#4,ss_customer_sk#5,ss_cdemo_sk#6,ss_hdemo_sk#7,ss_addr_sk#8,ss_store_sk#9,ss_promo_sk#10,ss_ticket_number#11,ss_quantity#12,ss_wholesale_cost#13,ss_list_price#14,ss_sales_price#15,ss_ext_discount_amt#16,ss_ext_sales_price#17,ss_ext_wholesale_cost#18,ss_ext_list_price#19,ss_ext_tax#20,ss_coupon_amt#21,ss_net_paid#22,ss_net_paid_inc_tax#23,ss_net_profit#24] parquet
   +- SubqueryAlias spark_catalog.ds1g.date_dim
      +- Relation ds1g.date_dim[d_date_sk#25,d_date_id#26,d_date#27,d_month_seq#28,d_week_seq#29,d_quarter_seq#30,d_year#31,d_dow#32,d_moy#33,d_dom#34,d_qoy#35,d_fy_year#36,d_fy_quarter_seq#37,d_fy_week_seq#38,d_day_name#39,d_quarter_name#40,d_holiday#41,d_weekend#42,d_following_holiday#43,d_first_dom#44,d_last_dom#45,d_same_day_ly#46,d_same_day_lq#47,d_current_day#48,... 4 more fields] parquet

== Optimized Logical Plan ==
Aggregate [d_date_sk#25], [d_date_sk#25, count(1) AS cnt#0L]
+- Project [d_date_sk#25]
   +- Join Inner, (ss_sold_date_sk#2 = d_date_sk#25)
      :- Project [ss_sold_date_sk#2]
      :  +- Filter isnotnull(ss_sold_date_sk#2)
      :     +- Relation ds1g.store_sales[ss_sold_date_sk#2,ss_sold_time_sk#3,ss_item_sk#4,ss_customer_sk#5,ss_cdemo_sk#6,ss_hdemo_sk#7,ss_addr_sk#8,ss_store_sk#9,ss_promo_sk#10,ss_ticket_number#11,ss_quantity#12,ss_wholesale_cost#13,ss_list_price#14,ss_sales_price#15,ss_ext_discount_amt#16,ss_ext_sales_price#17,ss_ext_wholesale_cost#18,ss_ext_list_price#19,ss_ext_tax#20,ss_coupon_amt#21,ss_net_paid#22,ss_net_paid_inc_tax#23,ss_net_profit#24] parquet
      +- Project [d_date_sk#25]
         +- Filter isnotnull(d_date_sk#25)
            +- Relation ds1g.date_dim[d_date_sk#25,d_date_id#26,d_date#27,d_month_seq#28,d_week_seq#29,d_quarter_seq#30,d_year#31,d_dow#32,d_moy#33,d_dom#34,d_qoy#35,d_fy_year#36,d_fy_quarter_seq#37,d_fy_week_seq#38,d_day_name#39,d_quarter_name#40,d_holiday#41,d_weekend#42,d_following_holiday#43,d_first_dom#44,d_last_dom#45,d_same_day_ly#46,d_same_day_lq#47,d_current_day#48,... 4 more fields] parquet

== Physical Plan ==
AdaptiveSparkPlan isFinalPlan=true
+- == Final Plan ==
   *(3) HashAggregate(keys=[d_date_sk#25], functions=[count(1)], output=[d_date_sk#25, cnt#0L])
   +- AQEShuffleRead coalesced
      +- ShuffleQueryStage 1
         +- Exchange hashpartitioning(d_date_sk#25, 200), ENSURE_REQUIREMENTS, [id=#144]
            +- *(2) HashAggregate(keys=[d_date_sk#25], functions=[partial_count(1)], output=[d_date_sk#25, count#107L])
               +- *(2) Project [d_date_sk#25]
                  +- *(2) BroadcastHashJoin [ss_sold_date_sk#2], [d_date_sk#25], Inner, BuildRight, false
                     :- *(2) Filter isnotnull(ss_sold_date_sk#2)
                     :  +- *(2) ColumnarToRow
                     :     +- FileScan parquet ds1g.store_sales[ss_sold_date_sk#2] Batched: true, DataFilters: [isnotnull(ss_sold_date_sk#2)], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/store_sales], PartitionFilters: [], PushedFilters: [IsNotNull(ss_sold_date_sk)], ReadSchema: struct<ss_sold_date_sk:int>
                     +- BroadcastQueryStage 0
                        +- BroadcastExchange HashedRelationBroadcastMode(List(cast(input[0, int, false] as bigint)),false), [id=#86]
                           +- *(1) Filter isnotnull(d_date_sk#25)
                              +- *(1) ColumnarToRow
                                 +- FileScan parquet ds1g.date_dim[d_date_sk#25] Batched: true, DataFilters: [isnotnull(d_date_sk#25)], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/date_dim], PartitionFilters: [], PushedFilters: [IsNotNull(d_date_sk)], ReadSchema: struct<d_date_sk:int>
+- == Initial Plan ==
   HashAggregate(keys=[d_date_sk#25], functions=[count(1)], output=[d_date_sk#25, cnt#0L])
   +- Exchange hashpartitioning(d_date_sk#25, 200), ENSURE_REQUIREMENTS, [id=#62]
      +- HashAggregate(keys=[d_date_sk#25], functions=[partial_count(1)], output=[d_date_sk#25, count#107L])
         +- Project [d_date_sk#25]
            +- BroadcastHashJoin [ss_sold_date_sk#2], [d_date_sk#25], Inner, BuildRight, false
               :- Filter isnotnull(ss_sold_date_sk#2)
               :  +- FileScan parquet ds1g.store_sales[ss_sold_date_sk#2] Batched: true, DataFilters: [isnotnull(ss_sold_date_sk#2)], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/store_sales], PartitionFilters: [], PushedFilters: [IsNotNull(ss_sold_date_sk)], ReadSchema: struct<ss_sold_date_sk:int>
               +- BroadcastExchange HashedRelationBroadcastMode(List(cast(input[0, int, false] as bigint)),false), [id=#57]
                  +- Filter isnotnull(d_date_sk#25)
                     +- FileScan parquet ds1g.date_dim[d_date_sk#25] Batched: true, DataFilters: [isnotnull(d_date_sk#25)], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/date_dim], PartitionFilters: [], PushedFilters: [IsNotNull(d_date_sk)], ReadSchema: struct<d_date_sk:int>

```







