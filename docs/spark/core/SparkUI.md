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
