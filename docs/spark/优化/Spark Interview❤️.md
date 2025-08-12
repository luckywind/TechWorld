# spark3

主要有四个方面的优化：

1. 自适应执行
   1. 自动调整分区数，分区数过小，意味着分区很大，处理这个分区的task可能需要把数据落盘拖慢查询速度；分区数过大，意味着大量小task执行大量小的网络数据拉取，效率低。 <font color=red>spark3会自动把小的map结果合并，用一个reduce task来处理。</font>
   2. <font color=red>动态切换join策略</font>
      自适应执行会根据stage的统计结果，自动调整更优的join策略
   3. 动态优化倾斜的join，spark3检测到倾斜分区，且分为多个分区启用多个task进行join
2. 动态分区裁剪
   1. Spark会首先过滤维表，根据过滤后的结果找到只需要读事实表的分区，这样就能极大的提升性能
3. join 提示
   1. 也是优化join策略的手段
4. 查询编译加速

# join

## 5中join策略

一共五种Join策略：

1. **shuffle hash Join**:
   核心逻辑是参与Join的两个表先按照相同的分区算法进行洗牌，这样同一个key的数据在一个分区里，然后分区对之间进行局部join，而局部join是把小表的分区放入map结构  
   缺点就是需要shuffle,  且因为小表的分区需要放入map结构，所以数据倾斜的情况容易导致OOM
   优点是不需要排序

2. **broadcast hash join**
   如果一个表特别小，直接放入内存然后广播到每个Executor，大表的每个分区直接进行一个map side join即可
   优点是不需要shuffle, 不需要排序，速度非常快
   缺点是需要有个表足够小

3. **sort merge join**

   一般用这个join的场景较多，核心逻辑是两个表先按照相同的分区算法进行洗牌，然后分区内都进行排序，分区对儿之间进行局部join的逻辑是按顺序迭代匹配，匹配上就输出，匹配不上就继续迭代小的一侧。

   优点是适用性较广，对表的大小没有要求

   缺点是需要shuffle和排序

4. **笛卡尔积**

5. **Broadcast Nested Loop Join**

​		核心逻辑是，小表广播到每个Executor,然后大表的每个分区的每条记录执行一个循环匹配，一一去匹配小表的所有记录。效率低下，这个策略是最后的join策略。

# 图

​          最近在研究图计算，考虑到Spark的生态，项目中采用了GraphX框架，相比其他框架，它的优势在于既可以将底层数据看成一个完整的图，也可以对边RDD和顶点RDD使用数据并行处理原语。API大致分为三层，存储层定义了点RDD/边RDD以及三元组；操作层由GraphImpl类和GraphOps类实现； 算法层实现了常见的图算法，且大部分算法都基于Pregel算法实现。

​        Pregel核心原理： 是一种基于BSP模型实现的并行图处理系统。整个流程分为多个超步，每个超步内执行一轮消息传递。核心逻辑：

1. 首先所有顶点接收到一个初始消息进行初始化，用户可定义顶点接收到消息后如何处理，也就是如何更新自己的数据，这个逻辑称为点更新逻辑
2. 接着，接收到消息的顶点成为活跃顶点，没有接收到的顶点进入钝化态，框架统计活跃顶点数和迭代次数如果活跃顶点数大于0且迭代次数没有达到终止条件，就继续按照需求发送消息。消息从活跃的顶点发出，可定义向出边发送/入边发送还是都发送还是不发送。发送的消息内容可以基于原点数据以及边数据构造。接收到消息的顶点再次成为活跃顶点，如果一个顶点接收到多个消息，会先对消息进行聚合，然后再按照顶点更新逻辑更新自己。
3. 当达到终止条件后算法停止

​		目前有几个大的应用： 

