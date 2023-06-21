# SparkSessionExtensions

## 简介

在Spark2.2版本中，引入了新的扩展点，使得用户可以在Spark session中自定义自己的parser，analyzer，optimizer以及physical planning stragegy rule。[参考](https://developer.aliyun.com/article/672130#slide-11)

SparkSessionExtensions保存了所有用户自定义的扩展规则，自定义规则保存在成员变量中，对于不同阶段的自定义规则，SparkSessionExtensions提供了不同的接口。

当前提供下列扩展点

- Analyzer Rules.
- Check Analysis Rules.
- Cache Plan Normalization Rules.
- Optimizer Rules.
- Pre CBO Rules.
- Planning Strategies.
- Customized Parser.
- (External) Catalog listeners.
- Columnar Rules.
- Adaptive Query Stage Preparation Rules.
- Adaptive Query Execution Runtime Optimizer Rules.

这些扩展可以调用withExtensions,它是个高阶函数，接受一个自定义函数作为参数，这个自定义函数以SparkSessionExtensions作为参数，用户可以实现这个函数，通过SparkSessionExtensions的inject开头的方法添加用户自定义规则。

```scala
SparkSession.builder()
     .master("...")
     .config("...", true)
     .withExtensions { extensions =>
       extensions.injectResolutionRule { session =>
         ...
       }
       extensions.injectParser { (session, parser) =>
         ...
       }
     }
     .getOrCreate()
```

也可以使用配置属性`spark.sql.extensions`， 多个配置用逗号分隔：

```scala
SparkSession.builder()
     .master("...")
     .config("spark.sql.extensions", "org.example.MyExtensions,org.example.YourExtensions")
     .getOrCreate()

   class MyExtensions extends Function1[SparkSessionExtensions, Unit] {
     override def apply(extensions: SparkSessionExtensions): Unit = {
       extensions.injectResolutionRule { session =>
         ...
       }
       extensions.injectParser { (session, parser) =>
         ...
       }
     }
   }

   class YourExtensions extends SparkSessionExtensionsProvider {
     override def apply(extensions: SparkSessionExtensions): Unit = {
       extensions.injectResolutionRule { session =>
         ...
       }
       extensions.injectFunction(...)
     }
   }

trait SparkSessionExtensionsProvider extends Function1[SparkSessionExtensions, Unit]
```



## inject接口

- injectOptimizerRule – 添加optimizer自定义规则，optimizer负责逻辑执行计划的优化。
- injectParser – 添加parser自定义规则，parser负责SQL解析。
- injectPlannerStrategy – 添加planner strategy自定义规则，planner负责物理执行计划的生成。
- injectResolutionRule – 添加Analyzer自定义规则到Resolution阶段，analyzer负责逻辑执行计划生成。
- injectPostHocResolutionRule – 添加Analyzer自定义规则到Post Resolution阶段。
- injectCheckRule – 添加Analyzer自定义Check规则。









### SparkSessionExtensionsProvider

SparkSessionExtensionsProvider是[`SparkSessionExtensions`](https://spark.apache.org/docs/latest/api/java/org/apache/spark/sql/SparkSessionExtensions.html) 的实现的基础trait，例如我们有一个外部函数Age，想要注册为SparkSession的一个扩展

```scala
package org.apache.spark.examples.extensions

   import org.apache.spark.sql.catalyst.expressions.{CurrentDate, Expression, RuntimeReplaceable, SubtractDates}

   case class Age(birthday: Expression, child: Expression) extends RuntimeReplaceable {

     def this(birthday: Expression) = this(birthday, SubtractDates(CurrentDate(), birthday))
     override def exprsReplaced: Seq[Expression] = Seq(birthday)
     override protected def withNewChildInternal(newChild: Expression): Expression = copy(newChild)
   }
```

我们需要继承SparkSessionExtensionsProvider来创建一个扩展

```scala
package org.apache.spark.examples.extensions

   import org.apache.spark.sql.{SparkSessionExtensions, SparkSessionExtensionsProvider}
   import org.apache.spark.sql.catalyst.FunctionIdentifier
   import org.apache.spark.sql.catalyst.expressions.{Expression, ExpressionInfo}

   class MyExtensions extends SparkSessionExtensionsProvider {
     override def apply(v1: SparkSessionExtensions): Unit = {
       v1.injectFunction(
         (new FunctionIdentifier("age"),
           new ExpressionInfo(classOf[Age].getName, "age"),
           (children: Seq[Expression]) => new Age(children.head)))
     }
   }
```

我们有三种方式注入扩展

- withExtensions of [`SparkSession.Builder`](https://spark.apache.org/docs/latest/api/java/org/apache/spark/sql/SparkSession.Builder.html)
- Config - spark.sql.extensions
- `ServiceLoader` - Add to src/main/resources/META-INF/services/org.apache.spark.sql.SparkSessionExtensionsProvider

### RuntimeReplaceable

```
 An expression that gets replaced at runtime (currently by the optimizer) into a different
 expression for evaluation. This is mainly used to provide compatibility with other databases.
 For example, we use this to support "nvl" by replacing it with "coalesce".
```

## 加载逻辑

SparkSession初始化

```scala
        loadExtensions(extensions) //加载ServiceLoader，我们可不管
        applyExtensions(
          sparkContext.getConf.get(StaticSQLConf.SPARK_SESSION_EXTENSIONS).getOrElse(Seq.empty),
          extensions)
```

applyExtensions会实例化扩展并调用其apply到extensions，我们的扩展在apply里调用injectOptimizerRule注册了规则

```scala
  def injectOptimizerRule(builder: RuleBuilder): Unit = {
    optimizerRules += builder
  }
```



自定义的规则将会追加在operatorOptimizationRuleSet的后面，而operatorOptimizationRuleSet构成的Batch会在Infer Filters前后出现两次：

operatorOptimizationBatch

```scala
    val operatorOptimizationBatch: Seq[Batch] = {
      // Infer Filters前面执行一次
      Batch("Operator Optimization before Inferring Filters", fixedPoint,
        operatorOptimizationRuleSet: _*) ::
      Batch("Infer Filters", Once,
        InferFiltersFromGenerate,
        InferFiltersFromConstraints) ::
      // Infer Filters后面又执行一次
      Batch("Operator Optimization after Inferring Filters", fixedPoint,
        operatorOptimizationRuleSet: _*) ::
      // Set strategy to Once to avoid pushing filter every time because we do not change the
      // join condition.
      Batch("Push extra predicate through join", fixedPoint,
        PushExtraPredicateThroughJoin,
        PushDownPredicates) :: Nil
    }
```

![image-20230606113552853](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230606113552853.png)