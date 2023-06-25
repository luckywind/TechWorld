我们关掉规则DecimalAggregates

```sql
spark.conf.set("spark.sql.optimizer.excludedRules", "org.apache.spark.sql.catalyst.optimizer.DecimalAggregates")
```



# 规则DecimalAggregates

聚合表达式

1. 如果是decimal的sum，且prec <=5，则会优化为UnscaledValue的sum
2. 如果是decimal的avg，且prec <=11，则会优化为UnscaledValue的avg

```scala
object DecimalAggregates extends Rule[LogicalPlan] {
  import Decimal.MAX_LONG_DIGITS

  /** Maximum number of decimal digits representable precisely in a Double */
  private val MAX_DOUBLE_DIGITS = 15

  def apply(plan: LogicalPlan): LogicalPlan = plan.transformWithPruning(
    _.containsAnyPattern(SUM, AVERAGE), ruleId) {
    case q: LogicalPlan => q.transformExpressionsDownWithPruning(
      _.containsAnyPattern(SUM, AVERAGE), ruleId) {
      case we @ WindowExpression(ae @ AggregateExpression(af, _, _, _, _), _) => af match {
        case Sum(e @ DecimalType.Expression(prec, scale), _) if prec + 10 <= MAX_LONG_DIGITS =>
          MakeDecimal(we.copy(windowFunction = ae.copy(aggregateFunction = Sum(UnscaledValue(e)))),
            prec + 10, scale)

        case Average(e @ DecimalType.Expression(prec, scale), _) if prec + 4 <= MAX_DOUBLE_DIGITS =>
          val newAggExpr =
            we.copy(windowFunction = ae.copy(aggregateFunction = Average(UnscaledValue(e))))
          Cast(
            Divide(newAggExpr, Literal.create(math.pow(10.0, scale), DoubleType)),
            DecimalType(prec + 4, scale + 4), Option(conf.sessionLocalTimeZone))

        case _ => we
      }
      //聚合表达式
      case ae @ AggregateExpression(af, _, _, _, _) => af match {
         //如果是decimal的sum，且prec <=5，则会优化为UnscaledValue的sum
        case Sum(e @ DecimalType.Expression(prec, scale), _) if prec + 10 <= MAX_LONG_DIGITS =>
          MakeDecimal(ae.copy(aggregateFunction = Sum(UnscaledValue(e))), prec + 10, scale)
         //如果是decimal的avg，且prec <=11，则会优化为UnscaledValue的avg
        case Average(e @ DecimalType.Expression(prec, scale), _) if prec + 4 <= MAX_DOUBLE_DIGITS =>
          val newAggExpr = ae.copy(aggregateFunction = Average(UnscaledValue(e)))
          Cast(
            Divide(newAggExpr, Literal.create(math.pow(10.0, scale), DoubleType)),
            DecimalType(prec + 4, scale + 4), Option(conf.sessionLocalTimeZone))

        case _ => ae
      }
    }
  }
}
```



## sql语句

select avg(w_gmt_offset) from tpcds1gv.warehouse

w_gmt_offset:decimal(5,2)

## 原始plan

```scala
scala> spark.sql("select avg(w_gmt_offset) from tpcds1gv.warehouse").explain(true)
== Optimized Logical Plan ==
Aggregate [cast((avg(UnscaledValue(w_gmt_offset#63)) / 100.0) as decimal(14,6)) AS avg(w_gmt_offset)#65]
+- Project [w_gmt_offset#63]
   +- Relation tpcds1gv.warehouse[w_warehouse_sk#50,w_warehouse_id#51,w_warehouse_name#52,w_warehouse_sq_ft#53,w_street_number#54,w_street_name#55,w_street_type#56,w_suite_number#57,w_city#58,w_county#59,w_state#60,w_zip#61,w_country#62,w_gmt_offset#63] parquet

== Physical Plan ==
AdaptiveSparkPlan isFinalPlan=false
+- HashAggregate(keys=[], functions=[avg(UnscaledValue(w_gmt_offset#63))], output=[avg(w_gmt_offset)#65])
   +- Exchange SinglePartition, ENSURE_REQUIREMENTS, [id=#33]
      +- HashAggregate(keys=[], functions=[partial_avg(UnscaledValue(w_gmt_offset#63))], output=[sum#69, count#70L])
         +- FileScan parquet tpcds1gv.warehouse[w_gmt_offset#63] Batched: true, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/tpcds1gv.db/warehouse], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<w_gmt_offset:decimal(10,2)>
```

### 关掉规则

关掉后，没有出现UnscaledValue

