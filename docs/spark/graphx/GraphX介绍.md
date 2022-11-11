# GraphX框架

GraphX的核心抽象是Resilient Distributed Property Graph，一种点和边都带属性的有向多重图。它扩展了Spark RDD的抽象，有Table和Graph两种视图，而只需要一份物理存储。两种视图都有自己独有的操作符，从而获得了灵活操作和执行效率。

![clip_image004](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/211422478311760.jpg)

中大部分的实现，都是围绕Partition的优化进行的

![clip_image008](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/211422497692320.jpg)

两种视图底层共用的物理数据，由RDD[Vertex-Partition]和RDD[EdgePartition]这两个RDD组成。 

图的分布式存储采用点分割模式，而且使用partitionBy方法，由用户指定不同的划分策略（PartitionStrategy）。划分策略会将边分配到各个EdgePartition，顶点Master分配到各个VertexPartition，EdgePartition也会缓存本地边关联点的Ghost副本。划分策略的不同会影响到所需要缓存的Ghost副本数量，以及每个EdgePartition分配的边的均衡程度，需要根据图的结构特征选取最佳策略。目前有EdgePartition2d、EdgePartition1d、RandomVertexCut和CanonicalRandomVertexCut这四种策略。

## 图的存储模式

**边分割（Edge-Cut）**：每个顶点都存储一次，但有的边会被打断分到两台机器上。这样做的好处是节省存储空间；坏处是对图进行基于边的计算时，对于一条两个顶点被分到不同机器上的边来说，要跨机器通信传输数据，内网通信流量大。

**点分割（Vertex-Cut）**：每条边只存储一次，都只会出现在一台机器上。邻居多的点会被复制到多台机器上，增加了存储开销，同时会引发数据同步问题。好处是可以大幅减少内网通信量。

[![clip_image012](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/211422548165194.jpg)](http://images0.cnblogs.com/blog/107289/201508/211422527224822.jpg)

如上，我们现在要将一个有4个顶点的图存储到3台机器上，这三台机器分别叫1，2，3；  机器1的两个点A、C一共对应了三个边，2对应两个边，3对应1个边。

1. 按照边拆分后，虽然边分成了6个，但是点只存了一次
2. 按照点拆分后，每条边只存储一次，但是有两个点被复制

## GraphX存储模式

[参考](https://www.jianshu.com/p/ad5cedc30ba4)

<font color=red>Graphx借鉴PowerGraph，使用的是Vertex-Cut(点分割)方式存储图，用三个RDD存储图数据信息：</font>

**VertexTable(id, data)**：id为Vertex id，data为Edge data

**EdgeTable(pid, src, dst, data)**：pid为Partion id，src为原定点id，dst为目的顶点id

**RoutingTable(id, pid)**：id为Vertex id，pid为Partion id

![clip_image014](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/211422562851210.jpg)

### 路由表

顶点 RDD 中还拥有**顶点到边 RDD 分区**的路由信息——路由表．路由表存在顶点 RDD 的分区中，它记录**分区内顶点跟所有边 RDD 分区的关系**．在边 RDD 需要顶点数据时（如构造边三元组），顶点 RDD 会根据路由表把顶点数据发送至边 RDD 分区。

如下图按顶点分割方法将图分解后得到顶点 RDD、边 RDD 和路由表：

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/3521279-4e0c8b27c944f1fb.png)

**路由表的分区和顶点RDD的分区一一对应**，顶点分区A内有三个点，**路由表记录了每个边分区包含该分区的哪些点，**例如边分区A中包含顶点分区A的1/2/3三个顶点，边分区B/C分别包含顶点分区A的顶点1

### 重复顶点视图

GraphX 会依据路由表，从顶点 RDD 中生成与边 RDD 分区相对应的重复顶点视图（ ReplicatedVertexView），它的作用是作为中间 RDD，将顶点数据传送至边 RDD 分区。重复顶点视图按边 RDD 分区并携带顶点数据的 RDD，如图下图所示，重复顶点分区 A 中便携了带边 RDD 分区 A 中的所有的顶点，它与边 RDD 中的顶点是 co-partition（即分区个数相同，且分区方法相同），**在图计算时， GraphX 将重复顶点视图和边 RDD 按分区进行拉链（ zipPartition）操作，即将重复顶点视图和边 RDD 的分区一一对应地组合起来，从而将边与顶点数据连接起来，使边分区拥有顶点数据**。在整个形成边三元组过程中，只有在顶点 RDD 形成的重复顶点视图中存在分区间数据移动，拉链操作不需要移动顶点数据和边数据．由于顶点数据一般比边数据要少的多，而且随着迭代次数的增加，需要更新的顶点数目也越来越少，重复顶点视图中携带的顶点数据也会相应减少，这样就可以大大减少集群中数据的移动量，加快执行速度。

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/3521279-82d656fc7e78971f.png)

