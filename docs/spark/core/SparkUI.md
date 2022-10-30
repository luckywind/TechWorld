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

