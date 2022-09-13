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

水位线的生成策略可以分为两类：

- 基于事件时间生成
- 基于时间周期生成

通过调用 WatermarkStrategy 的静态辅助方法来创建。它们都是周期性 生成水位线的，分别对应着处理有序流和乱序流的场景。

1. 有序流

   直接调用WatermarkStrategy.forMonotonousTimestamps()方法就可以实现。简单来说，就是直接拿当前最 大的时间戳作为水位线就可以了。这里需要注意的是，时间戳和水位线的单位，必须都是毫秒。

   ```scala
   stream.assignTimestampsAndWatermarks( WatermarkStrategy.forMonotonousTimestamps[Event]()
      //指定当前水位线生成策略的时间戳生成器                                  
       .withTimestampAssigner(
         new SerializableTimestampAssigner[Event] {
           /**
           t: 被赋予时间戳的事件
           l: 当前事件的时间戳，或者一个负值(未赋予时间戳)
           */
           override def extractTimestamp(t: Event, l: Long): Long = t.timestamp
         }
       ))
   ```

   

2. 乱序流
   WatermarkStrategy. forBoundedOutOfOrderness() 设置最大乱序时间
   
   ```scala
       stream.assignTimestampsAndWatermarks( WatermarkStrategy.forBoundedOutOfOrderness[Event](Duration.ofSeconds(5))
       .withTimestampAssigner(
         new SerializableTimestampAssigner[Event] {
           override def extractTimestamp(t: Event, l: Long): Long = t.timestamp
         }
       ))
   ```
   
   

### 自定义水位线策略

在 WatermarkStrategy 中，时间戳分配器 TimestampAssigner 都是大同小异的，指定字段提 取时间戳就可以了;而不同策略的关键就在于 WatermarkGenerator 的实现。<font color=red>整体说来，Flink 有两种不同的生成水位线的方式:一种是周期性的(Periodic)，另一种是断点式的(Punctuated)。</font>

(1)周期性水位线生成器(Periodic Generator)
 周期性生成器一般是通过 onEvent()观察判断输入的事件，而在 onPeriodicEmit()里发出水位线。
 (2)断点式水位线生成器(Punctuated Generator)

```scala
    stream.assignTimestampsAndWatermarks( new WatermarkStrategy[Event] {
      //(一) 提取时间戳，当然提取时间戳也可以像上面内置水位线生成器那样放到外面程序里提取
      override def createTimestampAssigner(context: TimestampAssignerSupplier.Context): TimestampAssigner[Event] = {
        new SerializableTimestampAssigner[Event] {
          override def extractTimestamp(t: Event, l: Long): Long = t.timestamp
        }
      }
     //(二) 实现水位器生成器， 这里是周期性发送时间戳，如果想要断点式生成时间戳，则在onEvent里更新时间戳并发送即可，onPeriodicEmit无需实现
      override def createWatermarkGenerator(context: WatermarkGeneratorSupplier.Context): WatermarkGenerator[Event] = {
        new WatermarkGenerator[Event] {
          // 定义一个延迟时间
          val delay = 5000L
          //1 定义属性保存最大时间戳
          var maxTs = Long.MinValue + delay + 1  //+xx防止溢出  

          override def onEvent(t: Event, l: Long, watermarkOutput: WatermarkOutput): Unit = {
            //2 根据当前事件更新当前最大时间戳
            maxTs = math.max(maxTs, t.timestamp)
          }

          override def onPeriodicEmit(watermarkOutput: WatermarkOutput): Unit = {
            //3 框架会定期调用这个方法，该方法周期性发送时间戳
            val watermark = new Watermark(maxTs - delay - 1)
            watermarkOutput.emitWatermark(watermark)
          }
        }
      }
    } )
```

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
   
   ![image-20220913104205899](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220913104205899.png)

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

### 窗口分配器

所谓的分配，就是分配窗口数据，和窗口的分配是一样的。

![image-20220913104752924](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220913104752924.png)

