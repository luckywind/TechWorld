# distinct和groupby的执行计划

```scala
object DistinctGroupby extends App{
  val spark: SparkSession = SparkSession.builder()
    .master("local[1]")
    .appName(this.getClass.getSimpleName)
    .getOrCreate()
  spark.sparkContext.setLogLevel("ERROR")
  import spark.implicits._

  (0 to 10).map(id => (s"id#${id}", s"login${id % 25}"))
    .toDF("id", "login").createTempView("users")
  spark.sql("SELECT login FROM users GROUP BY login").explain(true)
  spark.sql("SELECT DISTINCT(login) FROM users").explain(true)
}

```

发现两者物理计划都是

```scala
== Physical Plan ==
*(2) HashAggregate(keys=[login#8], functions=[], output=[login#8])
+- Exchange hashpartitioning(login#8, 200), ENSURE_REQUIREMENTS, [id=#33]
   +- *(1) HashAggregate(keys=[login#8], functions=[], output=[login#8])
      +- *(1) LocalTableScan [login#8]
```

这是因为spark有一条逻辑优化规则*ReplaceDistinctWithAggregate*会把distinct表达式转化为一个aggregation:

```scala
object ReplaceDistinctWithAggregate extends Rule[LogicalPlan] {
  def apply(plan: LogicalPlan): LogicalPlan = plan transform {
    case Distinct(child) => Aggregate(child.output, child.output, child)
  }
}
```

