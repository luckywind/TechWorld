# job时间

## Total uptime>total duration time

`Total uptime` is time since Spark application or driver started. `Jobs durations` is the time spent in processing the tasks on `RDDs/DataFrames`.

All the statements which are executed by the driver program contribute to the total uptime but not necessarily to the job duration. For eg:

```scala
val rdd: RDD[String] = ???
(0 to 100).foreach(println)  // contribute in total uptime not in job duration
Thread.sleep(10000)          // contribute in total uptime not in job duration
rdd.count                    // contribute in total uptime as well as in job duration
```

## total job  duration time>total stage duration time

[参考](https://stackoverflow.com/questions/40466265/spark-why-the-spark-job-duration-is-not-equal-to-the-sum-of-each-stage-duratio)

stage重试会浪费时间，这些时间不算在stage 的duration里。

job的执行时间 is a wall clock time

It means that the time is still measured while stages are not actually computed: The cluster may be busy doing anything else between the stages of your job。This may also work in the opposite direction: While multiple stages are executed in parallel, the sum of stage execution time may be greater then job execution time.

## Shuffle spill

[参考](https://community.cloudera.com/t5/Support-Questions/Spark-shuffle-spill-Memory/td-p/186859)

[shuffle vs spill](https://xuechendi.github.io/2019/04/15/Spark-Shuffle-and-Spill-Explained)

Shuffle spill (memory)：spill之前(序列化之前)在内存中的大小

 is the size of the deserialized form of the data in memory at the time when we spill it, whereas shuffle spill (disk) ：spill之后(序列化之后)在磁盘中的大小，比之前要小很多

两者在task执行过程中不断累加

is the size of the serialized form of the data on disk after we spill it. This is why the latter tends to be much smaller than the former. Note that both metrics are aggregated over the entire duration of the task (i.e. within each task you can spill multiple times)

# 故障排查

[Spark报错与日志问题查询姿势指南](Spark报错与日志问题查询姿势指南)

[UI分析](https://.f.mioffice.cn/docs/dock48t38NuarkrNLW3J27F1H2c)

