# cache

- 保存到哪？ 

rdd.cache() 只保存到内存

DataFrame/DataSet.cache()是保存到memory_and_disk



>  将要计算 RDD partition 的时候（而不是已经计算得到第一个 record 的时候）就去判断 partition 要不要被 cache。如果要被 cache 的话，先将 partition 计算出来，然后 cache 到内存。cache 只使用 memory，写磁盘的话那就叫 checkpoint 了。

调用 rdd.cache() 后， rdd 就变成 persistRDD 了



# persist

无参方法内部调用cache()

有参数方法传入Storage Levels



# Checkpoint

spark中的checkpoint机制主要有两种作用，一是对RDD做checkpoint，可以将该RDD触发计算并将其数据保存到hdfs目录中去，可以斩断其RDD的依赖链，这对于频繁增量更新的RDD或具有很长lineage的RDD具有明显的效果。另一种用途是用于Spark Streaming用于保存DStreamGraph及其配置，以便Driver崩溃后的恢复

1. ck计算时机及计算顺序
   对于需要缓存的RDD，每计算出一个record就将其缓存到内存或者磁盘中，但checkpoint一般是写到HDFS，写入延时很高，所以Spark采用了一种比较粗暴的方法：用户设置checkpoint()后，只是标记某个RDD需要持久化，等到当前job计算结束时再重新启动该job计算一遍，对RDD进行持久化，即当前job结束后会启动专门的job去完成checkpoint，需要ck的RDD会被计算两次。因此，Spark推荐需要被ck的数据先进行缓存，缓存后，再次ck时直接读取缓存。
2. ck的读取
   ck的格式是序列化后的RDD，需要反序列化恢复RDD。ck还存放了RDD的分区信息，也可以恢复，从而在后序的join中用于决定依赖关系。

## 使用

在应用启动时加上**spark**.sparkContext.setCheckpointDir("file:///ckpttmp")用于设置checkpoint文件的存储目录

在需要的切断lineage的rdd执行

```scala
rdd.checkpoint()
```

## 实现

RDD需要经过[initialized初始化、 ckInProgress、Checkpointed]三个阶段

1. initialized初始化
   为RDD添加一个checkpiontData属性，用来管理RDD相关的checkpoint信息
2. ckInProgress
   job结束后，调用该job最后一个RDD的doChekcpoint()方法，按照血缘回溯扫描，遇到需要ck的RDD就将其标记为CheckpointInProgress。 之后，Spark会调用专门的runJob()再次提交一个job完成checkpoint
3. Checkpointed
   ck的job完成后，Spark会建一个ReliableCheckpointRDD,用来表示被checkpoint到磁盘上的RDD。该RDD的依赖是空，不再保留血缘。并和原RDD建立映射关系，后序读取该RDD都会读取ck的数据。

主要是构造了一个ReliableCheckpointRDD，它把调用checkpoint的原始rdd写入文件系统，这个写入过程会调用sc.runJob提交一个Job.

ReliableCheckpointRDD的几个方法实现：

### docheckpoint

rdd的runJob方法会调用doCheckpoint()方法，该方法实现如下：

```scala
  private[spark] def doCheckpoint(): Unit = {
    RDDOperationScope.withScope(sc, "checkpoint", allowNesting = false, ignoreParent = true) {
      if (!doCheckpointCalled) {
        doCheckpointCalled = true
        if (checkpointData.isDefined) {
          if (checkpointAllMarkedAncestors) {
            //递归调用父rdd的doCheckpoint
            dependencies.foreach(_.rdd.doCheckpoint())
          }
          checkpointData.get.checkpoint()
        } else {
          dependencies.foreach(_.rdd.doCheckpoint())
        }
      }
    }
  }
```



rdd的checkpoint过程经历三个阶段

Initialized->CheckpointingInProgress->Checkpointed 

```scala
  final def checkpoint(): Unit = {
    // Guard against multiple threads checkpointing the same RDD by
    // atomically flipping the state of this RDDCheckpointData
    RDDCheckpointData.synchronized {
      if (cpState == Initialized) {
        cpState = CheckpointingInProgress
      } else {
        return
      }
    }
//这个由具体子类实现具体的checkpoint过程
    val newRDD = doCheckpoint()

    // Update our state and truncate the RDD lineage
    // 完成checkpoint后，这里切断血缘
    RDDCheckpointData.synchronized {
      cpRDD = Some(newRDD)
      cpState = Checkpointed
      rdd.markCheckpointed()
    }
  }
```

具体的实现类是ReliableCheckpointRDD，它内部就调用一个方法写入分区

```scala
  protected override def doCheckpoint(): CheckpointRDD[T] = {
    val newRDD = ReliableCheckpointRDD.writeRDDToCheckpointDirectory(rdd, cpDir)
    new RDD
  }
```

写分区，写入完成后再加载，这样就完成了血缘的切断

```scala
 def writeRDDToCheckpointDirectory[T: ClassTag](
      originalRDD: RDD[T],
      checkpointDir: String,
      blockSize: Int = -1): ReliableCheckpointRDD[T] = {
    val sc = originalRDD.sparkContext
    // Save to file, and reload it as an RDD
    val broadcastedConf = sc.broadcast(
      new SerializableConfiguration(sc.hadoopConfiguration))
    // TODO: This is expensive because it computes the RDD again unnecessarily (SPARK-8582)
   //内部提交一个job 进行写入
    sc.runJob(originalRDD,
      writePartitionToCheckpointFile[T](checkpointDirPath.toString, broadcastedConf) _)

    if (originalRDD.partitioner.nonEmpty) {
      writePartitionerToCheckpointDir(sc, originalRDD.partitioner.get, checkpointDirPath)
    }


   //再把写入的文件加载为一个ReliableCheckpointRDD
    val newRDD = new ReliableCheckpointRDD[T](
      sc, checkpointDirPath.toString, originalRDD.partitioner)
    newRDD
  }
```



### getPartitions

根据checkpoint目录下part-文件个数创建一个CheckpointRDDPartition数组

### compute

这个就很简单，就是读取checkpoint文件

# cache与checkpoint对比

cache和checkpoint都可以起到减少重复计算的作用。但区别还是比较大的：

1. 目的不同：缓存的目的是加速计算，ck的目的是快速恢复
1. 存储性质和位置不同：缓存主要食用内存，偶尔食用硬盘；ck主要食用HDFS
1. 写入速度和规则不同: 缓存较快，对job的执行影响较小，ck写入速度慢，因此启用专门的job进行持久化
1. 对数据血缘的影响不同： cache如果放到内存，可能会因为节点失败导致分区丢失，还是需要重新计算，这样就必须保留血缘。而checkpoint是把rdd写入文件系统了，节点失败也不会导致重算，因此是直接切断了血缘。
1. 应用场景不同： 缓存适合多次读取、占用空间不是非常大的RDD，ck适用于依赖关系比较复杂、重算代价高的RDD。
2. cache即使使用persist(StorageLevel.DISK_ONLY) 与 checkpoint 也有区别。前者虽然可以将 RDD 的 partition 持久化到磁盘，但该 partition 由 blockManager 管理。一旦 driver program 执行结束，也就是 executor 所在进程 CoarseGrainedExecutorBackend stop，blockManager 也会 stop，被 cache 到磁盘上的 RDD 也会被清空（整个 blockManager 使用的 local 文件夹被删除）。 但是checkpoint的内容以被下一个 driver program 使用。