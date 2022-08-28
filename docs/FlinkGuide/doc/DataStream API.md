# 执行环境

从1.12.0版本起，Flink实现了API上的流批统一。DataStream API新增了一个重要特性： 可以支持不同执行模式，通过简单设置就可以让一段Flink程序在流处理和批处理之间切换。这样以来，DataSet API就没有存在的必要了。

1. 流执行模式，是默认的执行模式
2. 批执行模式，执行方式类似于MapReduce框架
3. 自动模式，程序根据输入数据源是否有界自动选择执行模式
4. 手动指定批处理模式：
   1. 命令行传参数： -Dexecution.runtime-mode=BATCH
   2. 代码指定

#   Source

需要实现 SourceFunction 接口

```scala
//读取集合
env.fromCollection(clicks)

//读取序列
env.fromElements(
Event("Mary", "/.home", 1000L),
Event("Bob", "/.cart", 2000L))

//读取文件
env.readTextFile("clicks.csv")

//读socket
env.socketTextStream("localhost", 7777)
```

注意：读取hdfs目录时，需要加依赖

```xml
<dependency>
   <groupId>org.apache.hadoop</groupId>
   <artifactId>hadoop-client</artifactId>
   <version>2.7.5</version>
   <scope>provided</scope>
</dependency>
```

# flink支持的数据类型

使用TypeInfomation来统一表示数据类型

1. Flink 对 POJO 类型的要求如下:

2. - ⚫  类是公共的(public)和独立的(standalone，也就是说没有非静态的内部类);

   - ⚫  类有一个公共的无参构造方法;

   - ⚫  类中的所有字段是public且非final修饰的;或者有一个公共的getter和setter方法，

     这些方法需要符合 Java bean 的命名规范。
      Scala 的样例类就类似于 Java 中的 POJO 类，所以我们看到，之前的 Event，就是我们创

     建的一个 Scala 样例类，使用起来非常方便。

# 聚合算子

flink中要聚合，需要先通过keyBy()算子进行分区，分区字段可以通过lambda表达式，属性，自定义KeySelector指定

```scala
    val stream=env.fromElements(
      ("a", 1), ("a", 3), ("b", 3), ("b", 4)
    )

    stream.keyBy(_._1).sum(1).print() //对元组的索引 1 位置数据求和
    stream.keyBy(_._1).sum("_2").print() //对元组的第 2 个位置数据求和
    stream.keyBy(_._1).max(1).print() //对元组的索引 1 位置求最大值
    stream.keyBy(_._1).max("_2").print() //对元组的第 2 个位置数据求最大值
    stream.keyBy(_._1).min(1).print() //对元组的索引 1 位置求最小值
    stream.keyBy(_._1).min("_2").print() //对元组的第 2 个位置数据求最小值
    stream.keyBy(_._1).maxBy(1).print() //对元组的索引 1 位置求最大值
    stream.keyBy(_._1).maxBy("_2").print() //对元组的第 2 个位置数据求最大值
    stream.keyBy(_._1).minBy(1).print() //对元组的索引 1 位置求最小值
    stream.keyBy(_._1).minBy("_2").print() //对元组的第 2 个位置数据求最小值


    val stream1 = env.fromElements(
      Event("Mary", "./home", 1000L),
      Event("Bob", "./cart", 2000L)
    )
    // 使用 user 作为分组的字段，并计算最大的时间戳 
    stream1.keyBy(_.user).max("timestamp").print()
```

# udf

1. 函数类
   Flink 暴露了所有 UDF 函数的接口，具体实现方式为接口或者抽象类， 例如 MapFunction、FilterFunction、ReduceFunction 等
   当然也可以用Lambda表达式
2. 富函数类

“富函数类”也是DataStream API提供的一个函数类的接口，所有的Flink函数类都有其 Rich 版本。富函数类一般是以抽象类的形式出现的。例如:RichMapFunction、RichFilterFunction、 RichReduceFunction 等。