1. 时间窗口

   - 滚动时间窗口

   ```scala
   stream.keyBy(...)
   .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
   .aggregate(...)
   ```

   - 滑动时间窗口

   ```scala
   SlidingEventTimeWindows.of(Time.seconds(10),Time.seconds(5))
   SlidingProcessingTimeWindows.of(Time.seconds(10),Time.seconds(5))
   ```

   - 事件时间会话窗口

   ```scala
   EventTimeSessionWindows.withGap(Time.seconds(10)))
   ```

2. 计数窗口

- 滚动计数

  ```scala
   stream.keyBy(...)
       .countWindow(10)
  ```

- 滑动计数

  ```scala
   stream.keyBy(...)
       .countWindow(10,3)
  ```

3. 全局窗口

```scala
 stream.keyBy(...)
     .window(GlobalWindows.create())
```



### 窗口函数

![image-20220913111338447](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220913111338447.png)



<font color=red>窗口函数总结</font>： 

- 增量聚合函数(计算分摊到窗口收集数据过程中，更加高效)

1. reduce后跟ReduceFunction
2. aggregate后跟AggregateFunction

- 全窗口函数(提供了更多信息)

1. apply后跟WindowFunction  能提供的上下文信息较少，逐渐弃用
2. process后跟ProcessWindowFunction 最底层的通用窗口函数接口

两者优点可以结合在一起使用， 方法就是在reduce/aggregate里多传一个全窗口函数。大体思路，就是使用增量聚合函数聚合中间结果，等到窗口需要触发计算时，调用全窗口函数；这样即加速了计算，又不用缓存全部数据。





根据处理方式，可分成两类：

1. **增量聚合函数**

   1. ReduceFunction

      > **聚合状态、结果类型必须和输入一样**

      ```scala
      WindowedStream
      .reduce( new ReduceFunction)
      ```

   2. **AggretateFunction**

      > **输出类型可以和输入不一样**

      AggregateFunction 接口中有四个方法:
       ⚫ createAccumulator():创建一个累加器，这就是为聚合创建了一个初始状态，每个聚合任务只会调用一次。
       ⚫ add():将输入的元素添加到累加器中。这就是基于聚合状态，对新来的数据进行进

      一步聚合的过程。方法传入两个参数:当前新到的数据 value，和当前的累加器 accumulator;返回一个新的累加器值，也就是对聚合状态进行更新。每条数据到来之 后都会调用这个方法。

      ⚫ getResult():从累加器中提取聚合的输出结果。也就是说，我们可以定义多个状态， 然后再基于这些聚合的状态计算出一个结果进行输出。比如之前我们提到的计算平均 值，就可以把 sum 和 count 作为状态放入累加器，而在调用这个方法时相除得到最终 结果。这个方法只在窗口要输出结果时调用。

      ⚫ merge():合并两个累加器，并将合并后的状态作为一个累加器返回。这个方法只在 需要合并窗口的场景下才会被调用;最常见的合并窗口(Merging Window)的场景 就是会话窗口(Session Windows)。

   3. 另外，Flink 也为窗口的聚合提供了一系列预定义的简单聚合方法，可以直接基于 WindowedStream 调用。主要包括 sum()/max()/maxBy()/min()/minBy()，与 KeyedStream 的简单 聚合非常相似。它们的底层，其实都是通过 AggregateFunction 来实现的。

2. **全窗口函数**

   1. WindowFunction

      ```scala
      stream
      .keyBy(<key selector>) 
      .window(<window assigner>) 
      .apply(new MyWindowFunction())
      ```

      可拿到窗口所有元素和窗口本身的信息， 但缺乏上下文信息以及更高级的功能，逐渐被ProcessWindowFunction替代。

   2. **ProcessWindowFunction**

Window API 中最底层的通用窗口函数接口，最强大，它不仅可以获取窗口信息还可以获取到一个 “上下文对象”(Context)。这个上下文对象非常强大，不仅能够获取窗口信息，还可以访问当 前的时间和状态信息。

基于 WindowedStream 调用 process()方法，传入一个 ProcessWindowFunction 的实现类

3. **增量聚合和全窗口聚合的结合**

- **增量聚合函数处理计算会更高效。增量聚合相当于把计算量“均摊”到了窗口收集数据的 过程中，自然就会比全窗口聚合更加高效、输出更加实时。**
- **而全窗口函数的优势在于提供了更多的信息，可以认为是更加“通用”的窗口操作，窗口 计算更加灵活，功能更加强大**。

