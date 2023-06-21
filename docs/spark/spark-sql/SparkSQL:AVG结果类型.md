# sql和计划

"select avg(w_warehouse_sq_ft) from tpcds1gv.warehouse"

```java
== Parsed Logical Plan ==
'Project [unresolvedalias('avg('w_warehouse_sq_ft), None)]
+- 'UnresolvedRelation [tpcds1gv, warehouse], [], false

== Analyzed Logical Plan ==
avg(w_warehouse_sq_ft): double
Aggregate [avg(w_warehouse_sq_ft#3) AS avg(w_warehouse_sq_ft)#15]
+- SubqueryAlias spark_catalog.tpcds1gv.warehouse
   +- Relation tpcds1gv.warehouse[w_warehouse_sk#0,w_warehouse_id#1,w_warehouse_name#2,w_warehouse_sq_ft#3,w_street_number#4,w_street_name#5,w_street_type#6,w_suite_number#7,w_city#8,w_county#9,w_state#10,w_zip#11,w_country#12,w_gmt_offset#13] parquet

== Optimized Logical Plan ==
Aggregate [avg(w_warehouse_sq_ft#3) AS avg(w_warehouse_sq_ft)#15]
+- Project [w_warehouse_sq_ft#3]
   +- Relation tpcds1gv.warehouse[w_warehouse_sk#0,w_warehouse_id#1,w_warehouse_name#2,w_warehouse_sq_ft#3,w_street_number#4,w_street_name#5,w_street_type#6,w_suite_number#7,w_city#8,w_county#9,w_state#10,w_zip#11,w_country#12,w_gmt_offset#13] parquet

== Physical Plan ==
AdaptiveSparkPlan isFinalPlan=false
+- HashAggregate(keys=[], functions=[avg(w_warehouse_sq_ft#3)], output=[avg(w_warehouse_sq_ft)#15])
   +- Exchange SinglePartition, ENSURE_REQUIREMENTS, [id=#11]
      +- HashAggregate(keys=[], functions=[partial_avg(w_warehouse_sq_ft#3)], output=[sum#33, count#34L])
         +- FileScan parquet tpcds1gv.warehouse[w_warehouse_sq_ft#3] Batched: true, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/tpcds1gv.db/warehouse], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<w_warehouse_sq_ft:int>

```

# Average dataType/sumDataType

## output触发Average的dataType计算

规则：PullOutNondeterministic

从LogicalPlan(不是Project或Filter)中取出不确定表达式，将它们放入内部project中，最后将它们投射到外部project中。它用到了Aggregate的output，如果我们改了它，是否有影响未知。

目前只对output和child相同的UnaryNode做处理

```scala
    case p: UnaryNode if p.output == p.child.output && p.expressions.exists(!_.deterministic) =>
      val nondeterToAttr = getNondeterToAttr(p.expressions)
      val newPlan = p.transformExpressions { case e =>
        nondeterToAttr.get(e).map(_.toAttribute).getOrElse(e)
      }
      val newChild = Project(p.child.output ++ nondeterToAttr.values, p.child)
      Project(p.output, newPlan.withNewChildren(newChild :: Nil))
```

注意，这里会触发计算逻辑计划的output, 对于<font color=red>Aggregate逻辑算子</font>，其output计算逻辑：

```scala
override def output: Seq[Attribute] = aggregateExpressions.map(_.toAttribute)
```

aggregateExpressions是Alias, 其toAttribute又触发了AggregateExpression的dataType

```scala
  override def toAttribute: Attribute = {
    if (resolved) {
      AttributeReference(name, child.dataType, child.nullable, metadata)(exprId, qualifier)
    } else {
      UnresolvedAttribute(name)
    }
  }
```

AggregateExpression的dataType是其aggregateFunction即Average的dataType，又触发父类AverageBase的dataType的计算：

```scala
  override def dataType: DataType = resultType

  protected lazy val resultType = child.dataType match {
    case DecimalType.Fixed(p, s) =>
      DecimalType.bounded(p + 4, s + 4)
    case _: YearMonthIntervalType => YearMonthIntervalType()
    case _: DayTimeIntervalType => DayTimeIntervalType()
    case _ => DoubleType
  }
```

除了这个规则外，还有其他规则例如V2ScanRelationPushDown会计算Agregate的output

## sumDataType

再QueryPlanner把逻辑计划转为物理计划时，Aggregation Strategy收集aggregateExpressions的aggBufferAttributes，触发了sum/sumDataType的计算

# 实验

## Optimizer插入位置

会插入到以下两个Batch中

Operator Optimization before Inferring Filters

