# SubqueryExpression

包含一个逻辑计划的表达式

实现类

1. ScalarSubquery: 只返回单行单列的子查询

2. Exists

3. DynamicPruningSubquery

   使用join一边的filter过滤另一边的表，在应用分区裁剪时插入的

4. LateralSubquery

5. ListQuery
   只能和in表达式一起使用，作为in的查询范围

InSubquery也是一个子查询，但非SubqueryExpression的实现类



```scala
case class  ScalarSubquery(
    plan: LogicalPlan,
    outerAttrs: Seq[Expression] = Seq.empty,
    exprId: ExprId = NamedExpression.newExprId,
    joinCond: Seq[Expression] = Seq.empty)
  extends SubqueryExpression
```

