# hint位置

写到子查询外面也行，但只能使用外面的表,而且会提示非join的表

```scala
    val df = spark.sql(
      """
        |select /*+ BROADCAST(ss) */ ss_sold_date_sk, count(1) cnt  from  ds1g.store_sales ss
        |  where not exists(select  1 from  ds1g.date_dim dd where
        |   d_date_sk=2415022
        |  and ss.ss_sold_date_sk=dd.d_date_sk )
        |group by ss_sold_date_sk
        |""".stripMargin)
```

23/10/13 16:28:53 WARN HintErrorLogger: A join hint (strategy=broadcast) is specified but it is not part of a join relation.





子查询里找不到外面的表

```scala
    val df = spark.sql(
      """
        |select  ss_sold_date_sk, count(1) cnt  from  ds1g.store_sales ss
        |  where not exists(select /*+ BROADCAST(ss) */ 1 from  ds1g.date_dim dd where
        |   d_date_sk=2415022
        |  and ss.ss_sold_date_sk=dd.d_date_sk )
        |group by ss_sold_date_sk
        |""".stripMargin)
```

23/10/13 16:30:20 WARN HintErrorLogger: Count not find relation 'ss' specified in hint 'BROADCAST(ss)'

子查询里写自己的表也提示非join的表

```scala
    val df = spark.sql(
      """
        |select  ss_sold_date_sk, count(1) cnt  from  ds1g.store_sales ss
        |  where not exists(select /*+ BROADCAST(dd) */ 1 from  ds1g.date_dim dd where
        |   d_date_sk=2415022
        |  and ss.ss_sold_date_sk=dd.d_date_sk )
        |group by ss_sold_date_sk
        |""".stripMargin)
```

23/10/13 16:31:11 WARN HintErrorLogger: A join hint (strategy=broadcast) is specified but it is not part of a join relation

> 能不能找到表直接搜索HintErrorLogger

# 物理计划中决定join策略

