[参考](https://waltyou.github.io/Mastering-Apache-Spark-Core-7-Services-DAGScheduler/)

实现面向Stage的高阶调度层，对每个job计算一个stage的DAG，跟踪RDD和stage的物化并找到最小的调度来运行job。然后，它把stage以TaskSets的形式提交给一个运行在集群上的TaskScheduler实现。TaskSet包含了完全独立的任务，可以根据已经在集群中的数据 (例如从以前的stages映射出输出文件)来立即运行，尽管如果这些数据不可用，它可能会失败。


Spark stage是通过在shuffle边界上打破RDD图创建的。RDD操作具有“窄”的依赖关系，如map()和filter()，
它们在每个stage都被连接到一组任务中，但是处理带有shuffle依赖性的操作需要多个stage(一个要编写一组映射
输出文件，另一个要在一个barrier后读取这些文件)。最后，每个stage只会对其他stage的依赖进行调整，
并可能计算其中的多个操作。这些操作的实际流水线发生在各种RDDs(MappedRDD、FilteredRDD等)的RDD.compute()函数中


除了有一个stages的DAG之外，DAGScheduler还根据当前的缓存状态确定首选的位置来运行每个任务，
并将这些任务传递给低阶的TaskScheduler。此外，它处理由于shuffle输出文件丢失而导致的故障，在这种情况下，
旧stage可能需要重新提交。在一个不是由shuffle文件丢失引起的stage中的失败由TaskScheduler处理，
它将在取消整个stage之前对每个任务进行多次重新尝试。

几个关键的概念：

1. Jobs:   ActiveJob 提交给调度器的最高阶工作项，例如Action算子

2. Stages: Stage 是用于产生Job中间结果的Task集合，以shuffle为边界划分stage。引入了barrier来等待上一个stage完成。有两种类型的Stage：

   1. ResultStage： 用于执行最后一个Stage
   2. ShuffleMapStage： 为shuffle写map输出文件

3. Task:  发送到单机的工作单元

4. Cache tracking: 缓存跟踪:DAGScheduler指明了那些RDDs被缓存，以避免重新计算它们，同样的，shuffle map stages已经产生了输出文件，以避免重做shuffle的map side
5. Preferred locations： 
6. Cleanup

AGScheduler是主要产生各类SparkListenterEvent的源头，它将各种SparkListenterEvent发送到listenerBus的事件队列中，listenerBus通过定时器将
 SparkListenerEvent事件匹配到具体的SparkListener,改变SparkListener中的统计监控数据，最终由SparkUI的界面
 进行展示。

   DAGScheduler的数据结构主要是维护jobId和stageId的关系，Stage,ActiveJob,以及缓存的RDD的partitions的
位置信息。

>  DAGScheduler负责将Task拆分成不同Stage的具有依赖关系（包含RDD的依赖关系）的多批任务，然后提交给TaskScheduler
> 进行具体处理。DAG全称 Directed Acyclic Graph，有向无环图。简单的来说，就是一个由顶点和有方向性的边构成的图，从任
> 意一个顶点出发，没有任何一条路径会将其带回到出发的顶点。在作业调度系统中，调度的基础就在于判断多个作业任务的依赖关系，这些任务之间可能存在多重的依赖关系，也就是说有些任务必须先获得执行，然后另外的相关依赖任务才能执行，但是任务之间显然不应该出现任何直接或间接的循环依赖关系，所以本质上这种关系适合用DAG有向无环图来表示。
> 作业调度核心——DAGScheduler
>    用户代码都是基于RDD的一系列计算操作，实际运行时，这些计算操作是Lazy执行的，并不是所有的RDD操作都会触发Spark往
>  Cluster上提交实际作业，基本上只有一些需要返回数据或者向外部输出的操作才会触发实际计算工作（Action算子），其它的
>  变换操作基本上只是生成对应的RDD记录依赖关系（Transformation算子）。

 在这些RDD.Action操作中（如count,collect）会自动触发runJob提交作业，不需要用户显式的提交作业（这一部分可以看下Spark DAGSheduler生成Stage过程分析实验）

 作业调度的两个主要入口是submitJob 和 runJob，两者的区别在于前者返回一个Jobwaiter对象，可以用在异步调用中，
 用来判断作业完成或者取消作业，runJob在内部调用submitJob，阻塞等待直到作业完成（或失败）



为了从失败中恢复，一个stage可能需要多次运行，称为attempts。





transformation类算子会创建窄依赖的RDD, action类算子会创建shuffle依赖的RDD。

