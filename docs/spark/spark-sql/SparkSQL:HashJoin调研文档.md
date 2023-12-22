# Hashjoin调研文档

## 概况

#### 说明

（1）本文以 InnerJoin 为主，其他 Join 方式，相差不大，仅做部分差异性说明。
（2）本文Join key为 Long 类型，定长类型，可变长类型如 String 是否有差异，后续补充。
（3）Spark的核心逻辑中，因只作为基础参考和加深理解，开发中参考和借鉴较少，因此只介绍InnerJoin的方式。
（4）本文暂时只考虑 InnerJoin 和 LeftJoin 的实现，其他Join方式使用较少，后续按需补充。

#### 执行案例

案例一（10万小数据量）

```
val df = spark.read.parquet("file:/home/luotianran/tmp/parquet_test_10ws")
df.createTempView("table")
sc.setLogLevel("INFO")
val r = spark.sql("select t1.id from table t1 join table t2 on t1.id = t2.id")
r.write.mode("overwrite").parquet("file:/home/luotianran/tmp/parquet_test14/")
```

案例二（1000万较大数据量）

```
val df = spark.read.parquet("file:/home/luotianran/tmp/parquet_test_1000ws")
df.createTempView("table")
sc.setLogLevel("INFO")
val r = spark.sql("select t1.id from table t1 join table t2 on t1.id = t2.id")
r.write.mode("overwrite").parquet("file:/home/luotianran/tmp/parquet_test14/")
```

说明：
这里只用了同一张表进行 Join 操作，两个案例的逻辑一致，区别只在于数据量不同。

#### 执行计划树

**案例一**
命中BroadcastHashJoin策略 

```scala
DpuProject [id#0L]
// 行转列
+- DpuRowToColumnar [id#0L, id#6L]
   // Broadcast HashJoin算子
   +- BroadcastHashJoin [id#0L], [id#6L], Inner, BuildRight, false
      :- DpuColumnarToRow [id#0L]
      :  +- DpuFilter dpuisnotnull(id#0L), true
      :     +- DpuFileScan parquet [id#0L] Batched: true, DataFilters: [isnotnull(id#0L)], Format: Parquet, Location: InMemoryFileIndex[file:/home/luotianran/tmp/parquet_test_10ws], PartitionFilters: [], PushedFilters: [IsNotNull(id)], ReadSchema: struct<id:bigint>
      +- BroadcastQueryStage 0
         // Broadcast Shuffle算子，DPU版本目前暂不支持
         +- BroadcastExchange HashedRelationBroadcastMode(List(input[0, bigint, false]),false), [id=#80]
            +- DpuColumnarToRow [id#6L]
               +- DpuFilter dpuisnotnull(id#6L), true
                  +- DpuFileScan parquet [id#6L] Batched: true, DataFilters: [isnotnull(id#6L)], Format: Parquet, Location: InMemoryFileIndex[file:/home/luotianran/tmp/parquet_test_10ws], PartitionFilters: [], PushedFilters: [IsNotNull(id)], ReadSchema: struct<id:bigint>
```

**案例二**
命中SortMergeJoin策略（默认）

```scala
DpuProject [id#0L]
// 行转列
+- DpuRowToColumnar [id#0L, id#6L]
   // SortMerge HashJoin算子
   +- SortMergeJoin [id#0L], [id#6L], Inner
       // Sort算子，DPU版本暂不支持
      :- Sort [id#0L ASC NULLS FIRST], false, 0
      :  +- DpuColumnarToRow [id#0L]
            // Shuffle Reader 算子
      :     +- DpuAQEShuffleReader coalesced
      :        +- ShuffleQueryStage 0
                  // Dpu版本的Hash Shuffle算子，目前部分支持
      :           +- DpuColumnarExchange dpuhashpartitioning(id#0L, 200, false), false, [id=#479]
      :              +- DpuFilter dpuisnotnull(id#0L), true
      :                 +- DpuFileScan parquet [id#0L] Batched: true, DataFilters: [isnotnull(id#0L)], Format: Parquet, Location: InMemoryFileIndex[file:/home/luotianran/tmp/parquet_test_1000ws], PartitionFilters: [], PushedFilters: [IsNotNull(id)], ReadSchema: struct<id:bigint>
      +- Sort [id#6L ASC NULLS FIRST], false, 0
         +- DpuColumnarToRow [id#6L]
            // Shuffle Reader 算子
            +- DpuAQEShuffleReader coalesced
               +- ShuffleQueryStage 2
                  // 复用的Shuffle算子
                  +- ReusedExchange [id#6L], DpuColumnarExchange dpuhashpartitioning(id#0L, 200, false), false, [id=#479]
```

命中ShuffledHashJoin策略（修改参数）