```json
== Parsed Logical Plan ==
'Aggregate ['ss_sold_date_sk], ['ss_sold_date_sk, 'count(1) AS cnt#0]
+- 'Filter NOT exists#1 []
   :  +- 'Project [unresolvedalias(1, None)]
   :     +- 'Filter (('d_date_sk = 2415022) AND ('ss.ss_sold_date_sk = 'dd.d_date_sk))
   :        +- 'SubqueryAlias dd
   :           +- 'UnresolvedRelation [ds1g, date_dim], [], false
   +- 'SubqueryAlias ss
      +- 'UnresolvedRelation [ds1g, store_sales], [], false

== Analyzed Logical Plan ==
ss_sold_date_sk: int, cnt: bigint
Aggregate [ss_sold_date_sk#3], [ss_sold_date_sk#3, count(1) AS cnt#0L]
+- Filter NOT exists#1 [ss_sold_date_sk#3]
   :  +- Project [1 AS 1#83]
   :     +- Filter ((d_date_sk#26 = 2415022) AND (outer(ss_sold_date_sk#3) = d_date_sk#26))
   :        +- SubqueryAlias dd
   :           +- SubqueryAlias spark_catalog.ds1g.date_dim
   :              +- Relation ds1g.date_dim[d_date_sk#26,d_date_id#27,d_date#28,d_month_seq#29,d_week_seq#30,d_quarter_seq#31,d_year#32,d_dow#33,d_moy#34,d_dom#35,d_qoy#36,d_fy_year#37,d_fy_quarter_seq#38,d_fy_week_seq#39,d_day_name#40,d_quarter_name#41,d_holiday#42,d_weekend#43,d_following_holiday#44,d_first_dom#45,d_last_dom#46,d_same_day_ly#47,d_same_day_lq#48,d_current_day#49,... 4 more fields] parquet
   +- SubqueryAlias ss
      +- SubqueryAlias spark_catalog.ds1g.store_sales
         +- Relation ds1g.store_sales[ss_sold_date_sk#3,ss_sold_time_sk#4,ss_item_sk#5,ss_customer_sk#6,ss_cdemo_sk#7,ss_hdemo_sk#8,ss_addr_sk#9,ss_store_sk#10,ss_promo_sk#11,ss_ticket_number#12,ss_quantity#13,ss_wholesale_cost#14,ss_list_price#15,ss_sales_price#16,ss_ext_discount_amt#17,ss_ext_sales_price#18,ss_ext_wholesale_cost#19,ss_ext_list_price#20,ss_ext_tax#21,ss_coupon_amt#22,ss_net_paid#23,ss_net_paid_inc_tax#24,ss_net_profit#25] parquet

== Optimized Logical Plan ==
Aggregate [ss_sold_date_sk#3], [ss_sold_date_sk#3, count(1) AS cnt#0L]
+- Join LeftAnti, (ss_sold_date_sk#3 = d_date_sk#26)
   :- Project [ss_sold_date_sk#3]
   :  +- Relation ds1g.store_sales[ss_sold_date_sk#3,ss_sold_time_sk#4,ss_item_sk#5,ss_customer_sk#6,ss_cdemo_sk#7,ss_hdemo_sk#8,ss_addr_sk#9,ss_store_sk#10,ss_promo_sk#11,ss_ticket_number#12,ss_quantity#13,ss_wholesale_cost#14,ss_list_price#15,ss_sales_price#16,ss_ext_discount_amt#17,ss_ext_sales_price#18,ss_ext_wholesale_cost#19,ss_ext_list_price#20,ss_ext_tax#21,ss_coupon_amt#22,ss_net_paid#23,ss_net_paid_inc_tax#24,ss_net_profit#25] parquet
   +- Project [d_date_sk#26]
      +- Filter (isnotnull(d_date_sk#26) AND (d_date_sk#26 = 2415022))
         +- Relation ds1g.date_dim[d_date_sk#26,d_date_id#27,d_date#28,d_month_seq#29,d_week_seq#30,d_quarter_seq#31,d_year#32,d_dow#33,d_moy#34,d_dom#35,d_qoy#36,d_fy_year#37,d_fy_quarter_seq#38,d_fy_week_seq#39,d_day_name#40,d_quarter_name#41,d_holiday#42,d_weekend#43,d_following_holiday#44,d_first_dom#45,d_last_dom#46,d_same_day_ly#47,d_same_day_lq#48,d_current_day#49,... 4 more fields] parquet

== Physical Plan ==
AdaptiveSparkPlan isFinalPlan=true
+- == Final Plan ==
   HashAggregate(keys=[ss_sold_date_sk#3], functions=[count(1)], output=[ss_sold_date_sk#3, cnt#0L])
   +- AQEShuffleRead coalesced
      +- ShuffleQueryStage 1
         +- Exchange hashpartitioning(ss_sold_date_sk#3, 200), ENSURE_REQUIREMENTS, [id=#46]
            +- HashAggregate(keys=[ss_sold_date_sk#3], functions=[partial_count(1)], output=[ss_sold_date_sk#3, count#110L])
               +- BroadcastHashJoin [ss_sold_date_sk#3], [d_date_sk#26], LeftAnti, BuildRight, false
                  :- FileScan parquet ds1g.store_sales[ss_sold_date_sk#3] Batched: false, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/store_sales], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<ss_sold_date_sk:int>
                  +- BroadcastQueryStage 0
                     +- BroadcastExchange HashedRelationBroadcastMode(List(cast(input[0, int, false] as bigint)),false), [id=#26]
                        +- Filter (isnotnull(d_date_sk#26) AND (d_date_sk#26 = 2415022))
                           +- FileScan parquet ds1g.date_dim[d_date_sk#26] Batched: false, DataFilters: [isnotnull(d_date_sk#26), (d_date_sk#26 = 2415022)], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/date_dim], PartitionFilters: [], PushedFilters: [IsNotNull(d_date_sk), EqualTo(d_date_sk,2415022)], ReadSchema: struct<d_date_sk:int>
+- == Initial Plan ==
   HashAggregate(keys=[ss_sold_date_sk#3], functions=[count(1)], output=[ss_sold_date_sk#3, cnt#0L])
   +- Exchange hashpartitioning(ss_sold_date_sk#3, 200), ENSURE_REQUIREMENTS, [id=#30]
      +- HashAggregate(keys=[ss_sold_date_sk#3], functions=[partial_count(1)], output=[ss_sold_date_sk#3, count#110L])
         +- BroadcastHashJoin [ss_sold_date_sk#3], [d_date_sk#26], LeftAnti, BuildRight, false
            :- FileScan parquet ds1g.store_sales[ss_sold_date_sk#3] Batched: false, DataFilters: [], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/store_sales], PartitionFilters: [], PushedFilters: [], ReadSchema: struct<ss_sold_date_sk:int>
            +- BroadcastExchange HashedRelationBroadcastMode(List(cast(input[0, int, false] as bigint)),false), [id=#26]
               +- Filter (isnotnull(d_date_sk#26) AND (d_date_sk#26 = 2415022))
                  +- FileScan parquet ds1g.date_dim[d_date_sk#26] Batched: false, DataFilters: [isnotnull(d_date_sk#26), (d_date_sk#26 = 2415022)], Format: Parquet, Location: InMemoryFileIndex(1 paths)[hdfs://master:9000/user/hive/warehouse/ds1g.db/date_dim], PartitionFilters: [], PushedFilters: [IsNotNull(d_date_sk), EqualTo(d_date_sk,2415022)], ReadSchema: struct<d_date_sk:int>

end
```

