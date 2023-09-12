# Distribution

## Distribution接口

每个physical operator都实现了requiredChildDistribution方法，以获得一个Distribution的实例，**用于表示 operator对其input数据分布情况的要求**。

Distribution描述了同一个表达式下的tuple如何在多个机器上分布

这个信息可以让一些算子(例如Aggregate)执行分区局部操作

```scala
sealed trait Distribution {
  //需要的分区数，None意味着无所谓
  def requiredNumPartitions: Option[Int]

  /**
   * Creates a default partitioning for this distribution, which can satisfy this distribution while
   * matching the given number of partitions.
   */
  //创建默认的partitioning，分区数一致时就满足当前Distrbution
  def createPartitioning(numPartitions: Int): Partitioning
}
```

### UnspecifiedDistribution

未指定分布，无需确定数据无组之间的位置关系 。

```scala
case object UnspecifiedDistribution extends Distribution {
  override def requiredNumPartitions: Option[Int] = None
  override def createPartitioning(numPartitions: Int): Partitioning = {
    throw new IllegalStateException("UnspecifiedDistribution does not have default partitioning.")
  }
}
```

我们知道Distribution是physical operator **用于表示operator对其input数据（child节点的输出数据）分布情况的要求，**那UnspecifiedDistribution的意思就是对Child的分区规则没有要求，无所谓，你啥样都行

比如：

```
select a,count(b)  from testdata2  group by a

== Physical Plan ==
HashAggregate(keys=[a#3], functions=[count(1)], output=[a#3, count(b)#11L])
+- HashAggregate(keys=[a#3], functions=[partial_count(1)], output=[a#3, count#15L])
   +- SerializeFromObject [knownnotnull(assertnotnull(input[0, org.apache.spark.sql.test.SQLTestData$TestData2, true])).a AS a#3]
      +- Scan[obj#2]
```

SerializeFromObject节点，

它的requiredChildDistribution就是UnspecifiedDistribution：

```scala
SerializeFromObject-SerializeFromObjectExec   
requiredChildDistribution：List(UnspecifiedDistribution)
requiredChildOrdering：List(List())
outputPartitioning：UnknownPartitioning(0)
```

### BroadcastDistribution

广播分布，数据会被广播到所有节点上。构造参数mode为广播模式BroadcastMode，广播模式可以为原始数据IdentityBroadcastMode或转换为HashedRelation对象HashedRelationBroadcastMode。

```scala
case class BroadcastDistribution(mode: BroadcastMode) extends Distribution {
  override def requiredNumPartitions: Option[Int] = Some(1)

  override def createPartitioning(numPartitions: Int): Partitioning = {
    assert(numPartitions == 1,
      "The default partitioning of BroadcastDistribution can only have 1 partition.")
    BroadcastPartitioning(mode)
  }
}
```

以 BroadcastHashJoinExec 为例：

如果是Broadcast类型的Join操作假设左表做广播，那么requiredChildDistribution得到的列表就是[BroadcastDistribution(mode),UnspecifiedDistribution]，表示左表为广播分布；

如果是Broadcast类型的Join操作假设右表做广播，那么requiredChildDistribution得到的列表就是[UnspecifiedDistribution,BroadcastDistribution(mode)]，表示右表为广播分布；

### OrderedDistribution

构造参数ordering是seq[SortOrder]类型该，分布意味着数据元组会根据ordering计算后的结果排序。

```scala
case class OrderedDistribution(ordering: Seq[SortOrder]) extends Distribution {
  require(
    ordering != Nil,
    "The ordering expressions of an OrderedDistribution should not be Nil. " +
      "An AllTuples should be used to represent a distribution that only has " +
      "a single partition.")

  override def requiredNumPartitions: Option[Int] = None

  override def createPartitioning(numPartitions: Int): Partitioning = {
    RangePartitioning(ordering, numPartitions)
  }
}
```

以 SortExec 为例：

在全局排序的sort算子中，requiredChildDistribution得到的列表是[OrderedDistribution(sortOrder)]，其中sortOrder是排序表达式，相同的数据ordering计算结果相同因此能够保持连续性并被划分到相同分区中