```scala
DpuProject [id#0L]
+- DpuRowToColumnar [id#0L, id#6L]
   // ShuffledHashJoin 算子
   +- ShuffledHashJoin [id#0L], [id#6L], Inner, BuildRight
      :- DpuColumnarToRow [id#0L]
         // Shuffle Reader 算子
      :  +- DpuAQEShuffleReader coalesced
      :     +- ShuffleQueryStage 0
      :        +- DpuColumnarExchange dpuhashpartitioning(id#0L, 200, false), false, [id=#439]
      :           +- DpuFilter dpuisnotnull(id#0L), true
      :              +- DpuFileScan parquet [id#0L] Batched: true, DataFilters: [isnotnull(id#0L)], Format: Parquet, Location: InMemoryFileIndex[file:/home/luotianran/tmp/parquet_test_1000ws], PartitionFilters: [], PushedFilters: [IsNotNull(id)], ReadSchema: struct<id:bigint>
      +- DpuColumnarToRow [id#6L]
         // Shuffle Reader 算子
         +- DpuAQEShuffleReader coalesced
            +- ShuffleQueryStage 2
               // 复用的Shuffle算子
               +- ReusedExchange [id#6L], DpuColumnarExchange dpuhashpartitioning(id#0L, 200, false), false, [id=#439]
```

## JOIN选择策略

具体逻辑在 JoinSelection#apply 方法中，具体代码如下：

```scala
//广播HashJoin
createBroadcastHashJoin(true)
   //SortMerge HashJoin
    .orElse { if (hintToSortMergeJoin(hint)) createSortMergeJoin() else None }
   //Shuffle HashJoin
    .orElse(createShuffleHashJoin(true))
   //笛卡尔积的HashJoin
    .orElse { if (hintToShuffleReplicateNL(hint)) createCartesianProduct() else None }
    //未显示指定策略的默认规则
   .getOrElse(createJoinWithoutHint())
```

如主动指定策略，则使用显示指定的策略，如无显示指定，则使用默认策略，如下：

```scala
def createJoinWithoutHint() = {
   //广播HashJoin
    createBroadcastHashJoin(false)
    //Shuffle HashJoin
    .orElse(createShuffleHashJoin(false))
    //SortMerge HashJoin
    .orElse(createSortMergeJoin())
    //笛卡尔积的HashJoin
    .orElse(createCartesianProduct())
    //暂时先不管
    .getOrElse {
        // This join could be very slow or OOM
        val buildSide = getSmallerSide(left, right)
        Seq(joins.BroadcastNestedLoopJoinExec(
        planLater(left), planLater(right), buildSide, joinType, nonEquiCond))
    }
}
```

### 显示指定策略

按照策略的顺序

#### **BroadcastHashJoin**

```scala
//检查左表是否显示指定BroadcastHashJoin策略，右表也会以此相同的规则检查一次。
def hintToBroadcastLeft(hint: JoinHint): Boolean = {
  hint.leftHint.exists(_.strategy.contains(BROADCAST))
}
```

```scala
//具体的策略别名，命中其一即可
override def hintAliases: Set[String] = *Set*(
  "BROADCAST",
  "BROADCASTJOIN",
  "MAPJOIN")
```

当左右两表都命中时，取较小者

```
//如果左右表都符合BroadcastHashJoin策略，则按照数据量选取两表中的较小者
def getSmallerSide(left: LogicalPlan, right: LogicalPlan): BuildSide = {
  if (right.stats.sizeInBytes <= left.stats.sizeInBytes) BuildRight else BuildLeft
}
```

#### **SortMergeJoin**

```scala
//检查左右两表是否主动开启SortMergeJoin。
def hintToSortMergeJoin(hint: JoinHint): Boolean = {
   hint.leftHint.exists(_.strategy.contains(SHUFFLE_MERGE)) ||
    hint.rightHint.exists(_.strategy.contains(SHUFFLE_MERGE))
}
```

```scala
//具体的策略别名，命中其一即可
override def hintAliases: Set[String] = *Set*(
  "SHUFFLE_MERGE",
  "MERGE",
  "MERGEJOIN")
```

#### ShuffledHashJoinExec

```scala
//检查左表是否显示指定ShuffledHashJoinExec策略，右表也会以此相同的规则检查一次。
def hintToShuffleHashJoinLeft(hint: JoinHint): Boolean = {
  hint.leftHint.exists(_.strategy.contains(SHUFFLE_HASH))
}
```

```scala
//具体策略名
override def hintAliases: Set[String] = *Set*(
  "SHUFFLE_HASH")
```

### 默认策略

默认策略和显示指定不同的在于，使用 Spark 中默认的匹配规则进行逐一匹配。

#### **BroadcastHashJoin**

