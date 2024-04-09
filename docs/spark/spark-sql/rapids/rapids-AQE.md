# AQE支持

AQE非CPU特有的优势，也可以为GPU提供额外的加速。 AQE主要好处是在运行时根据统计信息优化查询，包括：

- Coalescing shuffle partitions
- Using broadcast hash joins if one side of the join can fit in memory
- Optimizing skew joins

通过把叶子exchange节点转为queryStage然后调度执行，只要完成一个query stage, 该plan的其余部分就可以重新优化(使用logical/physical优化规则)，这个过程一直重复直到所有query stage都完成物化，最后final query stage执行。这个逻辑包含在AdaptiveSparkPlanExec.getFinalPhysicalPlan方法中。

当启用AQE,物理计划将包含AdaptiveSparkPlanExec算子，这个是根节点或者包含在InsertIntoHadoopFsRelationCommand算子中(例如当query的action操作为写磁盘)。

Spark认为AdaptiveSparkPlanExec是基于行的，supportsColumnar返回false，调用doExecuteColumnar将抛异常。 因此rapids将插入一个列转行做为根节点。当然如果是写文件，会有专门的优化来避免不必要的列转行。

## Optimizer Rules

启动时，SQLExecPlugin插件注册了两个规则集

```scala
extensions.injectColumnar(_ => ColumnarOverrideRules())
extensions.injectQueryStagePrepRule(_ => GpuQueryStagePrepOverrides())
```

ColumnarOverrideRules不管AQE是否启用都会使用，而GpuQueryStagePrepOverrides只有AQE启用才会用。AQE使用了四个规则集：

1. queryStagePreparationRules
   在query stage创建前执行一次，在plan重优化前执行一次。GpuQueryStagePrepOverrides就是其中一个。这个规则不直接替换plan，只是tag那些在GPU不支持的节点

2. queryStageOptimizerRules

   在创建query stage时应用到Exchange节点，将创建一个BroadcastQueryStageLike或者ShuffleQueryStageLike节点。优化倾斜join以及coalescing shuffle partitions.

3. postStageCreationRules
   在query stage完成创建时应用，ColumnarOverrideRules将完成算子替换

4. finalStageCreationRules
   它是queryStageOptimizerRules和postStageCreationRules的组合，只是过滤掉了一些final query stage不是用的规则。



```scala
case class GpuQueryStagePrepOverrides() extends Rule[SparkPlan] with Logging {
  override def apply(sparkPlan: SparkPlan): SparkPlan = GpuOverrideUtil.tryOverride { plan =>
    // Note that we disregard the GPU plan returned here and instead rely on side effects of
    // tagging the underlying SparkPlan.
    // 执行替换，但替换后的计划，我们丢掉了，副作用是啥？见替换规则一节
    GpuOverrides().apply(plan)
    // return the original plan which is now modified as a side-effect of invoking GpuOverrides
    // 返回修改后的plan
    plan
  }(sparkPlan)
}
```



## Query Stage Re-use

AdaptiveSparkPlanExec.getFinalPhysicalPlan尝试缓存并重用query stage

# 替换规则

## 尝试替换

这个不是特别重要，只需要知道它能在替换失败时会滚就行

```scala
  def tryOverride(fn: SparkPlan => SparkPlan): //参数是一个plan到plan的函数
    SparkPlan => SparkPlan  // 返回一个函数:接受一个SparkPlan返回一个SparkPlan
   = { plan =>
    val planOriginal = plan.clone()
    val failOnError = TEST_CONF.get(plan.conf) || !SUPPRESS_PLANNING_FAILURE.get(plan.conf)
     // 尝试应用函数，失败则原样返回
    try {
      fn(plan)
    } catch {
      case NonFatal(t) if !failOnError =>
        logWarning("Failed to apply GPU overrides, falling back on the original plan: " + t, t)
        planOriginal
      case fatal: Throwable =>
        logError("Encountered an exception applying GPU overrides " + fatal, fatal)
        throw fatal
    }
  }
```

## 替换逻辑apply

替换plan，替换失败时会会滚。 

- 如果未开启AQE: 只会调用一次，失败会会滚
- 如果开启了AQE: 对每个query stage都会调用一次，且在替换前会修改plan, 替换失败可会滚，但是做的修改不会会滚

```scala
{ plan =>
    val conf = new RapidsConf(plan.conf)
    if (conf.isSqlEnabled) {
      GpuOverrides.logDuration(conf.shouldExplain,
        t => f"Plan conversion to the GPU took $t%.2f ms") {
        //副作用：当开启AQE时，对plan做一些修改
        val updatedPlan = if (plan.conf.adaptiveExecutionEnabled) {
          val newPlan = GpuOverrides.removeExtraneousShuffles(plan, conf)
          GpuOverrides.fixupReusedExchangeExecs(newPlan)
        } else {
          plan
        }
        // 执行替换，这里会调用wrapAndTagPlan和doConvertPlan
        applyOverrides(updatedPlan, conf)
      }
    } else {
      plan
    }
  }
```

### def removeExtraneousShuffles

删除多余的shuffleExchangeExec

```scala
  def removeExtraneousShuffles(plan: SparkPlan, conf: RapidsConf): SparkPlan = {
    plan.transformUp {
      case cpuShuffle: ShuffleExchangeExec =>
        cpuShuffle.child match {
          case sqse: ShuffleQueryStageExec =>
            GpuTransitionOverrides.getNonQueryStagePlan(sqse) match {
              case gpuShuffle: GpuShuffleExchangeExecBase =>
                val converted = convertPartToGpuIfPossible(cpuShuffle.outputPartitioning, conf)
                if (converted == gpuShuffle.outputPartitioning) {
                  sqse
                } else {
                  cpuShuffle
                }
              case _ => cpuShuffle
            }
          case _ => cpuShuffle
        }
    }
  }
```



### def fixupReusedExchangeExecs

搜索包含GPU shuffle的ReusedExchangeExec算子，修改它来匹配GPU shuffle输出类型。

```scala
  def fixupReusedExchangeExecs(plan: SparkPlan): SparkPlan = {
    def outputTypesMatch(a: Seq[Attribute], b: Seq[Attribute]): Boolean =
      a.corresponds(b)((x, y) => x.dataType == y.dataType)
    plan.transformUp {
      case sqse: ShuffleQueryStageExec =>
        sqse.plan match {
          // 找到子节点是GpuShuffleExchangeExecBase的ReusedExchangeExec算子，且类型不匹配
          case ReusedExchangeExec(output, gsee: GpuShuffleExchangeExecBase) if (
              !outputTypesMatch(output, gsee.output)) =>
            // 构造新的output
            val newOutput = sqse.plan.output.zip(gsee.output).map { case (c, g) =>
              assert(c.isInstanceOf[AttributeReference] && g.isInstanceOf[AttributeReference],
                s"Expected AttributeReference but found $c and $g")
              AttributeReference(c.name, g.dataType, c.nullable, c.metadata)(c.exprId, c.qualifier)
            }
            // 创建一个新的sqse并返回
            AQEUtils.newReuseInstance(sqse, newOutput)
          case _ => sqse
        }
    }
  }
```