但reduce/aggretate方法可以组合增量窗口和全量窗口一起使用：
处理机制是:基于第一个参数(增量聚合函数)来处理窗口数据，每来一个数 据就做一次聚合;等到窗口需要触发计算时，则调用第二个参数(全窗口函数)的处理逻辑输 出结果。需要注意的是，这里的全窗口函数就不再缓存所有数据了，而是直接将增量聚合函数 的结果拿来当作了 Iterable 类型的输入。一般情况下，这时的可迭代集合中就只有一个元素了

### 其他API

触发器主要是用来控制窗口什么时候触发计算。所谓的“触发计算”，本质上就是执行窗 口函数，所以可以认为是计算得到结果并输出的过程。

1. **触发器**
   WindowedStream 调用 trigger()方法，每个窗口分配器(WindowAssigner)都会对应一个默认 的触发器;对于 Flink 内置的窗口类型，它们的触发器都已经做了实现。例如，所有事件时间 窗口，默认的触发器都是 EventTimeTrigger;类似还有 ProcessingTimeTrigger 和 CountTrigger。 所以一般情况下是不需要自定义触发器的，不过我们依然有必要了解它的原理。

   Trigger 是一个抽象类，自定义时必须实现下面四个抽象方法:
    ⚫ onElement():窗口中每到来一个元素，都会调用这个方法。
    ⚫ onEventTime():当注册的事件时间定时器触发时，将调用这个方法。
    ⚫ onProcessingTime ():当注册的处理时间定时器触发时，将调用这个方法。

   ⚫ clear():当窗口关闭销毁时，调用这个方法。一般用来清除自定义的状态。
   Trigger 除了可以控制触发计算，还可以定义窗口什么时候关闭(销毁)

2. **移除器**(Evictor)

   Evictor 接口定义了两个方法:
    ⚫ evictBefore():定义执行窗口函数之前的移除数据操作
    ⚫ evictAfter():定义执行窗口函数之后的以处数据操作 默认情况下，预实现的移除器都是在执行窗口函数(window fucntions)之前移除数据的。

3. **允许延迟**(Allowed Lateness)

我们可以设定允许延迟一段时间，在这段时 间内，窗口不会销毁，继续到来的数据依然可以进入窗口中并触发计算。直到水位线推进到了 窗口结束时间 + 延迟时间，才真正将窗口的内容清空，正式关闭窗口。基于 WindowedStream 调用 allowedLateness()方法，传入一个 Time 类型的延迟时间，就可 以表示允许这段时间内的延迟数据。

4. **迟到数据放入侧输出流**

基于 WindowedStream 调用 sideOutputLateData() 方法，就可以实现这个功能。方法需要 传入一个“输出标签”(OutputTag)，用来标记分支的迟到数据流。因为保存的就是流中的原 始数据，所以 OutputTag 的类型与流中数据类型相同

```scala
val winAggStream = stream.keyBy(...)
      .window(TumblingEventTimeWindows.of(Time.hours(1)))
     .sideOutputLateData(outputTag)
.aggregate(new MyAggregateFunction)
val lateStream = winAggStream.getSideOutput(outputTag)
```

# 多流转换

## union/connect

这里其实，就是把两个流的数据放到一起

但，也可以通过keyBy()指定键进行分组后合并， 实现了类似于 SQL 中的 join 操作

1. 分流：一般通过侧输出流

2. 合流：合流算子比较丰富，union()/connect()/join()

   1. union的多个流必须类型相同(一次可以合并多个流)

   2. connect允许数据类型不同(一次只能合并两个流)

      > 在代码实现上，需要分为两步:首先基于一条 DataStream 调用 connect()方法，传入另外 一条 DataStream 作为参数，将两条流连接起来，得到一个 ConnectedStreams;然后再调用同处 理方法得到DataStream。这里可以的调用的同处理方法有map()/flatMap()，以及process()方法

