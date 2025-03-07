# SparkSQL中的subqueries

## where 中的等式值

```sql
    val df = spark.sql("select i_item_sk,i_item_id" +
      "  from ds1g.item where i_item_sk=(select i_item_sk from ds1g.item where i_item_sk=1)")
    df.explain(true)
```

```mathematica
== Parsed Logical Plan ==
'Project ['i_item_sk, 'i_item_id]
+- 'Filter ('i_item_sk = scalar-subquery#0 [])
   :  +- 'Project ['i_item_sk]
   :     +- 'Filter ('i_item_sk = 1)
   :        +- 'UnresolvedRelation [ds1g, item], [], false
   +- 'UnresolvedRelation [ds1g, item], [], false

== Analyzed Logical Plan ==
i_item_sk: int, i_item_id: string
Project [i_item_sk#1, i_item_id#2]
+- Filter (i_item_sk#1 = scalar-subquery#0 [])
   :  +- Project [i_item_sk#23]
   :     +- Filter (i_item_sk#23 = 1)
   :        +- SubqueryAlias spark_catalog.ds1g.item
   :           +- Relation ds1g.item[i_item_sk#23,i_item_id#24,i_rec_start_date#25,i_rec_end_date#26,i_item_desc#27,i_current_price#28,i_wholesale_cost#29,i_brand_id#30,i_brand#31,i_class_id#32,i_class#33,i_category_id#34,i_category#35,i_manufact_id#36,i_manufact#37,i_size#38,i_formulation#39,i_color#40,i_units#41,i_container#42,i_manager_id#43,i_product_name#44] parquet
   +- SubqueryAlias spark_catalog.ds1g.item
      +- Relation ds1g.item[i_item_sk#1,i_item_id#2,i_rec_start_date#3,i_rec_end_date#4,i_item_desc#5,i_current_price#6,i_wholesale_cost#7,i_brand_id#8,i_brand#9,i_class_id#10,i_class#11,i_category_id#12,i_category#13,i_manufact_id#14,i_manufact#15,i_size#16,i_formulation#17,i_color#18,i_units#19,i_container#20,i_manager_id#21,i_product_name#22] parquet

== Optimized Logical Plan ==
Project [i_item_sk#1, i_item_id#2]
+- Filter (isnotnull(i_item_sk#1) AND (i_item_sk#1 = scalar-subquery#0 []))
   :  +- Project [i_item_sk#23]
   :     +- Filter (isnotnull(i_item_sk#23) AND (i_item_sk#23 = 1))
   :        +- Relation ds1g.item[i_item_sk#23,i_item_id#24,i_rec_start_date#25,i_rec_end_date#26,i_item_desc#27,i_current_price#28,i_wholesale_cost#29,i_brand_id#30,i_brand#31,i_class_id#32,i_class#33,i_category_id#34,i_category#35,i_manufact_id#36,i_manufact#37,i_size#38,i_formulation#39,i_color#40,i_units#41,i_container#42,i_manager_id#43,i_product_name#44] parquet
   +- Relation ds1g.item[i_item_sk#1,i_item_id#2,i_rec_start_date#3,i_rec_end_date#4,i_item_desc#5,i_current_price#6,i_wholesale_cost#7,i_brand_id#8,i_brand#9,i_class_id#10,i_class#11,i_category_id#12,i_category#13,i_manufact_id#14,i_manufact#15,i_size#16,i_formulation#17,i_color#18,i_units#19,i_container#20,i_manager_id#21,i_product_name#22] parquet

== Physical Plan ==
AdaptiveSparkPlan isFinalPlan=false
+- Filter (isnotnull(i_item_sk#1) AND (i_item_sk#1 = Subquery subquery#0, [id=#12]))
   :  +- Subquery subquery#0, [id=#12]
   :     +- AdaptiveSparkPlan isFinalPlan=false
   :        +- Filter (isnotnull(i_item_sk#23) AND (i_item_sk#23 = 1))
   :           +- FileScan parquet ds1g.item[i_item_sk#23] Batched: true, DataFilters: [isnotnull(i_item_sk#23), (i_item_sk#23 = 1)], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/item], PartitionFilters: [], PushedFilters: [IsNotNull(i_item_sk), EqualTo(i_item_sk,1)], ReadSchema: struct<i_item_sk:int>
   +- FileScan parquet ds1g.item[i_item_sk#1,i_item_id#2] Batched: true, DataFilters: [isnotnull(i_item_sk#1)], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/item], PartitionFilters: [], PushedFilters: [IsNotNull(i_item_sk)], ReadSchema: struct<i_item_sk:int,i_item_id:string>

```