重复顶点视图有四种模式
 （1）bothAttr: 计算中需要每条边的源顶点和目的顶点的数据
 （2）srcAttrOnly：计算中只需要每条边的源顶点的数据
 （3）destAttrOnly：计算中只需要每条边的目的顶点的数据
 （4）noAttr：计算中不需要顶点的数据

**重复顶点视图创建之后就会被加载到内存，因为图计算过程中，他可能会被多次使用，如果程序不再使用重复顶点视图，那么就需要手动调用GraphImpl中的unpersistVertices，将其从内存中删除。**
 生成重复顶点视图时，在边RDD的每个分区中创建集合，存储该分区包含的源顶点和目的顶点的ID集合，该集合被称作**本地顶点ID映射**（local VertexId Map），在生成重复顶点视图时，若重复顶点视图是第一次被创建，则把本地顶点ID映射和发送给边RDD各分区的顶点数据组合起来，在每个分区中以分区的本地顶点ID映射为索引存储顶点数据，生成新的顶点分区，最后得到一个新的顶点RDD，若重复顶点视图不是第一次被创建，则使用之前重复顶点视图创建的顶点RDD预发送给边RDD各分区的带你更新数据进行连接（join）操作，更新顶点RDD中顶点的数据，生成新的顶点RDD。

GraphX 在顶点 RDD 和边 RDD 的分区中以数组形式存储顶点数据和边数据，目的是为了不损失元素访问性能。同时，GraphX 在分区里建立了众多索引结构，高效地实现快速访问顶点数据或边数据。在迭代过程中，图的结构不会发生变化，因而顶点 RDD、边 RDD 以及重复顶点视图中的索引结构全部可以重用，当由一个图生成另一个图时，只须更新顶点 RDD 和边 RDD 的数据存储数组，因此，索引结构的重用保持了GraphX 高性能，也是相对于原生 RDD 实现图模型性能能够大幅提高的主要原因。



[源码分析](https://github.com/shijinkui/spark_study/blob/master/spark_graphx_analyze.markdown)

## 图计算模式

Bulk Synchronous Parallell，即整体同步并行，它将计算分成一系列的超步（superstep）的迭代（iteration）。从纵向上看，它是一个串行模式，而从横向上看，它是一个并行的模式，每两个superstep之间设置一个栅栏（barrier），即整体同步点，确定所有并行的计算都完成后再启动下一轮superstep。

[![clip_image015](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/211422576753967.jpg)](http://images0.cnblogs.com/blog/107289/201508/211422568787323.jpg)

每一个超步（superstep）包含三部分内容：

1.**计算compute**：每一个processor利用上一个superstep传过来的消息和本地的数据进行本地计算；

2.**消息传递**：每一个processor计算完毕后，将消息传递个与之关联的其它processors

3.**整体同步点**：用于整体同步，确定所有的计算和消息传递都进行完毕后，进入下一个superstep。

## pregel在GraphX中的实现

消息是根据边三元组（EdgeTriplet）并行计算的，并且消息计算可以访问源顶点和目标顶点属性。在超级步中会跳过未收到消息的顶点。当没有消息剩余时，Pregel运算符终止迭代并返回最终图。

### pregel定义

为避免由于长的血统链而引起的stackOverflowError，pregel通过将“ spark.graphx.pregel.checkpointInterval”设置为a（正数，例如10）来定期支持检查点图和消息，并使用SparkContext.setCheckpointDir（directory：String））设置检查点目录

