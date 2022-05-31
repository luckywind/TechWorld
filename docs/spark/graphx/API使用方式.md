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

![image-20220117160539291](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220117160539291.png)

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

![image-20220119184225141](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220119184225141.png)

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


![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/20170818200351914.png)

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

最重要的图操作之一，主要用来高效解决相邻边或相邻顶点之间通信问题，例如：将与顶点相邻的边或顶点的数据聚集在顶点上，将顶点数据散发在相邻边上，能简单、高效地解决PageRank等图迭代应用。

计算分为三步:

1. 由边三元组生成消息
2. 向边三元组的顶点发送消息
3. 顶点聚合收到的消息

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
2. sendMsg有一个上下文EdgeContext对象可用，其主要提供了两个消息发送函数sendToSrc和sendToDst

3. tripletFields支持的值可从 枚举TripletFields中获取

作用：

每个点从其邻边和邻居点聚合值，sendMsg方法再每个边上调用，发送消息到该边的两个端点。 mergeMsg用于聚合每个点收集到的消息。

tripletFields： 传给sendMsg的EdgeContext包含哪些字段,默认是TripletFields.All，减少字段可提升性能

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

# 缓存操作

在Spark中，RDD默认并不保存在内存中。为了避免重复计算，当需要多次使用时，建议使用缓存。在迭代计算时，为了获得最佳性能，也可能需要清空缓存。默认情况下缓存的RDD和图表将保留在内存中，直到按照LRU（Least Recently Used）顺序被删除。对于迭代计算，之前迭代的中间结果将填补缓存。虽然缓存最终将被删除，但是内存中不必要的数据还是会使垃圾回收机制变慢。有效策略是，一旦缓存不再需要，应用程序立即清空中间结果的缓存。

![image-20220322095106498](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220322095106498.png)



# Pregel API

[pregel 与 spark graphX 的 pregel api](https://blog.csdn.net/u013468917/article/details/51199808)

**Demo: 单源最短路径**

```scala
import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

val graph = GraphLoader.edgeListFile(sc,"/Spark/web-Google.txt")
val sourceId: VertexId = 0
val initialGraph = graph.mapVertices((id, _) => if (id == sourceId) 0.0 else Double.PositiveInfinity)

val sssp = initialGraph.pregel(Double.PositiveInfinity)( //点初始值
  (id, dist, newDist) => math.min(dist, newDist), // 点更新： 拿自己的值与消息做比较，取最小作为新值
  triplet => { // 边发送消息：如果src的值加上边的值比dest要小，则向dest发送这个较小的值给它，让它更新自己的值
  if (triplet.srcAttr + triplet.attr < triplet.dstAttr) {
  Iterator((triplet.dstId, triplet.srcAttr + triplet.attr))
    } else {
    Iterator.empty
    }
  },
  (a,b) => math.min(a,b) // 点消息聚合： 点收到多条消息时如何聚合，这里取最小
)
sssp.vertices.take(10).mkString("\n")

```



## pregel方法

![image-20220322171650601](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220322171650601.png)



这个方法的签名实际是一个柯里化函数，第一个参数集的后两个都有默认值

```scala
  def pregel[A: ClassTag](      // A:消息的类型
      initialMsg: A,         //参数1: 第一次迭代时向每个点发送的消息
      maxIterations: Int = Int.MaxValue, //参数2:最大迭代次数，默认int最大值
      activeDirection: EdgeDirection = EdgeDirection.Either)(//参数3: 边激活条件(是否在下轮迭代发送消息)，详细解释见下文
      vprog: (VertexId, VD, A) => VD,     // 参数1： 点更新
      sendMsg: EdgeTriplet[VD, ED] => Iterator[(VertexId, A)],//参数2:消息发送(作用在边三元组上)
      mergeMsg: (A, A) => A)//参数3:点消息聚合
    : Graph[VD, ED] = {
    Pregel(graph, initialMsg, maxIterations, activeDirection)(vprog, sendMsg, mergeMsg)
  }
```

## 第一个参数集合

      initialMsg: A,         //参数1: 第一次迭代时向每个点发送的消息
      maxIterations: Int = Int.MaxValue, //参数2:最大迭代次数，默认int最大值
      activeDirection: EdgeDirection = EdgeDirection.Either  //参数3: 决定了边是否发送消息，详细解释见下文

**activeDirection：边的活跃方向**

活跃节点： 收到消息的节点

活跃消息：这轮迭代中所有被收成功收到的消息

如果一个边的所有端点在本轮迭代中都没收到消息，则这个边不再调用sendMsg方法。activeDirection参数指定了过滤器：

-   *EdgeDirection.Out*—sendMsg gets called if srcId received a message during the previous iteration, meaning this edge is considered an “out-edge” of srcId.

  出边调用，即源点接收到了消息

-   *EdgeDirection.In*—sendMsg gets called if dstId received a message during the previous iteration, meaning this edge is considered an “in-edge” of dstId.

 入边调用，即目的点接收到了消息

-   *EdgeDirection.Either*—sendMsg gets called if either srcId or dstId received a message during the previous iteration.

		有一个端点收到消息

-   *EdgeDirection.Both* —sendMsg gets called if both srcId and dstId received mes- sages during the previous iteration.

		两个端点都接收到消息





## 第二个参数集合

      vprog: (VertexId, VD, A) => VD,     // 参数1
      sendMsg: EdgeTriplet[VD, ED] => Iterator[(VertexId, A)],
      mergeMsg: (A, A) => A)

