# SparkSQL:执行全流程



![image-20230413160044857](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230413160044857.png)

一条sql执行的核心流程

![image-20230413160139589](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230413160139589.png)

```
select substring('name',0,2) from p 
```


protected case class Batch*(*name: String, strategy: Strategy, rules: Rule*[*TreeType*]***)*



## 规则执行策略

表示执行最大次数，如果达到fix point，则停止执行。

```scala
  abstract class Strategy {

    /** The maximum number of executions. */
    def maxIterations: Int

    /** Whether to throw exception when exceeding the maximum number. */
    def errorOnExceed: Boolean = false

    /** The key of SQLConf setting to tune maxIterations */
    def maxIterationsSetting: String = null
  }
```





## Analyzer

利用SessionCatalog中的信息把未解析的属性、关系转为一个有类型的对象

### batches

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
      ResolveUpCast ::   把UpCast替换为Cast
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





## Optimizer

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



## SparkPlanner

把逻辑计划转为物理计划，其父类QueryPlanner的plan方法会把各种策略应用到逻辑计划上





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