### AllTuples

只有一个分区，所有的数据元组存放在一起

```scala
case object AllTuples extends Distribution {
  override def requiredNumPartitions: Option[Int] = Some(1)

  override def createPartitioning(numPartitions: Int): Partitioning = {
    assert(numPartitions == 1, "The default partitioning of AllTuples can only have 1 partition.")
    SinglePartition
  }
}
```

以 GlobalLimitExec 为例：

选取全局前K条数据的GlobalLimit算子，requiredChildDistribution得到的列表就是AllTuples，表示执行该算子需要全部的数据参与

###ClusteredDistribution

构造参数clustering是Seq[Expression]类型，起到了hash函数的效果，数据经过clustering计算后，相同value的数据元组会被存放在一起。如果有多个分区的情况，则相同的数据会被存放在同一个分区中；如果只能是单个分区，则相同的数据会在分区内连续存放。

```scala
case class ClusteredDistribution(
    clustering: Seq[Expression],
    requiredNumPartitions: Option[Int] = None) extends Distribution {
  require(
    clustering != Nil,
    "The clustering expressions of a ClusteredDistribution should not be Nil. " +
      "An AllTuples should be used to represent a distribution that only has " +
      "a single partition.")

  override def createPartitioning(numPartitions: Int): Partitioning = {
    assert(requiredNumPartitions.isEmpty || requiredNumPartitions.get == numPartitions,
      s"This ClusteredDistribution requires ${requiredNumPartitions.get} partitions, but " +
        s"the actual number of partitions is $numPartitions.")
    HashPartitioning(clustering, numPartitions)
  }
}
```

以 HashAggregateExec 为例：

HashAggregateExec 沿用父类BaseAggregateExec 的requiredChildDistribution 方法 ，其执行的前提是“所有具有相同aggregation key的record放到同一个处理单元中”。

在Spark中，这样的处理单元就是RDD的一个partition，因此也就是要满足“所有group by 的column具有相同value的record被分配到RDD的同一个partition中”。

HashAggregateExec的requiredChildDistribution就是ClusteredDistribution。

###HashClusteredDistribution

HashClusteredDistribution与ClusteredDistribution类似，构造参数expressions是Seq[Expression]类型，起到了hash函数的效果。

但是比ClusteredDistribution更严格，不仅保证具有相同key的record被分配到同一个partition内，而且保证了对每一个key分配到的partition id也都是确定的。

```scala
case class HashClusteredDistribution(
    expressions: Seq[Expression],
    requiredNumPartitions: Option[Int] = None) extends Distribution {
  require(
    expressions != Nil,
    "The expressions for hash of a HashClusteredDistribution should not be Nil. " +
      "An AllTuples should be used to represent a distribution that only has " +
      "a single partition.")

  override def createPartitioning(numPartitions: Int): Partitioning = {
    assert(requiredNumPartitions.isEmpty || requiredNumPartitions.get == numPartitions,
      s"This HashClusteredDistribution requires ${requiredNumPartitions.get} partitions, but " +
        s"the actual number of partitions is $numPartitions.")
    HashPartitioning(expressions, numPartitions)
  }
}
```

以 SortMergeJoinExec 为例：

在Spark的实现里，SortMergeJoinExec的实现简单来说就是把join两边的RDD中具有相同id的partition zip到一起进行关联。

ClusteredDistribution保证的是具有相同key的record能聚集到同一个partition中，但对join来说这样还不够。

在RDD1中假设join key为1的record分配到了partition 0，那么如果RDD1和RDD2要进行join，则RDD2中所有join key为1的record也必须分配到partition 0中。Spark通过在**左右两边的shuffle中使用相同的hash函数和shuffle partition number**来保证这一点。

SortMergeJoinExec对join两边的requiredChildDistribution列表是

[HashClusteredDistribution(leftKeys),HashClusteredDistribution(rightKeys)]，表示左表数据根据leftKey表达式计算分区，右表数据根据rightKeys表达式计算分区。

# Partitioning

描述算子输出如何分区，两个关键属性

1. 分区数
2. 是否可满足给定的dristribution

