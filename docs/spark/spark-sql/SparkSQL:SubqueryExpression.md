# SparkSQL中的subqueries

- Table Subquery

  ```sql
  SELECT *
  FROM   (SELECT *
          FROM   t1) AS tab;
  ```

- Scalar Subquery Expressions

  ```sql
  SELECT col2,
         (SELECT Max(col1)
          FROM   t1) AS col1
  ```

  

- Subquery in WHERE clause

  ```sql
  SELECT *
  FROM   t1
  WHERE  col1 IN (SELECT col1
                  FROM   t2); 
  ```

  

- Correlated Subquery
  子查询使用外查询的列， 只支持等值join

  ```sql
  SELECT *
  FROM   t1 AS t1
  WHERE  EXISTS (SELECT 1
                 FROM   t2 AS t2
                 WHERE  t1.col1 = t2.col1);
  ```

  

- Correlated Scalar Subquery
  子查询使用了外查询的列，且只返回一个列值
  
  ```sql
  SELECT col1,
         COALESCE ((SELECT Max(col2)
                    FROM   t1
                    WHERE  t1.col1 = t2.col1), 0) AS col2
  FROM   t2;
  ```
  
  

package org.apache.spark.sql.execution

SparkPlan中使用的子查询

```scala
case class ScalarSubquery(
    plan: BaseSubqueryExec,
    exprId: ExprId)
  extends ExecSubqueryExpression

abstract class ExecSubqueryExpression extends PlanExpression

abstract class PlanExpression[T <: QueryPlan[_]] extends Expression
```





package org.apache.spark.sql.catalyst.expressions

```scala
case class ScalarSubquery(
    plan: LogicalPlan,
    outerAttrs: Seq[Expression] = Seq.empty,
    exprId: ExprId = NamedExpression.newExprId,
    joinCond: Seq[Expression] = Seq.empty)
  extends SubqueryExpression(plan, outerAttrs, exprId, joinCond)


abstract class SubqueryExpression(
    plan: LogicalPlan,
    outerAttrs: Seq[Expression],
    exprId: ExprId,
    joinCond: Seq[Expression] = Nil) extends PlanExpression[LogicalPlan]
```



我们先调查execution包下的ScalarSubquery



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

