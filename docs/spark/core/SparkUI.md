# UI详解

[官网](https://spark.apache.org/docs/3.0.0-preview/web-ui.html#stage-detail)

## Executors页签

展示对当前App创建的Executors的汇总信息，包括内存、磁盘使用，task和shuffle信息。

![image-20210629094904060](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210629094904060.png)

1. Storage Memory列：已使用/保留的内存用于缓存数据。
2. 除了资源信息，还提供了性能信息(GC时间和shuffle信息)
3. core个数，task个数

> 注意，Status拦能发现一些Dead的executor。Spark开启了资源动态分配（spark.dynamicAllocation.enabled=true），当executors空闲达到设定时间（spark.dynamicAllocation.executorIdleTimeout=60s）后会被移除。
> 所以，空闲达到设定时间的Executor的状态就变成了Dead。



## stage详情

- **Duration of tasks**.
- **GC time**  <font color=red> 和Duration时间比值判断内存分配是否合理</font>
- **Result serialization time** is the time spent serializing the task result on a executor before sending it back to the driver.
- **Getting result time** is the time that the driver spends fetching task results from workers.
- **Scheduler delay** is the time the task waits to be scheduled for execution.
- **Peak execution memory** <font color=red>shuffles, aggregations and joins 期间创建的内部数据结构占用的最大内存,可粗略估计内存使用的峰值</font>
- **Shuffle Read Size / Records**. Total shuffle bytes read, includes both data read locally and data read from remote executors.
- **Shuffle Read Blocked Time** <font color=red>从远程节点读取shuffle数据阻塞的时间，较长的话会导致shuffle效率低</font>
- **Shuffle Remote Reads** is the total shuffle bytes read from remote executors.
- **Shuffle spill (memory)** is the size of the deserialized form of the shuffled data in memory.
- **Shuffle spill (disk)** is the size of the serialized form of the data on disk.

## task处理的数据量

建议每个read task100-200M之间

每个task处理的数据量： 查看该stage shuffleRead的总数据量，除以task个数

例如下图：每个task处理的数据量=128*1024M/6315=20M

![image-20221114164606973](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221114164606973.png)

task处理的数据量确定后，task个数也就确定了，作业申请的总cpu核心数=task个数/2 OR 3

此外，还应该设置作业的并行度spark.defalut.parallelism

## task日志

缓存失效： 反复读取缓存前面的RDD:  Found block rdd_xx



## Jobs

2个task并行

![image-20210708224821776](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210708224821776.png)

## Rdd

### 保存

![image-20210708225110641](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210708225110641.png)

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210708225153299.png" alt="image-20210708225153299" style="zoom:50%;" />

# 时间

## 时间含义及计算公式

![在这里插入图片描述](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image4ba2b111182042988f8a60218afa0cda.jpg)

如图所示，时间轴上面的表示Driver 记录到的各个时间，时间轴下面的表示Executor记录到的各个时间。
我们反过来，先说 Executor 记录的各个时间，再说Driver记录的各个时间。

#### Executor 中Task运行时间
Task 在Executor端运行，有三个时间段，分别是 

1. deserializeTime  task反序列化时间，
2.  executorRunTime task执行时间，
3.  serializeTime  结果序列化时间。
   （很奇怪，Task 并没有选择和Driver端一样的方式，直接计算各个阶段的起止时间，而是选择将各个阶段的运行耗时计算好，再通过 metrics 返回给Driver



#### Driver中Task运行时间

1. **LaunchTime**: 在 TaskSetManager 中，也叫 resourceOfferTime ，即 taskSet.offerTask() 成功后，开始运行 Task 的时间
2. LaunchDelay: 表示 从Stage被提交到当前Task被调度的时间，计算公式: taskInfo.launchTime - stage.submissionTime
3. finishTime: Driver最后标识该Task生命周期结束的时间, 具体是从 handleSuccessTask(taskSet, tid, serializedData) 进入 markFinished() 的时间
4. gettingResultTime: 如果我们的 Task 返回的结果比较大，Task结果返回的方式是 IndirectTaskResult，Driver 会在记录开始调用 fetchIndirectTaskResult() 进行读取结果的时间。（实际上应该还包括对读取的数据的deserialize 时间）。如果是 DirectTaskResult ，该时间为 0。 因为代码风格问题，在偏前段的代码中，改名叫 resultFetchStart， gettingResultTime 被重新定义为函数 = {launchTime + duration - fetchStart }
5. duration: = finishTime - launchTime
6. schedulerDelay

```scala
gettingResultTime = launchTime + duration - fetchStart
schedulerDelay =  math.max(0, 
	duration - runTime - deserializeTime - serializeTime - gettingResultTime
	)
就是紫色部分的时间
```

### 参考

[各种时间含义](https://blog.csdn.net/wankunde/article/details/121403842)



## spark UI中的duration是如何计算的

```sql
spark.sql("use tpcds100gdv") spark.conf.set("spark.sql.autoBroadcastJoinThreshold", 2) spark.sql(""" select i_item_sk, sum(ss_coupon_amt) amt   from store_sales a  join item b on a.ss_item_sk =b.i_item_sk where i_category in ('Sports', 'Books', 'Home') group by  i_item_sk – i_item_sk limit 100   """).collect       spark.sql("use tpcds100gdv") spark.conf.set("spark.sql.autoBroadcastJoinThreshold", 2) spark.sql(""" select ss_store_sk, sum(ss_coupon_amt) amt   from store_sales a  join item b on a.ss_item_sk =b.i_item_sk where i_category in ('Sports', 'Books', 'Home') group by  ss_store_sk – i_item_sk limit 100   """).collect
```

1. **WSCG时间如何计算的？** WSCG duration是相关所有task从开始计算一直到WSCG计算结束这段时间的时间差（具体，就是所有task的 FinishedTime-LaunchedTime之和）， 包括了自己的逻辑和child的逻辑，以Job 0 为例， WSCG(1) 的时间包括了WSCG本身和它的child 即scan的时间。
2. **WSCG时间什么时候开始计时？为什么Job 1的duration为1.3min, 而其Stage/WSCG只有264ms?** 从task开始执行时开始计时，而非stage提交时。 本例子中，Job 0 和Job 1分别只有一个Stage, 同时提交，但Job 0的stage先获得了资源，由于只有一个线程的资源，Job 0执行完，Job 1的task才真正开始，因此在UI上我们看到Job 1 的duration是1.3min, 但 WSCG(2)却只有264ms 

## BUG:duration为零 

WholeStageCodegenExec的doExecute函数执行过程中累加duration的方式是迭代到当前分区最后一条时才累加duration。 假如我们的Sql带limit子句，那么可能没有迭代完一个分区，导致这个duration不会被累加，从而为0

```scala
mapPartitionsWithIndex { (index, zippedIter) =>
        val (leftIter, rightIter) = zippedIter.next()
        val (clazz, _) = CodeGenerator.compile(cleanedSource)
        val buffer = clazz.generate(references).asInstanceOf[BufferedRowIterator]
        buffer.init(index, Array(leftIter, rightIter))
        new Iterator[InternalRow] {
          override def hasNext: Boolean = {
            val v = buffer.hasNext
            if (!v) durationMs += buffer.durationMs()
            v
          }
          override def next: InternalRow = buffer.next()
        }
      }
```








# Spark SQL

## [通过UI高效定位问题](https://learn.lianglianglee.com/%E4%B8%93%E6%A0%8F/%E9%9B%B6%E5%9F%BA%E7%A1%80%E5%85%A5%E9%97%A8Spark/22%20Spark%20UI%EF%BC%88%E4%B8%8B%EF%BC%89%EF%BC%9A%E5%A6%82%E4%BD%95%E9%AB%98%E6%95%88%E5%9C%B0%E5%AE%9A%E4%BD%8D%E6%80%A7%E8%83%BD%E9%97%AE%E9%A2%98%EF%BC%9F.md)

1. 通过分析算子数据溢出以及内存使用峰值，判断当前Executor内存是否需要调整

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231208104854599.png" alt="image-20231208104854599" style="zoom:50%;" />



2. 观察Event Timeline 分析调度时间、shuffle时间

   - 鼠标放在"Show Additional Metrics"上可以看到每个指标具体包含哪些时间
   - 调度延迟:  包含调度器(master的一部分)把task调度到executor的时间+ executor发送结果到调度器的时间，如果调度延迟大，考虑减少task大小或者减小task的结果

   <img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231208105625206.png" alt="image-20231208105625206" style="zoom:50%;" />![image-20231208105644206](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231208105644206.png)

3. Task指标汇总中的Spill指标

​     Spill(Memory) 和Spill(Disk)分别代表溢出数据在内存和磁盘中的存储大小，Spill(Memory) /Spill(Disk) 可以认为是数据膨胀系数，可以用来估算磁盘中的数据消耗的内存。

4. Task指标，关注**Locality level**