## in 表达式中的

```sql
select i_item_sk,i_item_id from ds1g.item where i_item_sk in (select i_item_sk from ds1g.item where i_item_sk=1)
```

变成了hashJoin

```sql
== Physical Plan ==
AdaptiveSparkPlan isFinalPlan=false
+- BroadcastHashJoin [i_item_sk#912], [i_item_sk#934], LeftSemi, BuildRight, false
   :- FileScan parquet ds1g.item[i_item_sk#912,i_item_id#913] Batched: true, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/item], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<i_item_sk:int,i_item_id:string>
   +- BroadcastExchange HashedRelationBroadcastMode(List(cast(input[0, int, false] as bigint)),false), [id=#20]
      +- Filter (isnotnull(i_item_sk#934) AND (i_item_sk#934 = 1))
         +- FileScan parquet ds1g.item[i_item_sk#934] Batched: true, DataFilters: [isnotnull(i_item_sk#934), (i_item_sk#934 = 1)], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/item], PartitionFilters: [], PushedFilters: [IsNotNull(i_item_sk), EqualTo(i_item_sk,1)], ReadSchema: struct<i_item_sk:int>

```

## project中的

```sql
select  (select max(i_item_sk) from ds1g.item )
```

```sql

== Physical Plan ==
AdaptiveSparkPlan isFinalPlan=false
+- Project [Subquery subquery#911, [id=#20] AS scalarsubquery()#936]
   :  +- Subquery subquery#911, [id=#20]
   :     +- AdaptiveSparkPlan isFinalPlan=false
   :        +- HashAggregate(keys=[], functions=[max(i_item_sk#912)], output=[max(i_item_sk)#935])
   :           +- Exchange SinglePartition, ENSURE_REQUIREMENTS, [id=#18]
   :              +- HashAggregate(keys=[], functions=[partial_max(i_item_sk#912)], output=[max#939])
   :                 +- FileScan parquet ds1g.item[i_item_sk#912] Batched: true, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/item], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<i_item_sk:int>
   +- Scan OneRowRelation[]
```





- Table Subquery

  ```sql
  SELECT *
  FROM   (SELECT *
          FROM   t1) AS tab;    -- 只存在逻辑计划中
  ```

- Scalar Subquery Expressions

  ```sql
  SELECT col2,
         (SELECT Max(col1)
          FROM   t1) AS col1     -- ScalarSubquery表达式
  ```

  

- Subquery in WHERE clause

  ```sql
  SELECT *
  FROM   t1
  WHERE  col1 IN (SELECT col1
                  FROM   t2);      -- ListQuery 转为join
  ```

  

- Correlated Subquery
  子查询使用外查询的列， 只支持等值join

  ```sql
  SELECT *
  FROM   t1 AS t1
  WHERE  EXISTS (SELECT 1
                 FROM   t2 AS t2
                 WHERE  t1.col1 = t2.col1); -- join
  ```

  

- Correlated Scalar Subquery
  子查询使用了外查询的列，且只返回一个列值
  
  ```sql
  SELECT col1,
         COALESCE ((SELECT Max(col2)
                    FROM   t1
                    WHERE  t1.col1 = t2.col1), 0) AS col2
  FROM   t2;                        -- 转为join
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





# ScalarSubquery的一个例子🌰

select *  from p where age=(select age from p where age=30)

```mathematica
== Parsed Logical Plan ==
'Project [*]
+- 'Filter ('age = scalar-subquery#12 [])
   :  +- 'Project ['age]
   :     +- 'Filter ('age = 30)
   :        +- 'UnresolvedRelation [p], [], false
   +- 'UnresolvedRelation [p], [], false

