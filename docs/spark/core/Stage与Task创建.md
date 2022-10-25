# Stage
>  stage是一组计算相同函数的并行task集合，而函数是 spark Job的一部分，所有的task都有相同的shuffle 依赖。scheduler运行的每个DAG都在shuffle处分割成DAG，然后scheduler按照拓扑顺序运行stage。
> 每个stage可以是shuffleMapStage(task输出是其他stage的输入)或者是ResultStage(直接计算spark的Action)，对于shuffleMapStage，spark需要记录输出分区所在的节点。

## stage与rdd的绑定关系

为什么要知道stage与rdd的绑定关系？？？我认为理解这个绑定关系是知道task生成的关键，从而也是理解整个spark的调度的关键。如果能理解这个，后面计算task个数就是自然而然的了。

我们先看下stage的几个关键属性

```scala
 * @param id Unique stage ID
 * @param rdd RDD that this stage runs on: for a shuffle map stage, it's the RDD we run map tasks
 *   on, while for a result stage, it's the target RDD that we ran an action on
 * @param numTasks Total number of tasks in stage; result stages in particular may not need to
 *   compute all partitions, e.g. for first(), lookup(), and take().
 * @param parents List of stages that this stage depends on (through shuffle dependencies).
 * @param firstJobId ID of the first job this stage was part of, for FIFO scheduling.
 * @param callSite Location in the user program associated with this stage: either where the target
 *   RDD was created, for a shuffle map stage, or where the action for a result stage was called.
 */
private[scheduler] abstract class Stage(
    val id: Int,
    val rdd: RDD[_], //stage运行的rdd，如果是shuffle map stage，则在这个rdd上运行map task；如果是result stage，则在这个rdd上运行一个action
    val numTasks: Int,//stage包含的task个数
    val parents: List[Stage],
    val firstJobId: Int,
    val callSite: CallSite)
  extends Logging {
```

从而，我们发现一个stage是运行在一个rdd上的，我们可以认为一个stage是与这个rdd建立了绑定关系。