```scala
val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    val stream1: DataStream[Int] = env.fromElements(1,2,3)
    val stream2: DataStream[Long] = env.fromElements(1L,2L,3L,4L)
    val connectedStream: ConnectedStreams[Int, Long] = stream1.connect(stream2)
    val result=connectedStream
      .map(new CoMapFunction[Int,Long,String] {
        //处理第一条流
        override def map1(in1: Int): String = {
          "Int:"+in1
        }
                                                                                                                                                                                          //处理第二条流
        override def map2(in2: Long): String = {
          "Long:"+in2
        }
      })

    result.print()
    env.execute()
```

对于连接流 ConnectedStreams 的处理操作，需要分别定义对两条流的处理转换，因此接口 中就会有两个相同的方法需要实现，用数字“1”“2”区分，在两条流中的数据到来时分别调 用。我们把这种接口叫作“**协同处理函数**”(co-process function)。与 CoMapFunction 类似，如 果是调用 flatMap()就需要传入一个 CoFlatMapFunction，需要实现 flatMap1()、flatMap2()两个 方法;而调用 process()时，传入的则是一个 CoProcessFunction。

CoProcessFunction 也是“处理函数”家族中的一员，同样可以通过上下文ctx来 访问 timestamp、水位线，并通过 TimerService 注册定时器;另外也提供了 onTimer()方法，用 于定义定时触发的处理操作。

## join(基于时间的合流)

对于两条流的合并，很多情况我们并不是简单地将所有数据放在一起，而是希望根据某个 字段的值将它们联结起来，“配对”去做处理。类似于SQL中的join

1. connect: 最复杂，通过keyBy()分组后合并,自定义状态、触发器
2. window: 中间
3. join算子：最简单

### 窗口join

1. 接口API

```scala
stream1
.join(stream2)//的到JoinedStreams 
.where(<KeySelector>) //左key
.equalTo(<KeySelector>) //右key
.window(<WindowAssigner>) //出现在同一个窗口
.apply(<JoinFunction>)//同时处理
```



join逻辑

```scala
public interface JoinFunction<IN1, IN2, OUT> extends Function, Serializable {
   OUT join(IN1 first, IN2 second) throws Exception;
}
```



2. 处理流程

JoinFunction 中的两个参数，分别代表了两条流中的匹配的数据。

窗口中每有一对数据成功联结匹配，JoinFunction 的 join()方法就会被调用一次，并输出一 个结果。

除了 JoinFunction，在 apply()方法中还可以传入 FlatJoinFunction，用法非常类似，只是内 部需要实现的 join()方法没有返回值。

> **注意，join的结果是笛卡尔积**

### 间隔join

隔联结的思 路就是针对一条流的每个数据，开辟出其时间戳前后的一段时间间隔，看这期间是否有来自另 一条流的数据匹配。

1. 原理：
   对第一条流中的每一条数据a ：开辟一段时间间隔:[a.timestamp + lowerBound, a.timestamp + upperBound], 如果另一条流的数据在这个范围内，则成功匹配

![image-20220906075209055](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220906075209055.png)

与窗口联结不同的是，interval join 做匹配的**时间段是基于流中数据的**，所以并不确定;**而且流 B 中的数据可以不只在一个区间内被匹配。**

2. 接口API
  

```scala
stream1
.keyBy(_._1) //基于keyed流做内连接
.intervalJoin(stream2.keyBy(_._1))
.between(Time.milliseconds(-2),Time.milliseconds(1))
.process(new ProcessJoinFunction[(String, Long), (String, Long), String] {
    override def processElement(left: (String, Long), right: (String, Long), ctx: ProcessJoinFunction[(String, Long), (String, Long), String]#Context, out: Collector[String]) = {
           out.collect(left + "," + right)
     }
});
```

# 状态编程

## 状态分类

1. 托管状态(Managed State)和原始状态(Raw State)

推荐托管状态：Flink运行时维护

2. 算子状态(Operator State)和按键分区状态(Keyed State)

- Flink 能管理的状态在并行任务间是无法共享的，每个状态只能针对当前子任务的实例有效

> 在 Flink 中，一个算子任务会按照并行度分为多个并行子任务执行，而不同的子
>
> 任务会占据不同的任务槽(task slots)。由于不同的 slot 在计算资源上是物理隔离的

