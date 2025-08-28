[参考](https://blog.51cto.com/davidwang456/3085815)

BlockManager是一个分布式组件，主要负责Spark集群运行时的数据读写与存储。集群中的每个工作节点上都有BlockManager。其内部的主要组件包括：MemoryStore、DiskStore、BlockTransferService以及ConnectionManager。

MemoryStore：负责对内存中的数据进行操作。

> 使用LinkeHashMap保存块ID及其内存占用

DiskStore：负责对磁盘上的数据进行处理。

BlockTransferService：负责对远程节点上的数据进行读写操作。例如，当执行ShuffleRead时，如数据在远程节点，则会通过该服务拉取所需数据。

ConnectionManager：负责创建当前节点BlockManager到远程其他节点的网络连接。

此外在Driver进行中包含BlockManagerMaster组件，其功能主要是负责对各个节点上的BlockManager元数据进行统一维护与管理。

![image-20211209163827644](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20211209163827644.png)从Job运行角度来看，BlockManager工作流程如下：

1.当BlockManager创建之后，首先向Driver所在的BlockManagerMaster注册，此时BlockManagerMaster会为该BlockManager创建对应的BlockManagerInfo。BlockManagerInfo管理集群中每个Executor中的 BlockManager元数据信息，并存储到BlockStatus对象中。

2.当使用BlockManager执行写操作时，例如持久化RDD计算过程中的中间数据，此时会优先将数据写入内存中，如果内存不足且支持磁盘存储则会将数据写入磁盘。如果指定Replica主备模式，则会将数据通过BlockTransferService同步到其他节点。

3.当使用BlockManager执行了数据变更操作，则需要将BlockStatus信息同步到BlockManagerMaster节点上，并在对应的BlockManagerInfo更新BlockStatus对象，实现全局元数据的维护。



# Block基本实现

[Spark Core源码精读计划21 | Spark Block的基本实现](https://cloud.tencent.com/developer/article/1491360?policyId=1004)

## BlockId

```scala
sealed abstract class BlockId {
  def name: String

  def asRDDId: Option[RDDBlockId] = if (isRDD) Some(asInstanceOf[RDDBlockId]) else None
  def isRDD: Boolean = isInstanceOf[RDDBlockId]
  def isShuffle: Boolean = isInstanceOf[ShuffleBlockId]
  def isBroadcast: Boolean = isInstanceOf[BroadcastBlockId]

  override def toString: String = name
}
```

name方法返回该BlockId的唯一名称,  不同类型的数据块都有自己的BlockId 实现类，只是name 规则不一样

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/n6bqvcws0a.jpeg)

## BlockData

BlockData只是定义了数据转化的规范，并没有涉及具体的存储格式和读写流程，实现起来比较自由。BlockData目前有3个实现类：

1. 基于内存和ChunkedByteBuffer的ByteBufferBlockData
2. 基于磁盘和File的DiskBlockData，
3. 加密的EncryptedBlockData

## BlockInfo

```scala
private[storage] class BlockInfo(
    val level: StorageLevel,
    val classTag: ClassTag[_],
    val tellMaster: Boolean) {
  def size: Long = _size
  def size_=(s: Long): Unit = {
    _size = s
    checkInvariants()
  }
  private[this] var _size: Long = 0

  def readerCount: Int = _readerCount
  def readerCount_=(c: Int): Unit = {
    _readerCount = c
    checkInvariants()
  }
  private[this] var _readerCount: Int = 0

  def writerTask: Long = _writerTask
  def writerTask_=(t: Long): Unit = {
    _writerTask = t
    checkInvariants()
  }
  private[this] var _writerTask: Long = BlockInfo.NO_WRITER

  private def checkInvariants(): Unit = {
    assert(_readerCount >= 0)
    assert(_readerCount == 0 || _writerTask == BlockInfo.NO_WRITER)
  }

  checkInvariants()
}
```



# Block 管理

## MemoryStore

它主要用来在内存中存储Block数据，可以避免重复计算同一个RDD的Partition数据。一个Block对应着一个RDD的一个Partition的数据。

MemoryStore也提供了多种存储方式:

- 以序列化格式保存Block数据
  首先，通过MemoryManager来申请Storage内存，调用putBytes方法，会根据size大小去申请Storage内存，如果申请成功，则会将blockId对应的Block数据保存在内部的LinkedHashMap[BlockId, MemoryEntry[_]]映射表中，然后以SerializedMemoryEntry这种序列化的格式存储，实际SerializedMemoryEntry就是简单指向Buffer中数据的引用对象
- 基于记录迭代器，以反序列化Java对象形式保存Block数据
  - 如果内存中能放得下，则返回最终Block数据记录的大小，
  - 否则返回一个PartiallyUnrolledIterator[T]迭代器.
- 基于记录迭代器，以序列化二进制格式保存Block数据

## DiskStore

DiskStore提供了将Block数据写入到磁盘的基本操作，它是通过DiskBlockManager来管理逻辑上Block到物理磁盘上Block文件路径的映射关系。

DiskStore中数据的存取本质上就是字节序列与磁盘文件之间的转换，它通过putBytes方法把字节序列存入磁盘文件，再通过getBytes方法将文件内容转换为数据块。

## BlockManager

在Spark集群中，当提交一个Application执行时，该Application对应的Driver以及所有的Executor上，都存在一个BlockManager、BlockManagerMaster，而BlockManagerMaster是负责管理各个BlockManager之间通信，这个BlockManager管理集群

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/40rdwfxyrb.jpeg)