```scala
== Optimized Logical Plan ==
Aggregate [avg(w_gmt_offset#54) AS avg(w_gmt_offset)#56]
+- Project [w_gmt_offset#54]
   +- Relation tpcds1gv.warehouse[w_warehouse_sk#41,w_warehouse_id#42,w_warehouse_name#43,w_warehouse_sq_ft#44,w_street_number#45,w_street_name#46,w_street_type#47,w_suite_number#48,w_city#49,w_county#50,w_state#51,w_zip#52,w_country#53,w_gmt_offset#54] parquet

== Physical Plan ==
AdaptiveSparkPlan isFinalPlan=false
+- HashAggregate(keys=[], functions=[avg(w_gmt_offset#54)], output=[avg(w_gmt_offset)#56])
   +- Exchange SinglePartition, ENSURE_REQUIREMENTS, [id=#48]
      +- HashAggregate(keys=[], functions=[partial_avg(w_gmt_offset#54)], output=[sum#60, count#61L])
         +- FileScan parquet tpcds1gv.warehouse[w_gmt_offset#54] Batched: true, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/tpcds1gv.db/warehouse], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<w_gmt_offset:decimal(10,2)>
```



## 卸载后的plan

出现了UnscaledValue，说明命中了规则

```scala
== Optimized Logical Plan ==
Aggregate [cast((avg(UnscaledValue(w_gmt_offset#81)) / 100.0) as decimal(14,6)) AS avg(w_gmt_offset)#83]
+- Project [w_gmt_offset#81]
   +- Relation tpcds1gv.warehouse[w_warehouse_sk#68,w_warehouse_id#69,w_warehouse_name#70,w_warehouse_sq_ft#71,w_street_number#72,w_street_name#73,w_street_type#74,w_suite_number#75,w_city#76,w_county#77,w_state#78,w_zip#79,w_country#80,w_gmt_offset#81] parquet

== Physical Plan ==
DpuColumnarToRow [avg(w_gmt_offset)#83]
+- DpuAdaptiveSparkPlan isFinalPlan=false
   +- HashAggregate(keys=[], functions=[avg(UnscaledValue(w_gmt_offset#81))], output=[avg(w_gmt_offset)#83])
      +- Exchange SinglePartition, ENSURE_REQUIREMENTS, [id=#94]
         +- HashAggregate(keys=[], functions=[partial_avg(UnscaledValue(w_gmt_offset#81))], output=[sum#87, count#88L])
            +- FileScan parquet tpcds1gv.warehouse[w_gmt_offset#81] Batched: true, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/tpcds1gv.db/warehouse], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<w_gmt_offset:decimal(10,2)>
```

这里的执行计划并非最终执行的物理计划，我们从SQL UI上看到的最终的物理计划是这样的：

```scala
      DpuRowToColumnar (7)
      +- * HashAggregate (6)
         +- ShuffleQueryStage (5)
            +- Exchange (4)
               +- * HashAggregate (3)
                  +- DpuColumnarToRow (2)
                     +- Scan parquet tpcds1gv.warehouse (1)
```

也就是并未完全卸载

### 关掉规则后的plan

没有出现UnscaledValue，说明规则没有命中

```scala

== Optimized Logical Plan ==
Aggregate [avg(w_gmt_offset#129) AS avg(w_gmt_offset)#131]
+- Project [w_gmt_offset#129]
   +- Relation tpcds1gv.warehouse[w_warehouse_sk#116,w_warehouse_id#117,w_warehouse_name#118,w_warehouse_sq_ft#119,w_street_number#120,w_street_name#121,w_street_type#122,w_suite_number#123,w_city#124,w_county#125,w_state#126,w_zip#127,w_country#128,w_gmt_offset#129] parquet

== Physical Plan ==
DpuColumnarToRow [avg(w_gmt_offset)#131]
+- DpuAdaptiveSparkPlan isFinalPlan=false
   +- HashAggregate(keys=[], functions=[avg(w_gmt_offset#129)], output=[avg(w_gmt_offset)#131])
      +- Exchange SinglePartition, ENSURE_REQUIREMENTS, [id=#190]
         +- HashAggregate(keys=[], functions=[partial_avg(w_gmt_offset#129)], output=[sum#135, count#136L])
            +- FileScan parquet tpcds1gv.warehouse[w_gmt_offset#129] Batched: true, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/tpcds1gv.db/warehouse], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<w_gmt_offset:decimal(10,2)>

```

查看SQL  UI发现全部卸载

```sql
== Physical Plan ==
DpuColumnarToRow (11)
+- DpuAdaptiveSparkPlan (10)
   +- == Final Plan ==
      DpuHashAggregate (5)
      +- ShuffleQueryStage (4)
         +- DpuColumnarExchange (3)
            +- DpuHashAggregate (2)
               +- Scan parquet tpcds1gv.warehouse (1)
```



