# QueryExecution

## prepareForExecution

<font color=red>对一个优化好的执行计划，插入shuffle算子和行列转换算子，准备执行</font>

主要做了以下事情：

1. 保证子查询planed
2. 数据分区和排序正确
3. 插入全阶段代码生成
4. 重用exchange和子查询

prepareForExecution(
    preparations: Seq[Rule[SparkPlan]],
    plan: SparkPlan): SparkPlan

### preparations规则集

```scala
  protected def preparations: Seq[Rule[SparkPlan]] = {
    QueryExecution.preparations(sparkSession,
    //InsertAdaptiveSparkPlan这个规则插入AdaptiveSparkPlanExec算子                                
      Option(InsertAdaptiveSparkPlan(AdaptiveExecutionContext(sparkSession, this))), false)
  }

  private[execution] def preparations(
      sparkSession: SparkSession,
      adaptiveExecutionRule: Option[InsertAdaptiveSparkPlan] = None,
      subquery: Boolean): Seq[Rule[SparkPlan]] = {
    // `AdaptiveSparkPlanExec` is a leaf node. If inserted, all the following rules will be no-op
    // as the original plan is hidden behind `AdaptiveSparkPlanExec`.
    
    //AdaptiveSparkPlanExec是叶子节点，如果插入，则后续规则将不起作用，因为原始计划是AdaptiveSparkPlanExec的子树 
    adaptiveExecutionRule.toSeq ++
    Seq(
      CoalesceBucketsInJoin,
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

这些规则大多会在InsertAdaptiveSparkPlan规则里重复使用，详细可参考文章SparkSQL:AQE
