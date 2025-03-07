# Optimizer

SparkOptimizer在父类的defaultBatches基础上又加入了自己的batches:

```scala
abstract class Optimizer(catalogManager: CatalogManager)
  extends RuleExecutor[LogicalPlan]
  def defaultBatches: Seq[Batch] 


class SparkOptimizer(
    catalogManager: CatalogManager,
    catalog: SessionCatalog,
    experimentalMethods: ExperimentalMethods)
  extends Optimizer(catalogManager) 
  override def defaultBatches: Seq[Batch]=preOptimizationBatches ++ super.defaultBatches :+
```







# 规则划分

整体上分为标准的优化规则和特殊的优化规则，这是为了实现上的扩展性。

- 标准优化规则
  - 过滤推断前的算子优化**-**operatorOptimizationRuleSet
  - 过滤推断**-**Infer Filters
  - 过滤推断后的算子优化**-**operatorOptimizationRuleSet
  - 下推join的额外谓词**-**Push extra predicate through join
  - 算子下推（Operator push down）**-**Project、Join、Limit、列剪裁
  - 算子合并（Operator combine）**-**Repartition、Project、Window、Filter、Limit、Union
  - 常量折叠和强度消减（Constant folding and strength reduction）**-**Repartition、Window、Null、常量、In、Filter、整数类型、Like、Boolean、if/case、二义性、no-op、struct、取值操作（struct/array/map）、csv/json、Concat
  - analysis 阶段的收尾规则**-**Finish Analysis，比如EliminateSubqueryAliases实际是在Analyzer里定义的
  - 算子优化前**-**Union、Limit、[数据库](https://cloud.tencent.com/solution/database?from_column=20421&from=20421)关系、子查询、算子的替代、聚合算子
  - 算子优化**-**operatorOptimizationBatch
  - 依赖统计数据的优化规则**-**Project、Filter、Join、Sort、Decimal、Aggregate、对象表达式、数据库关系、笛卡尔积、子查询、Float、Struct
- **其他特殊的优化规则-**分区元数据、DPP（动态分区裁剪）、Filter、Python UDF以及用户自定义的优化规则



查看当前有哪些自定义优化规则

```scala
spark.sessionState.optimizer.extendedOperatorOptimizationRules
```

关掉一个优化规则

```scala
spark.conf.set("spark.sql.optimizer.excludedRules", "AverageReplaceRule")
```

列出所有batch

```scala
spark.sessionState.optimizer.batches.map(_.name).foreach(println(_))
```

spark3.2.0自定义规则在第9和11个

```scala
spark.sessionState.optimizer.batches(9).rules.foreach(println(_))
```



# defaultBatches

```scala
    val operatorOptimizationRuleSet =
      Seq(
        // Operator push down
        PushProjectionThroughUnion,
        ....
        PushdownPredicatesAndPruneColumnsForCTEDef) ++
        extendedOperatorOptimizationRules  //扩展规则

    val operatorOptimizationBatch: Seq[Batch] = {
      Batch("Operator Optimization before Inferring Filters", fixedPoint,
        operatorOptimizationRuleSet: _*) :: //扩展规则插入点一
      Batch("Infer Filters", Once,
        InferFiltersFromGenerate,
        InferFiltersFromConstraints) ::
      Batch("Operator Optimization after Inferring Filters", fixedPoint,
        operatorOptimizationRuleSet: _*) :: //扩展规则插入点一
      // Set strategy to Once to avoid pushing filter every time because we do not change the
      // join condition.
      Batch("Push extra predicate through join", fixedPoint,
        PushExtraPredicateThroughJoin,
        PushDownPredicates) :: Nil
    }

defaultBatches=
  ... +
    Batch("Aggregate", fixedPoint,
      RemoveLiteralFromGroupExpressions,
      RemoveRepetitionFromGroupExpressions) :: Nil ++
    operatorOptimizationBatch):+         //加载扩展规则
    Batch, ....

batches=
如果没有排出任何规则，那么会采用defaultBatches
否则，会拿defaultBatches过滤掉排出的规则
```



# batches

```scala
  final override def batches: Seq[Batch] = {
    //获取排除的规则
    val excludedRulesConf =
      SQLConf.get.optimizerExcludedRules.toSeq.flatMap(Utils.stringToSeq)
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
  }
```



# 规则列表

| rule【规则】                                            | batch【表示一组同类的规则】                   | strategy【迭代策略】 | 注释                                                         |
| ------------------------------------------------------- | --------------------------------------------- | -------------------- | ------------------------------------------------------------ |
| EliminateDistinct                                       | Eliminate Distinct                            | Once                 | 删移除关于MAX和MIN的无效DISTINCT。在 RewriteDistinctAggregates 之前，应该先应用此规则。 |
| EliminateResolvedHint                                   | Finish Analysis                               | Once                 | 替换计划中的ResolvedHint算子。将HintInfo移动到关联的Join算子，否则，如果没有匹配的Join算子，就将其删除。HintInfo 是要应用于特定节点的提示属性 |
| EliminateSubqueryAliases                                | Finish Analysis                               | Once                 | 消除子查询别名，对应逻辑算子树中的SubqueryAlias节点。一般来讲，Subqueries 仅用于提供查询的视角范围(Scope)信息，一旦 analysis 阶段结束， 该节点就可以被移除，该优化规则直接将SubqueryAlias替换为其子节点。 |
| EliminateView                                           | Finish Analysis                               | Once                 | 此规则将从计划中删除View算子。在analysis阶段结束之前，这个算子会一直受到尊重，因为我们希望看到AnalyzedLogicalPlan的哪一部分是从视图生成的。 |
| InlineCTE                                               | Finish Analysis                               | Once                 | 如果满足以下任一条件，则将CTE定义插入相应的引用中：1. CTE定义不包含任何非确定性表达式。如果此CTE定义引用了另一个具有非确定性表达式的CTE定义，则仍然可以内联当前CTE定义。2.在整个主查询和所有子查询中，CTE定义只被引用一次。此外，由于相关子查询的复杂性，无论上述条件如何，相关子查询中的所有CTE引用都是内联的。 |
| ReplaceExpressions                                      | Finish Analysis                               | Once                 | 查找所有无法执行的表达式，并用可计算的语义等价表达式替换/重写它们。目前，我们替换了两种表达式：1.RuntimeReplaceable表达式。2.无法执行的聚合表达式，如Every/Some/Any/CountIf 这主要用于提供与其他数据库的兼容性。很少有这样的例子：我们使用它来支持nvl，将其替换为coalesce。我们分别用Min和Max替换each和Any。 |
| RewriteNonCorrelatedExists                              | Finish Analysis                               | Once                 | 为了使用ScalarSubquery需要重写非关联的exists子查询，比如：WHERE EXISTS (SELECT A FROM TABLE B WHERE COL1 > 10) 会被重写成 WHERE (SELECT 1 FROM (SELECT A FROM TABLE B WHERE COL1 > 10) LIMIT 1) IS NOT NULL 。ScalarSubquery是只返回一行和一列的子查询。这将在planning阶段转换为物理标量（scalar）子查询。 |
| PullOutGroupingExpressions                              | Finish Analysis                               | Once                 | 此规则确保Aggregate节点在optimization阶段不包含复杂的分组表达式。复杂的分组表达式被拉到Aggregate下的Project节点，并在分组表达式和不带聚合函数的聚合表达式中引用。这些引用确保优化规则不会将聚合表达式更改为不再引用任何分组表达式的无效表达式，并简化节点上的表达式转换（只需转换表达式一次）。例如，在下面的查询中，Spark不应该将聚合表达式Not(IsNull(c))优化成IsNotNull(c)，因为IsNull(c)是一个分组表达式：SELECT not(c IS NULL) FROM t GROUP BY c IS NULL |
| ComputeCurrentTime                                      | Finish Analysis                               | Once                 | 计算当前日期和时间，以确保在单个查询中返回相同的结果。       |
| ReplaceCurrentLike                                      | Finish Analysis                               | Once                 | 用当前数据库名称替换CurrentDatabase的表达式。用当前catalog名称替换CurrentCatalog的表达式。 |
| SpecialDatetimeValues                                   | Finish Analysis                               | Once                 | 如果输入字符串是可折叠的，则用其日期/时间戳值强制转换成特殊日期时间字符串。 |
| RemoveNoopOperators                                     | Union                                         | Once                 | 从查询计划中删除不进行任何修改的 no-op 运算符。              |
| CombineUnions                                           | Union                                         | Once                 | 将所有相邻的Union运算符合并成一个                            |
| RemoveNoopUnion                                         | Union                                         | Once                 | 简化 Union 的子节点，或者从查询计划中删除不修改查询的 no-op Union |
| OptimizeLimitZero                                       | OptimizeLimitZero                             | Once                 | 将GlobalLimit 0和LocalLimit 0节点（子树）替换为空的Local Relation，因为它们不返回任何数据行。 |
| ConvertToLocalRelation                                  | LocalRelation early                           | fixedPoint           | 将LocalRelation上的本地操作（即不需要数据交换的操作）转换为另一个LocalRelation。 |
| PropagateEmptyRelation                                  | LocalRelation early                           | fixedPoint           | 简化了空或非空关系的查询计划。当删除一个Union空关系子级时，PropagateEmptyRelation可以将属性（attribute）的可空性从可空更改为非空 |
| UpdateAttributeNullability                              | LocalRelation early                           | fixedPoint           | 通过使用其子输出属性（Attributes）的相应属性的可空性，更新已解析LogicalPlan中属性的可空性。之所以需要此步骤，是因为用户可以在Dataset API中使用已解析的AttributeReference，而outer join可以更改AttributeReference的可空性。如果没有这个规则，可以为NULL的列的NULL字段实际上可以设置为non-NULL，这会导致非法优化（例如NULL传播）和错误的答案。 |
| OptimizeOneRowRelationSubquery                          | Pullup Correlated Expressions                 | Once                 | 此规则优化将OneRowRelation作为叶节点的子查询。               |
| PullupCorrelatedPredicates                              | Pullup Correlated Expressions                 | Once                 | 从给定的子查询中取出所有（外部）相关谓词。此方法从子查询Filter中删除相关谓词，并将这些谓词的引用添加到所有中间Project和Aggregate子句（如果缺少的话），以便能够在顶层评估谓词。 |
| OptimizeSubqueries                                      | Subquery                                      | FixedPoint(1)        | 优化表达式中的所有子查询。子查询批处理递归地应用优化器规则。因此，在其上强制幂等性这毫无意义，我们将这个批次从Once改成了FixedPoint(1)。 |
| RewriteExceptAll                                        | Replace Operators                             | fixedPoint           | 混合使用Union、Aggregate、Generate 运算符来替代逻辑的Except运算符。 |
| RewriteIntersectAll                                     | Replace Operators                             | fixedPoint           | 混合使用Union、Aggregate、Generate 运算符来替代逻辑的Intersect运算符。 |
| ReplaceIntersectWithSemiJoin                            | Replace Operators                             | fixedPoint           | 使用 left-semi Join 运算符替代逻辑Intersect运算符。          |
| ReplaceExceptWithFilter                                 | Replace Operators                             | fixedPoint           | 如果逻辑Except运算符中的一或两个数据集都纯粹地使用Filter转换过，这个规则会使用反转Except运算符右侧条件之后的Filter运算符替代。 |
| ReplaceExceptWithAntiJoin                               | Replace Operators                             | fixedPoint           | 使用 left-anti Join运算符替代逻辑Except运算符。              |
| ReplaceDistinctWithAggregate                            | Replace Operators                             | fixedPoint           | 使用Aggregate运算符替代逻辑Distinct运算符。                  |
| ReplaceDeduplicateWithAggregate                         | Replace Operators                             | fixedPoint           | 使用Aggregate运算符替代逻辑Deduplicate运算符。               |
| RemoveLiteralFromGroupExpressions                       | Aggregate                                     | fixedPoint           | 移除Aggregate运算符中分组表达式的文本值，因为它们除了使得分组键变得更大以外，对结果没有任何影响。 |
| RemoveRepetitionFromGroupExpressions                    | Aggregate                                     | fixedPoint           | 移除Aggregate运算符中分组表达式的重复内容，因为它们除了使得分组键变得更大以外，对结果没有任何影响。 |
| 【算子下推】PushProjectionThroughUnion                  | Operator Optimization after Inferring Filters | fixedPoint           | 将Project操作符推送到Union操作符的两侧。可安全下推的操作如下所示。Union：现在，Union就意味着Union ALL，它不消除重复行。因此，通过它下推Filter和Project是安全的。下推Filter是由另一个规则PushDownPredicates处理的。一旦我们添加了UNION DISTINCT，我们就无法下推Project了。 |
| 【算子下推】ReorderJoin                                 | Operator Optimization after Inferring Filters | fixedPoint           | 重新排列Join，并将所有条件推入Join，以便底部的条件至少有一个条件。如果所有Join都已具有至少一个条件，则Join的顺序不会更改。如果启用了星型模式检测，请基于启发式重新排序星型Join计划。 |
| 【算子下推】EliminateOuterJoin                          | Operator Optimization after Inferring Filters | fixedPoint           | 1.消除outer join，前提是谓词可以限制结果集，以便消除所有空行：如果两侧都有这个谓词，full outer -> inner；如果右侧有这个谓词，left outer -> inner；如果左侧有这个谓词，right outer -> inner；当且仅当左侧有这个谓词，full outer -> left outer；当且仅当右侧有这个谓词，full outer -> right outer 2.如果outer join仅在流侧具有distinct，则移除outer join：SELECT DISTINCT f1 FROM t1 LEFT JOIN t2 ON t1.id = t2.id ==> SELECT DISTINCT f1 FROM t1。当前规则应该在谓词下推之前执行。 |
| 【算子下推】PushDownPredicates                          | Operator Optimization after Inferring Filters | fixedPoint           | 常规的运算符和Join谓词下推的统一版本。此规则提高了级联join（例如：Filter-Join-Join-Join）的谓词下推性能。大多数谓词可以在一次传递中下推。 |
| 【算子下推】PushDownLeftSemiAntiJoin                    | Operator Optimization after Inferring Filters | fixedPoint           | 这个规则是PushPredicateThroughNonJoin的一个变体，它可以下推以下运算符的Left semi join和Left Anti join：1.Project2.Window3.Union4.Aggregate5.其他允许的一元运算符。 |
| 【算子下推】PushLeftSemiLeftAntiThroughJoin             | Operator Optimization after Inferring Filters | fixedPoint           | 此规则是PushPredicateThrowJoin的一个变体，它可以下推join运算符下面的Left semi join和Left Anti join：允许的Join类型有：1.Inner；2.Cross；3.LeftOuter；4.RightOuter |
| 【算子下推】LimitPushDown                               | Operator Optimization after Inferring Filters | fixedPoint           | 下推UNION ALL和JOIN下的LocalLimit。                          |
| 【算子下推】LimitPushDownThroughWindow                  | Operator Optimization after Inferring Filters | fixedPoint           | 下推Window下方的LocalLimit。此规则优化了以下情况：SELECT *, ROW_NUMBER() OVER(ORDER BY a) AS rn FROM Tab1 LIMIT 5 ==> SELECT *, ROW_NUMBER() OVER(ORDER BY a) AS rn FROM (SELECT * FROM Tab1 ORDER BY a LIMIT 5) t |
| 【算子下推】ColumnPruning                               | Operator Optimization after Inferring Filters | fixedPoint           | 试图消除查询计划中不需要的列读取。由于在Filter之前添加Project会和PushPredicatesThroughProject冲突，此规则将以以下模式删除Project p2：p1 @ Project(_, Filter(_, p2 @ Project(_, child))) if p2.outputSet.subsetOf(p2.inputSet) p2通常是按照这个规则插入的，没有用，p1无论如何都可以删减列。 |
| 【算子合并】CollapseRepartition                         | Operator Optimization after Inferring Filters | fixedPoint           | 合并相邻的RepartitionOperation和RebalancePartitions运算符    |
| 【算子合并】CollapseProject                             | Operator Optimization after Inferring Filters | fixedPoint           | 两个Project运算符合并为一个别名替换，在以下情况下，将表达式合并为一个表达式。1.两个Project运算符相邻时。2.当两个Project运算符之间有LocalLimit/Sample/Repartition运算符，且上层的Project由相同数量的列组成，且列数相等或具有别名时。同时也考虑到GlobalLimit(LocalLimit)模式。 |
| 【算子合并】OptimizeWindowFunctions                     | Operator Optimization after Inferring Filters | fixedPoint           | 将first(col)替换成nth_value(col, 1)来获得更好的性能          |
| 【算子合并】CollapseWindow                              | Operator Optimization after Inferring Filters | fixedPoint           | 折叠相邻的Window表达式。如果分区规格和顺序规格相同，并且窗口表达式是独立的，且属于相同的窗口函数类型，则折叠到父节点中。 |
| 【算子合并】CombineFilters                              | Operator Optimization after Inferring Filters | fixedPoint           | 将两个相邻的Filter运算符合并为一个，将非冗余条件合并为一个连接谓词。 |
| 【算子合并】EliminateLimits                             | Operator Optimization after Inferring Filters | fixedPoint           | 该规则由normal和AQE优化器应用，并通过以下方式优化Limit运算符：1.如果是child max row<=Limit，则消除Limit/GlobalLimit运算符。2.将两个相邻的Limit运算符合并为一个，将多个表达式合并成一个。 |
| 【算子合并】CombineUnions                               | Operator Optimization after Inferring Filters | fixedPoint           | 将所有相邻的Union运算符合并为一个。                          |
| 【常量折叠和强度消减】OptimizeRepartition               | Operator Optimization after Inferring Filters | fixedPoint           | 如果所有的分区表达式都被折叠了并且用户未指定的情况下，将RepartitionByExpression的分区数设置成 1 |
| 【常量折叠和强度消减】TransposeWindow                   | Operator Optimization after Inferring Filters | fixedPoint           | 转置相邻的窗口表达式。如果父窗口表达式的分区规范与子窗口表达式的分区规范兼容，就转置它们。 |
| 【常量折叠和强度消减】NullPropagation                   | Operator Optimization after Inferring Filters | fixedPoint           | 替换可以用等效Literal静态计算的Expression Expressions。对于从表达式树的底部到顶部的空值传播，这个规则会更加具体。 |
| 【常量折叠和强度消减】ConstantPropagation               | Operator Optimization after Inferring Filters | fixedPoint           | 用连接表达式中的相应值替换可以静态计算的属性，例如：SELECT * FROM table WHERE i = 5 AND j = i + 3 ==> SELECT * FROM table WHERE i = 5 AND j = 8 使用的方法：通过查看所有相等的谓词来填充属性 => 常量值的映射；使用这个映射，将属性的出现的地方替换为AND节点中相应的常量值。 |
| 【常量折叠和强度消减】FoldablePropagation               | Operator Optimization after Inferring Filters | fixedPoint           | 如果可能，用原始可折叠表达式的别名替换属性。其他优化将利用传播的可折叠表达式。例如，此规则可以将SELECT 1.0 x, 'abc' y, Now() z ORDER BY x, y, 3 优化成SELECT 1.0 x, 'abc' y, Now() z ORDER BY 1.0, 'abc', Now() 其他规则可以进一步优化它，并且删除ORDER BY运算符。 |
| 【常量折叠和强度消减】OptimizeIn                        | Operator Optimization after Inferring Filters | fixedPoint           | 优化IN谓词：1.当列表为空且值不可为null时，将谓词转换为false。2.删除文本值重复。3.将In (value, seq[Literal])替换为更快的优化版本InSet (value, HashSet[Literal])。 |
| 【常量折叠和强度消减】ConstantFolding                   | Operator Optimization after Inferring Filters | fixedPoint           | 替换可以用等效文本值静态计算的表达式。                       |
| 【常量折叠和强度消减】EliminateAggregateFilter          | Operator Optimization after Inferring Filters | fixedPoint           | 删除聚合表达式的无用FILTER子句。在RewriteDistinctAggregates之前，应该先应用这个规则。 |
| 【常量折叠和强度消减】ReorderAssociativeOperator        | Operator Optimization after Inferring Filters | fixedPoint           | 重新排列相关的整数类型运算符，并将所有常量折叠为一个。       |
| 【常量折叠和强度消减】LikeSimplification                | Operator Optimization after Inferring Filters | fixedPoint           | 简化了不需要完整正则表达式来计算条件的LIKE表达式。例如，当表达式只是检查字符串是否以给定模式开头时。 |
| 【常量折叠和强度消减】BooleanSimplification             | Operator Optimization after Inferring Filters | fixedPoint           | 简化了布尔表达式：1.简化了答案可以在不计算双方的情况下确定的表达式。2.消除/提取共同的因子。3.合并相同的表达式4。删除Not运算符。 |
| 【常量折叠和强度消减】SimplifyConditionals              | Operator Optimization after Inferring Filters | fixedPoint           | 简化了条件表达式（if / case）                                |
| 【常量折叠和强度消减】PushFoldableIntoBranches          | Operator Optimization after Inferring Filters | fixedPoint           | 将可折叠表达式下推到if / case分支                            |
| 【常量折叠和强度消减】RemoveDispensableExpressions      | Operator Optimization after Inferring Filters | fixedPoint           | 移除不必要的节点                                             |
| 【常量折叠和强度消减】SimplifyBinaryComparison          | Operator Optimization after Inferring Filters | fixedPoint           | 使用语义相等的表达式简化二进制比较：1.用true文本值替代<==>；2.如果操作数都是非空的，用true文本值替代 =， <=， 和 >=；3.如果操作数都是非空的，用false文本值替代>和<；4.如果有一边操作数是布尔文本值，就展开=、<=> |
| 【常量折叠和强度消减】ReplaceNullWithFalseInPredicate   | Operator Optimization after Inferring Filters | fixedPoint           | 一个用FalseLiteral替换Literal(null, BooleanType) 的规则，如果可能的话，在WHERE/HAVING/ON(JOIN)子句的搜索条件中，该子句包含一个隐式布尔运算符(search condition) = TRUE。当计算整个搜索条件时，只有当Literal(null, BooleanType)在语义上等同于FalseLiteral时，替换才有效。请注意，在大多数情况下，当搜索条件包含NOT和可空的表达式时，FALSE和NULL是不可交换的。因此，该规则非常保守，适用于非常有限的情况。例如，Filter(Literal(null, BooleanType))等同于Filter(FalseLiteral)。另一个包含分支的示例是Filter(If(cond, FalseLiteral, Literal(null, _)))；这可以优化为Filter(If(cond, FalseLiteral, FalseLiteral))，最终Filter(FalseLiteral)。此外，该规则还转换所有If表达式中的谓词，以及所有CaseWhen表达式中的分支条件，即使它们不是搜索条件的一部分。例如，Project(If(And(cond, Literal(null)), Literal(1), Literal(2)))可以简化为Project(Literal(2))。 |
| 【常量折叠和强度消减】SimplifyConditionalsInPredicate   | Operator Optimization after Inferring Filters | fixedPoint           | 一个规则，在WHERE/HAVING/ON(JOIN)子句的搜索条件中，如果可能，将条件表达式转换为谓词表达式，其中包含一个隐式布尔运算符(search condition) = TRUE。在这个转换之后，我们可以潜在地下推过滤到数据源。这个规则是安全可空的。支持的用例有：1.IF(cond, trueVal, false) => AND(cond, trueVal)；2.IF(cond, trueVal, true) => OR(NOT(cond), trueVal)；3.IF(cond, false, falseVal) => AND(NOT(cond), falseVal)；4.IF(cond, true, falseVal) => OR(cond, falseVal)；5.CASE WHEN cond THEN trueVal ELSE false END => AND(cond, trueVal)；6.CASE WHEN cond THEN trueVal END => AND(cond, trueVal)；7.CASE WHEN cond THEN trueVal ELSE null END => AND(cond, trueVal)；8.CASE WHEN cond THEN trueVal ELSE true END => OR(NOT(cond), trueVal)；9.CASE WHEN cond THEN false ELSE elseVal END => AND(NOT(cond), elseVal)；10.CASE WHEN cond THEN true ELSE elseVal END => OR(cond, elseVal) |
| 【常量折叠和强度消减】PruneFilters                      | Operator Optimization after Inferring Filters | fixedPoint           | 删除可以进行简单计算的Filter。这可以通过以下方式实现：1.在其计算结果始终为true的情况下，省略Filter。2.当筛选器的计算结果总是为false时，替换成一个伪空关系。3.消除子节点输出给定约束始终为true的条件。 |
| 【常量折叠和强度消减】SimplifyCasts                     | Operator Optimization after Inferring Filters | fixedPoint           | 删除不必要的强制转换，因为输入已经是正确的类型。             |
| 【常量折叠和强度消减】SimplifyCaseConversionExpressions | Operator Optimization after Inferring Filters | fixedPoint           | 删除不必要的内部大小写转换表达式，因为内部转换被外部转换覆盖。 |
| 【常量折叠和强度消减】RewriteCorrelatedScalarSubquery   | Operator Optimization after Inferring Filters | fixedPoint           | 此规则将相关的ScalarSubquery表达式重写为LEFT OUTER JOIN。    |
| 【常量折叠和强度消减】RewriteLateralSubquery            | Operator Optimization after Inferring Filters | fixedPoint           | 将LateralSubquery表达式重写成join                            |
| 【常量折叠和强度消减】EliminateSerialization            | Operator Optimization after Inferring Filters | fixedPoint           | 消除不必要地在对象和数据项的序列化（InternalRow）表示之间切换的情况。例如，背对背映射操作。 |
| 【常量折叠和强度消减】RemoveRedundantAliases            | Operator Optimization after Inferring Filters | fixedPoint           | 从查询计划中删除冗余别名。冗余别名是不会更改列的名称或元数据，也不会消除重复数据的别名。 |
| 【常量折叠和强度消减】RemoveRedundantAggregates         | Operator Optimization after Inferring Filters | fixedPoint           | 从查询计划中删除冗余聚合。冗余聚合是一种聚合，其唯一目标是保持不同的值，而其父聚合将忽略重复的值。 |
| 【常量折叠和强度消减】UnwrapCastInBinaryComparison      | Operator Optimization after Inferring Filters | fixedPoint           | 在二进制比较或In/InSet操作中使用如下模式进行展开强制转换：1. BinaryComparison(Cast(fromExp, toType), Literal(value, toType))；2.BinaryComparison(Literal(value, toType), Cast(fromExp, toType))；[http://3.In](https://link.zhihu.com/?target=http%3A//3.In)(Cast(fromExp, toType), Seq(Literal(v1, toType), Literal(v2, toType), ...)；4.InSet(Cast(fromExp, toType), Set(v1, v2, ...)) 该规则通过使用更简单的构造替换强制转换，或者将强制转换从表达式端移动到文本值端，从而使用上述模式优化表达式，这使它们能够在以后进行优化，并向下推送到数据源。 |
| 【常量折叠和强度消减】RemoveNoopOperators               | Operator Optimization after Inferring Filters | fixedPoint           | 从查询计划中删除不进行任何修改的no-op运算符。                |
| 【常量折叠和强度消减】OptimizeUpdateFields              | Operator Optimization after Inferring Filters | fixedPoint           | 优化UpdateFields表达式链。                                   |
| 【常量折叠和强度消减】SimplifyExtractValueOps           | Operator Optimization after Inferring Filters | fixedPoint           | 简化冗余的CreateNamedStruct/CreateArray/CreateMap表达式      |
| 【常量折叠和强度消减】OptimizeCsvJsonExprs              | Operator Optimization after Inferring Filters | fixedPoint           | 简化冗余的csv/json相关表达式。优化内容包括：1.JsonToStructs(StructsToJson(child)) => child.。2.删除GetStructField/GetArrayStructFields + JsonToStructs中不必要的列。3.CreateNamedStruct(JsonToStructs(json).col1, JsonToStructs(json).col2, ...) => If(IsNull(json), nullStruct, KnownNotNull(JsonToStructs(prunedSchema, ..., json)))如果JsonToStructs(json)在CreateNamedStruct的所有字段中共享。prunedSchema包含原始CreateNamedStruct中所有访问的字段。4.从GetStructField+CsvToStructs中删除不必要的列。 |
| 【常量折叠和强度消减】CombineConcats                    | Operator Optimization after Inferring Filters | fixedPoint           | 合并嵌套的Concat表达式                                       |
| 【过滤推断】InferFiltersFromGenerate                    | Infer Filters                                 | Once                 | 从Generate推断Filter，这样就可以在join之前和数据源中更早地通过这个Generate删除数据行。 |
| 【过滤推断】InferFiltersFromConstraints                 | Infer Filters                                 | Once                 | 基于运算符的现有约束生成附加过滤器的列表，但删除那些已经属于运算符条件一部分或属于运算符子节点约束一部分的过滤器。这些筛选器当前插入到Filter运算符的和Join运算符任一侧的现有条件中。注意：虽然这种优化适用于许多类型的join，但它主要有利于Inner Join和LeftSemi Join。 |
| PushExtraPredicateThroughJoin                           | Push extra predicate through join             | fixedPoint           | 尝试将JOIN条件下推到左和右子节点中。为了避免扩展JOIN条件，即使发生谓词下推，JOIN条件也将保持原始形式。 |
| preCBORules                                             | Pre CBO Rules                                 | Once                 | 这个规则批在算子优化后、在任何依赖统计数据的规则批之前重写了逻辑计划。 |
| earlyScanPushDownRules                                  | Early Filter and Projection Push-Down         | Once                 | 这个规则批将Filter和Project下推到Scan节点。在这个规则批之前，逻辑计划可能包含不报告统计数据的节点。任何使用统计数据的规则都必须在这个规则批之后运行。 |
| UpdateCTERelationStats                                  | Update CTE Relation Stats                     | Once                 | 更新CTE引用的统计信息。                                      |
| CostBasedJoinReorder                                    | Join Reorder                                  | FixedPoint(1)        | 基于成本的JOIN重新排序。未来我们可能会有几种JOIN重排序算法。这个类是这些算法的入口，并选择要使用的算法。由于AQP中的连接成本可能在多次运行之间发生变化，因此我们没有理由强制这个规则批上面的幂等性。因此，我们将其定为FixedPoint(1)，而不是Once。 |
| EliminateSorts                                          | Eliminate Sorts                               | Once                 | 如果排序操作不影响最终的输出顺序，则删除它们。请注意，最终输出顺序的更改可能会影响文件大小。这个规则处理下面的情况：1.如果子节点的最大行数小于或等于1；2.如果排序顺序为空或排序顺序没有任何引用；3.如果排序运算符是本地排序且子节点已排序；4.如果有另一个排序运算符被 0...n 个 Project、Filter、Repartition或RepartitionByExpression、RebalancePartitions（使用确定性表达式）运算符分隔；5.如果排序运算符位于JOIN内，被0...n 个 Project、Filter、Repartition或RepartitionByExpression、RebalancePartitions（使用确定性表达式）运算符分隔，且只有JOIN条件是确定性的；6.如果排序运算符位于GroupBy中，被0...n 个 Project、Filter、Repartition或RepartitionByExpression、RebalancePartitions（使用确定性表达式）运算符分隔，且只有聚合函数是顺序无关的。 |
| DecimalAggregates                                       | Decimal Optimizations                         | fixedPoint           | 通过在未标度的长整型值上执行固定精度小数来加速聚合。它使用和DecimalPrecision相同的规则来提高输出的精度和规模。Hive_Decimal_Precision_Scale_Support |
| RewriteDistinctAggregates                               | Distinct Aggregate Rewrite                    | Once                 | 此规则将具有不同聚合的聚合查询重写为扩展的双精度查询聚合，其中正则聚合表达式和每个不同的子句被聚合单独一组。然后将结果合并到第二个聚合中。此批处理必须在Decimal Optimizations之后运行，因为这样可能会更改aggregate distinct列 |
| EliminateMapObjects                                     | Object Expressions Optimization               | fixedPoint           | 满足以下条件时删除MapObjects：1.Mapobject(... lambdavariable(..., false) ...)，这意味着输入和输出的类型都是非空原始类型；2.没有自定义集合类指定数据项的表示形式。MapObjects将给定表达式应用于集合项的每个元素，并将结果作为ArrayType或ObjectType返回。这类似于典型的映射操作，但lambda函数是使用catalyst表达式表示的。 |
| CombineTypedFilters                                     | Object Expressions Optimization               | fixedPoint           | 将两个相邻的TypedFilter（它们在条件下对同一类型对象进行操作）合并为一个，将筛选函数合并为一个连接函数。TypedFilter将func应用于子元素的每个元素并按最终产生的布尔值过滤它们。这在逻辑上等于一个普通的Filter运算符，其条件表达式将输入行解码为对象，并将给定函数应用于解码的对象。然而，我们需要对TypedFilter进行封装，以使概念更清晰，并使编写优化器规则更容易。 |
| ObjectSerializerPruning                                 | Object Expressions Optimization               | fixedPoint           | 从查询计划中删除不必要的对象序列化程序。此规则将删除序列化程序中的单个序列化程序和嵌套字段。 |
| ReassignLambdaVariableID                                | Object Expressions Optimization               | fixedPoint           | 将每个查询的唯一ID重新分配给LambdaVariables，其原始ID是全局唯一的。这有助于Spark更频繁地访问codegen缓存并提高性能。LambdaVariables是MapObjects中使用的循环变量的占位符。不应该手动构造，而是将其传递到提供的lambda函数中。 |
| ConvertToLocalRelation                                  | Object Expressions Optimization               | fixedPoint           | 将LocalRelation上的本地操作（即不需要数据交换的操作）转换为另一个LocalRelation。LocalRelation是用于扫描本地集合中的数据的逻辑计划节点。ConvertToLocalRelation 在上面的LocalRelation early规则批中也出现过。 |
| PropagateEmptyRelation                                  | LocalRelation                                 | fixedPoint           | 简化了空或非空关系的查询计划。当删除一个Union空关系子级时，PropagateEmptyRelation可以将属性（attribute）的可空性从可空更改为非空 |
| UpdateAttributeNullability                              | LocalRelation                                 | fixedPoint           | 通过使用其子输出属性（Attributes）的相应属性的可空性，更新已解析LogicalPlan中属性的可空性。之所以需要此步骤，是因为用户可以在Dataset API中使用已解析的AttributeReference，而outer join可以更改AttributeReference的可空性。如果没有这个规则，可以为NULL的列的NULL字段实际上可以设置为non-NULL，这会导致非法优化（例如NULL传播）和错误的答案。UpdateAttributeNullability 在上面的LocalRelation early规则批中也出现过。 |
| CheckCartesianProducts                                  | Check Cartesian Products                      | Once                 | 检查优化计划树中任何类型的join之间是否存在笛卡尔积。如果在未显示指定cross join的情况下找到笛卡尔积，则引发错误。如果CROSS_JOINS_ENABLED标志为true，则此规则将被有效禁用。此规则必须在ReordJoin规则之后运行，因为在检查每个join是否为笛卡尔积之前，必须收集每个join的join条件。如果有SELECT * from R, S where R.r = S.s，则R和S之间的连接不是笛卡尔积，因此应该允许。谓词R.r=S.s在ReorderJoin规则之前不会被识别为join条件。此规则必须在批处理LocalRelation之后运行，因为具有空关系的join不应是笛卡尔积。从这往下的规则批都应该在规则批Join Reorder和LocalRelation之后执行。 |
| RewritePredicateSubquery                                | RewriteSubquery                               | Once                 | 这个规则将谓词子查询重写为left semi/anti join。支持以下谓词：1.EXISTS/NOT EXISTS将被重写为semi/anti join，Filter中未解析的条件将被提取为join条件。[http://2.IN/NOT](https://link.zhihu.com/?target=http%3A//2.IN/NOT) IN将被重写为semi/anti join，Filter中未解析的条件将作为join条件被拉出，value=selected列也将用作join条件。 |
| NormalizeFloatingNumbers                                | NormalizeFloatingNumbers                      | Once                 | 这个规则规范化了窗口分区键、join key和聚合分组键中的NaN和-0.0。这个规则必须在RewriteSubquery之后运行，后者会创建join |
| ReplaceUpdateFieldsExpression                           | ReplaceUpdateFieldsExpression                 | Once                 | 用可计算表达式替换UpdateFields表达式。UpdateFields用于在结构体中更新字段。 |