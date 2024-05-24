

# spark调优经验： 

1. 并行度优化
   1. 增加executor个数提升并行能力
   2. 增加分区数
2. Join优化
   1. **采用广播器进行Join代替shuffle Join**
   2. **让两个RDD共享分区器避免shuffle Join**
3. 缓存优化
   1. **公用RDD进行缓存**
   2. **checkpoint的使用**
4. 算子优化
   1. **reduceByKey代替groupByKey**
5. **调节数据本地性等待时长**







# 小文件问题

## 问题

数据按天分区，spark入库任务是16个并行度，10分钟一个批次，一天将产生24x6x16=2304个小文件，给NameNode带来压力。

## 解决方案

每晚凌晨合并前一天的数据，读取所有数据，写入到一个文件中，然后覆盖之前的所有小文件。

### 问题：

 合并程序本身效率低，且有一天的延迟

### 优化

合并写入变为并行写入，通过修改目录名称实现数据替换，去掉了回写步骤

使用coalesce代替repartition，这俩函数用于改变分区数，但coalesce跨分区移动的数据会比repartition少。

## 小表

[小表join大表](https://www.cnblogs.com/wwcom123/p/10586607.html)

# 代码优化

[参考](https://wenku.baidu.com/view/b4727846974bcf84b9d528ea81c758f5f61f29cf.html?re=view)

主要分stage、cache、partition三个方面

## stage

尽量减少shuffle

## cache

cache是lazy执行的，注意！

## partition

每个partition会产生一个task，task会返回MapStatus给driver，task数太大，超过spark.driver.maxResultSize会导致driver挂掉。或者task一半多的时间都在进行task序列化，造成了浪费。

reduceByKey(),如果不加参数，生成的rdd与父rdd的partition数相同，否则与参数相同。还可以使用coalesce()和repartition()降低partition数。

使用repartition而不使用coalesce()是为了不降低resultRDD计算的并发量。

# 资源优化

yarn-client模式下， 整个application申请的资源为：

```scala
total_vcores=executor-cores* num_executors+spark.yarn.am.cores
total_memory=(executor_memory+spark.yarn.executor.memoryOverhead)*num_executors+
				(spark.yarn.am.memory+spark.yarn.executor.memoryOverhead)
```

当申请的资源超出所指定队列的mincores和min memory时，executor就有被yarn kill的风险。

spark.yarn.executor.memoryOverhead默认是executor-memory * 0.1，最小是384M。比如，我们的executor-memory设置为1G，spark.yarn.executor.memoryOverhead是默认的384M，则我们向yarn申请使用的最大内存为1408M，但由于yarn的限制为倍数（不知道是不是只是我们的集群是这样），实际上yarn运行我们运行的最大内存为2G。这样感觉浪费申请的内存，申请的堆内存为1G，实际上却给我们分配了2G，如果对spark.yarn.executor.memoryOverhead要求不高的话，可以对executor-memory再精细化，比如申请executor-memory为640M，加上最小384M的spark.yarn.executor.memoryOverhead，正好一共是1G。



(executor_memory+spark.yarn.executor.memoryOverhead)是所能使用的内存上限，超过此上限也会被yarn kill掉。

executor默认的永久代内存是64K，永久代使用率长时间比较高，通过--conf spark.executor.extraJavaOptions="-XX:MaxPermSize=64m"增大永久带内存。

# 内存/GC优化

## OOM调参

[参考](http://www.nituchao.com/bigdata-spark/44.html)

### spark1.5.1

一个Executor对应一个JVM进程。 从Spark的角度看，Executor占用的内存分为两部分：ExecutorMemory和MemoryOverhead

1、 ExecutorMemory为JVM进程的Java堆区域。大小通过属性spark.executor.memory设置。用于缓存RDD数据的memoryStore位于这一区域。 一个Executor用于存储RDD的空间=(ExecutorMemory– MEMORY_USED_BY_RUNTIME) * spark.storage.memoryFraction *spark.storage.safetyFraction 参数说明：
spark.storage.memoryFraction该参数用于设置RDD持久化数据在Executor内存中能占的比例，默认是0.6。也就是说，默认Executor 60%的内存，可以用来保存持久化的RDD数据。
spark.storage.safetyFraction：Spark1.5.1进程的默认堆空间是1g，为了安全考虑同时避免OOM,Spark只允许利用90%的堆空间，spark中使用spark.storage.safetyFraction用来配置该值（默认是0.9).
(spark.shuffle.memoryFraction该参数用于设置shuffle过程中一个task拉取到上个stage的task的输出后，进行聚合操作时能够使用的Executor内存的比例，默认是0.2。)
2、 MemoryOverhead是JVM进程中除Java堆以外占用的空间大小，包括方法区（永久代）、Java虚拟机栈、本地方法栈、JVM进程本身所用的内存、直接内存（Direct Memory）等。 通过spark.yarn.executor.memoryOverhead设置，单位MB。如果Java堆或者永久代的内存不足，则会产生各种OOM异常，executor会被结束。 在Java堆以外的JVM进程内存占用较多的情况下，应该将MemoryOverhead设置为一个足够大的值，以防JVM进程因实际占用的内存超标而被kill。

当在YARN上运行Spark作业，每个Spark executor作为一个YARN容器运行。Spark可以使得多个Tasks在同一个容器里面运行。 同样，实际运行过程中ExecutorMemory+MemoryOverhead之和（JVM进程总内存）超过container的容量。YARN会直接杀死container。

Spark对Executor和Driver额外添加堆内存大小：

Executor端：由spark.yarn.executor.memoryOverhead设置，spark1.5.1默认值executorMemory * 0.10与384的最大值。
Driver端：由spark.yarn.driver.memoryOverhead设置，spark1.5.1默认值driverMemory * 0.10与384的最大值。

原文链接：https://blog.csdn.net/baolibin528/article/details/54406540

## Direct Memory

​        shuffle过程中block的传输使用netty，基于netty的shuffle使用direct memory进行buffer，在大数据量shuffle时，堆外内存使用较多。通过-XX:MaxDirectMemorySize可以指定最大的direct memory，默认与最大堆内存相同。

​       Direct Memory是受GC控制的，例如ByteBuffer bb=ByteBuffer.allocateDirect(1024),这段代码的执行会在堆外占用1k的内存，而Java堆内只会占用一个对象指针引用的大小，堆外的这1k的空间只有当bb对象被回收时，才会被回收，这里会发现一个明显的不对称现象，就是堆外可能占用了很多，而堆内没占用多少，导致还没出发GC。加上-XX:MaxDirectMemorySize这个大小限制后，则只要Direct Memory使用达到了这个大小，就会强制触发GC，这个大小如果设置的不够用，那么日志中会看到`java.lang.OutOfMemoryError: Direct buffer memory` 。

发现堆外内存飙升的比较快，很容易被yarn kill掉，所以应适当调小-XX:MaxDirectMemorySize（也不能过小，否则会报Direct buffer memory异常）。当然你也可以调大spark.yarn.executor.memoryOverhead，加大yarn对我们使用内存的宽容度，但是这样比较浪费资源了。





Direct Memory, 读写Parquet+Snappy, 如果采用小米内部的parquet mdh版本已经控制至多64MB, 内部Spark平台已经默认采用, 如果是其他版本的则不可控制.
Spark堆外内存控制参数:
堆外内存的使用总量 = jvmOverhead(off heap) + directMemoryOverhead(direct memory) + otherMemoryOverhead

| 参数                                     | 描述                                                         | 默认值                           |
| ---------------------------------------- | ------------------------------------------------------------ | -------------------------------- |
| spark.yarn.executor.jvmMemoryOverhead    | off heap内存控制                                             | max(0.1 * executorMemory, 384MB) |
| spark.yarn.executor.directMemoryOverhead | Direct Memory的控制参数                                      | 256MB                            |
| spark.yarn.driver.jvmMemoryOverhead      | 同Executor                                                   |                                  |
| spark.yarn.driver.directMemoryOverhead   | 同Executor                                                   |                                  |
| spark.yarn.executor.memoryOverhead       | 统筹参数, 如果设置了该值m, 会自动按比例分配off heap给jvmOverhead和directMemory,  分配比例为jvmOverhead = max(0.1 * executorMemory, 384MB), directMemoryOverhead =m  - jvmOverhead | 无                               |
| spark.yarn.driver.memoryOverhead         | 同Executor                                                   |                                  |

原文链接：https://blog.csdn.net/Joyce__Yin/article/details/79551341



## GC优化 

GC优化前，最好是把gc日志打出来，便于我们进行调试。

```shell
 --conf spark.executor.extraJavaOptions="-XX:+PrintGC 
 -XX:+PrintGCDetails 
 -XX:+PrintGCTimeStamps 
 -XX:+PrintGCDateStamps 
 -XX:+PrintGCApplicationStoppedTime 
 -XX:+PrintHeapAtGC 
 -XX:+PrintGCApplicationConcurrentTime 
 -Xloggc:gc.log"
```



 通过看gc日志，我们发现一个case，特定时间段内，堆内存其实很闲，堆内存使用率也就5%左右，长时间不进行full gc，导致Direct Memory一直不进行回收，一直在飙升。所以，我们的目标是让full gc更频繁些，多触发一些Direct Memory回收。 第一，可以减少整个堆内存的大小，当然也不能太小，否则堆内存也会报OOM。这里，我配置了1G的最大堆内存。 第二，可以让年轻代的对象尽快进入年老代，增加年老代的内存。这里我使用了-Xmn100m，将年轻代大小设置为100M。另外，年轻代的对象默认会在young gc 15次后进入年老代，这会造成年轻代使用率比较大，young gc比较多，但是年老代使用率低，full gc比较少，通过配置-XX:MaxTenuringThreshold=1，年轻代的对象经过一次young gc后就进入年老代，加快年老代full gc的频率。 第三，可以让年老代更频繁的进行full gc。一般年老代gc策略我们主要有-XX:+UseParallelOldGC和-XX:+UseConcMarkSweepGC这两种，ParallelOldGC吞吐率较大，ConcMarkSweepGC延迟较低。我们希望full gc频繁些，对吞吐率要求较低，而且ConcMarkSweepGC可以设置-XX:CMSInitiatingOccupancyFraction，即年老代内存使用率达到什么比例时触发CMS。我们决定使用CMS，并设置-XX:CMSInitiatingOccupancyFraction=10，即年老代使用率10%时触发父gc。 
通过对GC策略的配置，我们发现父gc进行的频率加快了，带来好处就是Direct Memory能够尽快进行回收，当然也有坏处，就是gc时间增加了，cpu使用率也有所增加。 最终我们对executor的配置如下： 

```shell
--executor-memory1G --num-executors 160 --executor-cores 1 --conf spark.yarn.executor.memoryOverhead=2048 --conf spark.executor.extraJavaOptions="
-XX:MaxPermSize=64m 
-XX:+CMSClassUnloadingEnabled 
-XX:MaxDirectMemorySize=1536m 
-Xmn100m -XX:MaxTenuringThreshold=1 
-XX:+UseConcMarkSweepGC 
-XX:+CMSParallelRemarkEnabled 
-XX:+UseCMSCompactAtFullCollection 
-XX:+UseCMSInitiatingOccupancyOnly 
-XX:CMSInitiatingOccupancyFraction=10 
-XX:+UseCompressedOops 
-XX:+PrintGC 
-XX:+PrintGCDetails 
-XX:+PrintGCTimeStamps 
-XX:+PrintGCDateStamps 
-XX:+PrintGCApplicationStoppedTime 
-XX:+PrintHeapAtGC 
-XX:+PrintGCApplicationConcurrentTime 
-Xloggc:gc.log 
-XX:+HeapDumpOnOutOfMemoryError" 
```



# oneid

1. 增加executor数量**executor越多，spark任务并行能力越强**
   executor为3，core为2，则同一时间可以同时执行6个task
   executor为6，core为2，则同一时间可以同时执行12个task

2. 增加core数量

   **core越多，spark任务并行执行能力越强**
   executor为3，core为2，则同一时间可以同时执行6个task
   executor为3，core为4，则同一时间可以同时执行12个task

3. 增加executor内存

- 可以cache更多的数据到内存，提高读性能
- shuffle操作聚合时，若内存不足也会写到磁盘，影响性能
- task执行过程会创建很多对象，若内存不足会频繁GC，影响性能

4. 增加driver内存

如果spark任务中有collect或者广播变量比较大时，可以适当调大该值避免OOM

5. 广播变量的使用如果执行任务的过程中，依赖的外部中间数据比较大，或者执行任务的task数量比较大，可以考虑采用广播变量

   采用广播变量后，数据不再加载`dataNum *taskNum ,而是仅加载 dataNum* ExecutorNum`
   减少内存使用量以及提高spark任务的性能

   如果每个task都加载一份占用大量内存，会直接导致

   - 持久化到内存的RDD会被溢写到磁盘，无法在提升读性能
   - 导致频繁的GC
   - 涉及到大量的网络传输

6. 避免shuffle
   reduceByKey代替groupByKey

   因为前两者会在shuffle上游对数据先做一次预聚合，这个操作的好处有

   - 大大减少shuffle阶段网络传输的数据
   - 上游先做一次预聚合，会大大减小下游做聚合时的计算时间

7. 采用Kryo序列化提供序列化性能

   Kryo比Java原生的序列化方式要快，且序列化之后的数据仅为Java的1/10

   带来的好处有

   - 优化读取外部数据性能
     序列化后的数据量变少，提高网络传输效率
   - 缓存时采用序列化
     持久化的RDD占用内存更少，task执行过程中GC的频率也会下降
   - shuffle阶段性能提升
     网络传输的性能提升

8. 调节数据本地化等待时长

9. 多个RDD进行union操作时，避免使用rdd.union(rdd).union(rdd).union(rdd)这种多重union，rdd.union只适合2个RDD合并，合并多个时采用SparkContext.union(Array(RDD))，避免union嵌套层数太多，导致的调用链路太长，耗时太久，且容易引发StackOverFlow
