目标：

要搞清楚spark和gluten做了啥，计算引擎做了啥，比如sql语法解析是谁做，计划树优化是谁做，task的调度是谁做，逻辑计划解析成物理计划是谁做，物理计划变成task又是谁来负责。整个sql的执行过程，每一个环节我们都需要搞清楚是谁。

包括velox，clickhouse，arrow，异构





JVM与native之间哪些场景共享数据



插件入口： --conf spark.plugins=io.glutenproject.GlutenPlugin

# GlutenPlugin

## GlutenDriverPlugin

1. 注册监听器GlutenSQLAppStatusListener
   - 处理GlutenBuildInfoEvent时，在kvstore里写入Build信息
   - 处理GlutenPlanFallbackEvent时，在kvstore里写入回退原因
   
2. setPredefinedConfigs
   追加扩展类GlutenSessionExtensions
   
3. BackendsApiManager.*initialize*()  目前啥也没做

4. BackendsApiManager.*getListenerApiInstance*.onDriverStart(conf)
  
  
  
  Backend公共接口

```scala
trait Backend {
  def name(): String

  def buildInfo(): GlutenPlugin.BackendBuildInfo

  def iteratorApi(): IteratorApi

  def sparkPlanExecApi(): SparkPlanExecApi
  /**生成*/

  def transformerApi(): TransformerApi

  def validatorApi(): ValidatorApi

  def metricsApi(): MetricsApi

  def listenerApi(): ListenerApi

  def broadcastApi(): BroadcastApi

  def settings(): BackendSettingsApi
}
```



  def iteratorApi(): IteratorApi

  def sparkPlanExecApi(): SparkPlanExecApi

- 生成行列转换算子、Transformer
- 自定义Catalyst扩展

  def transformerApi(): TransformerApi

  def validatorApi(): ValidatorApi

  def metricsApi(): MetricsApi

  def listenerApi(): ListenerApi

  def broadcastApi(): BroadcastApi

  def settings(): BackendSettingsApi



## GlutenExecutorPlugin

  启动一个*Gluten executor endpoint*

# GlutenSessionExtensions

注册了四个扩展

## ColumnarQueryStagePrepOverrides

调用injectQueryStagePrepRule添加规则到自适应执行创建QueryStage前，这显然是支持AQE。

下面这两个规则用在tag之前

- FallbackOnANSIMode:  gluten不支持ansi mode 全部tag为不能转换

- FallbackMultiCodegens：多个(12)连续join会被tag为不能转换

- FallbackBroadcastExchange

BroadcastHashJoinExec和他的子算子BroadcastExec被切到不同的QueryStage了，为了让BroadcastExec在BroadcastHashJoinExec Falback时也能Falback,所以先把BroadcastExec tag为不能transform

## ColumnarOverrides

插入了一个支持列式计算的规则 ColumnarOverrideRules，该规则复写了Spark列式规则的两个接口，我们分别看他们干了啥

> 在Spark Race中，pre负责算子卸载，post负责行列转换的卸载

### preColumnarTransitions

替换物理计划

```scala
    PhysicalPlanSelector.maybe(session, plan) {
      setAdaptiveContext()//从调用栈判断当前列式规则是否由AQE在使用，记录到sparkContext
      setOriginalPlan(plan)//记录下原始plan
      transformPlan(preOverrides(), plan, "pre")
    }
```

- maybe

这里maybe意思是有条件的替换，条件就是校验

1. 开启了gluten
2. 校验

   1. PhysicalPlanSelector
      BackendsApiManager.getValidatorApiInstance.doSparkPlanValidate(plan)： 校验整个SparkPlan，目前只是留了个口子



- preOverrides：加载Backend提供的列式规则以及扩展列式规则, 见2.2.1.1

- transformPlan: 依次应用这些规则，没啥说的

#### preOverrides 规则列表

```scala
  private def preOverrides(): List[SparkSession => Rule[SparkPlan]] = {
    val tagBeforeTransformHitsRules =
      if (isAdaptiveContext) {
        //当AQE开启时，这些规则会在ColumnarQueryStagePrepOverrides中被应用
        List.empty
      } else {
        TagBeforeTransformHits.ruleBuilders
      }
    tagBeforeTransformHitsRules :::
      List(
        (spark: SparkSession) => PlanOneRowRelation(spark),
        (_: SparkSession) => FallbackEmptySchemaRelation(),
        (_: SparkSession) => AddTransformHintRule(),
        (_: SparkSession) => TransformPreOverrides(isAdaptiveContext),
        (_: SparkSession) => EnsureLocalSortRequirements
      ) :::
      BackendsApiManager.getSparkPlanExecApiInstance.genExtendedColumnarPreRules() :::
      SparkRuleUtil.extendedColumnarRules(session, GlutenConfig.getConf.extendedColumnarPreRules)
    //预留了接口，支持计算引擎自定义列式规则
  }
```