```scala
trait Partitioning {
  val numPartitions: Int


  final def satisfies(required: Distribution): Boolean = {
    //分区数相等的前提下，
    required.requiredNumPartitions.forall(_ == numPartitions) && satisfies0(required)
  }


  def createShuffleSpec(distribution: ClusteredDistribution): ShuffleSpec =
    throw new IllegalStateException(s"Unexpected partitioning: ${getClass.getSimpleName}")

  //默认是满足无要求 和 分区数为1时满足AllTuples
  //用户也可重写
  protected def satisfies0(required: Distribution): Boolean = required match {
    case UnspecifiedDistribution => true
    case AllTuples => numPartitions == 1
    case _ => false
  }
}

```

## RoundRobinPartitioning(numPartitions: Int) extends Partitioning

均匀分布： 从一个随机分区数开始，以round-robin方式分发数据行，用于实现DataFrame.repartition()算子。

SinglePartition

## HashPartitioning

数据行基于hash(表达式)进行分发，hash值一样的一定在一个分区

分区表达式Pmod(new Murmur3Hash(expressions), *Literal*(numPartitions))

即先用Murmur3Hash计算表达式hash值，然后对分区数取模

```scala
case class HashPartitioning(expressions: Seq[Expression], numPartitions: Int)
  extends Expression with Partitioning with Unevaluable {

  override def children: Seq[Expression] = expressions
  override def nullable: Boolean = false
  override def dataType: DataType = IntegerType

  override def satisfies0(required: Distribution): Boolean = {
    super.satisfies0(required) || {
      required match {
        case h: StatefulOpClusteredDistribution =>
          expressions.length == h.expressions.length && expressions.zip(h.expressions).forall {
            case (l, r) => l.semanticEquals(r)
          }
        case c @ ClusteredDistribution(requiredClustering, requireAllClusterKeys, _) =>
          if (requireAllClusterKeys) {
            // Checks `HashPartitioning` is partitioned on exactly same clustering keys of
            // `ClusteredDistribution`.
            c.areAllClusterKeysMatched(expressions)
          } else {
            expressions.forall(x => requiredClustering.exists(_.semanticEquals(x)))
          }
        case _ => false
      }
    }
  }

  override def createShuffleSpec(distribution: ClusteredDistribution): ShuffleSpec =
    HashShuffleSpec(this, distribution)

 // 返回一个产出分区ID的表达式
  def partitionIdExpression: Expression = Pmod(new Murmur3Hash(expressions), Literal(numPartitions))

  override protected def withNewChildrenInternal(
    newChildren: IndexedSeq[Expression]): HashPartitioning = copy(expressions = newChildren)
}
```

## KeyGroupedPartitioning

```scala
case class KeyGroupedPartitioning(
    expressions: Seq[Expression],
    numPartitions: Int,
    partitionValuesOpt: Option[Seq[InternalRow]] = None) extends Partitioning {

  override def satisfies0(required: Distribution): Boolean = {
    super.satisfies0(required) || {
      required match {
        case c @ ClusteredDistribution(requiredClustering, requireAllClusterKeys, _) =>
          if (requireAllClusterKeys) {
            // Checks whether this partitioning is partitioned on exactly same clustering keys of
            // `ClusteredDistribution`.
            c.areAllClusterKeysMatched(expressions)
          } else {
            // We'll need to find leaf attributes from the partition expressions first.
            val attributes = expressions.flatMap(_.collectLeaves())
            attributes.forall(x => requiredClustering.exists(_.semanticEquals(x)))
          }

        case _ =>
          false
      }
    }
  }

  override def createShuffleSpec(distribution: ClusteredDistribution): ShuffleSpec =
    KeyGroupedShuffleSpec(this, distribution)
}
```

## RangePartitioning

## PartitioningCollection

## BroadcastPartitioning

# SparkPlan

## requiredChildDistribution

```scala
  def requiredChildDistribution: Seq[Distribution] =
    Seq.fill(children.size)(UnspecifiedDistribution)
```