1. 统一ID的应用。 采用联通分量算法实现，保持ID的统一和稳定不变； 实践过程中遇到的问题就是，发现有些数据合并错误了，也就是说多个实体，我们错误识别成一个，而分配了一个ID，我们设计了拆分逻辑， 其中有一个非常极端的案例，就是该ID下的id数据太庞大，尝试过直接丢弃掉，优化逻辑后让算法重新合并，但是这时算法的迭代次数会远超平时。这是因为这个巨型的图内部不停的在发送消息。目前的解决办法是找到这种巨型图的业务原因，过滤异常数据，且线上不丢弃它。
2. 大数据平台。数据血缘项目有个需求，数据血缘是一个有向图，顶点是HIve/talos/hdfs/doris等等不同种类的数据，边是数据之间的依赖关系；需求是计算每个数据表处在数据血缘中的第几层，上游不同种类的数据分别有多少。

   1. 数据上游层级这个解决起来比较简单， 核心思想就是下游数据层级=其所有上游数据层级的最大值+1，初始化时，所有数据表的上游层级都是0 ，然后上游向下游发送一个消息，该消息就是上游表的层级+1。 数据表把收到的消息取最大就是其上游数据的层级
   2. 需求二是计算上游不同种类数据源数量，这里需要注意去重，也就是说当前数据表的上游表是其所有父级表的所有上游数据再加上父级表构成，但在统计时需要注意去重。我在实现时采用了Set集合保存当前上游表id自动实现去重
   3. 上述两个需求最复杂的就是图中出现环的情况，开始我是设置了一个最大迭代次数强制结束算法。这会导致上游层数不对。发送消息时判断目的顶点是否在自己的上游集合中，如果在则不再发送。这样就解决了环的问题。


## oneid

1. 统计
2. 风控领域
   1. 垃圾注册、薅羊毛、刷单，app恶意刷下载量
3. 设备画像
4. 广告归因：例如，app的下载链接点击下载，判断两个渠道ID是否相同从而判断是否因为该链接而下载
5. 广告主进行广告投放计算，如果没有oneid,一个设备会被当成多个
6. 黑产识别： 风险评分，TZFID相对imei更难改写，通过TZFID对应的imei个数评级，判断风险系数，当前5%属于中风险
7. 做法： 维护设备ID倒排表，每来一批数据，按优先级逐一查倒排表，且总是基于更稳定的ID生成设备ID
8. 指标： 膨胀率4%，一致率99.6%



# RDMA

## Spark  UCX项目

1. CPU资源： jUCX调用RDMA read 吞吐接近硬件(CX5)极限(100Gbps)时，CPU占用为0，而使用TCP接口时，server端CPU占用100%，client端占用68%。
2. 加速原理: **减少了fetch wait time** 

<font color=red>由于shuffle read的网络传输部分是Async的，一个BlockingQueue被用来连接Async的网络传输与后续的compute逻辑</font>

具体来说shuffle read不断从网络拉取数，并且将准备就绪的数据放入BlockingQueue里，而后续compute逻辑从BlockingQueue里读取这些数据用于计算。若BlockingQueue里没有数据，则compute逻辑会被阻塞，直到有数据准备就绪。

![fetchWaitTime](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/fetchWaitTime.png)



- 在Shuffle read过程中，如上图红色部分所示，原生方案是使用java的NIO channel从磁盘读取数据，然后调用Netty库通过TCP拉取数据
- 使用Sparkucx这个插件后，如下图红色部分所示，shuffle read逻辑变为用UCX提供的Java接口调用RDMA read，直接从数据源拉取数据

由于RDMA的网络传输优于TCP，红色的部分将被加速

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20240814113344342.png" alt="image-20240814113344342" style="zoom: 67%;" />

   

## RDMA

RDMA可以简单理解为利用相关的硬件和网络技术，服务器1的网卡可以直接读写服务器2的内存，最终达到高带宽、低延迟和低资源利用率的效果。RDMA 具有零拷贝、协议栈卸载的特点。RDMA 将协议栈的实现下沉至 RDMA 网卡 (RNIC)，绕过内核直接访问远程内存中的数据。由于不经过系统内核协议栈，RDMA 与传统 TCP/IP 实现相比不仅节省了协议处理和数据拷贝所需的 CPU 资源，同时也提高了网络吞吐量、降低了网络通信时延。

对比传统网络协议，RDMA 网络协议具有以下三个特点：

- 旁路软件协议栈

RDMA 网络依赖 RNIC 在网卡内部完成数据包封装与解析，旁路了网络传输相关的软件协议栈。对于用户态应用程序，RDMA 网络的数据路径旁路了整个内核；对于内核应用程序，则旁路了内核中的部分协议栈。由于旁路了软件协议栈，将数据处理工作卸载到了硬件设备，因而 RDMA 能够有效降低网络时延。

- CPU 卸载

RDMA 网络中，CPU 仅负责控制面工作。数据路径上，有效负载由 RNIC 的 DMA 模块在应用缓冲区和网卡缓冲区中拷贝 (应用缓冲区提前注册，授权网卡访问的前提下)，不再需要 CPU 参与数据搬运，因此可以降低网络传输中的 CPU 占用率。

- 内存直接访问

RDMA 网络中，RNIC 一旦获得远程内存的访问权限，即可直接向远程内存中写入或从远程内存中读出数据，不需要远程节点参与，非常适合大块数据传输。