```scala
//（1）检查左表的数据大小，是否超过10MB（默认值，可调整）；
//（2）是否禁用 Broadcast；
// 右表也会按照这个规则检查一次
canBroadcastBySize(left, conf) && !hintToNotBroadcastLeft(hint)
```

检查表的数据大小，是否超过 autoBroadcastJoinThreshold，该值取自 spark.sql.adaptive.autoBroadcastJoinThreshold

```scala
def canBroadcastBySize(plan: LogicalPlan, conf: SQLConf): Boolean = {
  val autoBroadcastJoinThreshold = if (plan.stats.isRuntime) {
    conf.getConf(SQLConf.ADAPTIVE_AUTO_BROADCASTJOIN_THRESHOLD)
    .getOrElse(conf.autoBroadcastJoinThreshold)
  } else {
    conf.autoBroadcastJoinThreshold
  }
  plan.stats.sizeInBytes >= 0 && plan.stats.sizeInBytes <= autoBroadcastJoinThreshold
}
```

如两表都命中广播规则，则取较小者，同显示策略

#### ShuffledHashJoinExec

```scala
//这里有3个规则
//（1）显示指定PREFER_SHUFFLE_HASH 
// 或者 （2）不优先使用SortMergeJoin（默认优先使用SortMergeJoin，控制参数spark.sql.join.preferSortMergeJoin）
// 且 输入表大小大于默认值 且 左表的3倍小于等于右表的
// 或者（3）是否是测试模式，默认false 且 是否强制使用 HashJoin，默认flase
hintToPreferShuffleHashJoinLeft(hint) || 
  (!conf.preferSortMergeJoin && canBuildLocalHashMapBySize(left, conf) &&
    muchSmaller(left, right)) ||
  forceApplyShuffledHashJoin(conf)
```

```scala
//输入表大小大于默认值
private def canBuildLocalHashMapBySize(plan: LogicalPlan, conf: SQLConf): Boolean = {
  plan.stats.sizeInBytes < conf.autoBroadcastJoinThreshold * conf.numShufflePartitions
}
```

```scala
//左表的3倍小于等于右表
private def muchSmaller(a: LogicalPlan, b: LogicalPlan): Boolean = {
  a.stats.sizeInBytes * 3 <= b.stats.sizeInBytes
}
```

```scala
//是否强制使用 HashJoin，默认flase
private def forceApplyShuffledHashJoin(conf: SQLConf): Boolean = {
  Utils.isTesting &&
    conf.getConfString("spark.sql.join.forceApplyShuffledHashJoin", "false") == "true"
}
```

#### **SortMergeJoin**

```scala
//SortMergeJoin的规则比较简单，检查join key是否是可排序的。
def isOrderable(exprs: Seq[Expression]): Boolean = exprs.forall(e => *isOrderable*(e.dataType))
```

总结：
从上面的策略看，在没有显示指定策略的情况下，默认情况下，策略使用优先级为：BroadcastHashJoin →  ShuffledHashJoinExec → SortMergeJoin。

## 核心逻辑

### Spark

#### **BroadcastHashJoin**

以下为CodeGen生成并经过整理的的逻辑

```scala
//处理非Broadcast表的数据
protected void processNext() throws java.io.IOException {
    while ( inputadapter_input_0.hasNext()) {
        InternalRow inputadapter_row_0 = (InternalRow) inputadapter_input_0.next();
        long inputadapter_value_0 = inputadapter_row_0.getLong(0);
       //核心处理逻辑
        bhj_doConsume_0(inputadapter_row_0, inputadapter_value_0);
        if (shouldStop()) return;
    }
}
```

逻辑展开如下：

```scala
private void bhj_doConsume_0(InternalRow inputadapter_row_0, long bhj_expr_0_0) throws java.io.IOException {
   // 核心逻辑，从Broadcast表构建的类HashMap数据结构中匹配Join Key
    UnsafeRow bhj_buildRow_0 = false ? null: (UnsafeRow)bhj_relation_0.getValue(bhj_expr_0_0);
    if (bhj_buildRow_0 != null) {
        {
            ((org.apache.spark.sql.execution.metric.SQLMetric) references[1] /* numOutputRows */).add(1);
            long bhj_value_1 = bhj_buildRow_0.getLong(0);
            bhj_mutableStateArray_0[0].reset();
            bhj_mutableStateArray_0[0].write(0, bhj_expr_0_0);
            bhj_mutableStateArray_0[0].write(1, bhj_value_1);
           //如果匹配到 Join Key，缓存该 Row 数据
            append((bhj_mutableStateArray_0[0].getRow()));
        }
    }
}
```

#### ShuffledHashJoinExec

以下为CodeGen生成并经过整理的的逻辑

