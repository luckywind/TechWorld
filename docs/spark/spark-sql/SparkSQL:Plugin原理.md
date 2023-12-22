# Plugin原理

[spark 3.x Plugin Framework](https://www.jianshu.com/p/67cb71a196ce)spark 3.0引入了一个新的插件框架，其实这个插件在spark 2.x就已经存在了，只不过spark 3.0对该插件进行了重构。因为在之前该插件是不支持driver端的，具体可以见[SPARK-29396](https://links.jianshu.com/go?to=https%3A%2F%2Fissues.apache.org%2Fjira%2Fbrowse%2FSPARK-29396)。至于为什么引入这么一个插件 是为了更好的监控和定制一些指标，以便更好的进行spark调优。

[支持driver端和Executor端的Plugin Framework](https://issues.apache.org/jira/browse/SPARK-29397)

## 背景

Spark 3.0 引入了一个新的Plugin Framework。Plugin Framework 是一组新的API接口能够让使用者自定义Driver和Executor。这样用户可以根据不用的使用场景控制Driver 和 Executor JVM的初始化。

##  类关系

![Plugin](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/Plugin.png)

基本架构如上图所示，在Executor和Driver初始化的时候，会相应的加载Plugins，这是 “a sequence of several plugins”, 通过Class Loader进行加载，加载的是PluginContainer。PluginContainer作为一个接口类，提供了shutdown(),registerMetrics(), onTaskStart(), onTaskSucceed(), onTaskFailed等方法，分别在一个task初始化，成功，失败等情况下插件相应的实现接口的方法被调用。其中，registerMetrics()作用是将相应的插件的相关信息注册到Spark的信息统计中，相应的数据等会在Spark UI中被使用。

DriverPluginContainer和ExecutorPluginContainer又分别以SparkPlugin作为私有变量，SparkPlugin是对上述方法的又一层底层的封装，分别被DriverPlugin和ExecutorPlugin所继承，在实现Spark的插件时，只需要继承并实现SparkPlugin即可。

## 源码分析

以下是在Spark源码中，上图各个Interfacce和class的源码，配合其中的comments作为上述分析的论证。

拿Spark-rapids来说，它通过分别实现SparkPlugin的DriverPlugin和ExecutorPlugin来将自己作为插件被Spark调用。而Spark-rapids真正的工作是用来拦截通过spark catalyst生成的Logical Plan，在它变成Physical Plan之前转化成为GPU Plan。这关键的一步是利用SparkSessionExtension来实现的，SparkSessionExtension是整个spark-rapids真正的entry point，是Logical Plan向GPU Plan转化的切入点。

### SparkPlugin

所谓的插件是什么呢？ 从这个接口看，就是获取DriverPlugin和ExecutorPlugin

```scala
public interface SparkPlugin {

  /**
   * Return the plugin's driver-side component.
   *
   * @return The driver-side component, or null if one is not needed.
   */
  DriverPlugin driverPlugin();

  /**
   * Return the plugin's executor-side component.
   *
   * @return The executor-side component, or null if one is not needed.
   */
  ExecutorPlugin executorPlugin();

}
```



### PluginContainer

是DriverPluginContainer和ExecutorPluginContainer的接口，

PluginContainer本身提供了metrics和task相关的接口

其次，伴生对象的apply方法是SparkContext初始化时调用的，用来加载SparkPlugin，提取其中的driverPlugin和executorPlugin

```scala
// 接口类
sealed abstract class PluginContainer {

  def shutdown(): Unit
  def registerMetrics(appId: String): Unit
  def onTaskStart(): Unit
  def onTaskSucceeded(): Unit
  def onTaskFailed(failureReason: TaskFailedReason): Unit

}


// 半生对象
object PluginContainer {
private def apply(
      ctx: Either[SparkContext, SparkEnv],
      resources: java.util.Map[String, ResourceInformation]): Option[PluginContainer] = {
    val conf = ctx.fold(_.conf, _.conf)
  // 加载配置spark.plugins指定的插件，逗号分隔，所谓的插件必须继承SparkPlugin接口
    val plugins = Utils.loadExtensions(classOf[SparkPlugin], conf.get(PLUGINS).distinct, conf)
    if (plugins.nonEmpty) {
      ctx match {
        //DriverPluginContainer拿到driver plugin列表
        case Left(sc) => Some(new DriverPluginContainer(sc, resources, plugins))
        //ExecutorPluginContainer拿到executor plugin列表
        case Right(env) => Some(new ExecutorPluginContainer(env, resources, plugins))
      }
    } else {
      None
    }
  }
}
```

### DriverPluginContainer

是DriverPlugin的容器，它触发driver插件的init方法

```scala
private class DriverPluginContainer(
    sc: SparkContext,
    resources: java.util.Map[String, ResourceInformation],
    plugins: Seq[SparkPlugin])
  extends PluginContainer with Logging {

  private val driverPlugins: Seq[(String, DriverPlugin, PluginContextImpl)] = plugins.flatMap { p =>
    val driverPlugin = p.driverPlugin()
    if (driverPlugin != null) {
      val name = p.getClass().getName()
      val ctx = new PluginContextImpl(name, sc.env.rpcEnv, sc.env.metricsSystem, sc.conf,
        sc.env.executorId, resources)
      //初始化插件，即调用插件init方法
      val extraConf = driverPlugin.init(sc, ctx)
      if (extraConf != null) {
        extraConf.asScala.foreach { case (k, v) =>
          sc.conf.set(s"${PluginContainer.EXTRA_CONF_PREFIX}$name.$k", v)
        }
      }
      logInfo(s"Initialized driver component for plugin $name.")
      Some((p.getClass().getName(), driverPlugin, ctx))
    } else {
      None
    }
  }

  if (driverPlugins.nonEmpty) {
    val pluginsByName = driverPlugins.map { case (name, plugin, _) => (name, plugin) }.toMap
    sc.env.rpcEnv.setupEndpoint(classOf[PluginEndpoint].getName(),
      new PluginEndpoint(pluginsByName, sc.env.rpcEnv))
  }

  override def registerMetrics(appId: String): Unit = {
    driverPlugins.foreach { case (_, plugin, ctx) =>
      plugin.registerMetrics(appId, ctx)
      ctx.registerMetrics()
    }
  }

  override def shutdown(): Unit = {
    driverPlugins.foreach { case (name, plugin, _) =>
      try {
        logDebug(s"Stopping plugin $name.")
        plugin.shutdown()
      } catch {
        case t: Throwable =>
          logInfo(s"Exception while shutting down plugin $name.", t)
      }
    }
  }

  override def onTaskStart(): Unit = {
    throw new IllegalStateException("Should not be called for the driver container.")
  }

  override def onTaskSucceeded(): Unit = {
    throw new IllegalStateException("Should not be called for the driver container.")
  }

  override def onTaskFailed(failureReason: TaskFailedReason): Unit = {
    throw new IllegalStateException("Should not be called for the driver container.")
  }
}

```

### ExecutorPluginContainer

是executorPlugin的容器，它触发executorPlugin的init方法

```scala
private class ExecutorPluginContainer(
    env: SparkEnv,
    resources: java.util.Map[String, ResourceInformation],
    plugins: Seq[SparkPlugin])
  extends PluginContainer with Logging {

  private val executorPlugins: Seq[(String, ExecutorPlugin)] = {
    val allExtraConf = env.conf.getAllWithPrefix(PluginContainer.EXTRA_CONF_PREFIX)

    plugins.flatMap { p =>
      val executorPlugin = p.executorPlugin()
      if (executorPlugin != null) {
        val name = p.getClass().getName()
        val prefix = name + "."
        val extraConf = allExtraConf
          .filter { case (k, v) => k.startsWith(prefix) }
          .map { case (k, v) => k.substring(prefix.length()) -> v }
          .toMap
          .asJava
        val ctx = new PluginContextImpl(name, env.rpcEnv, env.metricsSystem, env.conf,
          env.executorId, resources)
        executorPlugin.init(ctx, extraConf)
        ctx.registerMetrics()

        logInfo(s"Initialized executor component for plugin $name.")
        Some(p.getClass().getName() -> executorPlugin)
      } else {
        None
      }
    }
  }

  override def registerMetrics(appId: String): Unit = {
    throw new IllegalStateException("Should not be called for the executor container.")
  }

  override def shutdown(): Unit = {
    executorPlugins.foreach { case (name, plugin) =>
      try {
        logDebug(s"Stopping plugin $name.")
        plugin.shutdown()
      } catch {
        case t: Throwable =>
          logInfo(s"Exception while shutting down plugin $name.", t)
      }
    }
  }

  override def onTaskStart(): Unit = {
    executorPlugins.foreach { case (name, plugin) =>
      try {
        plugin.onTaskStart()
      } catch {
        case t: Throwable =>
          logInfo(s"Exception while calling onTaskStart on plugin $name.", t)
      }
    }
  }

  override def onTaskSucceeded(): Unit = {
    executorPlugins.foreach { case (name, plugin) =>
      try {
        plugin.onTaskSucceeded()
      } catch {
        case t: Throwable =>
          logInfo(s"Exception while calling onTaskSucceeded on plugin $name.", t)
      }
    }
  }

  override def onTaskFailed(failureReason: TaskFailedReason): Unit = {
    executorPlugins.foreach { case (name, plugin) =>
      try {
        plugin.onTaskFailed(failureReason)
      } catch {
        case t: Throwable =>
          logInfo(s"Exception while calling onTaskFailed on plugin $name.", t)
      }
    }
  }
}
```

## SparkRace

### 插件加载

<font color=red>--conf spark.plugins=com.yusur.spark.SQLPlugin</font>

加载的插件是com.yusur.spark.SQLPlugin,它继承了SparkPlugin，且创建了RaceDriverPlugin和RaceExecutorPlugin

```scala
class SQLPlugin extends SparkPlugin with Logging {  
override def driverPlugin(): DriverPlugin = {
    new RaceDriverPlugin
  }

  override def executorPlugin(): ExecutorPlugin = {
    logWarning("Return executor plugin.")
    new RaceExecutorPlugin
  }
}
```

### RaceDriverPlugin

RaceDriverPlugin做的最重要的事就是在init里追加扩展SQLExecPlugin

```scala
// RaceDriverPlugin.scala 
override def init(
    sc: SparkContext,
    pluginContext: PluginContext
  ): java.util.Map[String, String] = {
    logWarning("Race Executor Plugin init successfully.")
    val sparkConf = pluginContext.conf
    RacePluginUtils.fixupConfigs(sparkConf)
    val conf = new RaceConf(sparkConf)
    conf.raceConfMap
  }



object RacePluginUtils extends Logging {
//  val PLUGIN_PROPS_FILENAME = "race4spark-version-info.properties"

  private val SQL_PLUGIN_NAME = classOf[SQLExecPlugin].getName
  private val SQL_PLUGIN_CONF_KEY = StaticSQLConf.SPARK_SESSION_EXTENSIONS.key
  private val SERIALIZER_CONF_KEY = "spark.serializer"
  private val JAVA_SERIALIZER_NAME = classOf[JavaSerializer].getName
  private val KRYO_SERIALIZER_NAME = classOf[KryoSerializer].getName
  private val KRYO_REGISTRATOR_KEY = "spark.kryo.registrator"
  private val KRYO_REGISTRATOR_NAME = classOf[DpuKryoRegistrator].getName

  def fixupConfigs(conf: SparkConf): Unit = {
    // First add in the SQL executor plugin because that is what we need at a minimum
    if (conf.contains(SQL_PLUGIN_CONF_KEY)) {
      logWarning("Goes to conf contains SQL_PLUGIN_CONF_KEY.")
      //如果配置了spark.sql.extensions， 则先获取配置的插件，如果不包含则追加SQLExecPlugin
      val pluginName = SQL_PLUGIN_NAME
      val previousValue = conf.get(SQL_PLUGIN_CONF_KEY).split(",").map(_.trim)
      if (!previousValue.contains(pluginName)) {
        conf.set(SQL_PLUGIN_CONF_KEY, (previousValue :+ pluginName).mkString(","))
      } else {
        conf.set(SQL_PLUGIN_CONF_KEY, previousValue.mkString(","))
      }
    } else {
      //未配置spark.sql.extensions， 则默认只使用SQLExecPlugin
      conf.set(SQL_PLUGIN_CONF_KEY, SQL_PLUGIN_NAME)
    }

    val serializer = conf.get(SERIALIZER_CONF_KEY, JAVA_SERIALIZER_NAME)
    if (KRYO_SERIALIZER_NAME.equals(serializer)) {
      if (conf.contains(KRYO_REGISTRATOR_KEY)) {
        if (!KRYO_REGISTRATOR_NAME.equals(conf.get(KRYO_REGISTRATOR_KEY))) {
          logWarning(
            "The RACE Accelerator when used with Kryo needs to register some " +
            s"serializers using $KRYO_REGISTRATOR_NAME. Please call it from your registrator " +
            " to let the plugin work properly."
          )
        } // else it is set and we are good to go
      } else {
        // We cannot set the kryo key here, it is not early enough to be picked up everywhere
        throw new UnsupportedOperationException(
          "The RACE Accelerator when used with Kryo " +
          "needs to register some serializers. Please set the spark config " +
          s"$KRYO_REGISTRATOR_KEY to $KRYO_REGISTRATOR_NAME or some operations may not work " +
          "properly."
        )
      }
    } else if (!JAVA_SERIALIZER_NAME.equals(serializer)) {
      throw new UnsupportedOperationException(
        "$serializer is not a supported serializer for " +
        "the RACE Accelerator. Please disable the RACE Accelerator or use a supported " +
        "serializer ($JAVA_SERIALIZER_NAME, $KRYO_SERIALIZER_NAME)."
      )
    }
    // set driver timezone
    conf.set(RaceConf.DRIVER_TIMEZONE.key, ZoneId.systemDefault().normalized().toString)
  }
}

```

### RaceExecutorPlugin

它的init()方法做的事非常简单，就是在Executor上初始化设备

### SQLExecPlugin

RaceDriverPlugin加载了这个扩展，它做了什么呢？apply方法注入了一个规则

```scala
class SQLExecPlugin extends (SparkSessionExtensions => Unit) with Logging {
  override def apply(extensions: SparkSessionExtensions): Unit = {
    logWarning("Apply extensions of injectColumnar Rules : inject columnar overrides.")
    extensions.injectColumnar(columnarOverrides)
  }

  private def columnarOverrides(sparkSession: SparkSession): ColumnarRule = {
    logWarning("New Columnar Overrides.")
    ColumnarOverrideRules()
  }
}
```

这个规则会作为SessionState的构造参数，QueryExecution在prepare物理计划时用到的规则会从SessionState里拿到：

```scala
  private[execution] def preparations(
      sparkSession: SparkSession,
      adaptiveExecutionRule: Option[InsertAdaptiveSparkPlan] = None,
      subquery: Boolean): Seq[Rule[SparkPlan]] = {
    adaptiveExecutionRule.toSeq ++
    Seq(
      CoalesceBucketsInJoin,
      PlanDynamicPruningFilters(sparkSession),
      PlanSubqueries(sparkSession),
      RemoveRedundantProjects,
      EnsureRequirements(),
      ReplaceHashWithSortAgg,
      RemoveRedundantSorts,
      DisableUnnecessaryBucketedScan,
      ApplyColumnarRulesAndInsertTransitions(
        //从SessionState拿到用户插入的规则
        sparkSession.sessionState.columnarRules, outputsColumnar = false),
      CollapseCodegenStages()) ++
      (if (subquery) {
        Nil
      } else {
        Seq(ReuseExchangeAndSubquery)
      })
  }

```



#### ColumnarOverrideRules

这个规则继承了ColumnarRule，持有用户自定义的规则，注入算子的列式实现。

1. preColumnarTransitions 用于替换算子， 之后，spark会自动插入行列转换算子
2. postColumnarTransitions用于替换行列转换算子，或者其他优化

```scala
case class ColumnarOverrideRules() extends ColumnarRule with Logging {
  lazy val overrides: Rule[SparkPlan] = DpuOverrides()
  lazy val overrideTransitions: Rule[SparkPlan] = new DpuTransitionOverrides()

  override def preColumnarTransitions: Rule[SparkPlan] = overrides

  override def postColumnarTransitions: Rule[SparkPlan] = overrideTransitions
}
```

## ApplyColumnarRulesAndInsertTransitions

Spark再优化物理计划，即QueryExecution调用prepareForExecution时，会应用一批规则，规则ApplyColumnarRulesAndInsertTransitions(
  sparkSession.*sessionState*.columnarRules, outputsColumnar = false)

首先获取到用户定义的ColumnarRule，然后完成替换算子、插入行列转换、替换行列转换算子

