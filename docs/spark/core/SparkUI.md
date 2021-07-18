# UI详解

## Executors页签

展示对当前App创建的Executors的汇总信息，包括内存、磁盘使用，task和shuffle信息。

![image-20210629094904060](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210629094904060.png)

1. Storage Memory列：已使用/保留的内存用于缓存数据。
2. 除了资源信息，还提供了性能信息(GC时间和shuffle信息)
3. core个数，task个数

> 注意，Status拦能发现一些Dead的executor。Spark开启了资源动态分配（spark.dynamicAllocation.enabled=true），当executors空闲达到设定时间（spark.dynamicAllocation.executorIdleTimeout=60s）后会被移除。
> 所以，空闲达到设定时间的Executor的状态就变成了Dead。

## Jobs

2个task并行

![image-20210708224821776](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210708224821776.png)

## Rdd

### 保存

![image-20210708225110641](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210708225110641.png)

<img src="https://gitee.com/luckywind/PigGo/raw/master/image/image-20210708225153299.png" alt="image-20210708225153299" style="zoom:50%;" />

