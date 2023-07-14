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