与常规函数类的不同主要在于，富函数类可以获取运行环境的上下文，并拥有一些生命周 期方法，所以可以实现更复杂的功能。典型的生命周期方法有:
 ⚫ open()方法，是RichFunction的初始化方法，也就是会开启一个算子的生命周期。当

一个算子的实际工作方法例如 map()或者 filter()方法被调用之前，open()会首先被调 用。所以像文件 IO 流的创建，数据库连接的创建，配置文件的读取等等这样一次性 的工作，都适合在 open()方法中完成。

⚫ close()方法，是生命周期中的最后一个调用的方法，类似于解构方法。一般用来做一 些清理工作。

# 物理分区

keyBy是逻辑分区操作，如何实现数据流的重分区？ 

1. 随机分区， shuffle()方法，随机均匀分配到下游
2. 轮询分区(Round-Robin), rebalance()方法，平均分配到下游的并行任务中
3. 重缩放分区,rescale()方法，底层也是Round-Robin算法轮询，但只会轮询到下游并行任务中的一部分任务中
4. 广播， broadcast()方法，数据会在不同的分区都保留一份，可能进行重复处理
5. 全局分区， global()方法，全发送到下游第一个字任务中去
6. 自定义分区，实现Partitioner

# Sink

实现的是 SinkFunction 接口

![image-20220827013410474](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220827013410474.png)

![image-20220827013420963](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220827013420963.png)

对于输出到文件系统，Flink 为此专门提供了一个流式文件系统的连接器:StreamingFileSink，它继承自抽象类 RichSinkFunction，而且集成了 Flink 的检查点(checkpoint)机制，用来保证精确一次(exactly once)的一致性语义。

StreamingFileSink 为批处理和流处理提供了一个统一的 Sink，它可以将分区文件写入 Flink 支持的文件系统。它可以保证精确一次的状态一致性，大大改进了之前流式文件输出的方式。 它的主要操作是将数据写入桶(buckets)，每个桶中的数据都可以分割成一个个大小有限的分 区文件。

StreamingFileSink 支持行编码(Row-encoded)和批量编码(Bulk-encoded，比如 Parquet) 格式。这两种不同的方式都有各自的构建器(builder)，调用方法也非常简单，可以直接调用 StreamingFileSink 的静态方法:

⚫ 行编码:StreamingFileSink.forRowFormat t(basePath,rowEncoder)。
 ⚫ 批量编码:StreamingFileSink.forBulkFormat(basePath,bulkWriterFactory)。

# 时间

1. 处理时间：执行处理操作的机器的系统时间，最简单的时间语义
2. 事件时间：每个事件在对应设备上发生的时间，即数据生成的时间，是数据的时间戳

> 但是由于网络等原因，数据的时间戳不能直接当作时钟，因为总有乱序数据，导致时间倒退的情况，这需要用到事件时间的**水位线**来标志事件时间进展



## 水位线

水位线可以看作一条特殊的数据记录，是插入到数据流中的一个标记点，主要内容就是一个时间戳，用来指示当前的事件时间。而它插入流中的位置，就应该是在某个 数据到来之后;这样就可以从这个数据中提取时间戳，作为当前水位线的时间戳了

![image-20220827141440516](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220827141440516.png)

为了提高效率，一般会每隔一段时间生成一个水位 线，这个水位线的时间戳，就是当前最新数据的时间戳。 插入时间戳时先判断时间戳是否比之前的大，否则就不再生成新的时间戳。

![image-20220827141530085](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220827141530085.png)

为了让窗口能够正确收集到迟到的数据，我们可以等上 2 秒;也就是用当前已有数据的最

大时间戳减去 2 秒，就是要插入的水位线的时间戳：

![image-20220827143849490](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220827143849490.png)

```
我们可以总结一下水位线的特性:
```

- ⚫  水位线是插入到数据流中的一个标记，可以认为是一个特殊的数据

- ⚫  水位线主要的内容是一个时间戳，用来表示当前事件时间的进展

