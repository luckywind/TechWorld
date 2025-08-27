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



# 基本实现

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

