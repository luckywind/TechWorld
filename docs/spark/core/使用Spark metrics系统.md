

# spark metrics系统

​          spark有一个可配置的metrics系统，允许用户把spark metrics报告给HTTP/JMX/CSV文件等多个sink。metrics由spark内置的source生成，提供特定动作和组件的指标。metrics系统通过配置文件$SPARK_HOME/conf/metrics.properties配置，也可以通过配置spark.metrics.conf自定义；除了使用配置文件，spark.metrics.conf.开头的一些配置参数也可以使用。默认情况下，driver/executor metrics使用的根空间是`${spark.app.id}`,即application ID，然而，通常用户想跨app来追踪metrics，这种情况下， 可用`${spark.metrics.namespace }`来指定自定义的metrics空间。

​       spark 的metrics依据spark的不同组件可分为不同的实例，每个实例，可以给metrics指定多个sink，支持以下实例：

- `master`: The Spark standalone master process.
- `applications`: A component within the master which reports on various applications.
- `worker`: A Spark standalone worker process.
- `executor`: A Spark executor.
- `driver`: The Spark driver process (the process in which your SparkContext is created).
- `shuffleService`: The Spark shuffle service.
- `applicationMaster`: The Spark ApplicationMaster when running on YARN.
- `mesos_cluster`: The Spark cluster scheduler when running on Mesos.

org.apache.spark.metrics.sink包中有如下实例

- `ConsoleSink`: Logs metrics information to the console.
- `CSVSink`: Exports metrics data to CSV files at regular intervals.
- `JmxSink`: Registers metrics for viewing in a JMX console.
- `MetricsServlet`: Adds a servlet within the existing Spark UI to serve metrics data as JSON data.
- `PrometheusServlet`: (Experimental) Adds a servlet within the existing Spark UI to serve metrics data in Prometheus format.
- `GraphiteSink`: Sends metrics to a Graphite node.
- `Slf4jSink`: Sends metrics to slf4j as log entries.
- `StatsdSink`: Sends metrics to a StatsD node.



在配置文件``$SPARK_HOME/conf/metrics.properties.template`.`中定义了每个sink的可用参数。

当使用配置参数而不是用配置文件时，相关参数名称格式如下：

``spark.metrics.conf.[instance|*].sink.[sink_name].[parameter_name]`.`

spark metrics配置默认值

```properties
"*.sink.servlet.class" = "org.apache.spark.metrics.sink.MetricsServlet"
"*.sink.servlet.path" = "/metrics/json"
"master.sink.servlet.path" = "/metrics/master/json"
"applications.sink.servlet.path" = "/metrics/applications/json"
```

其他的source可通过配置文件或者spark.metrics.conf.[component_name].source.jvm.class=[source_name]来添加，当前JVM source是仅有的可选source。例如

```shell
"spark.metrics.conf.*.source.jvm.class"="org.apache.spark.metrics.source.JvmSource"
```







## 可用的metrics providers

metrics类型： gauge, counter, histogram, meter and timer

1. driver

2. executor

3. applicationMaster

4. master

5. worker

   

## 配置文件

语法：`[instance].sink|source.[name].[options]=[value]`

1. instance: "master", "worker", "executor", "driver", "applications"

2. source: 有两类
   - spark内部source:MasterSource, WorkerSource等是自动追加的
   - Common sources： 例如JvmSource可以通过配置追加
3. sink指定metrics分发的地址
4. name指定source/sink的名字

注意：

1. sink需要全类名
2. 拉取周期最小是1秒
3. 通配符会被更精确的属性覆盖
4. MetricsServlet sink是master/worker/driver默认的sink, 可请求/metrics/json



# 如何自定义一个source和sink

[翻译自](https://kb.databricks.com/metrics/spark-metrics.html)

本文给出一个示例，使用 [Spark configurable metrics system](https://spark.apache.org/docs/latest/monitoring.html#metrics)来监控Spark组件，主要是如何设置一个source和启用一个sink。

metrics系统如何工作的呢？每个executor再实例化时与driver建立一个传递metrics的连接。

1. 创建Source

   ```scala
   class MySource extends Source {
     override val sourceName: String = "MySource"
   
     override val metricRegistry: MetricRegistry = new MetricRegistry
   
     val FOO: Histogram = metricRegistry.histogram(MetricRegistry.name("fooHistory"))
     val FOO_COUNTER: Counter = metricRegistry.counter(MetricRegistry.name("fooCounter"))
   }
   ```

2. 启用sink,例如打印到控制台

      ```scala
      val spark: SparkSession = SparkSession
          .builder
          .master("local[*]")
          .appName("MySourceDemo")
          .config("spark.driver.host", "localhost")
          .config("spark.metrics.conf.*.sink.console.class", "org.apache.spark.metrics.sink.ConsoleSink")
      .getOrCreate()
      ```

3. 实例化source并注册到SparkEnv

```scala
val source: MySource = new MySource
SparkEnv.get.metricsSystem.registerSource(source)
```

There are two ways to register a `Source`:

- during `start` let `MetricsSystem` use `Utils` to load the class and register a `newInstance`.
- otherwise, `registerSource` an instance to `MetricsSystem`.

# SQL UI中的metrics

## class SQLMetric(val metricType: String, initValue: Long = 0L) extends AccumulatorV2[Long, Long]

SQL计划中使用的metric，executor中的更新会通过metrics自动传播并展示在SQL UI上，driver端的更新必须使用SQLMetrics.postDriverMetricUpdates()进行推送。

Spark内置有以下几种metrics，分别有对应的方法来创建

private val *SUM_METRIC* = "sum"
private val *SIZE_METRIC* = "size"

> UI:  data size total (min, med, max):

private val *TIMING_METRIC* = "timing"

> UI: duration total (min, med, max)

private val *NS_TIMING_METRIC* = "nsTiming"

> 和timing类似，只是单位不同

private val *AVERAGE_METRIC* = "average"

> UI: probe avg (min, med, max)

例如默认创建sum metric

```scala
  def createMetric(sc: SparkContext, name: String): SQLMetric = {
    val acc = new SQLMetric(SUM_METRIC)
    acc.register(sc, name = metricsCache.get(name), countFailedValues = false)
    acc
  }
```