指定当前算子对所有子算子的数据分布要求，默认每个子算子都是*UnspecifiedDistribution*，即没有要求。如果一个算子重写了这个方法，并且对多个child指定了分布需求(不是*UnspecifiedDistribution*，和*BroadcastDistribution*), Spark会保证这些child输出相同的分区，因此算子可以把这些子节点的分区RDD进行zip。一些 算子会利用这个保证来满足一些有趣的需求，例如非广播join可以对左孩子指定*HashClusteredDistribution(a,b)*，对右孩子指定*HashClusteredDistribution(c,d)*，这将保证左右孩子是被a,b/c,d进行同分区的，意味着相同的tuple在相同索引分区里，例如*(a=1,b=2) 和 (c=1,d=2)*都是左右孩子的第二个分区中。



# Partitioner

```scala
abstract class Partitioner extends Serializable {
  def numPartitions: Int
  def getPartition(key: Any): Int
}
```

把key映射到分区ID，有以下几种分区器：

![image-20230831161404203](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230831161404203.png)

## HashPartitioner

源码解读如下

```scala
class HashPartitioner(partitions: Int) extends Partitioner {
  require(partitions >= 0, s"Number of partitions ($partitions) cannot be negative.")

  def numPartitions: Int = partitions

  def getPartition(key: Any): Int = key match {
    // key为空，统一扔到0号分区
    case null => 0
    //否则，利用hashCode和分区数计算分区ID
    case _ => Utils.nonNegativeMod(key.hashCode, numPartitions)
  }

  override def equals(other: Any): Boolean = other match {
    case h: HashPartitioner =>
      h.numPartitions == numPartitions
    case _ =>
      false
  }

  override def hashCode: Int = numPartitions
}
```

### 几个常见的使用HashPartitioner的例子

1. Reducebykey

def reduceByKey(func: (V, V) => V): RDD[(K, V)] = self.withScope {	
  reduceByKey(defaultPartitioner(self), func)	
}

2. aggregateByKey

def aggregateByKey[U: ClassTag](zeroValue: U, numPartitions: Int)(seqOp: (U, V) => U,	
    combOp: (U, U) => U): RDD[(K, U)] = self.withScope {	
  aggregateByKey(zeroValue, new HashPartitioner(numPartitions))(seqOp, combOp)	
}

3. join

def join[W](other: RDD[(K, W)]): RDD[(K, (V, W))] = self.withScope {	
  join(other, defaultPartitioner(self, other))	
}

如果两个RDD都有分区器，defaultPartitioner取分区数最大的，如果都没有，则会根据并行度取HashPartitioner。

# ShuffleExchangeExec

从宏观上来看，shuffleexchangeExec运算符主要负责两件事：

首先，根据父节点所需的分区方案，准备对子节点的输出行进行分区的宽依赖（ShuffleDependency）。

其次，添加一个ShuffleRowRDD并指定准备好的ShuffleDependency作为该RDD的依赖。

DAGScheduler检测ShuffleRowRDD的ShuffleDependency，并创建一个ShuffleMapStage，包裹上游的RDDs，为shuffle操作产生数据。ShuffleMapStage由executor端ShuffleMapTasks针对输入RDD的每个分区执行。每个ShuffleMapTask反序列化广播给executor端的ShuffleDependency，并调用ShuffleDependency中定义的shuffleWriterProcessor的写入方法，该方法从shuffle管理器中获取shuffle writer，进行shuffle写入，并返回包含信息的MapStauts，以便以后进行shuffle读取。



ShuffleExchangeExec操作符提供的prepareShuffleDependency方法封装了根据预期输出分区定义ShuffleDependency的逻辑。简而言之，<font color=red>prepareShuffleDependency方法所做的是决定输入RDD中的每一行应该被放入哪个分区。换句话说，prepareShuffleDependency方法旨在为输入RDD中的每一条记录创建一个键值对记录，其中键是目标分区的id，值是原始行记录。</font>

执行一个shuffle产生期望的Partitioning的数据,期望的分区由参数outputPartitioning指定

```scala
case class ShuffleExchangeExec(
    override val outputPartitioning: Partitioning,
    child: SparkPlan,
    shuffleOrigin: ShuffleOrigin = ENSURE_REQUIREMENTS)
```