- ⚫  水位线是基于数据的时间戳生成的

- ⚫  水位线的时间戳必须单调递增，以确保任务的事件时间时钟一直向前推进

- ⚫  水位线可以通过设置延迟，来保证正确处理乱序数据

- ⚫  一个水位线 Watermark(t)，表示在当前流中事件时间已经达到了时间戳 t, 这代表 t 之

  前的所有数据都到齐了，之后流中不会出现时间戳 t’ ≤ t 的数据
   水位线是 Flink 流处理中保证结果正确性的核心机制，它往往会跟窗口一起配合，完成对

  乱序数据的正确处理。Flink 中的水位线，其实是流处理中对低延迟和结果正确性的一个权衡机制，而且把 控制的权力交给了程序员，我们可以在代码中定义水位线的生成策略。

  ### 水位线的生成策略

  assignTimestampsAndWatermarks()
  它主要用来为流中的数据分配时间戳，并生成水位线来指 示事件时间。

  ```scala
  public interface WatermarkStrategy<T>
  extends TimestampAssignerSupplier<T>,
            WatermarkGeneratorSupplier<T>{
  //提取时间戳
  @Override
     TimestampAssigner<T>
  createTimestampAssigner(TimestampAssignerSupplier.Context context);
  //基于时间戳生成水位线
  @Override
     WatermarkGenerator<T>
  createWatermarkGenerator(WatermarkGeneratorSupplier.Context context);
  }
  /**
  WatermarkGenerator 接口中，主要又有两个方法:onEvent()和 onPeriodicEmit()。
  ⚫ onEvent:每个事件(数据)到来都会调用的方法，它的参数有当前事件、时间戳， 以及允许发出水位线的一个 WatermarkOutput，可以基于事件做各种操作
  ⚫ onPeriodicEmit:周期性调用的方法，可以由WatermarkOutput发出水位线。周期时间
  为处理时间，可以调用环境配置的 setAutoWatermarkInterval()方法来设置，默认为
  200ms。 env.getConfig.setAutoWatermarkInterval(60 * 1000L)
  */
  
  ```

  

### 内置水位线生成器

通过调用 WatermarkStrategy 的静态辅助方法来创建。它们都是周期性 生成水位线的，分别对应着处理有序流和乱序流的场景。

1. 有序流

   直接调用WatermarkStrategy.forMonotonousTimestamps()方法就可以实现。简单来说，就是直接拿当前最 大的时间戳作为水位线就可以了。这里需要注意的是，时间戳和水位线的单位，必须都是毫秒。

2. 乱序流
   WatermarkStrategy. forBoundedOutOfOrderness() 设置延迟时间

### 自定义水位线策略

在 WatermarkStrategy 中，时间戳分配器 TimestampAssigner 都是大同小异的，指定字段提 取时间戳就可以了;而不同策略的关键就在于 WatermarkGenerator 的实现。整体说来，Flink 有两种不同的生成水位线的方式:一种是周期性的(Periodic)，另一种是断点式的(Punctuated)。

(1)周期性水位线生成器(Periodic Generator)
 周期性生成器一般是通过 onEvent()观察判断输入的事件，而在 onPeriodicEmit()里发出水

位线。
 (2)断点式水位线生成器(Punctuated Generator)

断点式生成器会不停地检测 onEvent()中的事件，当发现带有水位线信息的特殊事件时， 就立即发出水位线。一般来说，断点式生成器不会通过 onPeriodicEmit()发出水位线。

**在自定义数据源中生成水位线和在程序中使用 assignTimestampsAndWatermarks 方法生成水位线二者只能取其一。**

### 水位线的传递

上游不同分区进度不同时 ，以最慢的那个时钟，即最小的那个水位线为准。

# 窗口

将无限的数据流切割成有限的“数据块”进行处理，这就是窗口，是无界流处理的核心。

1. 为了明确数据划分到哪一个窗口，定义窗口都是包含起始时间、不包含结束时间 的，用数学符号表示就是一个左闭右开的区间。