```scala
protected void processNext() throws java.io.IOException {
    while ( inputadapter_input_0.hasNext()) {
       //stream表row
        InternalRow inputadapter_row_0 = (InternalRow) inputadapter_input_0.next();
       //stream表join key值
        long inputadapter_value_0 = inputadapter_row_0.getLong(0);
       //核心处理逻辑
        shj_doConsume_0(inputadapter_row_0, inputadapter_value_0);
        if (shouldStop()) return;
    }
}
```

展开 shj_doConsume_0 方法，如下：

```scala
private void shj_doConsume_0(InternalRow inputadapter_row_0, long shj_expr_0_0) throws java.io.IOException {
   //从shj_relation_0（数据结构为HashRelation）中进行match，HashRelation数据结构类似一个HashMap
    scala.collection.Iterator shj_matches_0 = false ?
            null : (scala.collection.Iterator)shj_relation_0.get(shj_expr_0_0);
    if (shj_matches_0 != null) {
       //遍历match到的row Iterator
        while (shj_matches_0.hasNext()) {
            UnsafeRow shj_buildRow_0 = (UnsafeRow) shj_matches_0.next();
            {
                ((org.apache.spark.sql.execution.metric.SQLMetric) references[1] /* numOutputRows */).add(1);
               // join表的key值
                long shj_value_1 = shj_buildRow_0.getLong(0);
                shj_mutableStateArray_0[0].reset();
               //将两表数据分别取出并写入到Buffer中
                shj_mutableStateArray_0[0].write(0, shj_expr_0_0);
                shj_mutableStateArray_0[0].write(1, shj_value_1);
                append((shj_mutableStateArray_0[0].getRow()).copy());
            }
        }
    }
}
```

#### **SortMergeJoin**

以下为CodeGen生成并经过整理的的逻辑

```scala
protected void processNext() throws java.io.IOException {
   //匹配stream表和Join表的指针
    while (smj_findNextJoinRows_0(smj_streamedInput_0, smj_bufferedInput_0)) {
        long smj_value_4 = -1L;
       //smj_streamedRow_0为主表的当前row，smj_value_4为需要取出来的值
        smj_value_4 = smj_streamedRow_0.getLong(0);
       //smj_iterator_0为Join表数据的Iterator
        scala.collection.Iterator<UnsafeRow> smj_iterator_0 = smj_matches_0.generateIterator();
       //向后遍历Join表
       while (smj_iterator_0.hasNext()) {
            InternalRow smj_bufferedRow_1 = (InternalRow) smj_iterator_0.next();
            ((org.apache.spark.sql.execution.metric.SQLMetric) references[0] /* numOutputRows */).add(1);
            long smj_value_5 = smj_bufferedRow_1.getLong(0);
            smj_mutableStateArray_0[0].reset();
            smj_mutableStateArray_0[0].write(0, smj_value_4);
            smj_mutableStateArray_0[0].write(1, smj_value_5);
           //将两表数据分别取出并写入到Buffer中
            append((smj_mutableStateArray_0[0].getRow()).copy());
        }
        if (shouldStop()) return;
    }
    ((org.apache.spark.sql.execution.joins.SortMergeJoinExec) references[1] /* plan */).cleanupResources();
}
```

展开smj_findNextJoinRows_0逻辑如下：

```scala
//该方法的核心逻辑就是采用双指针的方式，遍历两个有序链表，直到匹配上相同的key，返回true
private boolean smj_findNextJoinRows_0(
        scala.collection.Iterator streamedIter,
        scala.collection.Iterator bufferedIter) {
    smj_streamedRow_0 = null;
    int comp = 0;
    while (smj_streamedRow_0 == null) {
        if (!streamedIter.hasNext()) return false;
        smj_streamedRow_0 = (InternalRow) streamedIter.next();
       //取出streamed表的key
        long smj_value_0 = smj_streamedRow_0.getLong(0);
        if (false) {
            smj_streamedRow_0 = null;
            continue;
        }
        if (!smj_matches_0.isEmpty()) {
            comp = 0;
            if (comp == 0) {
                comp = (smj_value_0 > smj_value_3 ? 1 : smj_value_0 < smj_value_3 ? -1 : 0);
            }
            if (comp == 0) {
                return true;
            }
            smj_matches_0.clear();
        }
        do {
            if (smj_bufferedRow_0 == null) {
                if (!bufferedIter.hasNext()) {
                    smj_value_3 = smj_value_0;
                    return !smj_matches_0.isEmpty();
                }
                smj_bufferedRow_0 = (InternalRow) bufferedIter.next();
                long smj_value_1 = smj_bufferedRow_0.getLong(0);
                if (false) {
                    smj_bufferedRow_0 = null;
                    continue;
                }
                smj_value_2 = smj_value_1;
            }
            comp = 0;
            if (comp == 0) {
                comp = (smj_value_0 > smj_value_2 ? 1 : smj_value_0 < smj_value_2 ? -1 : 0);
            }
            if (comp > 0) {
                smj_bufferedRow_0 = null;
            } else if (comp < 0) {
                if (!smj_matches_0.isEmpty()) {
                   //如果Join表的key与streamed表的key相等，则返回true，此处记录下了两表的指针
                    smj_value_3 = smj_value_0;
                    return true;
                } else {
                    smj_streamedRow_0 = null;
                }
            } else {
                smj_matches_0.add((UnsafeRow) smj_bufferedRow_0);
                smj_bufferedRow_0 = null;
            }
        } while (smj_streamedRow_0 != null);
    }
    return false; // unreachable
}
```