它的doExecute()方法会用shuffleDependency来创建一个ShuffledRowRDD，那么shuffleDependency就是关键：

它将根据' newPartitioning '中定义的分区方案对其子节点的行进行分区。返回的ShuffleDependency的那些分区将是shuffle的输入。

```scala
 
lazy val shuffleDependency : ShuffleDependency[Int, InternalRow, InternalRow] = {
    val dep = ShuffleExchangeExec.prepareShuffleDependency(
      inputRDD,
      child.output,
      outputPartitioning,
      serializer,
      writeMetrics)
    metrics("numPartitions").set(dep.partitioner.numPartitions)
    val executionId = sparkContext.getLocalProperty(SQLExecution.EXECUTION_ID_KEY)
    SQLMetrics.postDriverMetricUpdates(
      sparkContext, executionId, metrics("numPartitions") :: Nil)
    dep
  }


  protected override def doExecute(): RDD[InternalRow] = {
    // Returns the same ShuffleRowRDD if this plan is used by multiple plans.
    if (cachedShuffleRDD == null) {
      //把shuffleDependency作为ShuffledRowRDD的依赖
      cachedShuffleRDD = new ShuffledRowRDD(shuffleDependency, readMetrics)
    }
    cachedShuffleRDD
  }
```

关键就是prepareShuffleDependency这个方法，它返回了一个ShuffleDependency[Int, InternalRow, InternalRow],

基于newPartitioning中定义的分区scheme把子节点的数据进行分区，这些分区将作为shuffle的输入。

## prepareShuffleDependency

prepareShuffleDependency源码大体逻辑分为三步：

1. **根据目标分区描述创建一个对应的分区器，对输入的分区key输出分区ID**
2. **根据目标分区描述创建一个分区key提取函数，这个函数对每一行输出一个分区key;    然后把key传给第一步创建的分区器得到分区ID；最后得到一个带有分区ID的RDD**
3. **对带有分区ID的RDD创建一个ShuffleDependency**



```scala
  def prepareShuffleDependency(
      rdd: RDD[InternalRow],
      outputAttributes: Seq[Attribute],
      newPartitioning: Partitioning,
      serializer: Serializer,
      writeMetrics: Map[String, SQLMetric])
    : ShuffleDependency[Int, InternalRow, InternalRow] = {
    //1. 根据newPartitioning计算分区器
    val part: Partitioner = newPartitioning match {
      case RoundRobinPartitioning(numPartitions) => 
        //计算分区键mod分区id的numPartitions，使行均匀地分布在所有输出分区上。
        new HashPartitioner(numPartitions)
      case HashPartitioning(_, n) =>
      //由于先前用partitionIdExpression生成的分区密钥已经是一个有效的分区ID，因此该分区密钥被返回作为分区ID。
        new Partitioner {
          override def numPartitions: Int = n
          override def getPartition(key: Any): Int = key.asInstanceOf[Int]
        }
      case RangePartitioning(sortingExpressions, numPartitions) =>
      //RangePartitioner对排序键进行采样，以计算定义输出分区的分区边界。
        val rddForSampling = rdd.mapPartitionsInternal { iter =>
          val projection =
            UnsafeProjection.create(sortingExpressions.map(_.child), outputAttributes)
          val mutablePair = new MutablePair[InternalRow, Null]()
          iter.map(row => mutablePair.update(projection(row).copy(), null))
        }
        val orderingAttributes = sortingExpressions.zipWithIndex.map { case (ord, i) =>
          ord.copy(child = BoundReference(i, ord.dataType, ord.nullable))
        }
        implicit val ordering = new LazilyGeneratedOrdering(orderingAttributes)
        new RangePartitioner(
          numPartitions,
          rddForSampling,
          ascending = true,
          samplePointsPerPartitionHint = SQLConf.get.rangeExchangeSampleSizePerPartition)
      case SinglePartition =>
      //返回0作为所有行的分区ID
        new Partitioner {
          override def numPartitions: Int = 1
          override def getPartition(key: Any): Int = 0
        }
      case _ => sys.error(s"Exchange not implemented for $newPartitioning")
      // TODO: Handle BroadcastPartitioning.
    }
    //2. 计算rddWithPartitionIds，即给每行分配一个分区ID
  val rddWithPartitionIds: RDD[Product2[Int, InternalRow]]   
              newRdd.mapPartitionsWithIndexInternal((_, iter) => {
          //根据newPartitioning提取key
          val getPartitionKey = getPartitionKeyExtractor()
          //利用第一步获取的分区器计算分区ID
          iter.map { row => (part.getPartition(getPartitionKey(row)), row.copy()) }
        }, isOrderSensitive = isOrderSensitive)
      
    //3. 创建一个ShuffleDependency, 此时所有行都有分区ID了，所以ShuffleDependency的分区器是虚拟分区器PartitionIdPassthrough
      val dependency =
      new ShuffleDependency[Int, InternalRow, InternalRow](
        rddWithPartitionIds,
        new PartitionIdPassthrough(part.numPartitions),
        serializer,
        shuffleWriterProcessor = createShuffleWriteProcessor(writeMetrics))
```