## k8s存储卸载

通过硬件模拟向宿主机提供标准的nvme/virtio块设备，对网络存储协议的处理都卸载到DPU，提供硬件加速的NVME over RDMA能力。

spark on  k8s：

在一些磁盘IO成为性能瓶颈的场景下，基于DPU的云盘挂载可以有效加速Spark数据加载过程，从而提升整体查询效率。经测试，相对于NVMe云盘，使用基于DPU 实现的加速云盘，Spark SQL数据加载性能最高提升2.81倍，E2E时间缩短50%。

另一方面CPU、内存资源没有增加，但占用时间更短，达到节省资源的目的。

# 向社区提交的BUG

## duration 为0

WholeStageCodegenExec的doExecute函数执行过程中累加duration的方式是迭代到当前分区最后一条时才累加duration。 假如我们的Sql带limit子句，那么可能没有迭代完一个分区，导致这个duration不会被累加，从而为0

# OOM

## 常见场景

[参考1](https://blog.csdn.net/yhb315279058/article/details/51035631)

[参考2](https://blog.51cto.com/wang/4634620)

[Spark调优 | Spark OOM问题常见解决方式](https://cloud.tencent.com/developer/article/1904928)

Spark中的OOM问题不外乎以下三种情况：

- map执行中内存溢出
- shuffle后内存溢出
- driver内存溢出
  - collect了大数据集合
  - 作业的task数太多，可考虑缩小分区数
  - 通过参数spark.ui.retainedStages(默认1000)/spark.ui.retainedJobs(默认1000)控制.
  - 增加 内存

**Executor端OOM**

Spark内存模型，一个Executor中的内存主要分为Excution内存和Storage内存和other内存

1. Excution内存是执行内存，join、聚合、shuffle等操作都会用到这个内存，默认占比0.2，是**OOM高发区**
2. Storage内存是存储broadcast\cache\persist数据的地方，默认占比0.6
3. other是程序预留内存，占比较少

spark1.6之后，Excution和Storage可互相借用，且加入了堆外内存，减少频繁的full gc。

1. map过程产生大量对象导致内存溢出

   > 可在产生大量对象的操作之前增大分区数，从而减少每个task的数据量

2. 数据倾斜导致内存溢出

   > 增大分区数

3. coalesce调用导致内存溢出

   > coalesce减少分区的同时也减少了整个流程task个数(因为没有shuffle)，导致单个task内存占用过大。可用repartition代替，因为它是一个shuffle操作，shuffle前的stage task数量没有减少

4. shuffle后单个文件过大内存溢出

   > shuffle后单个文件过大导致OOM，需要传入分区器，或者传入分区数使用默认的HashPartitioner。

5. 内存评估误差：因为有spill过程，数据倾斜理论上不会导致OOM，但是JVM是采样评估内存占用的， 可能会因为误差导致OOM。

6. 单个Block过大： 单个block是需要足够的内存，可能因为数据倾斜导致单个block过大而OOM，可以设置 spark.maxRemoteBlockSizeFetchToMem 参数，设置这个参数以后，超过一定的阈值，会自动将数据 Spill 到磁盘，此时便可以避免因为数据倾斜造成 OOM 的情况



## 内存问题定位

用哪些方法、工具

- OneID项目中，GraphX代码执行很慢。UI上看到stage retry， 
- 

## 内存管理

内存的分布

执行内存对存储内存的借用过程，存储内存中的数据落盘过程。

![image-20220416162818460](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220416162818460.png)

> 1. 下面两块内存都是堆外内存,spark2.4.5以前有一些区别，就是Overhead包含下面的offHeap内存
>
> 2. 第一块和第三块内存是Spark Core使用的
>
> 3. Spark内存设计不合理的地方： 如果不配置MaxDirectMemorySize，那么netty就认为executory内存就是可用的最大堆外内存。executor申请的内存未必会全用完，剩余的空间会被netty当作Direct Memory使用，所以executor的内存就被netty认为是可以申请的最大内存(不管spark已经使用多少了)，所以就会可劲儿使用。一旦使用超过Executor申请的总量，就会OOM。

1. **统一内存**

执行内存和存储内存可互相借用，执行内存可借用未使用的存储内存，可强制归还被借用的执行内存，但不能强制非借用的已被使用的存储内存落盘。

2. **Task内存**

为了更好地使用内存，Executor 内运行的 Task 之间共享着 Execution 内存，因此，可能会出现这样的情况：先到达的任务可能占用较大的内存，而后到的任务因得不到足够的内存而挂起。具体的，Spark 内部维护了一个 HashMap 用于记录每个 Task 占用的内存。

源代码中申请过程是一个while循环，退出条件是申请到最小内存，每个 Task 可以使用 Execution 内存大小范围为 1/2N ~ 1/N，其中 N 为当前 Executor 内正在运行的 Task 个数。

> - 在 MemoryConsumer 中有 Spill 方法，当 MemoryConsumer 申请不到足够的内存时，可以 Spill 当前内存到磁盘，从而避免无节制的使用内存。但是，对于堆内内存的申请和释放实际是由 JVM 来管理的。因此，在统计堆内内存具体使用量时，考虑性能等各方面原因，Spark 目前采用的是抽样统计的方式来计算 MemoryConsumer 已经使用的内存，从而造成堆内内存的实际使用量不是特别准确。从而有可能因为不能及时 Spill 而导致 OOM。
>
> - 在 Reduce 获取数据时，由于数据倾斜，有可能造成**单个 Block(注意，不是分区)** 的数据非常的大，默认情况下是<font color=red>需要有足够的内存来保存单个 Block 的数据</font>。因此，此时极有可能因为数据倾斜造成 OOM。 **可以设置 spark.maxRemoteBlockSizeFetchToMem 参数，设置这个参数以后，超过一定的阈值，会自动将数据 Spill 到磁盘，此时便可以避免因为数据倾斜造成 OOM 的情况**。

3. **堆外内存**

spark.memory.offHeap.size是spark Core(memory manager)使用的，spark.executor.memoryOverhead是资源管理器使用的，例如YARN，可以理解为jvm本身维持运行所需要的额外内存。

4. 内存调优
   - 通过控制Executor的核数和task的cpu数控制并行度，从而控制Task分配的内存
   - 调整应用的并行度，从而从总体上限制task数量
   - UI上算子会给出内存峰值和数据溢写情况



## 遇到过的问题分析过程

1. <font color=red>图计算作业异常慢</font>
   **根因**：节点故障导致分区重算，而重算用到了Task的状态，由于是迭代计算，重算的数据链路特别长，导致重试要非常长时间。
   **解决**：首先想到使用checkpoint斩断DAG图，但是Pregel算法是迭代计算，并没有给用户留下可以手动做checkpoint的地方，查文档发现GraphX提供了一个定期checkpoint的参数。

2. <font color=red>SparkStreaming作业持续延迟</font>
   现象：从UI上看每个作业都使用了4s，超过batch间隔2s。

   分析过程： 

   - **部分task启动晚了**：通过查看每个batch的执行时间，发现每个都用了4s，再进入stage里面发现每个task的执行时间只有20ms左右，但是部分task的发起时间比其他task晚了4s，导致整个stage晚4s完成。
   - 再分析**启动慢的task的本地化级别**，发现都是RACK_LOCAL。 
   - 作业分配的executor数少于集群的数量，导致**有些机器上没有executor**，出现数据和代码不在同一个节点上的task，等待3s才退而求其次采用RACK_LOCAL的办法启动。
   - spark.locality.wait默认值就是3s 佐证了分析

   解决办法： 调整作业的executor数量和集群节点数量一致

3. <font color=red>Spark Netty内存泄露问题</font>
   现象：同一个SQL执行多次，机器内存占用阶梯上涨，且不释放。最终导致netty OutOfDirectMemoryError
   分析过程：netty是spark shuffle read过程使用的通信框架，猜想到shuffle read的代码，TableReader的输入流没有准确释放，输入流读取完成时、异常时都没有释放。

4. Spark其他内存释放问题
   steam表和build表都是分批次的，join过程是两个批次流的两两join。stream batch在以此和所有build batch     join时，要等到最后一个build batch完成join才能释放。



# 参数哪些调优？

1. driver内存
2. rdd压缩
3. 使用KryoSerializer序列化器
4. 增加本地执行等待时间
5. 推测执行



1. 并行度优化
   1. 增加executor个数提升并行能力
   2. 增加分区数
2. Join优化
   1. **采用广播器进行Join代替shuffle Join**
   2. **让两个RDD共享分区器避免shuffle Join**
3. 缓存优化
   1. **公用RDD进行缓存**
   2. **checkpoint的使用**
4. 算子优化
   1. **reduceByKey代替groupByKey**
5. **调节数据本地性等待时长**



# SparkSQL调优的场景？问题定位？调优方法？效果？更好的方案？





# groupbykey实现细节



