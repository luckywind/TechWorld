# 结构操作

## subgraph

接受点/边判断式，只返回满足条件的点，以及满足条件的边

```scala
    val validGraph = graph.subgraph(vpred = (id, attr) => attr._2 != "Missing")
```

返回一个新图，所有点满足点的判断式vpred

## mask

构建一个子图并返回，子图的点边要求也出现在输入图中。与subgraph一起使用实现基于另一个图的属性约束图。

说白了，就是两个图的点作交集，但只取左边图的数据(点和边)。

例如，使用包含缺失点的图跑出一个连通图，然后把结果约束到有效图(不含缺失点)中

> 如果不包含缺失点，则会有两个连通子图

![image-20220117160539291](https://gitee.com/luckywind/PigGo/raw/master/image/image-20220117160539291.png)

```scala
 val users: RDD[(VertexId, (String, String))] =
      sc.parallelize(Seq((3L, ("rxin", "student")), (7L, ("jgonzal", "postdoc")),
        (5L, ("franklin", "prof")), (2L, ("istoica", "prof")),
        (4L, ("peter", "student"))))
    // Create an RDD for edges
    val relationships: RDD[Edge[String]] =
      sc.parallelize(Seq(Edge(3L, 7L, "collab"),    Edge(5L, 3L, "advisor"),
        Edge(2L, 5L, "colleague"), Edge(5L, 7L, "pi"),
        Edge(4L, 0L, "student"),   Edge(5L, 0L, "colleague")))
    // Define a default user in case there are relationship with missing user
    // 边相关点的默认属性(如果一个user只出现在边里，即只有vid,它的属性就是这个默认值)
    val defaultUser = ("John Doe", "Missing")
    // Build the initial Graph
    val graph = Graph(users, relationships, defaultUser)   
val ccGraph = graph.connectedComponents() 
    println("包含缺失点的连通图-----")
    ccGraph.vertices.collect.foreach(println(_))
    
val validGraph = graph.subgraph(vpred = (id, attr) => attr._2 != "Missing")
    // 4,5与0的关系都会被删除
     println("子图----")
    validGraph.vertices.collect.foreach(println(_)
    // Restrict the answer to the valid subgraph
    val validCCGraph = ccGraph.mask(validGraph)
    println("validCCGraph-----")
    validCCGraph.vertices.collect.foreach(println(_))

                                        
包含缺失点的连通图-----
(4,0)
(0,0)
(3,0)
(7,0)
(5,0)
(2,0)
子图----
(4,(peter,student))
(3,(rxin,student))
(7,(jgonzal,postdoc))
(5,(franklin,prof))
(2,(istoica,prof))   
                                        
validCCGraph-----
(4,0)
(3,0)
(7,0)
(5,0)
(2,0)
```

![image-20220119184225141](https://gitee.com/luckywind/PigGo/raw/master/image/image-20220119184225141.png)

## groupEdges

`groupEdges`操作合并多重图中的并行边(如顶点对之间重复的边)。在大量的应用程序中，并行的边可以合并（它们的权重合并）为一条边从而**降低图的大小**。

注意： 为了结果的正确性，在合并边之前确保已经调用partitionBy进行过分区

```scala
    graph.partitionBy(partitionStrategy = RandomVertexCut)
        .groupEdges((e1,e2)=>(e1+e2))
        .edges.foreach(println(_))
```



## Connected Components算法

这个方法计算连通体

返回一个新的Graph对象，其结构和输入是一样的。每个连通体用最小的点ID代表，且作为每个点的属性。

   Connected Components即连通体算法用id标注图中每个连通体，将连通体中序号最小的顶点的id作为连通体的id。如果在图G中，任意2个顶点之间都存在路径，那么称G为连通图，否则称该图为非连通图，则其中的极大连通子图称为连通体，如下图所示，该图中有两个连通体：


![img](https://gitee.com/luckywind/PigGo/raw/master/image/20170818200351914.png)

连通体的属性：

1. vertices

实际是一个tuple类型，key为所有顶点id， value为key所在连通体id(连通体中顶点id最小值)

**实现原理：**

其实就是每个点找自己能连通到的最小的ID.



# Join操作

## joinVertices

把点数据和一个rdd进行join,并转换获得一个新的点数据。

输入rdd一定要对VertexId去重，能关联上就更新，关联不上就保留旧值。

方法签名

```scala
  def joinVertices[U: ClassTag](table: RDD[(VertexId, U)])(mapFunc: (VertexId, VD, U) => VD)
    : Graph[VD, ED] = {
    val uf = (id: VertexId, data: VD, o: Option[U]) => {
      o match {
        case Some(u) => mapFunc(id, data, u)
        case None => data
      }
    }
    graph.outerJoinVertices(table)(uf)
  }
```

注意第一个参数rdd的形式，是一个tuple(VertexId, U)。

第二个参数是个map,它维持了原点数据的类型，即不能改变点的类型。

例如：

```sql
    /***
     * joinVertex:更新点的数据，但不能改变数据类型
     * 我们想把点职业那一列换成点的出度
     */
    val afterJoin: Graph[(String, String), String] = graph.joinVertices(outDegrees)((vid,oldv,vdata)=>(oldv._2,vdata.toString))
    afterJoin.vertices.foreach(println(_))
```

## outerJoinVertices

outerJoinVertices与joinVertices很类似，但在map方法中可以修改vertex的属性类型。由于并非所有的图的顶点都一定能跟传入的RDD匹配上，所以定义mapFunc的时候使用了option选项。

方法签名

```scala
  def outerJoinVertices[U: ClassTag, VD2: ClassTag](other: RDD[(VertexId, U)])
      (mapFunc: (VertexId, VD, Option[U]) => VD2)(implicit eq: VD =:= VD2 = null)
    : Graph[VD2, ED]
```

注意，新值是一个Option类型，再使用时搭配match来用

例如，我们给点数据加一列

```sql
   /***
     * outerJoinVertices: 即可更新值，又可改变类型
     * 给点数据加一列出度
     */
      println("outerJoinVertecies")
    val afterOuterJoin=graph.outerJoinVertices(outDegrees)(
      (vid,oldv,vdata)=>{
        vdata match {
          case Some(x)=>{
            (oldv._1,oldv._2,vdata.get)
          }
          case None=>(oldv._1,oldv._2,0)
        }
      }
    )
    afterOuterJoin.vertices.foreach(println(_))
输出如下：
(4,(peter,student,1))
(0,(John Doe,Missing,0))
(3,(rxin,student,1))
(7,(jgonzal,postdoc,0))
(5,(franklin,prof,3))
(2,(istoica,prof,1))
```

# 聚合操作

`GraphX`中提供的聚合操作有`aggregateMessages`、`collectNeighborIds`和`collectNeighbors`三个，其中`aggregateMessages`在`GraphImpl`中实现，`collectNeighborIds`和`collectNeighbors`在 `GraphOps`中实现。下面分别介绍这几个方法。

## aggregateMessages

方法签名

```scala
  def aggregateMessages[A: ClassTag](
      sendMsg: EdgeContext[VD, ED, A] => Unit,
      mergeMsg: (A, A) => A,
      tripletFields: TripletFields = TripletFields.All)
    : VertexRDD[A] = {
    aggregateMessagesWithActiveSet(sendMsg, mergeMsg, tripletFields, None)
  }
```

注意：

1. 这里首先要指定消息的类型参数
2. sendMsg有一个上下文EdgeContext对象可用

3. tripletFields支持的值可从 枚举TripletFields中获取

作用：

每个点从其邻边和邻居点聚合值，sendMsg方法再每个边上调用，发送消息到该边的两个端点。 mergeMsg用于聚合每个点收集到的消息。

tripletFields： 传给sendMsg的EdgeContext包含哪些字段，减少字段可提升性能

例如，计算每个点的出度:

```scala
    /**
     * 计算每个点的入度
     */
    val inDgree: VertexRDD[Int] = graph.aggregateMessages[Int](ctx=>ctx.sendToDst(1),_+_)
    inDgree.collect().foreach(println(_))
(0,2)
(3,1)
(7,2)
(5,1)
```

## collectNeighbors

方法签名

```scala
def collectNeighbors(edgeDirection: EdgeDirection): VertexRDD[Array[(VertexId, VD)]]
```

作用： 收集每个顶点的邻居顶点的顶点`id`和顶点属性。

```scala
    val neibors: VertexRDD[Array[(VertexId, (String, String))]] = graph.collectNeighbors(edgeDirection = EdgeDirection.Either)
    neibors.map{row =>
     val vid: VertexId = row._1
      val neiborList: List[(VertexId, (String, String))] = row._2.toList
      val neibors: String = neiborList.map(_._2._1).mkString(",")
      (vid,neibors)
    }.collect().foreach(println(_))
(4,John Doe)
(0,peter,franklin)
(3,jgonzal,franklin)
(7,rxin,franklin)
(5,istoica,John Doe,rxin,jgonzal)
(2,franklin)
```

## collectNeighborIds

和collectNeighbors很类似，只不过只收集ID

例如

```scala
    val neiborId: VertexRDD[Array[VertexId]] = graph.collectNeighborIds(edgeDirection = EdgeDirection.Either)
    neiborId.map(v=>{
      val vid: VertexId = v._1
      val neiborids: Array[VertexId] = v._2
      val neib: String = "["+neiborids.mkString(",")+"]"
      (vid,neib)
    }).collect().foreach(println(_))
(4,[0])
(0,[4,5])
(3,[7,5])
(7,[3,5])
(5,[2,0,3,7])
(2,[5])
```

# 迭代计算

Pregel API 就是用来进行迭代计算的

图本身是递归数据结构，顶点的属性依赖于它们邻居的属性，这些邻居的属性又依赖于自己邻居的属性。所以许多重要的图算法都是迭代的重新计算每个顶点的属性，直到满足某个确定的条件。

`GraphX`中实现的这个更高级的`Pregel`操作是一个约束到图拓扑的批量同步（`bulk-synchronous`）并行消息抽象。`Pregel`操作者执行一系列的超步（`super steps`），在这些步骤中，顶点从 之前的超步中接收进入(`inbound`)消息的总和，为顶点属性计算一个新的值，然后在以后的超步中发送消息到邻居顶点。在超步中，没有收到消息的顶点会被跳过。当没有消息遗留时，`Pregel`操作停止迭代并返回最终的图。

## 计算模型

方法签名：

```scala
 def pregel[A: ClassTag](     // 消息类型参数
      initialMsg: A,          // 初始值
      maxIterations: Int = Int.MaxValue,   //最大迭代次数，有默认值
      activeDirection: EdgeDirection = EdgeDirection.Either)(// 激活方向，有默认值
      vprog: (VertexId, VD, A) => VD,   //点数据更新逻辑
      sendMsg: EdgeTriplet[VD, ED] => Iterator[(VertexId, A)], //边发送消息的逻辑
      mergeMsg: (A, A) => A)   //消息合并逻辑
    : Graph[VD, ED] = {
    Pregel(graph, initialMsg, maxIterations, activeDirection)(vprog, sendMsg, mergeMsg)
  }
```



`Pregel`计算模型中有三个重要的函数，分别是`vertexProgram`、`sendMessage`和`messageCombiner`。

- `vertexProgram`：用户定义的顶点运行程序。它作用于每一个顶点，负责接收进来的信息，并计算新的顶点值。
- `sendMsg`：发送消息，会被EdgeTriplet调用
- `mergeMsg`：合并消息

程序首先用`vprog`函数处理图中所有的顶点，生成新的图。然后用生成的图调用聚合操作（`mapReduceTriplets`，实际的实现是我们前面章节讲到的`aggregateMessagesWithActiveSet`函数）获取聚合后的消息

例如求0到各点的最短路径

```scala
    val data = "data/web_edge_data/web-Google.txt"
    val graph = GraphLoader.edgeListFile(sc,data)

    val sourceId: VertexId = 0
    val initialGraph = graph.mapVertices((id, _) => if (id == sourceId) 0.0 else Double.PositiveInfinity)
    //一个柯里化函数
    val sssp = initialGraph.pregel(Double.PositiveInfinity)(//这里初始化最短路径为无穷大
      (id, dist, newDist) => math.min(dist, newDist), // 点数据更新逻辑，收到的消息是距离，取最小
      triplet => { // 边发送消息， 如果源距离加上这条边的长度比目的的距离小，则给目的发送一条消息：把这个更短的路径发给它
        if (triplet.srcAttr + triplet.attr < triplet.dstAttr) {
          Iterator((triplet.dstId, triplet.srcAttr + triplet.attr))
        } else {
          Iterator.empty
        }
      },
      (a,b) => math.min(a,b) // 点数据聚合，取最小
    )

    println(sssp.vertices.collect.mkString("\n"))
```



## 实现原理

```scala
 // compute the messages
    var messages = GraphXUtils.mapReduceTriplets(g, sendMsg, mergeMsg)
    val messageCheckpointer = new PeriodicRDDCheckpointer[(VertexId, A)](
      checkpointInterval, graph.vertices.sparkContext)
    messageCheckpointer.update(messages.asInstanceOf[RDD[(VertexId, A)]])
    var activeMessages = messages.count()

    // Loop
    var prevG: Graph[VD, ED] = null
    var i = 0
    while (activeMessages > 0 && i < maxIterations) {
      // Receive the messages and update the vertices.
      prevG = g
      g = g.joinVertices(messages)(vprog)
      graphCheckpointer.update(g)

      val oldMessages = messages
      // Send new messages, skipping edges where neither side received a message. We must cache
      // messages so it can be materialized on the next line, allowing us to uncache the previous
      // iteration.
      messages = GraphXUtils.mapReduceTriplets(
        g, sendMsg, mergeMsg, Some((oldMessages, activeDirection)))
      // The call to count() materializes `messages` and the vertices of `g`. This hides oldMessages
      // (depended on by the vertices of g) and the vertices of prevG (depended on by oldMessages
      // and the vertices of g).
      messageCheckpointer.update(messages.asInstanceOf[RDD[(VertexId, A)]])
      activeMessages = messages.count()

      logInfo("Pregel finished iteration " + i)

      // Unpersist the RDDs hidden by newly-materialized RDDs
      oldMessages.unpersist(blocking = false)
      prevG.unpersistVertices(blocking = false)
      prevG.edges.unpersist(blocking = false)
      // count the iteration
      i += 1
    }
    messageCheckpointer.unpersistDataSet()
    graphCheckpointer.deleteAllCheckpoints()
    messageCheckpointer.deleteAllCheckpoints()
    g
```

### mapReduceTriplets

# 分区策略



## EdgePartition2D

边基于两个端点的分区方式

点副本数上限2 * sqrt(numParts)

## EdgePartition1D

只根据源点分区

## RandomVertexCut

从实现来看，相同方向的边会分到一起

```scala
  case object RandomVertexCut extends PartitionStrategy {
    override def getPartition(src: VertexId, dst: VertexId, numParts: PartitionID): PartitionID = {
      math.abs((src, dst).hashCode()) % numParts
    }
  }
```

## CanonicalRandomVertexCut

两个点之间的所有边放到一起