这里计算rddWithPartitionIds稍复杂，根据目标分区描述，创建一个分区键提取函数，这个key提取函数对每一行输出一个分区key(可能是int值、hash值等)：

```scala
    def getPartitionKeyExtractor(): InternalRow => Any = newPartitioning match {
      //从一个随机的分区ID开始，为每一行增加1作为分区密钥，这使得行在输出分区中均匀分布。
      case RoundRobinPartitioning(numPartitions) =>
        // Distributes elements evenly across output partitions, starting from a random partition.
        var position = new Random(TaskContext.get().partitionId()).nextInt(numPartitions)
        (row: InternalRow) => {
          // The HashPartitioner will handle the `mod` by the number of partitions
          position += 1
          position
        }
      
      case h: HashPartitioning =>
      //用partitionIdExpression(Pmod(new Murmur3Hash(expressions), Literal(numPartitions))生成分区密钥。生成的分区密钥已经是一个有效的分区ID。
        val projection = UnsafeProjection.create(h.partitionIdExpression :: Nil, outputAttributes)
        row => projection(row).getInt(0)
      case RangePartitioning(sortingExpressions, _) =>
      //用排序表达式（SortOrder类型）生成分区键。同样的排序表达式将被用于计算RangePartitioner的分区边界。
        val projection = UnsafeProjection.create(sortingExpressions.map(_.child), outputAttributes)
        row => projection(row)
      case SinglePartition => identity
      //使用行标识作为分区key
      case _ => sys.error(s"Exchange not implemented for $newPartitioning")
    }
```

从行中生成分区键后，再用第一步创建的分区器将分区键作为输入，并返回输出分区ID（0和numPartition-1之间的数字）。



## ShuffleDependency

代表对shuffle stage输出的一个依赖。

**_rdd**  父RDD
***partitioner** *partitioner used to partition the shuffle output**
***serializer** [[*org.apache.spark.serializer.Serializer Serializer*]] *to use. If not set**
\*           *explicitly then the default serializer, as specified by* `*spark.serializer*`
           *config option, will be used.**
***keyOrdering** *key ordering for RDD's shuffles**
***aggregator** *map/reduce-side aggregator for RDD's shuffle**
***mapSideCombine** *whether to perform partial aggregation (also known as map-side combine)**
***shuffleWriterProcessor** *the processor to control the write behavior in ShuffleMapTask*

```scala
class ShuffleDependency[K: ClassTag, V: ClassTag, C: ClassTag](
    @transient private val _rdd: RDD[_ <: Product2[K, V]],
    val partitioner: Partitioner,
    val serializer: Serializer = SparkEnv.get.serializer,
    val keyOrdering: Option[Ordering[K]] = None,
    val aggregator: Option[Aggregator[K, V, C]] = None,
    val mapSideCombine: Boolean = false,
    val shuffleWriterProcessor: ShuffleWriteProcessor = new ShuffleWriteProcessor)
  extends Dependency[Product2[K, V]]
```