### Rapids

#### 说明

（1）Rapids 只支持 BroadcastHashJoinExec、SortMergeJoinExec 和 ShuffledHashJoinExec 三种算子，但 SortMergeJoinExec 和 ShuffledHashJoinExec 是都是使用 ShuffledHashJoin 的方式实现，所以实际上， Rapids 只有两种 HashJoin 实现，即 BroadcastHashJoin 和 ShuffledHashJoin。
（2）BroadcastHashJoin 和 ShuffledHashJoin 算子的实现方式非常相似，不同的地方主要在于 Exchange 算子的不同， BroadcastHashJoinExec 是基于 Broadcast Exchange，而 ShuffledHashJoin 是基于 Hashpartitioning Exchange。

**GpuBroadcastHashJoin执行计划**

```
GpuBroadcastHashJoin [id#0L], [id#51L], Inner, GpuBuildRight
:- GpuCoalesceBatches targetsize(2147483647)
:  +- GpuFilter gpuisnotnull(id#0L), true
:     +- GpuFileGpuScan parquet [id#0L] Batched: true, DataFilters: [isnotnull(id#0L)], Format: Parquet, Location: InMemoryFileIndex[file:/root/luotianran/data/student_100w], PartitionFilters: [], PushedFilters: [IsNotNull(id)], ReadSchema: struct<id:bigint>
+- BroadcastQueryStage 0
      //Broadcast Exchange
   +- GpuBroadcastExchange HashedRelationBroadcastMode(List(input[0, bigint, false]),false), [id=#1230]
      +- GpuCoalesceBatches targetsize(2147483647)
         +- GpuFilter gpuisnotnull(id#51L), true
            +- GpuFileGpuScan parquet [id#51L] Batched: true, DataFilters: [isnotnull(id#51L)], Format: Parquet, Location: InMemoryFileIndex[file:/root/luotianran/data/student_100w], PartitionFilters: [], PushedFilters: [IsNotNull(id)], ReadSchema: struct<id:bigint>
```

**GpuShuffledHashJoin执行计划**

```
GpuProject [cast(id#0L as string) AS id#49, cast(age#2L as string) AS age#50, name#41]
+- GpuShuffledHashJoin [id#0L], [id#40L], Inner, GpuBuildRight, false
   :- GpuShuffleCoalesce 2147483647
   :  +- GpuCustomShuffleReader coalesced
   :     +- ShuffleQueryStage 0
               //Hashpartitioning Exchange
   :        +- GpuColumnarExchange gpuhashpartitioning(id#0L, 200), ENSURE_REQUIREMENTS, [id=#834]
   :           +- GpuCoalesceBatches targetsize(2147483647)
   :              +- GpuFilter gpuisnotnull(id#0L), true
   :                 +- GpuFileGpuScan parquet [id#0L,age#2L] Batched: true, DataFilters: [isnotnull(id#0L)], Format: Parquet, Location: InMemoryFileIndex[file:/root/luotianran/data/student_500w], PartitionFilters: [], PushedFilters: [IsNotNull(id)], ReadSchema: struct<id:bigint,age:bigint>
   +- GpuCoalesceBatches RequireSingleBatch
      +- GpuShuffleCoalesce 2147483647
         +- GpuCustomShuffleReader coalesced
            +- ShuffleQueryStage 1
                  //Hashpartitioning Exchange
               +- GpuColumnarExchange gpuhashpartitioning(id#40L, 200), ENSURE_REQUIREMENTS, [id=#867]
                  +- GpuCoalesceBatches targetsize(2147483647)
                     +- GpuFilter gpuisnotnull(id#40L), true
                        +- GpuFileGpuScan parquet [id#40L,name#41] Batched: true, DataFilters: [isnotnull(id#40L)], Format: Parquet, Location: InMemoryFileIndex[file:/root/luotianran/data/student_500w], PartitionFilters: [], PushedFilters: [IsNotNull(id)], ReadSchema: struct<id:bigint,name:string>
```

#### **BroadcastHashJoin**

GpuBroadcastHashJoinExec 内部的 doExecuteColumnar() 方法的核心逻辑为

