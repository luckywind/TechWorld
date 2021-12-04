当参与 Join 的一方足够小，可全部置于 Executor 内存中时，可使用 Broadcast 机制将整个 RDD 数据广播到每一个 Executor 中，该 Executor 上运行的所有 Task 皆可直接读取其数据。

与 SortMergeJoin 相比，BroadcastJoin 不需要 Shuffle，减少了 Shuffle 带来的开销，同时也避免了 Shuffle 带来的数据倾斜，从而极大地提升了 Job 执行效率

开启 Adaptive Execution 后，可直接根据 Shuffle Write 数据判断是否适用 BroadcastJoin





todo

1. 技术分享

2. 手机X AIOT分析的贡献，业务价值

a. PID的优化

3. 招聘，内推5个简历



整理一个cfr