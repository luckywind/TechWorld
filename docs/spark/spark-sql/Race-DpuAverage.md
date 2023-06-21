# 查询计划

```scala
scala> spark.sql("select avg(w_warehouse_sq_ft) from tpcds1gv.warehouse").explain(true)
== Parsed Logical Plan ==
'Project [unresolvedalias('avg('w_warehouse_sq_ft), None)]
+- 'UnresolvedRelation [tpcds1gv, warehouse], [], false
 
== Analyzed Logical Plan ==
avg(w_warehouse_sq_ft): decimal(18,1)
Aggregate [avg(w_warehouse_sq_ft#48) AS avg(w_warehouse_sq_ft)#60]
+- SubqueryAlias spark_catalog.tpcds1gv.warehouse
   +- Relation tpcds1gv.warehouse[w_warehouse_sk#45,w_warehouse_id#46,w_warehouse_name#47,w_warehouse_sq_ft#48,w_street_number#49,w_street_name#50,w_street_type#51,w_suite_number#52,w_city#53,w_county#54,w_state#55,w_zip#56,w_country#57,w_gmt_offset#58] parquet
 
== Optimized Logical Plan ==
Aggregate [avg(w_warehouse_sq_ft#48) AS avg(w_warehouse_sq_ft)#60]
+- Project [w_warehouse_sq_ft#48]
   +- Relation tpcds1gv.warehouse[w_warehouse_sk#45,w_warehouse_id#46,w_warehouse_name#47,w_warehouse_sq_ft#48,w_street_number#49,w_street_name#50,w_street_type#51,w_suite_number#52,w_city#53,w_county#54,w_state#55,w_zip#56,w_country#57,w_gmt_offset#58] parquet
 
== Physical Plan ==
DpuColumnarToRow [avg(w_warehouse_sq_ft)#60]
+- DpuAdaptiveSparkPlan isFinalPlan=false
   +- HashAggregate(keys=[], functions=[avg(w_warehouse_sq_ft#48)], output=[avg(w_warehouse_sq_ft)#60])
      +- Exchange SinglePartition, ENSURE_REQUIREMENTS, [id=#69]
         +- HashAggregate(keys=[], functions=[partial_avg(w_warehouse_sq_ft#48)], output=[sum#64, count#65L])
            +- FileScan parquet tpcds1gv.warehouse[w_warehouse_sq_ft#48] Batched: true, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/tpcds1gv.db/warehouse], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<w_warehouse_sq_ft:int>

```