```
//builtBatch 为Join表的全量Batch，stIt为 Stream 表的BatchIterator
doJoin(builtBatch, stIt, targetSize, spillCallback,
  numOutputRows, joinOutputRows, numOutputBatches, opTime, joinTime)
```

进入doJoin，核心逻辑为 HashJoinIterator

```
val joinIterator =
    new HashJoinIterator(spillableBuiltBatch, boundBuildKeys, lazyStream, boundStreamKeys,
    streamedPlan.output, realTarget, joinType, buildSide, compareNullsEqual, spillCallback,
    opTime, joinTime)
```

进一步分析 HashJoinIterator 的逻辑，其父类 AbstractGpuJoinIterator 包含了核心的 hasNext() 和 next() 方法。

```scala
override def hasNext: Boolean = {
    if (closed) {
      return false
    }
    val startTime = System.nanoTime()
    var totalHasNextTime = 0L
    var mayContinue = true
    while (nextCb.isEmpty && mayContinue) {
      if (gathererStore.exists(!_.isDone)) {
        nextCb = nextCbFromGatherer()
      } else {
        // The only part we want to skip in the timing is hasNextStreamBatch
        val (hasNextValue, hasNextTime) = timedHasNextStreamBatch
        totalHasNextTime += hasNextTime
        if (hasNextValue) {
          // Need to refill the gatherer
          gathererStore.foreach(_.close())
          gathererStore = None
          //核心方法，初始化Gatherer
          gathererStore = setupNextGatherer()
          //核心方法，从构建好的Gatherer里取出已经Joined Batch
          nextCb = nextCbFromGatherer()
        } else {
          mayContinue = false
        }
      }
    }
    if (nextCb.isEmpty) {
      // Nothing is left to return so close ASAP.
      close()
    }
    opTime += (System.nanoTime() - startTime) - totalHasNextTime
    nextCb.isDefined
  }
```

如上，setupNextGatherer() 和 setupNextGatherer() 为Join 的核心方法，里面包含了大量的处理逻辑，接下来会进一步展开。


```
  override def next(): ColumnarBatch = {
    if (!hasNext) {
      throw new NoSuchElementException()
    }
    val ret = nextCb.get
    nextCb = None
    ret
  }
```

next() 方法并不复杂，跳过。
经过多层的跳转，进入如下方法：

```scala
  private def joinGathererLeftRight(
      leftKeys: Table,
      leftData: LazySpillableColumnarBatch,
      rightKeys: Table,
      rightData: LazySpillableColumnarBatch): Option[JoinGatherer] = {
    withResource(new NvtxWithMetrics("hash join gather map", NvtxColor.ORANGE, joinTime)) { _ =>
      val maps = joinType match {
        case LeftOuter => leftKeys.leftJoinGatherMaps(rightKeys, compareNullsEqual)
        case RightOuter =>
          // Reverse the output of the join, because we expect the right gather map to
          // always be on the right
          rightKeys.leftJoinGatherMaps(leftKeys, compareNullsEqual).reverse
        case _: InnerLike => leftKeys.innerJoinGatherMaps(rightKeys, compareNullsEqual)
        case LeftSemi => Array(leftKeys.leftSemiJoinGatherMap(rightKeys, compareNullsEqual))
        case LeftAnti => Array(leftKeys.leftAntiJoinGatherMap(rightKeys, compareNullsEqual))
        case FullOuter => leftKeys.fullJoinGatherMaps(rightKeys, compareNullsEqual)
        case _ =>
          throw new NotImplementedError(s"Joint Type ${joinType.getClass} is not currently" +
              s" supported")
      }
      //该方式为 Join Gatherer 的构建
      makeGatherer(maps, leftData, rightData, joinType)
    }
  }
```

进入 InnerLike case，展开 innerJoinGatherMaps 方法。

```scala
  public GatherMap[] innerJoinGatherMaps(Table rightKeys, boolean compareNullsEqual) {
    if (getNumberOfColumns() != rightKeys.getNumberOfColumns()) {
      throw new IllegalArgumentException("column count mismatch, this: " + getNumberOfColumns() +
          "rightKeys: " + rightKeys.getNumberOfColumns());
    }
    long[] gatherMapData =
        innerJoinGatherMaps(getNativeView(), rightKeys.getNativeView(), compareNullsEqual);
    return buildJoinGatherMaps(gatherMapData);
  }
```

```
//该方法会将左右表的key进行Join操作，返回包含左右表Joined key的一个列Handle的数组
private static native long[] innerJoinGatherMaps(long leftKeys, long rightKeys,
                                                 boolean compareNullsEqual) throws CudfException;
```

LeftJoin的则调用leftJoinGatherMaps，其他逻辑同InnerJoin。

```
private static native long[] leftJoinGatherMaps(long leftKeys, long rightKeys,
                                                boolean compareNullsEqual) throws CudfException;
```

