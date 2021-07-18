[参考](https://cloud.tencent.com/developer/article/1491370?from=article.detail.1491374),[磁盘存储](https://zhuanlan.zhihu.com/p/354816818)

[磁盘存储DiskStore](https://cloud.tencent.com/developer/article/1491368?from=article.detail.1491372)

# 概述

spark在shuffle中map阶段的数据或缓存RDD时内存不够用时可以把数据写入磁盘，在yarn模式，会在yarn的yarn.nodemanager.log-dirs目录下写入文件，多个目录以逗号分隔，这样可以把读写压力分散到不同的磁盘，提高读写性能。

磁盘存储主要涉及两个类：

- DiskBlockManager，主要用来创建并维持逻辑 blocks 与磁盘上的 blocks之间的映射关系，一个block通过blockId中的name映射到磁盘上的一个文件。
- diskStore，主要来在磁盘上存储block，具体操作block的磁盘文件。

初始化
在各个节点包括driver和executor初始化BlockManager的时候，会初始化DiskBlockManager和diskStore。

```scala
//在BlockManager构造的过程中
// 磁盘存储管理器，只负责blockId和磁盘文件的映射关系
val diskBlockManager = {new DiskBlockManager(conf, deleteFilesOnStop)}
 
//直接负责操作文件，比如创建、打开
val diskStore = new DiskStore(conf, diskBlockManager, securityManager)
```

# block

DiskStore是通过DiskBlockManager进行管理存储到磁盘上的Block数据文件的，在同一个节点上的多个Executor共享相同的磁盘文件路径，相同的Block数据文件也就会被同一个节点上的多个Executor所共享。而对应MemoryStore，因为每个Executor对应独立的JVM实例，从而具有独立的Storage/Execution内存管理，所以使用MemoryStore不能共享同一个Block数据，但是同一个节点上的多个Executor之间的MemoryStore之间拷贝数据，比跨网络传输要高效的多

## **MemoryStore**

数据在内存中存储的形式

1. 以序列化格式
2. 以反序列化的形式  2.1 Block数据记录能够完全放到内存中  2.2 Block数据记录只能部分放到内存中：申请Unroll内存（预占内存）
3. 以序列化二进制格式保存Block数据

```scala
MEMORY_ONLY
MEMORY_ONLY_2
MEMORY_ONLY_SER
MEMORY_ONLY_SER_2
MEMORY_AND_DISK
MEMORY_AND_DISK_2
MEMORY_AND_DISK_SER
MEMORY_AND_DISK_SER_2
OFF_HEAP
```

## **DiskStore**

数据罗盘的几种形式：

1. 通过文件流写Block数据
2. 将二进制Block数据写入文件

```scala
DISK_ONLY
DISK_ONLY_2
MEMORY_AND_DISK
MEMORY_AND_DISK_2
MEMORY_AND_DISK_SER
MEMORY_AND_DISK_SER_2
OFF_HEAP
```



# **BlockManager**

BlockManagerMaster管理BlockManager. BlockManager在每个Dirver和Executor上都有，用来管理Block数据，包括数据的获取和保存等

BlockManager，他负责管理在每个Dirver和Executor上的Block数据，可能是本地或者远程的。具体操作包括查询Block、将Block保存在指定的存储中，如内存、磁盘、堆外（Off-heap）。而BlockManager依赖的后端，对Block数据进行内存、磁盘存储访问，都是基于前面讲到的MemoryStore、DiskStore。 在Spark集群中，当提交一个Application执行时，该Application对应的Driver以及所有的Executor上，都存在一个BlockManager、BlockManagerMaster，而BlockManagerMaster是负责管理各个BlockManager之间通信，这个BlockManager管理集群

```
* BlockManager是Spark存储体系中的核心组件。
* BlockManager主要由以下部分组成：
*   1.shuffle客户端ShuffleClient;
*   2.BlockManagerMaster(对存在于所有Executor上的BlockManager统一管理)
*   3.磁盘块管理器DiskBlockManager;
*   4.内存存储MemoryStore；
*   5.磁盘存储DiskStore;
*   6.Tachyon存储TachyonStore;
*   7.非广播Block清理器metadataCleanner和广播Block清理器broadcastCleaner;
*   8.压缩算法实现CompressionCodee;
```

## **BlockManager集群**

关于一个Application运行过程中Block的管理，主要是基于该Application所关联的一个Driver和多个Executor构建了一个Block管理集群：Driver上的(BlockManagerMaster, BlockManagerMasterEndpoint)是集群的Master角色，所有Executor上的(BlockManagerMaster, RpcEndpointRef)作为集群的Slave角色。当Executor上的Task运行时，会查询对应的RDD的某个Partition对应的Block数据是否处理过，这个过程中会触发多个BlockManager之间的通信交互

##  **状态管理**

BlockManager在进行put操作后，通过blockInfoManager来控制当前put等操作是否完成以及是否成功。

对于BlockManager中的存储的每个Block,不一定是对应的数据都PUT成功了,不一定可以立即提供对外的读取,因为PUT是一个过程,有成功还是有失败的状态. ,拿ShuffleBlock来说,在shuffleMapTask需要Put一个Block到BlockManager中,在Put完成之前,该Block将处于Pending状态,等待Put完成了不代表Block就可以被读取, 因为Block还可能Put"fail"了.

因此BlockManager通过BlockInfo来维护每个Block状态,在BlockManager的代码中就是通过一个TimeStampedHashMap来维护BlockID和BlockInfo之间的map.

private val blockInfo = new TimeStampedHashMap[BlockId, BlockInfo] 注： 2.2中此处是通过线程安全的hashMap和一个计数器实现的

# **读写控制**

BlockInfoManager通过同步机制防止多个task处理同一个block数据块

用户提交一个Spark Application程序，如果程序对应的DAG图相对复杂，其中很多Task计算的结果Block数据都有可能被重复使用，这种情况下如何去控制某个Executor上的Task线程去读写Block数据呢？其实，BlockInfoManager就是用来控制Block数据读写操作，并且跟踪Task读写了哪些Block数据的映射关系，这样如果两个Task都想去处理同一个RDD的同一个Partition数据，如果没有锁来控制，很可能两个Task都会计算并写同一个Block数据，从而造成混乱

```scala
class BlockInfoManager{
	val infos = 
		new mutable.HashMap[BlockId, BlockInfo]
	// 存放被锁定任务列表
	val writeLocksByTask =
    	new mutable.HashMap[
			TaskAttemptId, mutable.Set[BlockId]]
	 val readLocksByTask =
    	new mutable.HashMap[TaskAttemptId, 		ConcurrentHashMultiset[BlockId]]
 
	def lockForReading(）{
		infos.get(blockId) match {
			case Some(info) =>
				// 没有写任务
          		if (info.writerTask == 					BlockInfo.NO_WRITER) {
					// 读task数量加一
            		info.readerCount += 1
					//	放入读多锁定队列
 					readLocksByTask(
					currentTaskAttemptId).
					add(blockId)
	}
}
def lockForWriting(）{
		case Some(info) =>
          if (info.writerTask == 				BlockInfo.NO_WRITER && 				info.readerCount == 0) {
            info.writerTask = currentTaskAttemptId
 			writeLocksByTask.addBinding(
			currentTaskAttemptId, blockId)
}
```



# 具体功能

## **diskBlockManager**

diskBlockManager初始化的时候会在本地磁盘的spark.local.dir（yarn模式是yarn.nodemanager.log-dirs）目录（以blockmgr开头）下创建子目录，默认有64个子目录，这些子目录用来存放applicaton运行是产生的中间数据，如 cached RDD paratition对应的block、shuffle write生成的数据等，会根据文件名hash到不同的子目录下。

```scala
class DiskBlockManager(conf: SparkConf, deleteFilesOnStop: Boolean) extends Logging {
 
  //如果创建不了子目录，会报错退出
  private[spark] val localDirs: Array[File] = createLocalDirs(conf)
  if (localDirs.isEmpty) {
    logError("Failed to create any local dir.")
    System.exit(ExecutorExitCode.DISK_STORE_FAILED_TO_CREATE_DIR)
  }
 
  //每个目录下默认会创建64个子目录,主要是通过这个结构来维护blockId和文件的对应关系
  private val subDirs = Array.fill(localDirs.length)(new Array[File](subDirsPerLocalDir))
 
  // 根据文件名进行hash，看文件应该放到哪个子目录下，  存在的话就返回，不存在的话就创建文件
  def getFile(filename: String): File = {}
 
   //是否存在指定的blockId
   def containsBlock(blockId: BlockId): Boolean = {
    getFile(blockId.name).exists()
  }
 
  // 返会被这个disk manager存储在磁盘上的所有块信息
  def getAllBlocks(): Seq[BlockId] = {}
   
  //用来创建Spark计算过程中的中间结果以及Shuffle Write阶段输出的存储文件
  def createTempLocalBlock(): (TempLocalBlockId, File) = {}
  def createTempShuffleBlock(): (TempShuffleBlockId, File) = {}

}
```

## **DiskStore**

在磁盘上存储BlockManager的块数据，实际读写文件。

当MemoryStore没有足够空间时，就会使用DiskStore将块存入磁盘

```scala
private[spark] class DiskStore( conf: SparkConf,diskManager: DiskBlockManager,securityManager: SecurityManager) extends Logging {
 
  //维护数据的哈希表结构，blockId到块大小的映射
  private val blockSizes = new ConcurrentHashMap[BlockId, Long]()
 
  // 返回块大小，在put的时候会往hash表里插入记录
  def getSize(blockId: BlockId): Long = blockSizes.get(blockId)
 
  //先判断指定的BlockId是否存在，如果已经存在抛出异常返回，如果不存在，调用diskManager.getFile创建文件，然后调用writeFunc写文件，关闭结束
  def put(blockId: BlockId)(writeFunc: WritableByteChannel => Unit): Unit ={}
 
  //调用put函数，put函数返回ChunkedByteBuffer，即这里的channel，相当于文件流
  def putBytes(blockId: BlockId, bytes: ChunkedByteBuffer): Unit = { }
 
  //获取块内容，先调用diskManager.getFile获取文件名，再调用getSize获取块大小，然后封装成BlockData
  def getBytes(blockId: BlockId): BlockData = {}
 
  //本地hash表中删除块大小信息， 并且调用DiskBlockManager返回文件名，然后删除文件
  def remove(blockId: BlockId): Boolean = {}
 
  //是否存在指定的blockId，也是调用iskManager.getFile看返回的文件名是否为空来判断的
  def contains(blockId: BlockId): Boolean = { }
 
  //根据文件名打开文件并返回句柄
  private def openForWrite(file: File): WritableByteChannel = {}
 
}
```

