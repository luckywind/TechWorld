[参考](https://kontext.tech/article/1153/spark-join-strategy-hints-for-sql-queries)

### hints类型

1. BROADCAST  别名 BROADCASTJOIN 和MAPJOIN
2. MERGE别名SHUFFLE_MERGE和MERGEJOIN
3. SHUFFLE_HASH
4. **SHUFFLE_REPLICATE_NL**

### 使用

```sql
-- Join Hints for broadcast join
SELECT /*+ BROADCAST(t1) */ * FROM t1 INNER JOIN t2 ON t1.id = t2.id;
SELECT /*+ BROADCASTJOIN (t1) */ * FROM t1 left JOIN t2 ON t1.id = t2.id;
SELECT /*+ MAPJOIN(t2) */ * FROM t1 right JOIN t2 ON t1.id = t2.id;

-- Join Hints for shuffle sort merge join
SELECT /*+ SHUFFLE_MERGE(t1) */ * FROM t1 INNER JOIN t2 ON t1.id = t2.id;
SELECT /*+ MERGEJOIN(t2) */ * FROM t1 INNER JOIN t2 ON t1.id = t2.id;
SELECT /*+ MERGE(t1) */ * FROM t1 INNER JOIN t2 ON t1.id = t2.id;

-- Join Hints for shuffle hash join
SELECT /*+ SHUFFLE_HASH(t1) */ * FROM t1 INNER JOIN t2 ON t1.id = t2.id;

-- Join Hints for shuffle-and-replicate nested loop join
SELECT /*+ SHUFFLE_REPLICATE_NL(t1) */ * FROM t1 INNER JOIN t2 ON t1.id = t2.id;

-- When different join strategy hints are specified on both sides of a join, Spark
-- prioritizes the BROADCAST hint over the MERGE hint over the SHUFFLE_HASH hint
-- over the SHUFFLE_REPLICATE_NL hint.
-- Spark will issue Warning in the following example
-- org.apache.spark.sql.catalyst.analysis.HintErrorLogger: Hint (strategy=merge)
-- is overridden by another hint and will not take effect.
SELECT /*+ BROADCAST(t1), MERGE(t1, t2) */ * FROM t1 INNER JOIN t2 ON t1.id = t2.id;

```