**说明：**
innerJoinGatherMaps 和 leftJoinGatherMaps 方法的返回结果是不同的，前者取交集，后者取并集。

buildJoinGatherMaps方法展开如下：

```scala
//该方法主要是对左右表的 Joined key 列进行封装，左右表的 bufferSize 是相同的
 private GatherMap[] buildJoinGatherMaps(long[] gatherMapData) {
    //Joined key的size
    long bufferSize = gatherMapData[0];
    //Joined key的地址
    long leftAddr = gatherMapData[1];
    //left key的Handle
    long leftHandle = gatherMapData[2];
    //Joined key的地址
    long rightAddr = gatherMapData[3];
    //right key的Handle
    long rightHandle = gatherMapData[4];
    GatherMap[] maps = new GatherMap[2];
    //构建左GatherMap
    maps[0] = new GatherMap(DeviceMemoryBuffer.fromRmm(leftAddr, bufferSize, leftHandle));
    //构建右GatherMap
    maps[1] = new GatherMap(DeviceMemoryBuffer.fromRmm(rightAddr, bufferSize, rightHandle));
    return maps;
 }
```

展开 makeGatherer 方法

```
*JoinGatherer*(lazyLeftMap, leftData, lazyRightMap, rightData,
  leftOutOfBoundsPolicy, rightOutOfBoundsPolicy)
```

该方法的核心逻辑为 JoinGatherer ** 的构造，继续展开如下：

```scala
def apply(leftMap: LazySpillableGatherMap,
    leftData: LazySpillableColumnarBatch,
    rightMap: LazySpillableGatherMap,
    rightData: LazySpillableColumnarBatch,
    outOfBoundsPolicyLeft: OutOfBoundsPolicy,
    outOfBoundsPolicyRight: OutOfBoundsPolicy): JoinGatherer = {
    //构造left JoinGatherer
    val left = JoinGatherer(leftMap, leftData, outOfBoundsPolicyLeft)
    //构造right JoinGatherer
    val right = JoinGatherer(rightMap, rightData, outOfBoundsPolicyRight)
    //构造 MultiJoinGather
    MultiJoinGather(left, right)
}
```

setupNextGatherer() 核心逻辑完毕，继续分析 nextCbFromGatherer() 逻辑。

```scala
  private def nextCbFromGatherer(): Option[ColumnarBatch] = {
    withResource(new NvtxWithMetrics(gatherNvtxName, NvtxColor.DARK_GREEN, joinTime)) { _ =>
      val ret = gathererStore.map { gather =>
        //获取Joined行数，该值默认取自left的totalRows
        val nextRows = JoinGatherer.getRowsInNextBatch(gather, targetSize)
        //核心方法，进入
        gather.gatherNext(nextRows)
      }
      if (gathererStore.exists(_.isDone)) {
        gathererStore.foreach(_.close())
        gathererStore = None
      }

      if (ret.isDefined) {
        // We are about to return something. We got everything we need from it so now let it spill
        // if there is more to be gathered later on.
        gathererStore.foreach(_.allowSpilling())
      }
      ret
    }
  }
```

继续展开 gatherNext 方法

```scala
override def gatherNext(n: Int): ColumnarBatch = {
    //调用left的 gatherNext 方法，返回 left 的 Joined batch
    withResource(left.gatherNext(n)) { leftGathered =>
        ////调用right的 gatherNext 方法，返回 right 的 Joined batch
        withResource(right.gatherNext(n)) { rightGathered =>
        //将left batch 和 right batch 的列合并
        val vectors = Seq(leftGathered, rightGathered).flatMap { batch =>
            (0 until batch.numCols()).map { i =>
            val col = batch.column(i)
            col.asInstanceOf[GpuColumnVector].incRefCount()
            col
            }
        }.toArray
        new ColumnarBatch(vectors, n)
        }
    }
}
```

继续展开 left.gatherNext(n) 或者 right.gatherNext(n)方法，如下：

```scala
  override def gatherNext(n: Int): ColumnarBatch = {
    val start = gatheredUpTo
    assert((start + n) <= totalRows)
    //1.gatherMap.toColumnView(start, n)方法可以获得joined key对应的ColumnView
    //2.此处 Inner join 和 Left join 有所差异，n（行数）不同，Inner取交集，Left取并集
    //3.left和right的 n 值相同，都自left的（*totalRows* - *gatheredUpTo*），gatheredUpTo默认为0
    val ret = withResource(gatherMap.toColumnView(start, n)) { gatherView =>
      val batch = data.getBatch
      val gatheredTable = withResource(GpuColumnVector.from(batch)) { table =>
        //由此进入，继续深入
        table.gather(gatherView, boundsCheckPolicy)
      }
      withResource(gatheredTable) { gt =>
        GpuColumnVector.from(gt, GpuColumnVector.extractTypes(batch))
      }
    }
    gatheredUpTo += n
    ret
  }
```