- PlanOneRowRelation

- FallbackEmptySchemaRelation

- AddTransformHintRule
  
  **把plan转为plantransformer, doValidate函数用于校验是否支持，如果不支持则在plan上追加一个row守卫来阻止转换。这一步相当于tag**
  
  ```scala
    def apply(plan: SparkPlan): SparkPlan = {
      addTransformableTags(plan)
    }
  
    /** Inserts a transformable tag on top of those that are not supported. */
    private def addTransformableTags(plan: SparkPlan): SparkPlan = {
      addTransformableTag(plan)
      plan.withNewChildren(plan.children.map(addTransformableTags))  //递归
    }
  
  
  def addTransformableTag(plan: SparkPlan): Unit = {
    TransformHints.tagNotTransformable
    TransformHints.tagTransformable(plan)
    TransformHints.tag(plan, transformer.doValidate().toTransformHint)
  }
  
  ```
  
  这里就是调用TransformHints的tag方法， 对plan的Tag("io.glutenproject.transformhint")进行设置

例如Project算子

```scala
        case plan: ProjectExec =>
          if (!enableColumnarProject) {
            // 可以直接tag该plan不能转
            TransformHints.tagNotTransformable(plan, "columnar project is disabled")
          } else {
            //构造一个Transformer，让它决定是否转
            val transformer = ProjectExecTransformer(plan.projectList, plan.child)
            //transformer调用 doValidate方法
            TransformHints.tag(plan, transformer.doValidate().toTransformHint)
          }
  //ProjectExecTransformer
  def apply(projectList: Seq[NamedExpression], child: SparkPlan): ProjectExecTransformer =
    new ProjectExecTransformer(processProjectExecTransformer(projectList), child)
```

ProjectExecTransformer内部校验逻辑：

```scala
ProjectExecTransformer.scala
override protected def doValidateInternal(): ValidationResult = {
      // 新建一个substraitContext
    val substraitContext = new SubstraitContext
    // 首先确保该算子的Substrait plan 可以成功生成
    val operatorId = substraitContext.nextOperatorId(this.nodeName)
    //由substrait包的RelBulder生成substrait plan
    val relNode =
      getRelNode(substraitContext, projectList, child.output, operatorId, null, validation = true)
    // 最后再在native引擎上校验生成的plan generated plan in native engine.
    doNativeValidation(substraitContext, relNode)
  }


  protected def doNativeValidation(context: SubstraitContext, node: RelNode): ValidationResult = {
    if (node != null && enableNativeValidation) {
      //先构造一个substrait的PlanNode
      val planNode = PlanBuilder.makePlan(context, Lists.newArrayList(node))
      //调用Backend的校验接口
      val info = BackendsApiManager.getValidatorApiInstance
        .doNativeValidateWithFailureReason(planNode)
      ValidationResult.convertFromValidationInfo(info)
    } else {
      ValidationResult.ok
    }
  }
```

这里调用了getRelNode会先对所有表达式进行转换，使用RelBuilder.*makeProjectRel*来构造一个ProjectRel,  最后使用native引擎doNativeValidate

> 所以这里获取substrait Plan的目的是交给native引擎去doNativeValidate。TransformPreOverrides在转换到plan transformer时并没有先转为substrait Plan

- TransformPreOverrides
  实施Spark plan到 plan transformer的转换，这个过程是递归进行的。  先校验tag，不支持则直接返回。支持的话则进行转换

  ```scala
    tag支持后      
          case plan: ProjectExec =>
          //这里递归对子节点进行替换
          val columnarChild = replaceWithTransformerPlan(plan.child)
          logDebug(s"Columnar Processing for ${plan.getClass} is currently supported.")
          //子节点替换后，在子节点上加一个ProjectExecTransformer,完成替换
          ProjectExecTransformer(plan.projectList, columnarChild)
  ```

  

- EnsureLocalSortRequirements
  与*EnsureRequirements*类似，但只处理SortExec。  这个规则出现的原因是glutenPlan不需要局部排序，比较激进的删掉了sort，但是有的算子又需要sort，例如*SortAggregate*之后的SortMergeJoin， 这个规则负责把sort再加回来。

追加自定义规则--spark.gluten.sql.columnar.extended.columnar.pre.rules，默认空