这个绑定关系怎么建立的呢？可以参考  [Spark之调度模块-DAGScheduler](https://mp.weixin.qq.com/s?__biz=MzU2ODg2NDMwMw==&mid=2247484083&idx=1&sn=7100ca30007873fb859e5783447bb826&chksm=fc8639f3cbf1b0e5eda53a203f249edfc4bc097fe58d71f0aecd8fbb2a6fcc3fcbfc246810db&token=524765659&lang=zh_CN#rd)中stage的划分一节：

1.  如果rdd触发了action算子，则这个rdd创建一个stage并与之建立联系

2. 如果rdd的依赖是shuffle依赖，且下游需要这个rdd物化，那么根据这个rdd的ShuffleDependency生成这个stage。注意， 是不是这个stage就与这个rdd建立绑定关系呢？不是！这个stage会跟这个rdd的ShuffleDependency建立绑定关系！确切的说是和这个ShuffleDependency对应的rdd建立绑定关系，而ShuffleDependency的rdd是当前rdd的父rdd!  看源码注释也能知道这点：

   ```scala
    * @param _rdd the parent RDD   注意⚠️：父RDD
    * @param partitioner partitioner used to partition the shuffle output
    * @param serializer [[org.apache.spark.serializer.Serializer Serializer]] to use. If not set
    *                   explicitly then the default serializer, as specified by `spark.serializer`
    *                   config option, will be used.
    * @param keyOrdering key ordering for RDD's shuffles
    * @param aggregator map/reduce-side aggregator for RDD's shuffle
    * @param mapSideCombine whether to perform partial aggregation (also known as map-side combine)
    */
   @DeveloperApi
   class ShuffleDependency[K: ClassTag, V: ClassTag, C: ClassTag](
       @transient private val _rdd: RDD[_ <: Product2[K, V]],
       val partitioner: Partitioner,
       val serializer: Serializer = SparkEnv.get.serializer,
       val keyOrdering: Option[Ordering[K]] = None,
       val aggregator: Option[Aggregator[K, V, C]] = None,
       val mapSideCombine: Boolean = false)
     extends Dependency[Product2[K, V]] {
   ```

   

# task的创建

对于每个stage，spark要为它创建task集合，这个创建也是按需创建，如果某个分区的数据已经ready，则不需要再为其创建task。task的构建在DAGScheduler的submitMissingTasks方法里，它根据stage的类型创建相对应的task：

1.  对于ShuffleMapStages生成ShuffleMapTask
2. 对于ResultStage生成ResultTask

 来看task的创建源码：

```scala
    val tasks: Seq[Task[_]] = try {
      val serializedTaskMetrics = closureSerializer.serialize(stage.latestInfo.taskMetrics).array()
      stage match {
        //（一） 对于ShuffleMapStages生成ShuffleMapTask
        case stage: ShuffleMapStage =>
          stage.pendingPartitions.clear()
          // 每个分区对应一个ShuffleMapTask（这样更加高效）
          partitionsToCompute.map { id =>
            val locs = taskIdToLocations(id)
            val part = stage.rdd.partitions(id)
            stage.pendingPartitions += id
            //< 使用上述获得的 task 对应的优先位置，即 locs 来构造ShuffleMapTask
            // 生成ShuffleMapTask
            //可见一个partition，一个task，一个位置信息
            new ShuffleMapTask(stage.id, stage.latestInfo.attemptId,
              taskBinary, part, locs, properties, serializedTaskMetrics, Option(jobId),
              Option(sc.applicationId), sc.applicationAttemptId)
          }

        //（二） 对于ResultStage生成ResultTask
        case stage: ResultStage =>
          // 每个分区对应一个ResultTask
          partitionsToCompute.map { id =>
            val p: Int = stage.partitions(id)
            val part = stage.rdd.partitions(p)
            val locs = taskIdToLocations(id)
            //< 使用上述获得的 task 对应的优先位置，即 locs 来构造ResultTask
            new ResultTask(stage.id, stage.latestInfo.attemptId,
              taskBinary, part, locs, id, properties, serializedTaskMetrics,
              Option(jobId), Option(sc.applicationId), sc.applicationAttemptId)
          }
      }
    }
```

具体创建多少个task呢？partitionsToCompute其实就是计算rdd缺失的分区数：

```scala
  override def findMissingPartitions(): Seq[Int] = {
    val missing = (0 until numPartitions).filter(id => outputLocs(id).isEmpty)
    assert(missing.size == numPartitions - _numAvailableOutputs,
      s"${missing.size} missing, expected ${numPartitions - _numAvailableOutputs}")
    missing
  }
```

但是！是哪个rdd缺失的分区数？答案是与当前stage绑定的那个rdd的分区数！

## 案例推演

假设我们有如下RDD拓扑：

![image-20210725224421488](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210725224421488.png)

我们猜想下，应该怎么划分stage，怎么创建task（其实特想问每个task负责什么逻辑？这涉及到task的pipeline，放在下回分解）。

stage划分我们上一篇文章  [Spark之调度模块-DAGScheduler](https://mp.weixin.qq.com/s?__biz=MzU2ODg2NDMwMw==&mid=2247484083&idx=1&sn=7100ca30007873fb859e5783447bb826&chksm=fc8639f3cbf1b0e5eda53a203f249edfc4bc097fe58d71f0aecd8fbb2a6fcc3fcbfc246810db&token=524765659&lang=zh_CN#rd)讲过了，不再赘述。

这个拓扑有一个shuffle算子，一个action算子，

1. RDD4的依赖是一个shuffle依赖，它会触发stage1的生成，  即stage1是根据RDD4的shuffleDependency创建的，而这个shuffleDependency对应的RDD是RDD3, 于是stage1与RDD3建立绑定关系！ RDD3有三个parition,那么stage1就创建三个task。
2. action算子count是由RDD4创建的，stage2与RDD4建立绑定关系，RDD4有两个分区，所以stage2创建两个task。

有了上述理解，我们脑海里stage的划分、task的创建应该是下面这样：

![image-20210725231659543](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210725231659543.png)

细心的同学可能会问stage不是从后往前创建的吗？为啥前面的编号比后面的小？

答案还是上一篇文章，stage的创建是一个递归过程，从后往前递归推动stage的创建，真正的创建动作还是从前往后创建，从而stage序号是递增的。

# 总结

总结里，我最想让大家记住的是stage与rdd的绑定关系：

1.  如果rdd触发了action算子，则其创建的stage与之建立绑定关系
2. 如果rdd的依赖是shuffle依赖，则其创建的stage与父rdd建立绑定关系

<font color=red>另外一点就是stage创建的task个数就是它绑定的那个rdd的分区数, 也是该stage最后一个RDD中的分区数决定。   需要注意的是Spark UI上一个stage的task个数是它的父rdd也就是上一个stage最后一个rdd的分区数</font>



再有就是shuffleMapStage创建shuffleMapTask，而ResultStage创建ResultTask

最后，还有一个最后先埋个坑：task的pipeline，且容下次分解。