1. Vprog: 点更新程序，在VertexRDD的每个点上运行，消息列表作为输入,输出作为点的新属性
2. sendMsg: 在EdgeRDD有需要的边上运行，triplet作为输入，给源点生成一条消息
3. mergeMsg: 合并点收到的消息，生成mesageRDD，其key是vertexId,value是该vertex的消息

## pregel vs aggregateMessages

pregel的功能其实和迭代版本的aggregateMessages有点像，但有一些区别：

1. aggregateMessages只需要定义两个函数定义它的行为： sendMsg 和 mergeMsg.

在aggregateMessages中，mergeMsg函数将返回的消息结果直接对顶点更新，而pregel的mergeMsg函数将返回的消息结果传递给顶点处理程序vprog

vprog提供了更灵活的逻辑，一些场景下点的数据类型和消息的类型相同，只需要简单的逻辑就可以更新点的值。 还有一些情况，点的值类型和消息类型不同，就需要vprog。

2. **sendMsg函数的签名**

```scala
Pregel中
sendMsg: EdgeTriplet[VD, ED] => Iterator[(VertexId, A)]
//pregel内部实现实际上把EdgeTriplet转换成了EdgeContext，从而可以使用sendToSrc 和 sendToDst.

aggregateMessages中：
EdgeContext[VD, ED, Msg] => Unit
EdgeContext额外增加了两个方法sendToSrc 和 sendToDst.
```

这个差异的原因是Pregel还依赖过时的mapReduceTriplets，还没有升级到aggregateMessages上来，但这个工作已经在进行中了。

用户需要通过Iterator[(VertexId,A)]指定发送哪些消息，发给那些节点，发送的内容是什么，因为在一条边上可以发送多个消息，所以这里是个Iterator，每一个元素是一个tuple，其中的vertexId表示要接收此消息的节点的id，它只能是该边上的srcId或dstId，而A就是要发送的内容，因此如果是需要由src发送一条消息A给dst，则有：Iterator((dstId,A))；如果什么消息也不发送，则可以返回一个空的Iterator：Iterator.empty

3. **mergeMsg: 邻**居节点收到多条消息时的合并逻辑，注意它区别于vprog函数，mergeMsg仅能合并消息内容，但合并后并不会更新到节点中去？，而vprog函数可以根据收到的消息(就是mergeMsg产生的结果)更新节点属性。
3. aggregateMessages返回的是一个VertexRDD对象，而pregel直接返回一个新的Graph对象。
3. Pregel的终止条件是不再有需要发送的消息，所以要求有更灵活的终止条件的算法可以用aggregateMessages()实现。



### 用aggregateMessages也可以实现最远路径

