[官网地址](https://db-blog.web.cern.ch/blog/luca-canali/2018-08-sparkmeasure-tool-performance-troubleshooting-apache-spark-workloads)

[项目源码](https://github.com/LucaCanali/sparkMeasure)

# sparkMeasure

主要概念:

1. 基于Spark Listener接口的工具，Spark Listener把Spark executor的Metrics数据传回driver
2. Metrics可以在task完成/stage完成时收集
3. Metrics数据可以保存下来离线分析

## task Metrics

[参考](https://github.com/LucaCanali/Miscellaneous/blob/master/Spark_Notes/Spark_TaskMetrics.md)

| Spark Executor Task Metric name | Short description                                            |
| ------------------------------- | ------------------------------------------------------------ |
| executorRunTime                 | Time the executor spent running this task. This includes time fetching shuffle data. The value is expressed in milliseconds. |
| executorCpuTime                 | CPU Time the executor spent running this task. This includes time fetching shuffle data. The value is expressed in nanoseconds. |
| executorDeserializeTime         | Time taken on the executor to deserialize this task. The value is expressed in milliseconds. |
| executorDeserializeCpuTime      | CPU Time taken on the executor to deserialize this task. The value is expressed in nanoseconds. |
| resultSize                      | The number of bytes this task transmitted back to the driver as the TaskResult. |
| jvmGCTime                       | Amount of time the JVM spent in garbage collection while executing this task. The value is expressed in milliseconds. |
| resultSerializationTime         | Amount of time spent serializing the task result. The value is expressed in milliseconds. |
| memoryBytesSpilled              | The number of in-memory bytes spilled by this task.          |
| diskBytesSpilled                | The number of on-disk bytes spilled by this task.            |
| peakExecutionMemory             | Peak memory used by internal data structures created during shuffles, aggregations and joins. The value of this accumulator should be approximately the sum of the peak sizes across all such data structures created in this task. For SQL jobs, this only tracks all unsafe operators and ExternalSort. |
| inputMetrics.*                  | Metrics related to reading data from [[org.apache.spark.rdd.HadoopRDD]] or from persisted data. |
| .bytesRead                      | Total number of bytes read.                                  |
| .recordsRead                    | Total number of records read.                                |
| outputMetrics.*                 | Metrics related to writing data externally (e.g. to a distributed filesystem), defined only in tasks with output. |
| .bytesWritten                   | Total number of bytes written                                |
| .recordsWritten                 | Total number of records written                              |
| shuffleReadMetrics.*            | Metrics related to shuffle read operations.                  |
| .recordsRead                    | Number of records read in shuffle operations                 |
| .remoteBlocksFetched            | Number of remote blocks fetched in shuffle operations        |
| .localBlocksFetched             | Number of local (as opposed to read from a remote executor) blocks fetched in shuffle operations |
| .totalBlocksFetched             | Number of blocks fetched in shuffle operations (both local and remote) |
| .remoteBytesRead                | Number of remote bytes read in shuffle operations            |
| .localBytesRead                 | Number of bytes read in shuffle operations from local disk (as opposed to read from a remote executor) |
| .totalBytesRead                 | Number of bytes read in shuffle operations (both local and remote) |
| .remoteBytesReadToDisk          | Number of remote bytes read to disk in shuffle operations. Large blocks are fetched to disk in shuffle read operations, as opposed to being read into memory, which is the default behavior. |
| .fetchWaitTime                  | Time the task spent waiting for remote shuffle blocks. This only includes the time blocking on shuffle input data. For instance if block B is being fetched while the task is still not finished processing block A, it is not considered to be blocking on block B. The value is expressed in milliseconds. |
| shuffleWriteMetrics.*           | Metrics related to operations writing shuffle data.          |
| .bytesWritten                   | Number of bytes written in shuffle operations                |
| .recordsWritten                 | Number of records written in shuffle operations              |
| .writeTime                      | Time spent blocking on writes to disk or buffer cache. The value is expressed in nanoseconds. |

## 使用方法

下载源码，根目录执行sbt package进行打包

### stage level

```scala
bin/spark-shell —jars spark-measure_2.12-0.24-SNAPSHOT.jar

val stageMetrics = ch.cern.sparkmeasure.StageMetrics(spark)
stageMetrics.runAndMeasure(spark.sql("select count(*) from range(1000) cross join range(1000) cross join range(1000)").show())
stageMetrics.printMemoryReport
```

### task level

```scala
bin/spark-shell —jar spark-measure_2.12-0.24-SNAPSHOT.jar

val taskMetrics = ch.cern.sparkmeasure.TaskMetrics(spark)
taskMetrics.runAndMeasure(spark.sql("select count(*) from range(1000) cross join range(1000) cross join range(1000)").show())
```

### 同时收集

```scala
val stageMetrics = ch.cern.sparkmeasure.StageMetrics(spark)
stageMetrics.begin()
val taskMetrics = ch.cern.sparkmeasure.TaskMetrics(spark)
taskMetrics.begin()

spark.sql("select count(*) from range(1000) cross join range(1000) cross join range(1000)").show()

stageMetrics.end()
taskMetrics.end()
stageMetrics.printReport
taskMetrics.printReport
```





### 分析

#### 导出为json文件

导出stage指标

```scala
val stageMetrics = ch.cern.sparkmeasure.StageMetrics(spark) 
  stageMetrics.runAndMeasure( ...your workload here ... )

  val df = stageMetrics.createStageMetricsDF("PerfStageMetrics")
  df.show()
  stageMetrics.saveData(df.orderBy("jobId", "stageId"), "/tmp/stagemetrics_test1")
```

导出task指标

```scala
val df = taskMetrics.createTaskMetricsDF("PerfTaskMetrics")
  spark.sql("select * from PerfTaskMetrics").show()
  df.show()
  taskMetrics.saveData(df.orderBy("jobId", "stageId", "index"), "<path>/taskmetrics_test3")
```



#### sql分析

[参考](https://github.com/LucaCanali/sparkMeasure/blob/master/docs/Notes_on_metrics_analysis.md)

```scala
// export task metrics collected by the Listener into a DataFrame and registers as a temporary view 
val df = taskMetrics.createTaskMetricsDF("PerfTaskMetrics")

// other option: read metrics previously saved on a json file
val df = spark.read.json("taskmetrics_test1")
df.createOrReplaceTempView("PerfTaskMetrics")

// show the top 5 tasks by duration
spark.sql("select jobId, host, duration from PerfTaskMetrics order by duration desc limit 5").show()
// show the available metrics
spark.sql("desc PerfTaskMetrics").show()
```



### 其他api

#### StageMetrics

```scala
def begin(): Long // Marks the beginning of data collection
def end(): Long  // Marks the end of data collection
def removeListener(): Unit // helper method to remove the listener

// Compute basic aggregation on the Stage metrics for the metrics report
// also filter on the time boundaries for the report
// The output is a map with metrics names and their aggregatd values
def aggregateStageMetrics() : LinkedHashMap[String, Long] 

// Extracts stages and their duration
def stagesDuration() : LinkedHashMap[Int, Long]

// Custom aggregations and post-processing of metrics data
def report(): String

// Runs report and prints it
def printReport(): Unit

// Custom aggregations and post-processing of executor metrics data with memory usage details
// Note this report requires per-stage memory (executor metrics) data which is sent by the executors
// at each heartbeat to the driver, there could be a small delay or the order of a few seconds
// between the end of the job and the time the last metrics value is received
// if you receive the error message java.util.NoSuchElementException: key not found,
// retry running the report after waiting for a few seconds.
def reportMemory(): String

// Runs the memory report and prints it
def printMemoryReport(): Unit

// Legacy transformation of data recorded from the custom Stage listener
// into a DataFrame and register it as a view for querying with SQL
def createStageMetricsDF(nameTempView: String = "PerfStageMetrics"): DataFrame

// Legacy metrics aggregation computed using SQL
def aggregateStageMetrics(nameTempView: String = "PerfStageMetrics"): DataFrame

// Custom aggregations and post-processing of metrics data
// This is legacy and uses Spark DataFrame operations,
// use report instead, which will process data in the driver using Scala
def reportUsingDataFrame(): String

// Shortcut to run and measure the metrics for Spark execution, built after spark.time()
def runAndMeasure[T](f: => T): T

// Helper method to save data, we expect to have small amounts of data so collapsing to 1 partition seems OK
def saveData(df: DataFrame, fileName: String, fileFormat: String = "json", saveMode: String = "default")

```

#### TaskMetrics

```scala
def begin(): Long // Marks the beginning of data collection
def end(): Long  // Marks the end of data collection
def removeListener(): Unit // helper method to remove the listener

// Compute basic aggregation on the Task metrics for the metrics report
// also filter on the time boundaries for the report
// The output is a map with metrics names and their aggregatd values
def aggregateTaskMetrics() : LinkedHashMap[String, Long] = {

// Custom aggregations and post-processing of metrics data
def report(): String

// Runs report and prints it
def printReport(): Unit

// Legacy transformation of data recorded from the custom Stage listener
// into a DataFrame and register it as a view for querying with SQL
def createTaskMetricsDF(nameTempView: String = "PerfTaskMetrics"): DataFrame = {

// legacy metrics aggregation computed using SQL
def aggregateTaskMetrics(nameTempView: String = "PerfTaskMetrics"): DataFrame

// Custom aggregations and post-processing of metrics data
// This is legacy and uses Spark DataFrame operations,
// use report instead, which will process data in the driver using Scala
def reportUsingDataFrame(): String = {

// Shortcut to run and measure the metrics for Spark execution, built after spark.time()
def runAndMeasure[T](f: => T): T

// Helper method to save data, we expect to have small amounts of data so collapsing to 1 partition seems OK
def saveData(df: DataFrame, fileName: String, fileFormat: String = "json", saveMode: String = "default")

def sendReportPrometheus(serverIPnPort: String,
                           metricsJob: String,
                           labelName: String = sparkSession.sparkContext.appName,
                           labelValue: String = sparkSession.sparkContext.applicationId): Unit
```







# Spark listeners

spark listeners是spark 监控信息的主要来源，在job/stage/task的开始/结束事件发生时，可以使用listener收集指标信息。

使用自定义listeners收集stage数据

```scala

val myConf = new org.apache.spark.SparkConf()

val myListener = new org.apache.spark.ui.jobs.JobProgressListener(myConf)

sc.addSparkListener(myListener)
myListener.completedStages.foreach(si => (

  println("runTime: " + si.taskMetrics.executorRunTime +

          ", cpuTime: " + si.taskMetrics.executorCpuTime)))
```



关键是把listener加到SparkContext上，还有另一个办法，就是使用--conf spark.extraListeners







# 内置工具

```scala
spark.sql("select count(*) from range(10) cross join range(10)").explain(true)
spark.sql("explain select count(*) from range(10) cross join range(10)").collect.foreach(println)
// CBO
spark.sql("explain cost select count(*) from range(10) cross join range(10)").collect.foreach(println)
//Print Code generation
spark.sql("select count(*) from range(10) cross join range(10)").queryExecution.debug.codegen
spark.sql("explain codegen select count(*) from range(10) cross join range(10)").collect.foreach(println)

//for longer plans:
df.queryExecution.debug.codegenToSeq -> dumps to sequence of strings
df.queryExecution.debug.toFile -> dumps to filesystem file

// New in Spark 3.0, explain foramtted
spark.sql("explain formatted select count(*) from range(10) cross join range(10)").collect.foreach(println)
```