2. Flink 中窗口并不是静态准备好的，而是动态创建——当有落在这个 窗口区间范围的数据达到时，才创建对应的窗口

![image-20220827161712320](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220827161712320.png)

## 窗口的分类

按驱动类型分：

1. 时间窗口，左闭右开
2. 计数窗口

按数据分配规则分类

1. 滚动窗口：首尾相接、不重复

   1. 基于数据个数

      >  stream.keyBy(...)
      >      .countWindow(10)

   2. 基于时间长度: 

      > TumblingProcessingTimeWindows.of(size)
      >
      > TumblingEventTimeWindows.of(size)

2. 滑动窗口：窗口大小和滑动步长可调

   1. 基于数据个数

      >  stream.keyBy(...)
      >      .countWindow(size,  slide)

   2. 基于时间: 

      > SlidingProcessingTimeWindows.of( size , slide ) 
      >
      > SlidingEventTimeWindows.of

3. 会话窗口，只需指定会话间隔（可固定，也可自定义）

   > ProcessingTimeSessionWindows.withGap 
   >
   > EventTimeSessionWindows.withGap

4. 全局窗口

   >  stream.keyBy(...)
   >      .window(GlobalWindows.create())

把相同key的所有数据分配到同一个窗口中， 因为数据无界，所以窗口也不会结束，默认不会触发计算，但也可自定义**触发器**来计算

## 窗口API

### 按键分区和非按键分区

1. 按键分区窗口

   过按键分区 keyBy()操作后，数据流会按照 key 被分为多条逻辑流(logical streams)，这

   就是 KeyedStream。基于 KeyedStream 进行窗口操作时, 窗口计算会在多个并行子任务上同时 执行。相同 key 的数据会被发送到同一个并行子任务，而窗口操作会基于每个 key 进行单独的 处理。所以可以认为，每个 key 上都定义了一组窗口，各自独立地进行统计计算。

```scala
 stream.keyBy(...)
      .window(...)
```

2. 非按键分区

如果没有进行 keyBy()，那么原始的 DataStream 就不会分成多条逻辑流。这时窗口逻辑只

能在一个任务(task)上执行，就相当于并行度变成了 1。所以在实际应用中一般不推荐使用 这种方式。

在代码中，直接基于 DataStream 调用 windowAll()定义窗口。

```scala
stream.windowAll(...)
```

### 窗口API使用

主要两个部分， 窗口分配器窗口函数

```scala
stream.keyBy(<key selector>) 
.window(<window assigner>) 
.aggregate(<window function>)
```

### 窗口函数

![image-20220827183938019](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220827183938019.png)

根据处理方式，可分成两类：

1. 增量聚合函数

   1. ReduceFunction

      > 聚合状态、结果类型必须和输入一样

      ```scala
      WindowedStream
      .reduce( new ReduceFunction)
      ```

   2. AggretateFunction

      > 输出类型可以和输入不一样

      AggregateFunction 接口中有四个方法:
       ⚫ createAccumulator():创建一个累加器，这就是为聚合创建了一个初始状态，每个聚合任务只会调用一次。
       ⚫ add():将输入的元素添加到累加器中。这就是基于聚合状态，对新来的数据进行进

      一步聚合的过程。方法传入两个参数:当前新到的数据 value，和当前的累加器 accumulator;返回一个新的累加器值，也就是对聚合状态进行更新。每条数据到来之 后都会调用这个方法。

      ⚫ getResult():从累加器中提取聚合的输出结果。也就是说，我们可以定义多个状态， 然后再基于这些聚合的状态计算出一个结果进行输出。比如之前我们提到的计算平均 值，就可以把 sum 和 count 作为状态放入累加器，而在调用这个方法时相除得到最终 结果。这个方法只在窗口要输出结果时调用。

      ⚫ merge():合并两个累加器，并将合并后的状态作为一个累加器返回。这个方法只在 需要合并窗口的场景下才会被调用;最常见的合并窗口(Merging Window)的场景 就是会话窗口(Session Windows)。

   3. 另外，Flink 也为窗口的聚合提供了一系列预定义的简单聚合方法，可以直接基于 WindowedStream 调用。主要包括 sum()/max()/maxBy()/min()/minBy()，与 KeyedStream 的简单 聚合非常相似。它们的底层，其实都是通过 AggregateFunction 来实现的。

