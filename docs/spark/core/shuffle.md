[[Distributed Systems Architecture](https://0x0fff.com/)](https://0x0fff.com/spark-architecture-shuffle/)

[shuffle](https://gousios.gr/courses/bigdata/spark.html)

相同key的数据一定在一个节点上，内存放不下就存到磁盘

![shuffle](https://gitee.com/luckywind/PigGo/raw/master/image/shuffle.png)

# Spark架构:shuffle

相同key的数据在一个节点上，本文遵循MR的命名公约：在shuffle操作中，源头executor上产生数据的task称为mapper，消费数据到目标executor的task称为reducer，两者之间是shuffle。

shuffle通常有两个重要的参数：

1.  ***spark.shuffle.compress\*** –是否对输出进行压缩
2. ***spark.shuffle.spill.compress\*** –是否压缩shuffle过程中spill的中间文件

默认值都是true，都会使用***spark.io.compression.codec\***进行压缩，默认是snappy.

spark中有多种shuffle实现，通过参数***spark.shuffle.manager\*** 指定，可选值有hash、sort和tungsten-sort. 在spark1.2.0之前，sort是默认的shuffle实现。

## hash shuffle

​       spark1.2.0之前默认使用的shuffle算法，存在很多缺点，因为该实现创建的文件太多： 一个mapper task会为每个reducer创建一个不同的文件，当有M个mapper，R个reducer时，就会产生M*R个文件。系统的open files个数以及创建/删除这些文件的速度都会因为文件数太多而出现问题。

​        逻辑非常愚蠢：把reducer的个数作为reduce端的分区数，并创建相应个数的文件，然后迭代数据计算每个记录对应的partition并输出到对应的文件。看起来就像下图：

![spark_hash_shuffle_no_consolidation](https://gitee.com/luckywind/PigGo/raw/master/image/spark_hash_shuffle_no_consolidation-1024x484.png)

有一种优化版的实现，通过参数spark.shuffle.consolidateFiles(默认false)控制。当启用时，mapper输出文件的个数是固定的，例如集群有E个executor，且每个executor有C个core，每个task申请T个CPU(spark.task.cpus),那么集群execution slots的个数是`E*C/T`, shuffle过程中创建的文件个数是`E*C/T*R`

实现原理：创建一个输出文件池，而不是给每个reducer创建一个文件，当map task开始输出数据，它会从文件池中申请R个文件。当执行结束，它会返回这R个文件到文件池。因为每个executor只能并行执行C/T个Task,它只会创建C/T组输出文件，每组是R个文件。当第一组C/T个并行map task结束，下一组map task可以从文件池中重用这些文件组。看起来如下：

![spark_hash_shuffle_with_consolidation](https://gitee.com/luckywind/PigGo/raw/master/image/spark_hash_shuffle_with_consolidation-1024x500.png)

Pros:

1. Fast – no sorting is required at all, no hash table maintained;
2. No memory overhead for sorting the data;
3. No IO overhead – data is written to HDD exactly once and read exactly once.

Cons:

1. When the amount of partitions is big, performance starts to degrade due to big amount of output files
2. Big amount of files written to the filesystem causes IO skew towards random IO, which is in general up to 100x slower than sequential IO

# Sort Shuffle

​             从spark1.2.0开始，默认的shuffle算法改成sort了。这个shuffle逻辑类似MR，只输出一个文件，该文件按照reducer_id有序且可索引。这样在fread之前做一个fseek就很方便找到某个reducer相关的数据块。当然，当reducer个数很小时，hash到多个文件是比sorting到一个文件快的，因此，sort shuffle有一个回退计划：当reducer个数小于spark.shuffle.sort.bypassMergeThreshold(默认200)个时，启用回退计划，hash到多个文件然后再join到一个文件。该实现的源码参考BypassMergeSortShuffleWriter类。

​          有意思的是这个实现在map端排序，但当需要数据顺序时，在reduce端不对排序结果进行merge，而只是re-sort(reduce端re-sort使用TimSort算法实现，它对预排序的数据非常高效)。

​          如果没有足够内存放下map的输出，则需要把中间结果spill到磁盘。spark.shuffle.spill可以控制是否开启，如果不开启当内存放不下时会OOM。

可用于排序map输出的内存大小=`spark.shuffle.memoryFraction * spark.shuffle.safetyFraction`，

默认是`JVM堆*0.2*0.8`即`JVM堆*0.16`。 注意，如果在一个Executor上运行多个线程(设置`spark.executor.cores / spark.task.cpus`>1),则每个map task可用JVM堆大小要做相应除法。

​		spill到磁盘的文件只有在reducer请求时才会时时merge。

![spark_sort_shuffle](https://gitee.com/luckywind/PigGo/raw/master/image/spark_sort_shuffle-1024x459.png)

# Unsafe Shuffle or Tungsten Sort

可以通过`spark.shuffle.manager= tungsten-sort` 开启, 这是Tungsten[项目的一部分](https://issues.apache.org/jira/browse/SPARK-7081)。优化可以总结如下：

1. 无需反序列化，直接操作序列化后的二进制数据。它使用unsafe内存拷贝函数直接拷贝数据，操作字节数组非常高效。
2. 使用特殊的cache-efficient排序器[ShuffleExternalSorter](https://github.com/apache/spark/blob/master/core/src/main/java/org/apache/spark/shuffle/sort/ShuffleExternalSorter.java) ，直接排序压缩数组的指针和分区id
3. 因为数据没有反序列化，所以spill过程非常快
4. 当压缩编码支持序列化流的连接，则自动应用spill-merge优化

但这种方式无法利用mapper端的预排序优势，且貌似不稳定。图示如下：

![spark_tungsten_sort_shuffle](https://gitee.com/luckywind/PigGo/raw/master/image/spark_tungsten_sort_shuffle-1024x457.png)