算子状态可以用在所有算子上，使用的时候其实就跟一个本地变量没什么区别——因为本 地变量的作用域也是当前任务实例。在使用时，我们还需进一步实现 CheckpointedFunction 接 口。

- keyed流之后的算子应该只能访问当前key

  按键分区状态应用非常广泛。之前讲到的聚合算子必须在 keyBy()之后才能使用，就是因 为聚合的结果是以Keyed State的形式保存的。另外，也可以通过富函数类(Rich Function) 来自定义 Keyed State，所以只要提供了富函数类接口的算子，也都可以使用 Keyed State。

  所以即使是 map()、filter()这样无状态的基本转换算子，我们也可以通过富函数类给它们 “追加”Keyed State，或者实现CheckpointedFunction接口来定义Operator State;从这个角度

  讲，Flink 中所有的算子都可以是有状态的，不愧是“有状态的流处理”。在富函数中，我们可以调用.getRuntimeContext() 获取当前的运行时上下文(RuntimeContext)，进而获取到访问状态的句柄;这种富函数中自 定义的状态也是 Keyed State。

### 按键分区状态

### 状态结构类型

1. 值状态ValueState

```scala
public interface ValueState<T> extends State {
   T value() throws IOException;
   void update(T value) throws IOException;
}
```

2. 列表状态ListState

用方式 与一般的 List 非常相似。

3. 映射状态(MapState)
4. 归约状态(ReducingState)
5. 聚合状态(AggregatingState)

在具体使用时，为了让运行时上下文清楚到底是哪个状态，我们还需要创建一个“状态描

述器”(StateDescriptor)来提供状态的基本信息

### 状态使用

```scala
class MyFlatMapFunction extends RichFlatMapFunction[Long, String] {
// 声明状态,如果不使用懒加载的 方式初始化状态变量，则应该在 open()方法中也就是生命周期的开始初始化状态变量
lazy val state = getRuntimeContext.getState(new
ValueStateDescriptor[Long]("my state", classOf[Long]))
override defflatMap(input: Long, out: Collector[String] ): Unit = { // 访问状态
       var currentState = state.value()
currentState += 1 // 状态数值加 1 // 更新状态 state.update(currentState)
if (currentState >= 100) {
          out.collect("state: " + currentState)
state.clear() // 清空状态 }
} }
```

### 状态生存时间(TTL)

需要创建一个 StateTtlConfig 配置对象，然后调用状态描述器的 164

enableTimeToLive()方法启动 TTL 功能

```scala
val ttlConfig = StateTtlConfig
   .newBuilder(Time.seconds(10))//状态生存时间
   .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)//什么时候更新状态失效时间
//状态可见性，状态的清理并非实时的，过期了仍然可能没被清理而被读取到；然而我们可控制是否读取这个过期值
   .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
   .build()
val stateDescriptor = new ValueStateDescriptor[String](
"my-state",
     classOf[String]
   )
stateDescriptor.enableTimeToLive(ttlConfig)
```

## 算子状态

从某种意义上说，算子状态是更底层的状态类型，因为它只针对当前算子并行任务有效，不需 要考虑不同 key 的隔离。算子状态功能不如按键分区状态丰富，应用场景较少(没有key定义的场景)，它的调用方法 也会有一些区别。

算子状态(Operator State)就是一个算子并行实例上定义的状态，作用范围被限定为当前 算子任务。算子状态跟数据的 key 无关，所以不同 key 的数据只要被分发到同一个并行子任务， 就会访问到同一个 Operator State。

当算子的并行度发生变化时，算子状态也支持在并行的算子任务实例之间做重组分配。根 据状态的类型不同，重组分配的方案也会不同。

### 状态类型

- ListState

> 每个并行子任务只保留一个list来保存当前子任务的状态项；当算子并行度进行缩放调整时，算子的列表状态中的所有元素项会被统一收集起来，相当 于把多个分区的列表合并成了一个“大列表”，然后再均匀地分配给所有并行任务

- UnionListState 