最后调用的是该 native 方法。

```
/**
* tableHandle为原始batch的多个列封装后的Handle
* gatherView 为Joined key的列handle
* return 该batch Joined 的多个列handle
*/
private static native long[] gather(long tableHandle, long gatherView, boolean checkBounds);
```

#### ShuffledHashJoin

GpuShuffledHashJoinExec 内部的 doExecuteColumnar() 方法的核心逻辑为

```
//builtBatch 为Join表的一个Batch，这里和 GpuBroadcastHashJoinExec 是不同的，
//stIt为 Stream 表的BatchIterator
doJoin(builtBatch, stIt, targetSize, spillCallback,
  numOutputRows, joinOutputRows, numOutputBatches, opTime, joinTime)
```

其他逻辑同 BroadcastHashJoin **** 此处不再重复赘述**。**


## 接口设计参考

### join接口参考

```
/**
* leftKeyColumnId：左表待join key对应的列id
* rightKeyColumnId：右表join key对应的列id
* resLeftKeyColumnId：左表join后key对应的列id
* resRightKeyColumnId：右表join后key对应的列id
* joinType：join类型
* return 返回码
*/
public static native int join(long leftKeyColumnId, 
                              long rightKeyColumnId,
                              long joinedLeftKeyColumnId,
                              long joinedRightKeyColumnId, 
                              int joinType,           
                              boolean compareNullsEqual) throws RaceException;
```

### gather接口参考

```
/**
* columnIds：未join的多个列id
* joinedKeyId：该表中已经join处理后的key列id
* joinedColumnIds：join后的多列id
* return 返回码
*/
public static native int gather(long[] columnIds, 
                                long joinedKeyId,
                                long[] joinedColumnIds);
                              
```

### 调用示例

左表：[id=1, id=2]
join key：1 
右表：[id=3, id=4]
join key：3 
join类型：1 （inner join）
select的结果列：join后的左表[id=7, id=8]列，join后的右表[id=9]列

> 数字代表列handle

#### **join方法调用示例**

![image-20231012143157937](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231012143157937.png)

这张图，仅用于方便理解join的大概逻辑，不代表具体实现

> 左表索引列： 
>
> 0被匹配了两次，所以0的索引(0)出现两次
>
> 2被匹配一次，所以2的索引(1)出现一次，
>
> 第一个1被匹配2次，所以它的索引(2)出现2次， 
>
> 3没被匹配到，所以没出现，
>
> 第二个1被匹配两次，所以它的索引(4)出现两次。

左表joined key：id=5 
右表joined key：id=6 

```
join(1, 3, 5, 6, 1)
```

#### **gather方法调用示例**

gather方法的逻辑，引用该图进行说明，不代表最终实现

![image-20231012143708510](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231012143708510.png)

左表调用
join后的[id=1]列：id=7
join后的[id=2]列：id=8

```
gather([1, 2], 5, [7, 8])
```

右表调用
join后的[id=4]列：id=9

```
gather([4], 6, [9])
```



#### **join后的左右表列合并**

该环节在spark中进行，不再展开



# 源代码

```scala
  override def hasNextStreamBatch: Boolean = {
    //这里控制上游循环是否继续，上游循环会调用setupNextGatherer。
    //只要流表或者built表有未处理的batch就会继续循环
    pendingSplits.nonEmpty || hasNextBuiltBatch || stream.hasNext
  }

  protected def hasNextBuiltBatch: Boolean = currentStreamBatch.nonEmpty && nextBuiltBatchesIndex < builtBatches.length

  override def setupNextGatherer(joinTime: SQLMetric): Option[JoinGatherer] = {
    if (hasNextStreamBatch) {
      val cb = if (pendingSplits.nonEmpty) {
        pendingSplits.dequeue()
      } else {
        if (currentStreamBatch.isEmpty) {
          if (!stream.hasNext) {
            return None
          }
          currentStreamBatch = Some(stream.next())
        }
        if (!hasNextBuiltBatch) {
          nextBuiltBatchesIndex = 0
          currentStreamBatch.foreach(_.close())
          if (!stream.hasNext) {
            return None
          }
          currentStreamBatch = Some(stream.next())
        }
        val batch = currentStreamBatch.get
        nextBuiltBatchesIndex += 1
        batch
      }

//      withResource(cb) { cb =>
        val numJoinRows = computeNumJoinRows(cb)
        val start = System.nanoTime()
        val result = createGatherer(cb, Some(numJoinRows), nextBuiltBatchesIndex - 1)
        joinTime += (System.nanoTime() - start) / 1000 / 1000
        result
//      }
    } else {
      None
    }
  }

```