```scala
//定义sendMsg函数
scala> def sendMsg(ec:EdgeContext[Int,String,Int]):Unit = {
                   ec.sendToDst(ec.srcAttr+1)
            }
//定义mergeMsg函数
scala> def mergeMsg(a:Int,b:Int):Int = {
                   math.max(a,b)
            }
            
def propagateEdgeCount(g:Graph[Int,String]):Graph[Int,String] = {
                  //生成新的顶点集
                  val verts = g.aggregateMessages[Int](sendMsg,mergeMsg)
                  val g2 = Graph(verts,g.edges)//根据新顶点集生成一个新图
                 //将新图g2和原图g连接，查看顶点的距离值是否有变化
                  val check = g2.vertices.join(g.vertices).
                  map(x=>x._2._1-x._2._2).
                  reduce(_+_)
                  //判断距离变化，如果有变化，则继续递归，否则返回新的图对象
                  if(check>0)
                      propagateEdgeCount(g2)
                  else
                      g
            }
//初始化距离值，将每个顶点的值设置为0
scala> val newGraph = myGraph.mapVertices((_,_)=>0)
scala> propagateEdgeCount(newGraph).vertices.collect
res11: Array[(org.apache.spark.graphx.VertexId, Int)] = Array((1,0), (2,1), (3,2), (4,2), (5,3), (6,4))

```



# 源码解析

整个计算分为两部分：

1. 在vertexRDD上运行点程序： 第一个超步，所有点接收到初始消息且运行点程序。后续超步，点程序只在收到消息的点上运行，这是通过关联vertexRDD和messageRDD利用GraphX提供的mapVertices函数实现的
2. 为下次迭代生成消息:点程序在当前超步运行完后，基于边的方向，sendMsg程序在这些点关联的边上运行。triplet更新，它负责把新的vertices带给活跃的边(这一步会物化edge triplet RDD)。边计算完毕后，reduce阶段运行，它执行mergeMsg程序。整个步骤使用*mapReduceTriplets*函数，其内部调用了一个改编的*aggregateMessages*函数。



![源码剖析](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/20160913003817287.png)

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/pregel.png" alt="Smiley face" style="zoom:67%;" />

![剖析2](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/20160913003912326.png)







核心实现方法在GraphImpl的aggregateMessagesWithActiveSet方法，该方法聚合一个点收集到的所有消息，最终还是由edgePartition来实现：

edgePartition顺序地(sync)执行了sendMsg操作。

在sendMsg中，完成了pagerank 的sendMsg ，以及meger方法 的函数式调用

```scala
def aggregateMessagesEdgeScan[A: ClassTag](
      sendMsg: EdgeContext[VD, ED, A] => Unit,
      mergeMsg: (A, A) => A,
      tripletFields: TripletFields,
      activeness: EdgeActiveness): Iterator[(VertexId, A)] = {
    val aggregates = new Array[A](vertexAttrs.length)
    val bitset = new BitSet(vertexAttrs.length)

    var ctx = new AggregatingEdgeContext[VD, ED, A](mergeMsg, aggregates, bitset)
    var i = 0
    while (i < size) {
      val localSrcId = localSrcIds(i)
      val srcId = local2global(localSrcId)
      val localDstId = localDstIds(i)
      val dstId = local2global(localDstId)
      val edgeIsActive =
        if (activeness == EdgeActiveness.Neither) true
        else if (activeness == EdgeActiveness.SrcOnly) isActive(srcId)
        else if (activeness == EdgeActiveness.DstOnly) isActive(dstId)
        else if (activeness == EdgeActiveness.Both) isActive(srcId) && isActive(dstId)
        else if (activeness == EdgeActiveness.Either) isActive(srcId) || isActive(dstId)
        else throw new Exception("unreachable")
      if (edgeIsActive) {
        val srcAttr = if (tripletFields.useSrc) vertexAttrs(localSrcId) else null.asInstanceOf[VD]
        val dstAttr = if (tripletFields.useDst) vertexAttrs(localDstId) else null.asInstanceOf[VD]
        ctx.set(srcId, dstId, localSrcId, localDstId, srcAttr, dstAttr, data(i))
        sendMsg(ctx)
      }
      i += 1
    }

    bitset.iterator.map { localId => (local2global(localId), aggregates(localId)) }
  }
```



最终调用从RDD继承来的fold方法，fold方法内部调用sc.runJob来提交Job。

https://www.essi.upc.edu/dtim/blog/post/graphx-pregel-internals-and-tips-for-distributed-graph-processing