> 在并行度调整时，常规列表状态是轮询分 配状态项，而联合列表状态的算子则会直接广播状态的完整列表。这样，并行度缩放之后的并 行子任务就获取到了联合后完整的“大列表”

-  BroadcastState。

> 算子并行子任务都保持同一份“全局”状态，用来做统一的配置和规则设定。 这时所有分区的所有数据都会访问到同一个状态，状态就像被“广播”到所有分区一样，这种 特殊的算子状态，就叫作广播状态(BroadcastState)

### 代码实现

Flink提供了管理算子状态的接口，我们根据业务需要自行设计状态的快照保存和恢复逻辑

- CheckpointedFunction接口

  ```scala
  public interface CheckpointedFunction {
  // 保存状态快照到检查点时，调用这个方法； 注意这个快照Context可提供检查点相关信息，但无法获取状态
     void snapshotState(FunctionSnapshotContext context) throws Exception
  // 初始化状态时调用这个方法，也会在恢复状态时调用；这个函数初始化上下文是真的Context，可以获取到状态内容
  void initializeState(FunctionInitializationContext context) throws Exception;
  }
  ```

## 状态持久化和状态后端

Flink 对状态进行持久化的方式，就是将当前所 有分布式状态进行“快照”保存，写入一个“检查点”(checkpoint)或者保存点(savepoint) 保存到外部存储系统中

- 检查点： 

  > 至少一次：检查点之后又处理了一部分数据，如果出现故障，就需要源能重放这部分数据，否则就会丢失。例如，可以保存kafka的偏移量来重放数据

- 状态后端：

状态后端主要负责两件事:一是本地的状态管理，二是将检查 点(checkpoint)写入远程的持久化存储。

状态后端分类：

- 哈希表状态后端(HashMapStateBackend)

- 内嵌 RocksDB 状态后端(EmbeddedRocksDBStateBackend)

  > RocksDB 默认存储在 TaskManager 的本地数据目录里。EmbeddedRocksDBStateBackend 始终执行的是异步快照，也就是不会因为保存检查点而阻 塞数据的处理;而且它还提供了增量式保存检查点的机制，这在很多情况下可以大大提升保存 效率。
  >
  > 由于它会把状态数据落盘，而且支持增量化的检查点，所以在状态非常大、窗口非常长、 键/值状态很大的应用场景中是一个好选择，同样对所有高可用性设置有效。
  >
  > 

IDE中使用RocksDB状态后端，需要加依赖

```xml
<dependency>
   <groupId>org.apache.flink</groupId>
   <artifactId>flink-statebackend-rocksdb_2.11</artifactId>
   <version>1.13.0</version>
</dependency>
```

# 容错机制

检查点是 Flink 容错机制的核心。这里所谓的“检查”，其实是针对故障恢复的结果而言 的:故障恢复之后继续处理的结果，应该与发生故障前完全一致，我们需要“检查”结果的正 确性。

### 检查点的保存

- 周期性触发
- 一个数据**被所有任务处理完时保存**，或者就完全不保存该数据的状态

```scala
val wordCountStream = env.addSource(...)
       .map((_,1))
       .keyBy(_._1)
       .sum(1)
```



![image-20220907084434583](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220907084434583.png)

### 检查点配置

```scala
val env =
StreamExecutionEnvironment.getExecutionEnvironment
// 启用检查点，间隔时间 1 秒
env.enableCheckpointing(1000)
CheckpointConfig checkpointConfig = env.getCheckpointConfig
// 设置精确一次模式 checkpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE) // 最小间隔时间 500 毫秒 checkpointConfig.setMinPauseBetweenCheckpoints(500)
// 超时时间 1 分钟
checkpointConfig.setCheckpointTimeout(60000) // 同时只能有一个检查点 checkpointConfig.setMaxConcurrentCheckpoints(1) // 开启检查点的外部持久化保存，作业取消后依然保留 checkpointConfig.enableExternalizedCheckpoints(
ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)
// 启用不对齐的检查点保存方式 checkpointConfig.enableUnalignedCheckpoints
// 设置检查点存储，可以直接传入一个 String，指定文件系统的路径 checkpointConfig.setCheckpointStorage("hdfs://my/checkpoint/dir")
```



