# sql

```scala
161上
spark.sql("use tpcds100gdv")
spark.conf.set("spark.sql.autoBroadcastJoinThreshold", 2)

val stageMetrics = ch.cern.sparkmeasure.StageMetrics(spark)
stageMetrics.begin()
val taskMetrics = ch.cern.sparkmeasure.TaskMetrics(spark)
taskMetrics.begin()
// 示例代码
spark.sql("""
select ss_store_sk, sum(ss_coupon_amt) amt   from store_sales a  join item b
on a.ss_item_sk =b.i_item_sk
where i_category in ('Sports', 'Books', 'Home')
group by  ss_store_sk -- i_item_sk
limit 100
  """).collect

stageMetrics.end()
taskMetrics.end()
stageMetrics.printReport
taskMetrics.printReport




多线程跑
spark.sql("use tpcds1gv")
spark.conf.set("spark.sql.autoBroadcastJoinThreshold", 2)

val stageMetrics = ch.cern.sparkmeasure.StageMetrics(spark)
stageMetrics.begin()
val taskMetrics = ch.cern.sparkmeasure.TaskMetrics(spark)
taskMetrics.begin()
// 示例代码
spark.sql("""
select i_item_sk, sum(ss_coupon_amt) amt   from store_sales a  join item b
on a.ss_item_sk =b.i_item_sk
where i_category in ('Sports', 'Books', 'Home')
group by i_item_sk
limit 100
  """).collect

stageMetrics.end()
taskMetrics.end()
stageMetrics.printReport
taskMetrics.printReport
多线程测试，两个stage同时提交，抢到资源的第一个stage同时跑多个task， 当一些跑的快的task释放资源后，如果此时这个stage没有更多的task进来， 第二个stage就可以开始跑，也就是两个stage同时跑
```

[sql-metrics](https://spark.apache.org/docs/latest/web-ui.html#sql-metrics)

# WSCG

## duration计算

在迭代到最后一条记录时，更新duration

```scala
 // 初始化    
  val durationMs = longMetric("pipelineTime")

    // Even though rdds is an RDD[InternalRow] it may actually be an RDD[ColumnarBatch] with
    // type erasure hiding that. This allows for the input to a code gen stage to be columnar,
    // but the output must be rows.
    val rdds = child.asInstanceOf[CodegenSupport].inputRDDs()
    assert(rdds.size <= 2, "Up to two input RDDs can be supported")
    if (rdds.length == 1) {
   // 只有一个child   
      rdds.head.mapPartitionsWithIndex { (index, iter) =>
        val (clazz, _) = CodeGenerator.compile(cleanedSource)
        val buffer = clazz.generate(references).asInstanceOf[BufferedRowIterator]
        buffer.init(index, Array(iter))
        new Iterator[InternalRow] {
          override def hasNext: Boolean = {
            val v = buffer.hasNext
            // 在迭代到最后一条记录时，更新duration
            // buffer.durationMs() 就是子节点从开始迭代到结束的耗时
            if (!v) durationMs += buffer.durationMs()
            v
          }
          override def next: InternalRow = buffer.next()
        }
      }
    } else {
      // 有两个child
      rdds.head.zipPartitions(rdds(1)) { (leftIter, rightIter) =>
        Iterator((leftIter, rightIter))
        // a small hack to obtain the correct partition index
      }.mapPartitionsWithIndex { (index, zippedIter) =>
        val (leftIter, rightIter) = zippedIter.next()
        val (clazz, _) = CodeGenerator.compile(cleanedSource)
        val buffer = clazz.generate(references).asInstanceOf[BufferedRowIterator]
        buffer.init(index, Array(leftIter, rightIter))
        new Iterator[InternalRow] {
          override def hasNext: Boolean = {
            val v = buffer.hasNext
             // 在迭代到最后一条记录时，更新duration
            if (!v) durationMs += buffer.durationMs()
            v
          }
          override def next: InternalRow = buffer.next()
        }
      }
    }
```

从源码上看，只有迭代WSCG且迭代到hasNext为false才会计算duration

debug发现在有两个child的情况下，hasNext一直为true，导致duration一直加不了

猜测是我们有limit,spark不用迭代所有数据， 去掉limit 后果然有时间显示了。