Operator Optimization after Inferring Filters



## apply自定义规则

拿到的计划：

```java
Aggregate [avg(w_warehouse_sq_ft#3) AS avg(w_warehouse_sq_ft)#15]
+- Project [w_warehouse_sq_ft#3]
   +- Relation tpcds1gv.warehouse[w_warehouse_sk#0,w_warehouse_id#1,w_warehouse_name#2,w_warehouse_sq_ft#3,w_street_number#4,w_street_name#5,w_street_type#6,w_suite_number#7,w_city#8,w_county#9,w_state#10,w_zip#11,w_country#12,w_gmt_offset#13] parquet

```

Aggregate的aggregateExpressions是一个NamedExpression: Alias，Alias的child是一个AggregateExpression

AggregateExpression的aggregateFunction是一个Average

```scala
case class MyRule(spark: SparkSession) extends Rule[LogicalPlan] {
  override def apply(plan: LogicalPlan): LogicalPlan = plan transform  {
    // 拦截Aggregate算子
    case agg@Aggregate(groupingExpressions,aggregateExpressions,child)=>
         agg transformExpressions  {
           //拦截Average聚合表达式
           case e:AggregateExpression if e.aggregateFunction.isInstanceOf[Average]=> {
             val average: Average = e.aggregateFunction.asInstanceOf[Average]
             // if dataType==Double, overwrite it
             if(average.dataType.isInstanceOf[DoubleType])
              new Average(average.child) {
                override lazy val resultType = DecimalType(18, 1)
                override lazy val sumDataType = DecimalType(18, 1)
              }
             else{
               average
             }
           }
         }
  }

}
```

## 替换结果

因为我们的规则是应用到optimizer阶段的，所以要看optimizedPlan:
![image-20230607093300779](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230607093300779.png)

我们看到有两处变化

1. resultType/sumDataType确实被我们改成了DecimalType(18,1)
2. Average表达式被改成了一个匿名类

这个匿名类麻烦了，我们后续怎么知道如何卸载它？规避的办法是不用匿名类，构造一个新类MyAverage来继承Average类

```scala
case class MyRule(spark: SparkSession) extends Rule[LogicalPlan] {
  override def apply(plan: LogicalPlan): LogicalPlan = plan transform  {
    case agg@Aggregate(groupingExpressions,aggregateExpressions,child)=>
         agg transformExpressions  {
           case e:AggregateExpression if e.aggregateFunction.isInstanceOf[Average]=> {
             val average: Average = e.aggregateFunction.asInstanceOf[Average]
             // if dataType==Double, overwrite it
             if(average.dataType.isInstanceOf[DoubleType])
              new MyAverage(average.child)
             else{
               average
             }
           }
         }
  }

}
class MyAverage(override val child: Expression) extends Average(child: Expression){
  override lazy val resultType=DecimalType(18,1)
  override lazy val sumDataType =DecimalType(18,1)
}
```

此时，我们再检查一下optimizerPlan

![image-20230607094742900](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230607094742900.png)

发现，已经替换为我们的MyAverage类了



### AggregateExpression

```scala
// resultAttribute的类型根  aggregateFunction一致
lazy val resultAttribute: Attribute = if (aggregateFunction.resolved) {
    AttributeReference(
      aggregateFunction.toString,
      aggregateFunction.dataType,
      aggregateFunction.nullable)(exprId = resultId)
  } else {
    // This is a bit of a hack.  Really we should not be constructing this container and reasoning
    // about datatypes / aggregation mode until after we have finished analysis and made it to
    // planning.
    UnresolvedAttribute(aggregateFunction.toString)
  }



```

AggregateExpression的resultAttribute的类型在替换aggregateFunction的dataType之前确定， 所以aggregateFunction的dataType被替换后，AggregateExpression的resultAttribute的类型没有被替换。

AggregateExpression的resultAttribute的类型什么时候确定的呢？

参见2.1节output触发Average的dataType计算，PullOutNondeterministic规则提前出发了计算。



经过尝试，插入到analyzer阶段，这里计算的结果仍然是替换之前的类型，即DoubleType。 看来需要在创建AggregateExepression时就要把入参aggregateFunction替换掉

### ResolveFunctions

Analyzer阶段的Resolution阶段中的第22个规则

![image-20230621154520382](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230621154520382.png)

Replaces [[UnresolvedFunc]]s with concrete [[LogicalPlan]]s.
 Replaces [[UnresolvedFunction]]s with concrete [[Expression]]

在validateFunction函数内对Average函数进行包装得到*AggregateExpression*

