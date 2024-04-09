在SparkSessioin初始化时，加载配置

```scala
  val conf: SparkConf = new SparkConf()
    .set("spark.sql.extensions", "com.nvidia.spark.udf.Plugin")
```

自定义扩展类Plugin，它只注入了一个规则LogicalPlanRules

```scala
class Plugin extends (SparkSessionExtensions => Unit) with Logging {
  override def apply(extensions: SparkSessionExtensions): Unit = {
    logWarning("Installing rapids UDF compiler extensions to Spark. The compiler is disabled" +
        s" by default. To enable it, set `spark.rapids.sql.udfCompiler.enabled` to true")
    extensions.injectResolutionRule(logicalPlanRules)
  }

  def logicalPlanRules(sparkSession: SparkSession): Rule[LogicalPlan] = {
    //load LogicalPlanRules
    ShimLoader.newUdfLogicalPlanRules()
  }
}
```

规则LogicalPlanRules做的事情,替换表达式

```scala
  override def apply(plan: LogicalPlan): LogicalPlan = {
    val conf = new RapidsConf(plan.conf)
    if (conf.isUdfCompilerEnabled) {
      plan match {
        case project: Project =>
          Project(project.projectList.map(e => attemptToReplaceExpression(plan, e))
              .asInstanceOf[Seq[NamedExpression]], apply(project.child))
        case x => {
          x.transformExpressions(replacePartialFunc(plan))
        }
      }
    } else {
      plan
    }
  }
```



