

[参考](https://xuechendi.github.io/2019/04/15/Spark-Shuffle-and-Spill-Explained)

spill

1. shuffle时，先写到内存中对应的桶，当内存占用达到一个阈值，这块内存缓冲刷写到磁盘
2. shuffle的背后，有个外部排序操作，对桶做TimSort,因为插入数据需要大的内存块，当内存不足时，就需要把数据spill到磁盘并清空当前内存用于新一轮的插入排序。然后对spill的数据和内存中的数据进行一个merge 排序得到一个排序的结果。

![spilled_Explained](https://gitee.com/luckywind/PigGo/raw/master/image/external_sort_and_spill_explained.jpg)

评估spill的数据量

取决于JVM可用内存，spark设置5M内存阈值，超过则尝试spill。但当到达5M, spark发现有更多内存可用，则阈值会提高。



shuffle数据流

不管是shuffle写还是向外部spill, 当前spark依赖 [DiskBlockObkectWriter](https://github.com/apache/spark/blob/master/core/src/main/scala/org/apache/spark/storage/DiskBlockObjectWriter.scala) 获得kyro序列化缓冲流，内存达到阈值则刷写到磁盘。

当读取文件时，相同节点的读取和节点间的读取方式不同，相同节点的读取会获得 [FileSegmentManagedBuffer](https://github.com/apache/spark/blob/master/common/network-common/src/main/java/org/apache/spark/network/buffer/FileSegmentManagedBuffer.java)，而远程节点的读取则得到[NettyManagedBuffer](https://github.com/apache/spark/blob/master/common/network-common/src/main/java/org/apache/spark/network/buffer/NettyManagedBuffer.java).

对于spill数据的读取，spark首先返回给sorted RDD一个迭代器，读取操作定义在[interator.hasNext()](https://github.com/apache/spark/blob/d4420b455ab81b86c29fc45a3107e45873c72dc2/core/src/main/scala/org/apache/spark/util/collection/ExternalSorter.scala#L577)函数里，因此数据是懒加载。

![spark_shuffle_dataflow](https://gitee.com/luckywind/PigGo/raw/master/image/spark_shuffle_dataflow.jpg)