```scala
        // We get an aggregate function, we need to wrap it in an AggregateExpression.
        case agg: AggregateFunction =>
          u.filter match {
            case Some(filter) if !filter.deterministic =>
              throw QueryCompilationErrors.nonDeterministicFilterInAggregateError
            case Some(filter) if filter.dataType != BooleanType =>
              throw QueryCompilationErrors.nonBooleanFilterInAggregateError
            case Some(filter) if filter.exists(_.isInstanceOf[AggregateExpression]) =>
              throw QueryCompilationErrors.aggregateInAggregateFilterError
            case Some(filter) if filter.exists(_.isInstanceOf[WindowExpression]) =>
              throw QueryCompilationErrors.windowFunctionInAggregateFilterError
            case _ =>
          }
          if (u.ignoreNulls) {
            val aggFunc = agg match {
              case first: First => first.copy(ignoreNulls = u.ignoreNulls)
              case last: Last => last.copy(ignoreNulls = u.ignoreNulls)
              case _ =>
                throw QueryCompilationErrors.functionWithUnsupportedSyntaxError(
                  agg.prettyName, "IGNORE NULLS")
            }
            AggregateExpression(aggFunc, Complete, u.isDistinct, u.filter)
          } else {
            // 包装
            AggregateExpression(agg, Complete, u.isDistinct, u.filter)
          }
```



## HashAggregateExec的构建

QueryExecution创建sparkPlan的入口

```scala
  lazy val sparkPlan: SparkPlan = {
    // We need to materialize the optimizedPlan here because sparkPlan is also tracked under
    // the planning phase
    assertOptimized()
    executePhase(QueryPlanningTracker.PLANNING) {
      // Clone the logical plan here, in case the planner rules change the states of the logical
      // plan.
      QueryExecution.createSparkPlan(sparkSession, planner, optimizedPlan.clone())
    }
  }

  def createSparkPlan(
      sparkSession: SparkSession,
      planner: SparkPlanner,
      plan: LogicalPlan): SparkPlan = {
    // TODO: We use next(), i.e. take the first plan returned by the planner, here for now,
    //       but we will implement to choose the best plan.
    planner.plan(ReturnAnswer(plan)).next()
  }
```

SparkPlanner是SparkStrategies的一种实现，plan方法定义在父接口里

```scala
// SparkStrategies extends QueryPlanner
override def plan(plan: LogicalPlan): Iterator[SparkPlan] = {
  // 又会调用父接口QueryPlanner的plan
    super.plan(plan).map { p =>
      val logicalPlan = plan match {
        case ReturnAnswer(rootPlan) => rootPlan
        case _ => plan
      }
      p.setLogicalLink(logicalPlan)
      p
    }
  }
```







QueryPlanner在把逻辑计划转物理计划，object Aggregation extends Strategy  会使用AggUtils来plan对应的HashAggregateExec算子







## 插入到analyzer里

analyzer只能插入到Resolution阶段，该阶段的策略是FixedPoint， 也就是要求在指定迭代次数内一定要达到不动点。



提示错误

java.lang.RuntimeException: Max iterations (100) reached for batch Resolution, please set 'spark.sql.analyzer.maxIterations' to a larger value.

说明我们的规则在指定迭代次数内没有达到不动点,所谓的不动点，就是plan再应用规则后没有变化或者引用没变。







## spark无法处理自定义表达式

上节，我们完成了表达式类替换，交给spark执行时，它在生成代码阶段抛异常了：

```scala
Exception in thread "main" org.apache.spark.SparkUnsupportedOperationException: [INTERNAL_ERROR] Cannot generate code for expression: avg(w_warehouse_sq_ft#3)
	at org.apache.spark.sql.errors.QueryExecutionErrors$.cannotGenerateCodeForExpressionError(QueryExecutionErrors.scala:89)
	at org.apache.spark.sql.catalyst.expressions.aggregate.DeclarativeAggregate.doGenCode(interfaces.scala:447)
	at org.apache.spark.sql.catalyst.expressions.Expression.$anonfun$genCode$3(Expression.scala:151)
	at scala.Option.getOrElse(Option.scala:189)
	at org.apache.spark.sql.catalyst.expressions.Expression.genCode(Expression.scala:146)
	at org.apache.spark.sql.catalyst.expressions.Alias.genCode(namedExpressions.scala:160)
```

不支持的算子





# 方案

1. 所有double类型都替换成Decimal(18,1)
2. Average的resultType替换成Decimal  选这个

当前遇到一个问题，avg的类型修改后，万一我们无法卸载，spark则无法执行。所以第三个方案：

3. DpuAverage修改数据类型， 但问题是