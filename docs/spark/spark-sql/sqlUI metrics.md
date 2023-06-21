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

WSCG天然的把执行过程分为更容易计算duration的pipeline，且比task粒度更细(一个task可以有多个pipeline)

## duration计算BUG

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
   // 只有一个child  , 计算所有分区的duration并累加  
      rdds.head.mapPartitionsWithIndex { (index, iter) =>
        val (clazz, _) = CodeGenerator.compile(cleanedSource)
        val buffer = clazz.generate(references).asInstanceOf[BufferedRowIterator]
        buffer.init(index, Array(iter))
        new Iterator[InternalRow] {
          override def hasNext: Boolean = {
            val v = buffer.hasNext
            // 在迭代到最后一条记录时，更新duration
            // buffer.durationMs() 就是当前分区从开始迭代到结束的耗时
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

## 复现

```scala
spark.sql("use tpcds1gv")
spark.sql("""
select i_item_sk from item
limit 100
  """).collect
```

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230526150703593.png" alt="image-20230526150703593" style="zoom:33%;" />

物理计划

```
== Physical Plan ==
CollectLimit (3)
+- * ColumnarToRow (2)
   +- Scan parquet tpcds1gv.item (1)

(3) CollectLimit
Input [1]: [i_item_sk#0]
Arguments: 100
```

发现最后的CollectLimit算子，触发调用

CollectLimit.executeCollect()->SparkPlan.executeTake(100)->SparkPlan.getByteArrayRdd(100)

```scala
  private def getByteArrayRdd(
      n: Int = -1, takeFromEnd: Boolean = false): RDD[(Long, Array[Byte])] = {
    execute().mapPartitionsInternal { iter =>
      var count = 0
      val buffer = new Array[Byte](4 << 10)  // 4K
      val codec = CompressionCodec.createCodec(SparkEnv.get.conf)
      val bos = new ByteArrayOutputStream()
      val out = new DataOutputStream(codec.compressedOutputStream(bos))
     {
        // `iter.hasNext` may produce one row and buffer it, we should only call it when the
        // limit is not hit.
        // 这里的iter就是WholeStageCodegenExec算子生成的GeneratedIteratorForCodegenStage$1 类，代表一个分区的迭代器
        // 只有在n<0(不限制条数)或者没收集到n条数据时，才会调用iter.hasNext
        while ((n < 0 || count < n) && iter.hasNext) {
          val row = iter.next().asInstanceOf[UnsafeRow]
          out.writeInt(row.getSizeInBytes)
          row.writeToStream(out, buffer)
          count += 1
        }
      }
      out.writeInt(-1)
      out.flush()
      out.close()
      Iterator((count, bos.toByteArray))
    }
  }
```

也就是说CollectLimit只会遍历GeneratedIteratorForCodegenStage n次。

整体调用链：

1. WholeStageCodegenExec.doExecute返回一个迭代器GeneratedIteratorForCodegenStage
2. SparkPlan.getByteArrayRdd迭代GeneratedIteratorForCodegenStage

# metrics是怎么传播的

[spark中metrics是怎么传播的](https://blog.csdn.net/monkeyboy_tech/article/details/128294869?spm=1001.2101.3001.6650.3&utm_medium=distribute.pc_relevant.none-task-blog-2%7Edefault%7EBlogCommendFromBaidu%7ERate-3-128294869-blog-118899820.235%5Ev36%5Epc_relevant_default_base3&depth_1-utm_source=distribute.pc_relevant.none-task-blog-2%7Edefault%7EBlogCommendFromBaidu%7ERate-3-128294869-blog-118899820.235%5Ev36%5Epc_relevant_default_base3&utm_relevant_index=6)

# Metrics的展示

可以看SparkPlanGraph.scala