一个RDD的Partition对应一个ShuffleMapTask，一个ShuffleMapTask会在一个Executor上运行，它负责处理RDD的一个Partition对应的数据，基本处理流程，如下所示：

1. 根据该Partition的数据，创建一个RDDBlockId（由RDD ID和Partition Index组成），即得到一个稳定的blockId（如果该Partition数据被处理过，则可能本地或者远程Executor存储了对应的Block数据）。
2. 先从BlockManager获取该blockId对应的数据是否存在，如果本地存在（已经处理过），则直接返回Block结果（BlockResult）；否则，查询远程的Executor是否已经处理过该Block，处理过则直接通过网络传输到当前Executor本地，并根据StorageLevel设置，保存Block数据到本地MemoryStore或DiskStore，同时通过BlockManagerMaster上报Block数据状态（通知Driver当前的Block状态，亦即，该Block数据存储在哪个BlockManager中）。
3. 如果本地及远程Executor都没有处理过该Partition对应的Block数据，则调用RDD的compute方法进行计算处理，并将处理的Block数据，根据StorageLevel设置，存储到本地MemoryStore或DiskStore。
4. 根据ShuffleManager对应的ShuffleWriter，将返回的该Partition的Block数据进行Shuffle写入操作

## BlockInfoManager

用户提交一个Spark Application程序，如果程序对应的DAG图相对复杂，其中很多Task计算的结果Block数据都有可能被重复使用，这种情况下如何去控制某个Executor上的Task线程去读写Block数据呢？其实，BlockInfoManager就是用来控制Block数据读写操作，并且跟踪Task读写了哪些Block数据的映射关系，这样如果两个Task都想去处理同一个RDD的同一个Partition数据，如果没有锁来控制，很可能两个Task都会计算并写同一个Block数据，从而造成混乱。我们分析每种情况下，BlockInfoManager是如何管理Block数据（同一个RDD的同一个Partition）读写的：

- 第一个Task请求写Block数据

这种情况下，没有其他Task写Block数据，第一个Task直接获取到写锁，并启动写Block数据到本地MemoryStore或DiskStore。如果其他写Block数据的Task也请求写锁，则该Task会阻塞，等待第一个获取写锁的Task完成写Block数据，直到第一个Task写完成，并通知其他阻塞的Task，然后其他Task需要再次获取到读锁来读取该Block数据。

- 第一个Task正在写Block数据，其他Task请求读Block数据

这种情况，Block数据没有完成写操作，其他读Block数据的Task只能阻塞，等待写Block的Task完成并通知读Task去读取Block数据。

- Task请求读取Block数据

如果该Block数据不存在，则直接返回空，表示当前RDD的该Partition并没有被处理过。如果当前Block数据存在，并且没有其他Task在写，表示已经完成了些Block数据操作，则该Task直接读取该Block数据。