== Analyzed Logical Plan ==
age: bigint, name: string
Project [age#8L, name#9]
+- Filter (age#8L = scalar-subquery#12 [])
   :  +- Project [age#13L]
   :     +- Filter (age#13L = cast(30 as bigint))
   :        +- SubqueryAlias p
   :           +- View (`p`, [age#13L,name#14])
   :              +- Relation [age#13L,name#14] json
   +- SubqueryAlias p
      +- View (`p`, [age#8L,name#9])
         +- Relation [age#8L,name#9] json

== Optimized Logical Plan ==
逻辑计划这里scalar-subquery是catalyst.expressions包下面的SubqueryExpression的子类
Filter (isnotnull(age#8L) AND (age#8L = scalar-subquery#12 []))
:  +- Project [age#13L]
:     +- Filter (isnotnull(age#13L) AND (age#13L = 30))
:        +- Relation [age#13L,name#14] json
+- Relation [age#8L,name#9] json

== Physical Plan ==
到了物理计划这里，scalar-subquery是execution包下面的ExecSubqueryExpression的子类
AdaptiveSparkPlan isFinalPlan=false
+- Filter (isnotnull(age#8L) AND (age#8L = Subquery subquery#12, [id=#18]))
   :  +- Subquery subquery#12, [id=#18]
   :     +- AdaptiveSparkPlan isFinalPlan=false
   :        +- Filter (isnotnull(age#13L) AND (age#13L = 30))
   :           +- FileScan json [age#13L] Batched: false, DataFilters: [isnotnull(age#13L), (age#13L = 30)], Format: JSON, Location: InMemoryFileIndex(1 paths)[file:/Users/chengxingfu/code/open/spark/sourceCode/spark/examples/src/..., PartitionFilters: [], PushedFilters: [IsNotNull(age), EqualTo(age,30)], ReadSchema: struct<age:bigint>
   +- FileScan json [age#8L,name#9] Batched: false, DataFilters: [isnotnull(age#8L)], Format: JSON, Location: InMemoryFileIndex(1 paths)[file:/Users/chengxingfu/code/open/spark/sourceCode/spark/examples/src/..., PartitionFilters: [], PushedFilters: [IsNotNull(age)], ReadSchema: struct<age:bigint,name:string>

```

插件是修改的物理计划树，所以我们先调查execution包下的ScalarSubquery



SparkPlan的executeQuery方法源码：

```scala
  protected final def executeQuery[T](query: => T): T = {
    RDDOperationScope.withScope(sparkContext, nodeName, false, true) {
      prepare()
      waitForSubqueries()
      query
    }
  }
```

首先进行prepare,再waitForSubqueries； 其中prepare会把所有ExecSubqueryExpression类型的表达式当作subquery收集起来，waitForSubqueries会把收集到的subquery提交执行。

本例中，ScalarSubquery出现在Filter算子中，Filter算子在执行时会waitForSubqueries，它会调用ScalarSubquery的updateResult方法

![image-20230912171902049](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230912171902049.png)， 这会触发子查询的执行, 更重要的是获取到了该表达式的执行结果result，从而该表达式计算时只需要把这个result返回即可。

```scala
// case class ScalarSubquery
def updateResult(): Unit = {
    //触发ScalarSubquery的执行
    val rows = plan.executeCollect()
    if (rows.length > 1) {
      sys.error(s"more than one row returned by a subquery used as an expression:\n$plan")
    }
    if (rows.length == 1) {
      assert(rows(0).numFields == 1,
        s"Expects 1 field, but got ${rows(0).numFields}; something went wrong in analysis")
      result = rows(0).get(0, dataType)
    } else {
      // If there is no rows returned, the result should be null.
      result = null
    }
    updated = true
  }
```



调试时注意，打开codegen后，有的接口可能不会走到



[reuse adaptive subquery](https://www.waitingforcode.com/apache-spark-sql/what-new-apache-spark-3-reuse-adaptive-subquery/read)

[Spark-SQL中关于Subquery的处理](https://bbs.huaweicloud.com/blogs/189426)

# Spark对子查询的设计

子查询通常指嵌套在一个查询内部的完整查询，常见的是作为数据源出现在SQL的FROM关键字之后。 Spark2.0版本之后又支持了两种特殊的子查询，即Scalar类型和InSubquery类型

1. Scalar类型的子查询返回单个值，具体又分相关类型(Correlated)和不相关类型(Uncorrelated)。 Uncorrelated意味着子查询和主查询不存在相关性，Uncorrelated会在主查询之前执行。
2. InSubquery类型表示子查询作为过滤谓词，可以出现在EXISTS和IN语句中

通过下面的分析，我们发现

1. FROM关键字之后的嵌套查询只在解析时起到一个前后算子的桥接作用，优化后消失
2. InSubquery类型的子查询被规则RewritePredicateSubquery改为left semi/anti join
3. Correlated Scalar类型的子查询由规则RewriteCorrelatedScalarSubquery重写为left outer join
4. 我们只需处理Uncorrelated Scalar类型的子查询

## SubqueryExpression

### 依赖关系

表达式继承关系：

![image-20230925093549509](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230925093549509.png)

左侧在execution包(表示物理计划)，右侧在expressions包（表示逻辑计划）;  物理计划中的ScalarSubquery和InSubqueryExec表达式都依赖算子来计算结果(具体就是执行算子的executeCollect()方法)，他们依赖的算子的依赖关系如下：

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231007154632656.png" alt="image-20231007154632656" style="zoom:50%;" />







### SubqueryExpression实现类

包含一个逻辑计划的表达式

有5个实现类

1. ScalarSubquery: 只返回单行单列的子查询

2. Exists： 被重写成left semi/anti join

3. DynamicPruningSubquery

   使用join一边的filter过滤另一边的表，在应用分区裁剪时插入的

4. LateralSubquery:  返回多行/列的子查询，优化阶段会被RewriteLateralSubquery重写为join 

5. ListQuery
   只能和in表达式一起使用，作为in的查询范围，只存在于逻辑计划中
   
   ```sql
   SELECT  *
   FROM    a
   WHERE   a.id IN (SELECT  id
                    FROM    b)
   ```
   
   实验
   
   ```sql
   select  * from ds1g.inventory where ds1g.inventory.inv_item_sk in (select i_item_sk from ds1g.item)
   ```
   
   计划树：  逻辑计划中ListQuery被优化为left semi join
   
   ```json
   == Analyzed Logical Plan ==
   inv_date_sk: int, inv_item_sk: int, inv_warehouse_sk: int, inv_quantity_on_hand: int
   Project [inv_date_sk#1, inv_item_sk#2, inv_warehouse_sk#3, inv_quantity_on_hand#4]
   +- Filter inv_item_sk#2 IN (list#0 [])
      :  +- Project [i_item_sk#5]
      :     +- SubqueryAlias spark_catalog.ds1g.item
      :        +- Relation ds1g.item[i_item_sk#5,i_item_id#6,i_rec_start_date#7,i_rec_end_date#8,i_item_desc#9,i_current_price#10,i_wholesale_cost#11,i_brand_id#12,i_brand#13,i_class_id#14,i_class#15,i_category_id#16,i_category#17,i_manufact_id#18,i_manufact#19,i_size#20,i_formulation#21,i_color#22,i_units#23,i_container#24,i_manager_id#25,i_product_name#26] parquet
      +- SubqueryAlias spark_catalog.ds1g.inventory
         +- Relation ds1g.inventory[inv_date_sk#1,inv_item_sk#2,inv_warehouse_sk#3,inv_quantity_on_hand#4] parquet
   
   == Optimized Logical Plan ==
   Join LeftSemi, (inv_item_sk#2 = i_item_sk#5)
   :- Relation ds1g.inventory[inv_date_sk#1,inv_item_sk#2,inv_warehouse_sk#3,inv_quantity_on_hand#4] parquet
   +- Project [i_item_sk#5]
      +- Relation ds1g.item[i_item_sk#5,i_item_id#6,i_rec_start_date#7,i_rec_end_date#8,i_item_desc#9,i_current_price#10,i_wholesale_cost#11,i_brand_id#12,i_brand#13,i_class_id#14,i_class#15,i_category_id#16,i_category#17,i_manufact_id#18,i_manufact#19,i_size#20,i_formulation#21,i_color#22,i_units#23,i_container#24,i_manager_id#25,i_product_name#26] parquet
   
   == Physical Plan ==
   AdaptiveSparkPlan isFinalPlan=false
   +- BroadcastHashJoin [inv_item_sk#2], [i_item_sk#5], LeftSemi, BuildRight, false
      :- FileScan parquet ds1g.inventory[inv_date_sk#1,inv_item_sk#2,inv_warehouse_sk#3,inv_quantity_on_hand#4] Batched: true, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/inventory], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<inv_date_sk:int,inv_item_sk:int,inv_warehouse_sk:int,inv_quantity_on_hand:int>
      +- BroadcastExchange HashedRelationBroadcastMode(List(cast(input[0, int, true] as bigint)),false), [id=#18]
         +- FileScan parquet ds1g.item[i_item_sk#5] Batched: true, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/item], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<i_item_sk:int>
   
   ```
   

<font color=red>看起来只有ScalarSubquery和DynamicPruningSubquery不会被替换为Join,  从依赖关系图分析InSubqueryExec应该就是用于Dynamic Partition Pruning</font> ，[这里](https://git.odin.cse.buffalo.edu/ODIn/spark-instrumented-optimizer/commit/d2a5dad97c0cd9c2b7eede72a0a8df268714155f)也说它只用于DPP。

### ScalarSubquery(execution包)

从签名看，他是一个表达式，只是这个表达式内部有一个BaseSubqueryExec型的算子，该算子有四个实现类，ScalarSubquery在构造时

![image-20230927105143699](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230927105143699.png)

一般是SubqueryExec(AQE开启后，可能是其他类型，由规则PlanAdaptiveSubqueries来创建)

```scala
        ScalarSubquery(
          SubqueryExec.createForScalarSubquery(
            s"scalar-subquery#${subquery.exprId.id}", executedPlan),
          subquery.exprId)
```

SubqueryExec并不允许调用doExecute()，而只允许调用executeCollect()，从而提交job，返回数据。

既然ScalarSubquery是表达式，入口就是eval函数，eval很简单，直接把result返回。但是要先等待updateResult计算完。这个updateResult由waitForSubqueries触发。

```scala
case class ScalarSubquery(
    plan: BaseSubqueryExec,
    exprId: ExprId)
  extends ExecSubqueryExpression with LeafLike[Expression] {

  override def dataType: DataType = plan.schema.fields.head.dataType
  override def nullable: Boolean = true
  override def toString: String = plan.simpleString(SQLConf.get.maxToStringFields)
  override def withNewPlan(query: BaseSubqueryExec): ScalarSubquery = copy(plan = query)

  override lazy val preCanonicalized: Expression = {
    ScalarSubquery(plan.canonicalized.asInstanceOf[BaseSubqueryExec], ExprId(0))
  }

  // the first column in first row from `query`.
  @volatile private var result: Any = _
  @volatile private var updated: Boolean = false

  def updateResult(): Unit = {
    val rows = plan.executeCollect()
    if (rows.length > 1) {
      sys.error(s"more than one row returned by a subquery used as an expression:\n$plan")
    }
    if (rows.length == 1) {
      assert(rows(0).numFields == 1,
        s"Expects 1 field, but got ${rows(0).numFields}; something went wrong in analysis")
      result = rows(0).get(0, dataType)
    } else {
      // If there is no rows returned, the result should be null.
      result = null
    }
    updated = true
  }

  override def eval(input: InternalRow): Any = {
    require(updated, s"$this has not finished")
    result
  }

  override def doGenCode(ctx: CodegenContext, ev: ExprCode): ExprCode = {
    require(updated, s"$this has not finished")
    Literal.create(result, dataType).doGenCode(ctx, ev)
  }
}
```



### InSubqueryExec

#### InSubquery Exepression(逻辑计划)

代表逻辑计划中的IN判断(实验发现这种情况貌似已经被ListQuery给代替了)

```sql
NOT? IN '(' query ')'
```

也可以用在其他场景，例如Runtime Filtering,  Dynamic Partition Pruning

```scala
case class InSubquery(values: Seq[Expression], query: ListQuery)
  extends Predicate with Unevaluable {
```

创建场景：

- [InjectRuntimeFilter](https://books.japila.pl/spark-sql-internals/logical-optimizations/InjectRuntimeFilter/) logical optimization is executed (and [injectInSubqueryFilter](https://books.japila.pl/spark-sql-internals/logical-optimizations/InjectRuntimeFilter/#injectInSubqueryFilter))
- `AstBuilder` is requested to [withPredicate](https://books.japila.pl/spark-sql-internals/sql/AstBuilder/#withPredicate) (for `NOT? IN '(' query ')'` SQL predicate)
- [PlanDynamicPruningFilters](https://books.japila.pl/spark-sql-internals/physical-optimizations/PlanDynamicPruningFilters/) physical optimization is executed (with [spark.sql.optimizer.dynamicPartitionPruning.enabled](https://books.japila.pl/spark-sql-internals/configuration-properties/#spark.sql.optimizer.dynamicPartitionPruning.enabled) enabled，默认true)
- `RowLevelOperationRuntimeGroupFiltering` logical optimization is executed

##### Unevaluable

InSubquery不可执行：

- [RewritePredicateSubquery](https://books.japila.pl/spark-sql-internals/logical-optimizations/RewritePredicateSubquery/): 在逻辑计划优化时转化为left-semi  join(NOT IN 时转为left-anti join)
- [PlanSubqueries](https://books.japila.pl/spark-sql-internals/physical-optimizations/PlanSubqueries/) : 物理计划优化时转为SubqueryExec上的一个InSubqueryExec表达式

#### InSubqueryExec Exepression(物理计划)

代表InSubquery 表达式和[DynamicPruningSubquery](https://books.japila.pl/spark-sql-internals/expressions/DynamicPruningSubquery/)表达式的物理计划

```scala
case class InSubqueryExec(
    child: Expression,
    plan: BaseSubqueryExec,
    exprId: ExprId,
    shouldBroadcast: Boolean = false,
    private var resultBroadcast: Broadcast[Array[Any]] = null,
    @transient private var result: Array[Any] = null)
```



创建场景

- [PlanSubqueries](https://books.japila.pl/spark-sql-internals/physical-optimizations/PlanSubqueries/) physical optimization is executed (and plans [InSubquery](https://books.japila.pl/spark-sql-internals/expressions/InSubquery/) expressions)
- [PlanAdaptiveSubqueries](https://books.japila.pl/spark-sql-internals/physical-optimizations/PlanAdaptiveSubqueries/) physical optimization is executed (and plans [InSubquery](https://books.japila.pl/spark-sql-internals/expressions/InSubquery/) expressions)
- [PlanDynamicPruningFilters](https://books.japila.pl/spark-sql-internals/physical-optimizations/PlanDynamicPruningFilters/) physical optimization is executed (and plans [DynamicPruningSubquery](https://books.japila.pl/spark-sql-internals/expressions/DynamicPruningSubquery/) expressions)





## FROM后的子查询

实验语句

```sql
select max(sk),brd from (select i_item_sk as sk,i_brand as brd from item) group by brd
```

计划树：可以发现FROM后的子查询只在解析时起到一个前后算子的桥接作用，优化后已经消失。

```json
== Parsed Logical Plan ==
'Aggregate ['brd], [unresolvedalias('max('sk), None), 'brd]
+- 'SubqueryAlias __auto_generated_subquery_name
   +- 'Project ['i_item_sk AS sk#0, 'i_brand AS brd#1]
      +- 'UnresolvedRelation [item], [], false

== Analyzed Logical Plan ==
max(sk): int, brd: string
Aggregate [brd#1], [max(sk#0) AS max(sk)#25, brd#1]
+- SubqueryAlias __auto_generated_subquery_name
   +- Project [i_item_sk#2 AS sk#0, i_brand#10 AS brd#1]
      +- SubqueryAlias spark_catalog.ds1g.item
         +- Relation ds1g.item[i_item_sk#2,i_item_id#3,i_rec_start_date#4,i_rec_end_date#5,i_item_desc#6,i_current_price#7,i_wholesale_cost#8,i_brand_id#9,i_brand#10,i_class_id#11,i_class#12,i_category_id#13,i_category#14,i_manufact_id#15,i_manufact#16,i_size#17,i_formulation#18,i_color#19,i_units#20,i_container#21,i_manager_id#22,i_product_name#23] parquet

== Optimized Logical Plan ==
Aggregate [brd#1], [max(sk#0) AS max(sk)#25, brd#1]
+- Project [i_item_sk#2 AS sk#0, i_brand#10 AS brd#1]
   +- Relation ds1g.item[i_item_sk#2,i_item_id#3,i_rec_start_date#4,i_rec_end_date#5,i_item_desc#6,i_current_price#7,i_wholesale_cost#8,i_brand_id#9,i_brand#10,i_class_id#11,i_class#12,i_category_id#13,i_category#14,i_manufact_id#15,i_manufact#16,i_size#17,i_formulation#18,i_color#19,i_units#20,i_container#21,i_manager_id#22,i_product_name#23] parquet

== Physical Plan ==
AdaptiveSparkPlan isFinalPlan=false
+- HashAggregate(keys=[brd#1], functions=[max(sk#0)], output=[max(sk)#25, brd#1])
   +- Exchange hashpartitioning(brd#1, 200), ENSURE_REQUIREMENTS, [id=#16]
      +- HashAggregate(keys=[brd#1], functions=[partial_max(sk#0)], output=[brd#1, max#51])
         +- Project [i_item_sk#2 AS sk#0, i_brand#10 AS brd#1]
            +- FileScan parquet ds1g.item[i_item_sk#2,i_brand#10] Batched: false, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/item], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<i_item_sk:int,i_brand:string>
```

```sql
select sk_cnt, count(1) from (select max(i_item_sk) as sk_cnt,i_brand as brd from item group by brd) group by sk_cnt
```

计划树

```json
== Parsed Logical Plan ==
'Aggregate ['sk_cnt], ['sk_cnt, unresolvedalias('count(1), None)]
+- 'SubqueryAlias __auto_generated_subquery_name
   +- 'Aggregate ['brd], ['max('i_item_sk) AS sk_cnt#0, 'i_brand AS brd#1]
      +- 'UnresolvedRelation [item], [], false

== Analyzed Logical Plan ==
sk_cnt: int, count(1): bigint
Aggregate [sk_cnt#0], [sk_cnt#0, count(1) AS count(1)#26L]
+- SubqueryAlias __auto_generated_subquery_name
   +- Aggregate [i_brand#11], [max(i_item_sk#3) AS sk_cnt#0, i_brand#11 AS brd#1]
      +- SubqueryAlias spark_catalog.ds1g.item
         +- Relation ds1g.item[i_item_sk#3,i_item_id#4,i_rec_start_date#5,i_rec_end_date#6,i_item_desc#7,i_current_price#8,i_wholesale_cost#9,i_brand_id#10,i_brand#11,i_class_id#12,i_class#13,i_category_id#14,i_category#15,i_manufact_id#16,i_manufact#17,i_size#18,i_formulation#19,i_color#20,i_units#21,i_container#22,i_manager_id#23,i_product_name#24] parquet

== Optimized Logical Plan ==
Aggregate [sk_cnt#0], [sk_cnt#0, count(1) AS count(1)#26L]
+- Aggregate [i_brand#11], [max(i_item_sk#3) AS sk_cnt#0]
   +- Project [i_item_sk#3, i_brand#11]
      +- Relation ds1g.item[i_item_sk#3,i_item_id#4,i_rec_start_date#5,i_rec_end_date#6,i_item_desc#7,i_current_price#8,i_wholesale_cost#9,i_brand_id#10,i_brand#11,i_class_id#12,i_class#13,i_category_id#14,i_category#15,i_manufact_id#16,i_manufact#17,i_size#18,i_formulation#19,i_color#20,i_units#21,i_container#22,i_manager_id#23,i_product_name#24] parquet

== Physical Plan ==
AdaptiveSparkPlan isFinalPlan=false
+- HashAggregate(keys=[sk_cnt#0], functions=[count(1)], output=[sk_cnt#0, count(1)#26L])
   +- Exchange hashpartitioning(sk_cnt#0, 200), ENSURE_REQUIREMENTS, [id=#25]
      +- HashAggregate(keys=[sk_cnt#0], functions=[partial_count(1)], output=[sk_cnt#0, count#52L])
         +- HashAggregate(keys=[i_brand#11], functions=[max(i_item_sk#3)], output=[sk_cnt#0])
            +- Exchange hashpartitioning(i_brand#11, 200), ENSURE_REQUIREMENTS, [id=#21]
               +- HashAggregate(keys=[i_brand#11], functions=[partial_max(i_item_sk#3)], output=[i_brand#11, max#54])
                  +- FileScan parquet ds1g.item[i_item_sk#3,i_brand#11] Batched: false, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/item], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<i_item_sk:int,i_brand:string>

```





## 子查询解析

子查询分为CTE里的子查询和表达式里的子查询，CTE里的子查询由CTE场景处理，这里指表达式里的子查询

### ResolveSubquery



## 逻辑计划优化

### OptimizeSubqueries优化规则

是Subquery Batch里的唯一的规则。当SQL语句包含子查询时，会在逻辑算子树上生成SubqueryExpression表达式，这个规则递归对SubqueryExpression表达式的子计划调用Optimizer并进行优化。

```scala
  object OptimizeSubqueries extends Rule[LogicalPlan] {
    private def removeTopLevelSort(plan: LogicalPlan): LogicalPlan = {
      if (!plan.containsPattern(SORT)) {
        return plan
      }
      plan match {
        case Sort(_, _, child) => child
        case Project(fields, child) => Project(fields, removeTopLevelSort(child))
        case other => other
      }
    }
    def apply(plan: LogicalPlan): LogicalPlan = plan.transformAllExpressionsWithPruning(
      _.containsPattern(PLAN_EXPRESSION), ruleId) {
      case s: SubqueryExpression =>
      //对子查询表达式里的计划树进行优化，丢掉排序等
        val Subquery(newPlan, _) = Optimizer.this.execute(Subquery.fromExpression(s))
        // At this point we have an optimized subquery plan that we are going to attach
        // to this subquery expression. Here we can safely remove any top level sort
        // in the plan as tuples produced by a subquery are un-ordered.
        s.withNewPlan(removeTopLevelSort(newPlan))
    }
  }
```



### RewritePredicateSubquery规则

把InSubquery类型的子查询改写成left semi/anti join，所以这种子查询无需我们卸载。例如

EXISTS/NOT EXISTS 和 IN/NOT IN

1. Correlated condition

```sql
SELECT  *
FROM    a
WHERE   EXISTS (SELECT  *
                FROM    b
                WHERE   b.id = a.id)
```

2. Uncorrelated codition

```sql
SELECT  *
FROM    a
WHERE   EXISTS (SELECT  *
                FROM    b
                WHERE   b.id > 10)
```



### RewriteCorrelatedScalarSubquery规则

会把Correlated类型的ScalarSubquery重写为left outer join

## 逻辑计划转物理计划

### PlanSubqueries规则


spark在prepareForExecution获得可执行物理计划时会应用一批规则，其中有一个规则PlanSubqueries，会对Scalarubquery以及InSubquery进一步处理。会将subquery中的plan，再一次经过parser、analyzer、optimizer得到物理执行计划(executedPlan)，然后封装为ScalarSubquery和InSubqueryExec算子型表达式。

```scala
case class PlanSubqueries(sparkSession: SparkSession) extends Rule[SparkPlan] {
  def apply(plan: SparkPlan): SparkPlan = {
    //对当前算子下的表达式进行替换
    plan.transformAllExpressionsWithPruning(_.containsAnyPattern(SCALAR_SUBQUERY, IN_SUBQUERY)) {
      case subquery: expressions.ScalarSubquery =>
        //1)如果是一个ScalarSubquery表达式，那么先拿到它的逻辑计划， 接着转换成物理计划以及prepareExecutedPlan，得到可执行物理计划
        val executedPlan = QueryExecution.prepareExecutedPlan(sparkSession, subquery.plan)
          //3)把SubqueryExec算子包装成一个表达式
        ScalarSubquery(
              //2)在物理计划上加一个SubqueryExec算子
          SubqueryExec.createForScalarSubquery(
            s"scalar-subquery#${subquery.exprId.id}", executedPlan),
          subquery.exprId)
      case expressions.InSubquery(values, ListQuery(query, _, exprId, _, _)) =>
          //InSubquery表达式
        val expr = if (values.length == 1) {
          values.head
        } else {
          CreateNamedStruct(
            values.zipWithIndex.flatMap { case (v, index) =>
              Seq(Literal(s"col_$index"), v)
            }
          )
        }
          // 同样把子查询创建物理计划
        val executedPlan = QueryExecution.prepareExecutedPlan(sparkSession, query)
         //  把物理计划上加一个subQueryExec算子， 再封装为一个表达式
        InSubqueryExec(expr, SubqueryExec(s"subquery#${exprId.id}", executedPlan), exprId)
    }
  }
}
```



# tpcds-query中的subquery

## q06物理计划中的ScalarSubquery

```sql
spark.sql("""
SELECT state, cnt FROM (
 SELECT a.ca_state state, count(*) cnt
 FROM
    customer_address a, customer c, store_sales s, date_dim d, item i
 WHERE a.ca_address_sk = c.c_current_addr_sk
   AND c.c_customer_sk = s.ss_customer_sk
   AND s.ss_sold_date_sk = d.d_date_sk
   AND s.ss_item_sk = i.i_item_sk
   AND d.d_month_seq =
      (SELECT distinct (d_month_seq) FROM date_dim
        WHERE d_year = 2001 AND d_moy = 1)
   AND i.i_current_price > 1.2 *
             (SELECT avg(j.i_current_price) FROM item j
               WHERE j.i_category = i.i_category)
 GROUP BY a.ca_state
) x
WHERE cnt >= 10
ORDER BY cnt LIMIT 100
""").collect()
```

1. 条件  (SELECT avg(j.i_current_price) FROM item j
                  WHERE j.i_category = i.i_category)  是一个Corelated ScalarSubquery，逻辑计划阶段被改写为Join了。

2. 条件

```sql
d.d_month_seq =
      (SELECT distinct (d_month_seq) FROM date_dim
        WHERE d_year = 2001 AND d_moy = 1)
```

对应的物理计划树如下，子查询使用ScalarSubquery封装，它在计划树中的文字描述是Subquery subquery#297，其中Subquery指的是它包含的SubqueryExec plan的表示，“subquery#297”指的是该plan的名字。

![image-20230925171610665](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230925171610665.png)

其会阻止Filter的卸载：

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231123142430204.png" alt="image-20231123142430204" style="zoom:33%;" />



