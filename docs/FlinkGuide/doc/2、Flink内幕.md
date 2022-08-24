1. runtime层接受JobGraph形式的程序，JobGraph是并行数据流以及消费、产生数据流的任意的task。
2. DataStream API和DataSet API通过独立的编译过程生成JogGraphs。DataSet API使用优化器决定程序的最优执行计划，而DataStream API使用stream builder
3. JobGraph可以在不同的部署模式下执行，例如local , remote ,YARN等
4. Flink绑定的一些库和API用于生成DataSet和DataStream API程序。例如，查询逻辑表的Table ,复杂的事件处理以及用于图处理的Gelly

![Apache Flink: Stack](2、Flink内幕.assets/stack.png)

