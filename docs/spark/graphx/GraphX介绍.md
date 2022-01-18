# GraphX框架

GraphX的核心抽象是Resilient Distributed Property Graph，一种点和边都带属性的有向多重图。它扩展了Spark RDD的抽象，有Table和Graph两种视图，而只需要一份物理存储。两种视图都有自己独有的操作符，从而获得了灵活操作和执行效率。

![clip_image004](https://gitee.com/luckywind/PigGo/raw/master/image/211422478311760.jpg)

中大部分的实现，都是围绕Partition的优化进行的

![clip_image008](https://gitee.com/luckywind/PigGo/raw/master/image/211422497692320.jpg)

两种视图底层共用的物理数据，由RDD[Vertex-Partition]和RDD[EdgePartition]这两个RDD组成。 

图的分布式存储采用点分割模式，而且使用partitionBy方法，由用户指定不同的划分策略（PartitionStrategy）。划分策略会将边分配到各个EdgePartition，顶点Master分配到各个VertexPartition，EdgePartition也会缓存本地边关联点的Ghost副本。划分策略的不同会影响到所需要缓存的Ghost副本数量，以及每个EdgePartition分配的边的均衡程度，需要根据图的结构特征选取最佳策略。目前有EdgePartition2d、EdgePartition1d、RandomVertexCut和CanonicalRandomVertexCut这四种策略。

## 图的存储模式

**边分割（Edge-Cut）**：每个顶点都存储一次，但有的边会被打断分到两台机器上。这样做的好处是节省存储空间；坏处是对图进行基于边的计算时，对于一条两个顶点被分到不同机器上的边来说，要跨机器通信传输数据，内网通信流量大。

**点分割（Vertex-Cut）**：每条边只存储一次，都只会出现在一台机器上。邻居多的点会被复制到多台机器上，增加了存储开销，同时会引发数据同步问题。好处是可以大幅减少内网通信量。

[![clip_image012](https://gitee.com/luckywind/PigGo/raw/master/image/211422548165194.jpg)](http://images0.cnblogs.com/blog/107289/201508/211422527224822.jpg)

如上，我们现在要将一个有4个顶点的图存储到3台机器上，这三台机器分别叫1，2，3；  机器1的两个点A、C一共对应了三个边，2对应两个边，3对应1个边，按照边拆分后，变成了6个边，且多了5个节点副本。

## GraphX存储模式

Graphx借鉴PowerGraph，使用的是Vertex-Cut(点分割)方式存储图，用三个RDD存储图数据信息：

**VertexTable(id, data)**：id为Vertex id，data为Edge data

**EdgeTable(pid, src, dst, data)**：pid为Partion id，src为原定点id，dst为目的顶点id

**RoutingTable(id, pid)**：id为Vertex id，pid为Partion id

![clip_image014](https://gitee.com/luckywind/PigGo/raw/master/image/211422562851210.jpg)

## 图计算模式

Bulk Synchronous Parallell，即整体同步并行，它将计算分成一系列的超步（superstep）的迭代（iteration）。从纵向上看，它是一个串行模式，而从横向上看，它是一个并行的模式，每两个superstep之间设置一个栅栏（barrier），即整体同步点，确定所有并行的计算都完成后再启动下一轮superstep。

[![clip_image015](https://gitee.com/luckywind/PigGo/raw/master/image/211422576753967.jpg)](http://images0.cnblogs.com/blog/107289/201508/211422568787323.jpg)

每一个超步（superstep）包含三部分内容：

1.**计算compute**：每一个processor利用上一个superstep传过来的消息和本地的数据进行本地计算；

2.**消息传递**：每一个processor计算完毕后，将消息传递个与之关联的其它processors

3.**整体同步点**：用于整体同步，确定所有的计算和消息传递都进行完毕后，进入下一个superstep。