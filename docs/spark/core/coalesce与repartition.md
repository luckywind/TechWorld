# coalesce

## 源码

```scala
/**
    * Return a new RDD that is reduced into `numPartitions` partitions.
    * 返回一个新的RDD，该RDD被reduce为“numPartitions”个分区。（发生reduce就会发生洗牌操作）
    *
    * This results in a narrow dependency, e.g. if you go from 1000 partitions
    * to 100 partitions, there will not be a shuffle, instead each of the 100
    * new partitions will claim 10 of the current partitions. If a larger number
    * of partitions is requested, it will stay at the current number of partitions.

    * 窄依赖，例如1000个分区变到100个分区，将不会发生shuffle,仅仅是新的100个分区每个都代表当前的10个分区
    * 如果新分区数更大，则不起作用，仍然保留当前分区数
    *
    * However, if you're doing a drastic coalesce, e.g. to numPartitions = 1,
    * this may result in your computation taking place on fewer nodes than
    * you like (e.g. one node in the case of numPartitions = 1). To avoid this,
    * you can pass shuffle = true. This will add a shuffle step, but means the
    * current upstream partitions will be executed in parallel (per whatever
    * the current partitioning is).
    *
    * 但是，如果您正在进行一个激烈的合并，例如:to numPartitions = 1，这可能会导致您的计算
    * 发生在比您喜欢的节点更少的节点上(例如，numPartitions = 1中的一个节点)。为了避免这个问题，
    * 您可以通过shuffle = true。这将添加一个shuffle步骤，但是意味着当前的上游分区将并行执行
    * (无论当前分区是什么)。
    *
    * @note With shuffle = true, you can actually coalesce to a larger number
    * of partitions. This is useful if you have a small number of partitions,
    * say 100, potentially with a few partitions being abnormally large. Calling
    * coalesce(1000, shuffle = true) will result in 1000 partitions with the
    * data distributed using a hash partitioner. The optional partition coalescer
    * passed in must be serializable.
    *
    * 随着shuffle = true，您实际上可以合并到更多的分区。如果有一小部分分区，比如100，
    * 可能有几个分区异常大，这很有用。调用合并(1000,shuffle = true)将会导致1000个分区，
    * 数据分布使用散列分配程序。通过的可选分区合并器必须是可序列化的。
    *
    * 优化：
    * 当要对 rdd 进行重新分片时，如果目标片区数量小于当前片区数量，那么用 coalesce，不要用 repartition。
    * 因为这里默认shuffle=false是不需要进行洗牌操作的。
    */
  def coalesce(numPartitions: Int, shuffle: Boolean = false,
               partitionCoalescer: Option[PartitionCoalescer] = Option.empty)
              (implicit ord: Ordering[T] = null)
  : RDD[T] = withScope {
    // 分区数必须大于0
    require(numPartitions > 0, s"Number of partitions ($numPartitions) must be positive.")
    // 不使用repartition(shuffle = true)  方法
    if (shuffle) {
      /**这里定义一个函数，从一个随机分区开始，把分区元素依次分配到递增的虚拟分区序号上，
      最后，hashpartitioner会计算虚拟分区序号对新分区数取模得到真正的分区序号，这其实就把数据打散了
        * */
      val distributePartition = (index: Int, items: Iterator[T]) => {
        var position = (new Random(index)).nextInt(numPartitions)
        items.map { t =>
          // 注意，键的哈希代码本身就是关键字。hashpartiator将使用总分区的数量对其进行mod。
          position = position + 1
          (position, t)
        }
      } : Iterator[(Int, T)]

      // include a shuffle step so that our upstream tasks are still distributed
      new CoalescedRDD(
        new ShuffledRDD[Int, T, T](mapPartitionsWithIndex(distributePartition),
          new HashPartitioner(numPartitions)),
        numPartitions,
        partitionCoalescer).values
    } else {
      // 不使用repartition(shuffle = false)  方法,这里不洗牌，没有洗牌的默认操作
      new CoalescedRDD(this, numPartitions, partitionCoalescer)
    }
  }
```



## 分区聚集器

```
PartitionCoalescer
```







## 总结

1. coalesce的默认参数只能减少分区，且不会发生shuffle,只是新的分区对应多个老分区。

   > 因此只想减少分区时不要用repartition，它会导致shuffle发生

2. 如果新分区数过小(例如，1)将导致所有的计算只在某几个节点运行，降低并行度，可能会导致OOM等错误。可以使用shuffle=true来增加一个shuffle步骤。

3. shuffle=true(增加一个shuffle步骤)可以增大分区，可用于缓解数据倾斜



# repartition

## 源码

repartition其实就是shuffle=true的coalesce

```scala
 def repartition(numPartitions: Int)(implicit ord: Ordering[T] = null): RDD[T] = withScope {
    coalesce(numPartitions, shuffle = true)
  }
```



[参考](https://stackoverflow.com/questions/31610971/spark-repartition-vs-coalesce)

