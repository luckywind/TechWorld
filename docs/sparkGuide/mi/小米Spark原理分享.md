[参考](https://github.com/linbojin/spark-notes/blob/master/rdd-abstraction.md)

# RDD接口

## 五大属性

```scala
 *  - A list of partitions
 *  - A function for computing each split
 *  - A list of dependencies on other RDDs
 *  - Optionally, a Partitioner for key-value RDDs (e.g. to say that the RDD is hash-partitioned)
 *  - Optionally, a list of preferred locations to compute each split on (e.g. block locations for
 *    an HDFS file)
```

spark的所有调度和执行都是基于这些方法，允许每个RDD实现自己的计算方式。用户可以通过重写这些方法实现自定义RDD。

<img src="https://gitee.com/luckywind/PigGo/raw/master/image/image-20200925153036395.png" alt="image-20200925153036395" style="zoom: 33%;" /><img src="https://gitee.com/luckywind/PigGo/raw/master/image/image-20200925153126218.png" alt="image-20200925153126218" style="zoom:35%;" />

## 依赖

窄依赖：输入分区不会分叉到多个分区

宽依赖：输入分区分叉到多个分区

## RDD实例

HadoopRDD

```java
partitions = one per HDFS block
dependencies = none
compute(partition) = read corresponding block
preferredLocations(part) = HDFS block location
partitioner = none
```

MappedRDD

```java
partitions = same as parent RDD
dependencies = “one-to-one” on parent
compute(partition) = compute parent and map it
```

ShuffledRDD

```java
partitions = one per reduce task
dependencies = “shuffle” on parent
compute(partition) = read shuffled data
preferredLocations(part) = none
partitioner = HashPartitioner(numTasks)
```

![image-20200925154007159](https://gitee.com/luckywind/PigGo/raw/master/image/image-20200925154007159.png)

CoGroupedRDD

```java
partitions = one per reduce task
dependencies = “shuffle” / “one-to-one”
compute(partition) = read and join shuffled data
preferredLocations(part) = none
partitioner = HashPartitioner(numTasks)

```

<img src="https://gitee.com/luckywind/PigGo/raw/master/image/image-20200925154534177.png" alt="image-20200925154534177" style="zoom:50%;" />

# Spark内核(Job生命周期)

## Job

RDD中定义了一个collect()接口，它的作用是返回这个RDD的所有数据

```scala
  def collect(): Array[T] = withScope {
    val results = sc.runJob(this, (iter: Iterator[T]) => iter.toArray)
    Array.concat(results: _*)
  }
```

这里会调sc.runJob产生一个Job

SparkContext最终调用下面这个方法，这也是所有Action算子的主入口

```scala
  def runJob[T, U: ClassTag](
      rdd: RDD[T],
      func: (TaskContext, Iterator[T]) => U,
      partitions: Seq[Int],
      resultHandler: (Int, U) => Unit): Unit = {
    if (stopped.get()) {
      throw new IllegalStateException("SparkContext has been shutdown")
    }
    val callSite = getCallSite
    val cleanedFunc = clean(func)
    logInfo("Starting job: " + callSite.shortForm)
    if (conf.getBoolean("spark.logLineage", false)) {
      logInfo("RDD's recursive dependencies:\n" + rdd.toDebugString)
    }
    dagScheduler.runJob(rdd, cleanedFunc, partitions, callSite, resultHandler, localProperties.get)
    progressBar.foreach(_.finishAll())
    rdd.doCheckpoint()
  }
```

## Stage

从最后一个RDD开始往前推，遇到shuffle算子，就划分一个stage，依次划分出的stage编号递增。

<img src="https://gitee.com/luckywind/PigGo/raw/master/image/image-20200925155814335.png" alt="image-20200925155814335" style="zoom:50%;" />

### Final Stage/Result Task

![image-20200925160445365](https://gitee.com/luckywind/PigGo/raw/master/image/image-20200925160445365.png)

## 调度过程

通过RDD的API构建DAG

DAGScheduler把DAG划分成包含task的一个个Stage，

一旦一个Stage所依赖的父Stage完成了，就把这个Stage提交给TaskScheduler

TaskScheduler通过cluster manager提交task列表

Executor负责执行task。

![image-20200925160558282](https://gitee.com/luckywind/PigGo/raw/master/image/image-20200925160558282.png)

## Spark内核(容错)

1. 重新计算
2. persist
3. checkpoint

**Fault tolerance**: 其他的in-memory storage on clusters，基本单元是可变的，用细粒度更新（**fine-grained updates**）方式改变状态，如改变table/cell里面的值，这种模型的容错只能通过复制多个数据copy，需要传输大量的数据，容错效率低下。而RDD是**不可变的（immutable）**，通过粗粒度变换（**coarse-grained transformations**），比如map，filter和join，可以把相同的运算同时作用在许多数据单元上，这样的变换只会产生新的RDD而不改变旧的RDD。这种模型可以让Spark用**Lineage**很高效地容错

