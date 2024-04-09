 ExecutorLostFailure (executor 158 exited caused by one of the running tasks) Reason: Container killed by YARN for exceeding memory limits. 9.79 GB of 9.50 GB physical memory used. Consider boosting spark.yarn.executor.jvmMemoryOverhead.

[原文](https://www.jianshu.com/p/10e91ace3378)

# Executor内存划分

## Executor可用内存总量

Executor内存由由Heap内存和设定的Off-heap内存组成：

```python
Heap： 由“spark.executor.memory” 指定, 以下称为ExecutorMemory
Off-heap： 由 “spark.yarn.executor.memoryOverhead” 指定， 以下称为MemoryOverhead
```


ExecutorMemory + MemoryOverhead <= MonitorMemory


若应用提交之时，指定的 ExecutorMemory与MemoryOverhead 之和大于 MonitorMemory，则会导致Executor申请失败；若运行过程中，实际使用内存超过上限阈值，Executor进程会被Yarn终止掉（kill）。