### postColumnarTransitions

```scala
  override def postColumnarTransitions: Rule[SparkPlan] = plan =>
    PhysicalPlanSelector.maybe(session, plan) {
      val planWithFallbackPolicy = transformPlan(fallbackPolicy(), plan, "fallback")
      val finalPlan = planWithFallbackPolicy match {
        case FallbackNode(fallbackPlan) =>
          // we should use vanilla c2r rather than native c2r,
          // and there should be no `GlutenPlan` any more,
          // so skip the `postOverrides()`.
          fallbackPlan
        case plan =>
          transformPlan(postOverrides(), plan, "post")
      }
      resetOriginalPlan()
      resetAdaptiveContext()
      transformPlan(finallyRules(), finalPlan, "final")
    }

```

可以看到它会用transformPlan函数应用三组规则

#### fallbackPolicy

- ExpandFallbackPolicy

```scala
  override def apply(plan: SparkPlan): SparkPlan = {
    //是否整个stage回退： 统计stage里有多少个ColumnarToRow以及原生叶子节点数量，如果超过阈值则整个stage回退
    val reason = fallback(plan)
    if (reason.isDefined) {
      val fallbackPlan = fallbackToRowBasedPlan()
      TransformHints.tagAllNotTransformable(fallbackPlan, reason.get)
      FallbackNode(fallbackPlan)
    } else {
      plan
    }
  }
```

#### postOverrides

```scala
    List(
      (_: SparkSession) => TransformPostOverrides(isAdaptiveContext),
      (s: SparkSession) => VanillaColumnarPlanOverrides(s)
    ) :::
      BackendsApiManager.getSparkPlanExecApiInstance.genExtendedColumnarPostRules() :::
      List((_: SparkSession) => ColumnarCollapseTransformStages(GlutenConfig.getConf)) :::
      SparkRuleUtil.extendedColumnarRules(session, GlutenConfig.getConf.extendedColumnarPostRules)
```

- TransformPostOverrides
  把行列转换 卸载到native实现

- VanillaColumnarPlanOverrides
  追加RowToColumnarExecBase 和 ColumnarToRowExec 来支持 原生Spark的列式Scan

- genExtendedColumnarPostRules
  加载其他扩展规则，例如把AQE算子修改为列式，这样可以直接写列式数据出去

- ColumnarCollapseTransformStages
  查找支持transform的计划链，折叠为一个*WholeStageTransformer*， 这里参考了Spark的CodeGen。
  这里只是简单的在不支持transform的算子上加一个ColumnarInputAdapter算子，这个算子的作用就是隐藏掉不支持transform的算子，最重要的是它不支持CodeGen。

- extendedColumnarRules
  其他自定义规则，默认空

#### finallyRules

```scala
  private def finallyRules(): List[SparkSession => Rule[SparkPlan]] = {
    List(
      (s: SparkSession) => GlutenFallbackReporter(GlutenConfig.getConf, s),//收集fallback原因
      (_: SparkSession) => RemoveTransformHintRule() //丢掉gluten追加的tag
    )
  }
```



## StrategyOverrides

它在Catalyst的生成物理执行计划阶段参与了Join的选择

```scala
object StrategyOverrides extends GlutenSparkExtensionsInjector {
  override def inject(extensions: SparkSessionExtensions): Unit = {
    extensions.injectPlannerStrategy(JoinSelectionOverrides)
  }
}

case class JoinSelectionOverrides(session: SparkSession)
  extends Strategy
  with JoinSelectionHelper
  with SQLConfHelper
```

## OthersExtensionOverrides

把Catalyst的扩展点全暴露给了Backend

# 类设计及整体流程

![image-20231205172048970](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231205172048970.png)

![image-20231205172102871](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231205172102871.png)

## WholeStageTransformer

需要先了解一下Spark的全阶段代码生成

# 表达式的替换

以ProjectTransformer为例，在获取getRelNode时，同时需要把 projectList中的表达式进行doTransform， 这是在伴生对象里作的

```scala
    val args = context.registeredFunction
    val columnarProjExprs: Seq[ExpressionTransformer] = projectList.map(
      expr => {
        ExpressionConverter //先把表达式替换为ExpressionTransformer
          .replaceWithExpressionTransformer(expr, attributeSeq = originalInputAttributes)
      })
    val projExprNodeList = new java.util.ArrayList[ExpressionNode]()
    for (expr <- columnarProjExprs) {
      projExprNodeList.add(expr.doTransform(args))//再进行doTransform
    }
```

