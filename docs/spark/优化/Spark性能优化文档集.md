[美团Spark性能优化](https://tech.meituan.com/2016/04/29/spark-tuning-basic.html)

[性能优化总结](https://mp.weixin.qq.com/s/pBArMxoQ0G9NXu_3dFpa4Q)

[Spark开发中十一种调优的思路](https://blog.51cto.com/u_15294184/3052733#RDD_3)

[spark 分区调优](https://luminousmen.com/post/spark-tips-partition-tuning)

[spark调优](https://miracle-xing.github.io/2019/08/02/Spark-%E8%B0%83%E4%BC%98/)

# 基础篇

## 动态资源分配

spark.dynamicAllocation.enabled：是否开启动态资源分配，根据工作负载调整应用程序注册的executor的数量

spark.dynamicAllocation.minExecutors, 

spark.dynamicAllocation.maxExecutors,

spark.dynamicAllocation.initialExecutors, 默认取值为minExecutors

spark.dynamicAllocation.executorAllocationRatio

spark.dynamicAllocation.executorIdleTimeout  当executors空闲达到设定时间，超时会被移除

如果启用dynamicAllocation则spark.shuffle.service.enable必须设置为true，此选项用于启动外部的shuffle服务，免得在executor释放时造成数据丢失。外部的shuffle服务运行在NodeManager节点中，独立于spark的executor，在spark配置中通过spark.shuffle.service.port指定其端口，默认为7337

```java
2021-07-02,12:33:48,553 INFO org.apache.spark.deploy.yarn.YarnAllocator: Driver requested a total number of 15 executor(s).
2021-07-02,12:33:48,554 INFO org.apache.spark.ExecutorAllocationManager: Request to remove executorIds: 57
2021-07-02,12:33:48,554 INFO org.apache.spark.scheduler.cluster.YarnClusterSchedulerBackend: Requesting to kill executor(s) 57
2021-07-02,12:33:48,554 INFO org.apache.spark.scheduler.cluster.YarnClusterSchedulerBackend: Actual list of executor(s) to be killed is 57
2021-07-02,12:33:48,554 INFO org.apache.spark.scheduler.cluster.YarnClusterSchedulerBackend: Requesting total executors by 14 (numExistingExecutors: 85, numPendingExecutors: 0, executorsPendingToRemove: 5)
2021-07-02,12:33:48,554 WARN org.apache.spark.scheduler.cluster.YarnClusterSchedulerBackend: killExecutors(ArrayBuffer(57), false, false): Executor counts do not match:
requestedTotalExecutors  = 14
numExistingExecutors     = 85
numPendingExecutors      = 0
executorsPendingToRemove = 5
2021-07-02,12:33:48,555 INFO org.apache.spark.deploy.yarn.YarnAllocator: Driver requested a total number of 14 executor(s).
2021-07-02,12:33:48,555 INFO org.apache.spark.deploy.yarn.ApplicationMaster$AMEndpoint: Driver requested to kill executor(s) 57.
```





## 内存配置

```scala
spark.memory.fraction
spark.yarn.executor.memoryOverhead
```



## 配置

```scala
"spark.locality.wait", "30" //数据本地化级别等待时间，单位秒，默认是3秒
"spark.serializer", "org.apache.spark.serializer.KryoSerializer"
"spark.kryo.registrationRequired", "true")
"spark.rdd.compress", "true")
"spark.network.timeout", "600s")
"spark.executor.heartbeatInterval", "30s") //executor 向 the driver 汇报心跳的时间间隔，2.1版本单位毫秒，2.3版本单位秒,默认10000
"spark.speculation.quantile", "0.9") //该批次所有的task有90%执行完成即对剩余的task执行推测执行，默认是0.75
"spark.speculation.interval", "60s") //一分钟检查一次推测任务，默认是100ms
```

## 序列化

https://www.jianshu.com/p/e1e19aa51eeb

https://zhmin.github.io/2019/01/08/spark-serializer/

### 序列化含义

Spark是基于JVM运行的进行，其序列化必然遵守Java的序列化规则。

序列化就是指将一个对象转化为二进制的byte流（注意，不是bit流），然后以文件的方式进行保存或通过网络传输，等待被反序列化读取出来。序列化常被用于数据存取和通信过程中。

对于java应用实现序列化一般方法：

1. class实现序列化操作是让class 实现Serializable接口，但实现该接口不保证该class一定可以序列化，因为序列化必须保证该class引用的所有属性可以序列化。

2. 这里需要明白，static和transient修饰的变量不会被序列化，这也是解决序列化问题的方法之一，让不能序列化的引用用static和transient来修饰。（static修饰的是类的状态，而不是对象状态，所以不存在序列化问题。transient修饰的变量，是不会被序列化到文件中，在被反序列化后，transient变量的值被设为初始值，如int是0，对象是null）

3. 此外还可以实现readObject()方法和writeObject()方法来自定义实现序列化

### Spark的transformation操作为什么需要序列化？

Spark是分布式执行引擎，其核心抽象是弹性分布式数据集RDD，其代表了分布在不同节点的数据。Spark的计算是在executor上分布式执行的，故用户开发的关于RDD的map，flatMap，reduceByKey等transformation 操作（闭包）有如下执行过程：

1. 代码中对象在driver本地序列化

2. 对象序列化后传输到远程executor节点

3. 远程executor节点反序列化对象

4. 最终远程节点执行

   故对象在执行中需要序列化通过网络传输，则必须经过序列化过程。

### 参数

```
spark.kryoserializer.buffer.max
spark.kryoserializer.buffer.max=256m
spark.executorEnv.JAVA_HOME=/opt/soft/jdk1.8.0/jre
spark.serializer=org.apache.spark.serializer.KryoSerializer
```

