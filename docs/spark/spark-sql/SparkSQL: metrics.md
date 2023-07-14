
# metrics是怎么传播的

[spark中metrics是怎么传播的](https://blog.csdn.net/monkeyboy_tech/article/details/128294869?spm=1001.2101.3001.6650.3&utm_medium=distribute.pc_relevant.none-task-blog-2%7Edefault%7EBlogCommendFromBaidu%7ERate-3-128294869-blog-118899820.235%5Ev36%5Epc_relevant_default_base3&depth_1-utm_source=distribute.pc_relevant.none-task-blog-2%7Edefault%7EBlogCommendFromBaidu%7ERate-3-128294869-blog-118899820.235%5Ev36%5Epc_relevant_default_base3&utm_relevant_index=6)

在Driver端定义的metrics，会被反序列化到Executor端，在Executor端，通过两种方式传回Driver端：

- 在任务运行期间，利用heartbeat心跳来传递metrics
- 在任务结束以后，利用任务结果的更新来传递metrics
  最终，都是通过sparkListener：SQLAppStatusListener和 AppStatusListener分别完成Spark UI状态的更新。

实际还有一种方式，就是task失败了

## SQLMetric与注册
### class SQLMetric

```scala
abstract class AccumulatorV2[IN, OUT] 
```

这是一个累加器基类，累加IN类型的输入，输出为OUT类型。

SQLMetric是AccumulatorV2的一个实现(输入、输出都为Long类型)，executor端的更新通过metrics自动传播并展示到SQL UI上； 而driver端的更新必须显示的调用*SQLMetrics.postDriverMetricUpdates()*方法完成推送

```scala
class SQLMetric(val metricType: String, initValue: Long = 0L) extends AccumulatorV2[Long, Long] 
```

### 创建与注册流程

例如FilterExec创建一个metric 

```scala
  override lazy val metrics = Map(
    "numOutputRows" -> SQLMetrics.createMetric(sparkContext, "number of output rows"))

// object SQLMetrics
  def createMetric(sc: SparkContext, name: String): SQLMetric = {
    val acc = new SQLMetric(SUM_METRIC)
    acc.register(sc, name = metricsCache.get(name), countFailedValues = false)
    acc
  }
// AccumulatorV2
  private[spark] def register(
      sc: SparkContext,
      name: Option[String] = None,
      countFailedValues: Boolean = false): Unit = {
    if (this.metadata != null) {
      throw new IllegalStateException("Cannot register an Accumulator twice.")
    }
    //创建一个有全局唯一ID的AccumulatorMetadata
    this.metadata = AccumulatorMetadata(AccumulatorContext.newId(), name, countFailedValues)
    //注册
    AccumulatorContext.register(this)
    //清理Driver端的metrics
    sc.cleaner.foreach(_.registerAccumulatorForCleanup(this))
  }
//AccumulatorContext
  //注册一个driver端创建的AccumulatorV2，从而executor可以使用。这里注册的累加器可以用作一个跨作业的聚合容器
  def register(a: AccumulatorV2[_, _]): Unit = {
     //originals是一个ConcurrentHashMap，保存了driver端创建的累加器对象， 这个map保存了这些对象的弱引用从而当这些对象在不需要时可被垃圾回收掉
    originals.putIfAbsent(a.id, new jl.ref.WeakReference[AccumulatorV2[_, _]](a))
  }
```



## executor端如何回传到driver端

### 前置条件

#### TaskMetrics

要回答本节问题，我们看这个类的注释即可: 

```scala
* Metrics tracked during the execution of a task.
 *
 * This class is wrapper around a collection of internal accumulators that represent metrics
 * associated with a task. The local values of these accumulators are sent from the executor
 * to the driver when the task completes. These values are then merged into the corresponding
 * accumulator previously registered on the driver.
 *
 * The accumulator updates are also sent to the driver periodically (on executor heartbeat)
 * and when the task failed with an exception. The [[TaskMetrics]] object itself should never
 * be sent to the driver.
```

1. task结束时，累加器的值会被发送到driver端
2. 通过executor心跳周期性发送给driver端
3. task失败

#### metrics在driver端的序列化

DAGScheduler在调用submitMissingTasks时会使用taskMetrics创建Task，此时会把taskMetrics进行序列化

```scala
    val tasks: Seq[Task[_]] = try {
      //对taskMetrics序列化
      val serializedTaskMetrics = closureSerializer.serialize(stage.latestInfo.taskMetrics).array()
      stage match {
        case stage: ShuffleMapStage =>
          stage.pendingPartitions.clear()
          partitionsToCompute.map { id =>
            val locs = taskIdToLocations(id)
            val part = partitions(id)
            stage.pendingPartitions += id
            //创建ShuffleMapTask
            new ShuffleMapTask(stage.id, stage.latestInfo.attemptNumber, taskBinary,
              part, stage.numPartitions, locs, properties, serializedTaskMetrics, Option(jobId),
              Option(sc.applicationId), sc.applicationAttemptId, stage.rdd.isBarrier())
          }

        case stage: ResultStage =>
          partitionsToCompute.map { id =>
            val p: Int = stage.partitions(id)
            val part = partitions(p)
            val locs = taskIdToLocations(id)
            new ResultTask(stage.id, stage.latestInfo.attemptNumber,
              taskBinary, part, stage.numPartitions, locs, id, properties, serializedTaskMetrics,
              Option(jobId), Option(sc.applicationId), sc.applicationAttemptId,
              stage.rdd.isBarrier())
          }
      }
    } 
```



#### metrics在Executor端的反序列化与存储

driver端注册的metrics，随着task对象一起序列化传递到executor端，executor在执行task时会对metrics进行反序列化。

反序列化后的metrics会放入taskMetrics中，taskMetrics再使用一个数组来保存。

```scala
// AccumulatorV2  的反序列化
private def readObject(in: ObjectInputStream): Unit = Utils.tryOrIOException {
    in.defaultReadObject()
    if (atDriverSide) {
      atDriverSide = false
      val taskContext = TaskContext.get()
      if (taskContext != null) {
        //调用TaskContextImpl的注册方法
        taskContext.registerAccumulator(this)
      }
    } else {
      atDriverSide = true
    }
  }


// TaskContextImpl  是Task的一个属性
private[spark] override def registerAccumulator(a: AccumulatorV2[_, _]): Unit = {
    //放入TaskMetrics维护的数组中，new ArrayBuffer[AccumulatorV2[_, _]]
    taskMetrics.registerAccumulator(a)
  }

```

### TaskRunner.run()

TaskRunner是task反序列化的入口，这里会反序列化Task，从而会反序列化其metrics对象

且会真正执行task，task结束后，会把metrics同步给driver

```scala
        //反序列化task ,这里序列化器会调用AccumulatorV2的readObject方法      
       task = ser.deserialize[Task[Any]](
          taskDescription.serializedTask, Thread.currentThread.getContextClassLoader)
        task.localProperties = taskDescription.properties
        task.setTaskMemoryManager(taskMemoryManager)
  //执行task
       val value =task.run

   //计算metrics
       task.metrics.setXXX
// 更新metrics
val accumUpdates = task.collectAccumulatorUpdates()
   //serializedResult=task执行结果 + accumUpdates
   //这里会把metrics同步给driver
  execBackend.statusUpdate(taskId, TaskState.FINISHED, serializedResult)

 ... ...
//收集当前task使用的累加器的最新值，如果task失败，则过滤出不统计失败数据的累加器
  def collectAccumulatorUpdates(taskFailed: Boolean = false): Seq[AccumulatorV2[_, _]] = {
    if (context != null) {
      // Note: internal accumulators representing task metrics always count failed values
      context.taskMetrics.nonZeroInternalAccums() ++
        // zero value external accumulators may still be useful, e.g. SQLMetrics, we should not
        // filter them out.
        context.taskMetrics.externalAccums.filter(a => !taskFailed || a.countFailedValues)
    } else {
      Seq.empty
    }
  }
```



1. 这里会调用ser.deserialize方法，从而触发AccumulatorV2的readObject方法，从而该AccumulatorV2变量会保存在executor端，且保留了全局唯一id。
2. val accumUpdates = task.collectAccumulatorUpdates() 收集spark内置的metrics(如remoteBlocksFetched)和自定义的metrics，
   这个会通过execBackend.statusUpdate方法，传达Driver端，最终调用到DAGScheduler的updateAccumulators方法更新指标：

#### statusUpdate

```scala
// CoarseGrainedExecutorBackend.scala  
override def statusUpdate(taskId: Long, state: TaskState, data: ByteBuffer): Unit = {
    val resources = taskResources.getOrElse(taskId, Map.empty[String, ResourceInformation])
    val msg = StatusUpdate(executorId, taskId, state, data, resources)
    if (TaskState.isFinished(state)) {
      taskResources.remove(taskId)
    }
    driver match {
      //task结果发送给driver端
      case Some(driverRef) => driverRef.send(msg)
      case None => logWarning(s"Drop $msg because has not yet connected to driver")
    }
  }
```

### DAGScheduyler

#### handleTaskCompletion

```scala
    event.reason match {
      case Success =>
        task match {
          case rt: ResultTask[_, _] =>
            val resultStage = stage.asInstanceOf[ResultStage]
            resultStage.activeJob match {
              case Some(job) =>
                // Only update the accumulator once for each result task.
                if (!job.finished(rt.outputId)) {
                  updateAccumulators(event)
                }
              case None => // Ignore update if task's job has finished.
            }
          case _ =>
            updateAccumulators(event)
        }
      case _: ExceptionFailure | _: TaskKilled => updateAccumulators(event)
      case _ =>
    }
    postTaskEnd(event)
```



##### updateAccumulators

task结果通过Netty发送给了driver端，driver端最终通过DAGScheduler`的`updateAccumulators方法更新指标

主要逻辑就是把 task数据merge到driver之前注册的累加器上。

```scala
  private def updateAccumulators(event: CompletionEvent): Unit = {
    val task = event.task
    val stage = stageIdToStage(task.stageId)

    event.accumUpdates.foreach { updates =>
      val id = updates.id
      try {
        // Find the corresponding accumulator on the driver and update it
        val acc: AccumulatorV2[Any, Any] = AccumulatorContext.get(id) match {
          case Some(accum) => accum.asInstanceOf[AccumulatorV2[Any, Any]]
          case None =>
            throw SparkCoreErrors.accessNonExistentAccumulatorError(id)
        }
        // 执行merge，完成指标更新
        acc.merge(updates.asInstanceOf[AccumulatorV2[Any, Any]])
        // To avoid UI cruft, ignore cases where value wasn't updated
        if (acc.name.isDefined && !updates.isZero) {
          stage.latestInfo.accumulables(id) = acc.toInfo(None, Some(acc.value))
          // 更新当前event的指标到最新
          event.taskInfo.setAccumulables(
            acc.toInfo(Some(updates.value), Some(acc.value)) +: event.taskInfo.accumulables)
        }
      } catch {
        case NonFatal(e) =>
          // Log the class name to make it easy to find the bad implementation
          val accumClassName = AccumulatorContext.get(id) match {
            case Some(accum) => accum.getClass.getName
            case None => "Unknown class"
          }
          logError(
            s"Failed to update accumulator $id ($accumClassName) for task ${task.partitionId}",
            e)
      }
    }
  }

```

##### postTaskEnd

发送SparkListenerTaskEnd事件

```scala
  private def postTaskEnd(event: CompletionEvent): Unit = {
    val taskMetrics: TaskMetrics =
      if (event.accumUpdates.nonEmpty) {
        try {
          TaskMetrics.fromAccumulators(event.accumUpdates)
        } catch {
          case NonFatal(e) =>
            val taskId = event.taskInfo.taskId
            logError(s"Error when attempting to reconstruct metrics for task $taskId", e)
            null
        }
      } else {
        null
      }
// post SparkListenerTaskEnd事件
    listenerBus.post(SparkListenerTaskEnd(event.task.stageId, event.task.stageAttemptId,
      Utils.getFormattedClassName(event.task), event.reason, event.taskInfo,
      new ExecutorMetrics(event.metricPeaks), taskMetrics))
  }
```

这个SparkListenerTaskEnd事件最终被从而被 AppStatusListener的onTaskEnd方法接受，从而完成Spark UI的更新（被AppStatusStore调用）。
同时也被SQLAppStatusListener的onTaskEnd方法接受，结果也是完成Spark UI的更新(被SQLAppStatusStore调用)

### Executor

#### reportHeartBeat通过心跳周期性发送

周期性向driver发送心跳， 最终会调用到`DAGScheduler`的`executorHeartbeatReceived`方法，从而被`AppStatusListener`的`onExecutorMetricsUpdate`方法接受：

```scala
  /** Reports heartbeat and metrics for active tasks to the driver. */
  private def reportHeartBeat(): Unit = {
    // list of (task id, accumUpdates) to send back to the driver
    val accumUpdates = new ArrayBuffer[(Long, Seq[AccumulatorV2[_, _]])]()
    val curGCTime = computeTotalGcTime()

    if (pollOnHeartbeat) {
      metricsPoller.poll()
    }

    val executorUpdates = metricsPoller.getExecutorUpdates()

    for (taskRunner <- runningTasks.values().asScala) {
      if (taskRunner.task != null) {
        taskRunner.task.metrics.mergeShuffleReadMetrics()
        taskRunner.task.metrics.setJvmGCTime(curGCTime - taskRunner.startGCTime)
        val accumulatorsToReport =
          if (HEARTBEAT_DROP_ZEROES) {
            taskRunner.task.metrics.accumulators().filterNot(_.isZero)
          } else {
            taskRunner.task.metrics.accumulators()
          }
        accumUpdates += ((taskRunner.taskId, accumulatorsToReport))
      }
    }
 //  心跳消息
    val message = Heartbeat(executorId, accumUpdates.toArray, env.blockManager.blockManagerId,
      executorUpdates)
    try {
      //发送心跳消息
      val response = heartbeatReceiverRef.askSync[HeartbeatResponse](
        message, new RpcTimeout(HEARTBEAT_INTERVAL_MS.millis, EXECUTOR_HEARTBEAT_INTERVAL.key))
      if (!executorShutdown.get && response.reregisterBlockManager) {
        logInfo("Told to re-register on heartbeat")
        env.blockManager.reregister()
      }
      heartbeatFailures = 0
    } catch {
      case NonFatal(e) =>
        logWarning("Issue communicating with driver in heartbeater", e)
        heartbeatFailures += 1
        if (heartbeatFailures >= HEARTBEAT_MAX_FAILURES) {
          logError(s"Exit as unable to send heartbeats to driver " +
            s"more than $HEARTBEAT_MAX_FAILURES times")
          System.exit(ExecutorExitCode.HEARTBEAT_FAILURE)
        }
    }
  }
```

Metrics的展示

可以看SparkPlanGraph.scala

[metrics整理](https://www.jianshu.com/p/e42ad9cb66a1)

# SQL metrics的一个bug

## sql

```scala
161上
spark.sql("use tpcds100gdv")
spark.conf.set("spark.sql.autoBroadcastJoinThreshold", 2)

val stageMetrics = ch.cern.sparkmeasure.StageMetrics(spark)
stageMetrics.begin()
val taskMetrics = ch.cern.sparkmeasure.TaskMetrics(spark)
taskMetrics.begin()
// 示例代码
spark.sql("""
select ss_store_sk, sum(ss_coupon_amt) amt   from store_sales a  join item b
on a.ss_item_sk =b.i_item_sk
where i_category in ('Sports', 'Books', 'Home')
group by  ss_store_sk -- i_item_sk
limit 100
  """).collect

stageMetrics.end()
taskMetrics.end()
stageMetrics.printReport
taskMetrics.printReport




多线程跑
spark.sql("use tpcds1gv")
spark.conf.set("spark.sql.autoBroadcastJoinThreshold", 2)

val stageMetrics = ch.cern.sparkmeasure.StageMetrics(spark)
stageMetrics.begin()
val taskMetrics = ch.cern.sparkmeasure.TaskMetrics(spark)
taskMetrics.begin()
// 示例代码
spark.sql("""
select i_item_sk, sum(ss_coupon_amt) amt   from store_sales a  join item b
on a.ss_item_sk =b.i_item_sk
where i_category in ('Sports', 'Books', 'Home')
group by i_item_sk
limit 100
  """).collect

stageMetrics.end()
taskMetrics.end()
stageMetrics.printReport
taskMetrics.printReport
多线程测试，两个stage同时提交，抢到资源的第一个stage同时跑多个task， 当一些跑的快的task释放资源后，如果此时这个stage没有更多的task进来， 第二个stage就可以开始跑，也就是两个stage同时跑
```

[sql-metrics](https://spark.apache.org/docs/latest/web-ui.html#sql-metrics)

## WSCG duration计算BUG

WSCG天然的把执行过程分为更容易计算duration的pipeline，且比task粒度更细(一个task可以有多个pipeline)

在迭代到最后一条记录时，更新duration

```scala
 // 初始化    
  val durationMs = longMetric("pipelineTime")

    // Even though rdds is an RDD[InternalRow] it may actually be an RDD[ColumnarBatch] with
    // type erasure hiding that. This allows for the input to a code gen stage to be columnar,
    // but the output must be rows.
    val rdds = child.asInstanceOf[CodegenSupport].inputRDDs()
    assert(rdds.size <= 2, "Up to two input RDDs can be supported")
    if (rdds.length == 1) {
   // 只有一个child  , 计算所有分区的duration并累加  
      rdds.head.mapPartitionsWithIndex { (index, iter) =>
        val (clazz, _) = CodeGenerator.compile(cleanedSource)
        val buffer = clazz.generate(references).asInstanceOf[BufferedRowIterator]
        buffer.init(index, Array(iter))
        new Iterator[InternalRow] {
          override def hasNext: Boolean = {
            val v = buffer.hasNext
            // 在迭代到最后一条记录时，更新duration
            // buffer.durationMs() 就是当前分区从开始迭代到结束的耗时
            if (!v) durationMs += buffer.durationMs()
            v
          }
          override def next: InternalRow = buffer.next()
        }
      }
    } else {
      // 有两个child
      rdds.head.zipPartitions(rdds(1)) { (leftIter, rightIter) =>
        Iterator((leftIter, rightIter))
        // a small hack to obtain the correct partition index
      }.mapPartitionsWithIndex { (index, zippedIter) =>
        val (leftIter, rightIter) = zippedIter.next()
        val (clazz, _) = CodeGenerator.compile(cleanedSource)
        val buffer = clazz.generate(references).asInstanceOf[BufferedRowIterator]
        buffer.init(index, Array(leftIter, rightIter))
        new Iterator[InternalRow] {
          override def hasNext: Boolean = {
            val v = buffer.hasNext
             // 在迭代到最后一条记录时，更新duration
            if (!v) durationMs += buffer.durationMs()
            v
          }
          override def next: InternalRow = buffer.next()
        }
      }
    }
```

从源码上看，只有迭代WSCG且迭代到hasNext为false才会计算duration

debug发现在有两个child的情况下，hasNext一直为true，导致duration一直加不了

猜测是我们有limit,spark不用迭代所有数据， 去掉limit 后果然有时间显示了。

## 复现

```scala
spark.sql("use tpcds1gv")
spark.sql("""
select i_item_sk from item
limit 100
  """).collect
```

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230526150703593.png" alt="image-20230526150703593" style="zoom:33%;" />

物理计划

```
== Physical Plan ==
CollectLimit (3)
+- * ColumnarToRow (2)
   +- Scan parquet tpcds1gv.item (1)

(3) CollectLimit
Input [1]: [i_item_sk#0]
Arguments: 100
```

发现最后的CollectLimit算子，触发调用

CollectLimit.executeCollect()->SparkPlan.executeTake(100)->SparkPlan.getByteArrayRdd(100)

```scala
  private def getByteArrayRdd(
      n: Int = -1, takeFromEnd: Boolean = false): RDD[(Long, Array[Byte])] = {
    execute().mapPartitionsInternal { iter =>
      var count = 0
      val buffer = new Array[Byte](4 << 10)  // 4K
      val codec = CompressionCodec.createCodec(SparkEnv.get.conf)
      val bos = new ByteArrayOutputStream()
      val out = new DataOutputStream(codec.compressedOutputStream(bos))
     {
        // `iter.hasNext` may produce one row and buffer it, we should only call it when the
        // limit is not hit.
        // 这里的iter就是WholeStageCodegenExec算子生成的GeneratedIteratorForCodegenStage$1 类，代表一个分区的迭代器
        // 只有在n<0(不限制条数)或者没收集到n条数据时，才会调用iter.hasNext
        while ((n < 0 || count < n) && iter.hasNext) {
          val row = iter.next().asInstanceOf[UnsafeRow]
          out.writeInt(row.getSizeInBytes)
          row.writeToStream(out, buffer)
          count += 1
        }
      }
      out.writeInt(-1)
      out.flush()
      out.close()
      Iterator((count, bos.toByteArray))
    }
  }
```

也就是说CollectLimit只会遍历GeneratedIteratorForCodegenStage n次。

整体调用链：

1. WholeStageCodegenExec.doExecute返回一个迭代器GeneratedIteratorForCodegenStage
2. SparkPlan.getByteArrayRdd迭代GeneratedIteratorForCodegenStage