2. 全窗口函数

   1. WindowFunction

      ```scala
      stream
      .keyBy(<key selector>) 
      .window(<window assigner>) 
      .apply(new MyWindowFunction())
      ```

      可拿到窗口所有元素和窗口本身的信息， 但缺乏上下文信息以及更高级的功能，逐渐被ProcessWindowFunction替代。

   2. ProcessWindowFunction

Window API 中最底层的通用窗口函数接口，最强大，它不仅可以获取窗口信息还可以获取到一个 “上下文对象”(Context)。这个上下文对象非常强大，不仅能够获取窗口信息，还可以访问当 前的时间和状态信息。

基于 WindowedStream 调用 process()方法，传入一个 ProcessWindowFunction 的实现类

3. 增量聚合和全窗口聚合的结合

- 增量聚合函数处理计算会更高效。增量聚合相当于把计算量“均摊”到了窗口收集数据的 过程中，自然就会比全窗口聚合更加高效、输出更加实时。
- 而全窗口函数的优势在于提供了更多的信息，可以认为是更加“通用”的窗口操作，窗口 计算更加灵活，功能更加强大。

但reduce/aggretate方法可以组合增量窗口和全量窗口一起使用：
处理机制是:基于第一个参数(增量聚合函数)来处理窗口数据，每来一个数 据就做一次聚合;等到窗口需要触发计算时，则调用第二个参数(全窗口函数)的处理逻辑输 出结果。需要注意的是，这里的全窗口函数就不再缓存所有数据了，而是直接将增量聚合函数 的结果拿来当作了 Iterable 类型的输入。一般情况下，这时的可迭代集合中就只有一个元素了

### 其他API

1. 触发器
   WindowedStream 调用 trigger()方法，每个窗口分配器(WindowAssigner)都会对应一个默认 的触发器;对于 Flink 内置的窗口类型，它们的触发器都已经做了实现

   Trigger 是一个抽象类，自定义时必须实现下面四个抽象方法:
    ⚫ onElement():窗口中每到来一个元素，都会调用这个方法。
    ⚫ onEventTime():当注册的事件时间定时器触发时，将调用这个方法。
    ⚫ onProcessingTime ():当注册的处理时间定时器触发时，将调用这个方法。

   ⚫ clear():当窗口关闭销毁时，调用这个方法。一般用来清除自定义的状态。

2. 移除器(Evictor)

   Evictor 接口定义了两个方法:
    ⚫ evictBefore():定义执行窗口函数之前的移除数据操作
    ⚫ evictAfter():定义执行窗口函数之后的以处数据操作 默认情况下，预实现的移除器都是在执行窗口函数(window fucntions)之前移除数据的。

3. 允许延迟(Allowed Lateness)

我们可以设定允许延迟一段时间，在这段时 间内，窗口不会销毁，继续到来的数据依然可以进入窗口中并触发计算。直到水位线推进到了 窗口结束时间 + 延迟时间，才真正将窗口的内容清空，正式关闭窗口

4. 迟到数据放入侧输出流

基于 WindowedStream 调用 sideOutputLateData() 方法，就可以实现这个功能。方法需要 传入一个“输出标签”(OutputTag)，用来标记分支的迟到数据流。因为保存的就是流中的原 始数据，所以 OutputTag 的类型与流中数据类型相同

```scala
val winAggStream = stream.keyBy(...)
      .window(TumblingEventTimeWindows.of(Time.hours(1)))
     .sideOutputLateData(outputTag)
.aggregate(new MyAggregateFunction)
val lateStream = winAggStream.getSideOutput(outputTag)
```

