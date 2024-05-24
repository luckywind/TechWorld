

## 启用和配置Checkpointing

Flink默认不启用Checkpointing。如果要启用，可以在StreamExecutionEnvironment上调用enableCheckpointing(n)，其中n是以毫秒为单位的checkpoint间隔。

还有其他一些参数：

- exactly-once vs at-least-once：在enableCheckpointing(n)中可以传递模式，对于大多数应用可能exactly-once适合，但对于延迟要求在毫秒级别的，或许也可以设置为at-least-once。
- checkpoint timeout：如果超过这个时间checkpoint还没结束，就会被认为是失败的。
- minimum time between checkpoints：规定在两次checkpoints之间的最小时间是为了流应用可以在此期间有明显的处理进度。比如这个值被设置为5秒，则在上一次checkpoint结束5秒之内不会有新的checkpoint被触发。这也通常意味着checkpoint interval的值会比这个值要大。为什么要设置这个值？因为checkpiont interval有时候会不可靠，比如当文件系统反应比较慢的时候，checkpiont花费的时间可能就比预想的要多，这样仅仅只有checkpoint interval的话就会重叠。记住，设置minimum time between checkpoints也要求checkpoints的并发度是1。
- number of concurrent checkpoints：默认，Flink在有一个checkpoint在执行的时候不会触发另一次checkpoint。但如果非要做，比如对于处理有延迟的流水线操作而言，又希望能够高频的进行checkpoint，则可以更改这个值。如果设置了minimum time between checkpoints，就不要设置这个值。
- externalized checkpoints：externalized checkpoints将元数据也会写入持久化存储，并且在作业失败的时候不会自动清除数据。这样，你就获得了作业失败之后的一个恢复点。
- fail/continue task on checkpoint errors：这个值规定当某次checkpoint执行失败的时候，task是否要被认为是执行失败。Flink默认checkpoint失败则task处理失败。但是你可以改，如果改了，那么checkpoint失败的时候，task还会继续运行，只是会告诉checkpoint协调器这次checkpoint失败了。



TaskManager内存

![image-20211228145817840](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20211228145817840.png)



# 接口

## 转换操作

Map

> 对于Scala Option，Scala Case Class和Java/Scala Tuple类型，map的返回值不能是null；
>
> - Option类型可以返回None；
> - Case Class和Tuple，可以设置变量为null，比如new Tuple2(null, null);

KeyBy

> DataStream → KeyedStream，逻辑地将一个流拆分成不相交的分区，每个分区包含具有相同Key的元素. 在内部是以hash的形式实现的。 注意，以下两种类型不能作为Key：
>
> - POJO类型但是没有重写 hashCode 方法；
> - 数组类型；

```scala
dataStream.keyBy("someKey") // 如果dataStream的数据类型为POJO，可以通过这种方式，直接指定POJO的某个字段；
dataStream.keyBy(0) // 如果dataStream的数据类型为Tuple，可以通过这种方式指定第几个元素作为key；

// 如果是其他类型，可以使用KeySelector，推荐这种方式；
public class WC {public String word; public int count;}
DataStream<WC> words = // [...]
KeyedStream<WC> keyed = words
  .keyBy(new KeySelector<WC, String>() {
     public String getKey(WC wc) { return wc.word; }
   });
```

Window

> KeyedStream → WindowedStream，Windows 是在一个分区的 KeyedStreams中定义的. Windows 根据某些特性将每个key的数据进行分组 (例如:在5秒内到达的数据)

Window Reduce

> 在 WindowedStream执行reduce操作；

```scala
windowedStream.reduce (new ReduceFunction<Tuple2<String,Integer>>() {
    public Tuple2<String, Integer> reduce(Tuple2<String, Integer> value1, Tuple2<String, Integer> value2) throws Exception {
        return new Tuple2<String,Integer>(value1.f0, value1.f1 + value2.f1);
    }
});
```

## checkpoint

### 开启checkpoint

```scala
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
CheckpointConfig config = env.getCheckpointConfig();
config.setCheckpointInterval(5 * 60 * 1000); // Checkpoint的触发频率；
config.setMinPauseBetweenCheckpoints(5 * 60 * 1000); // Checkpoint之间的最小间隔；
config.setCheckpointTimeout(10 * 60 * 1000); // Checkpoint的超时时间；
config.setTolerableCheckpointFailureNumber(3); // 连续3次checkpoint失败，才会导致作业失败重启；默认值是0 。
config.enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION); // Cancel Job之后保留Checkpoint文件；
```



### 指定UID

Operator在恢复状态的时候(只需要给有状态的Operator指定UID)，是通过“UID”来判断要恢复的状态的，即UID和状态唯一绑定。如果不手动指定**UID**，那么修改代码后**UID**可能发生变化，导致状态无法正常恢复。

### **State Backend**

推荐使用RocksDB。

1. filesystem

- 当前状态数据会被保存在TaskManager的内存中（容易出现OOM问题）；
- 优势是状态存取效率比较高；

2. rocksdb

- RocksDB 是一种嵌入式的本地数据库，当前状态数据会被保存在TaskManager的本地磁盘上；（不容易出现内存问题）
- 状态存取效率比filesystem稍微低一些；

当使用 RocksDB 时，状态大小只受限于磁盘可用空间的大小。这也使得 RocksDBStateBackend 成为管理超大状态的最佳选择。

### checkpoint目录

有两种方式配置Checkpoint目录：

- 【框架参数】：

```
  state.backend=rocksdb
  state.checkpoints.dir=hdfs://c3prc-hadoop/user/xxxx/flink/flink-job-name
```

- 在代码中配置：优先级更高

```
  streamExecutionEnvironment.setStateBackend(new FsStateBackend("hdfs://c3prc-hadoop/user/xxxx/flink/flink-job-name"))
```

-  其他checkpoint配置

```
execution.checkpointing.interval=3min

execution.checkpointing.min-pause=3min

execution.checkpointing.timeout=10min
```

### 优化

1. 如果作业状态量比较小（每次checkpoint时单个task的状态量小于2G，如下图）：

此时建议关闭增量checkpoint:   state.backend.incremental=false（默认为true，开启了增量）

2. 如果状态量比较大（checkpoint单个task的状态量大于2G，如下图）

此时建议配置state.backend.checkpoint.stream-concat-enabled=true 来开启小文件优化

*注意：该配置开启后不要再去掉，否则无法从原来的checkpoint恢复*



## Flink State

### Keyed State

• ValueState 存储单个值，比如 Wordcount，用 Word 当 Key，State 就是它的 Count。这里面的单个值可能是数值或者字符串，作为单个值，访问接口可能有两种，get 和 set。在

State 上体现的是 update(T) / T value()。



• MapState 的状态数据类型是 Map，在 State 上有 put、remove等。需要注意的是在 MapState 中的 key 和 Keyed state 中的 key 不是同一个。



• ListState 状态数据类型是 List，访问接口如 add、update 等。



• ReducingState 和 AggregatingState 与 ListState 都是同一个父类，但状态数据类型上是单个值，原因在于其中的 add 方法不是把当前的元素追加到列表中，而是把当前元素直接更新进了 Reducing 的结果中。



• AggregatingState 的区别是在访问接口，ReducingState 中 add（T）和 T get() 进去和出来的元素都是同一个类型，但在 AggregatingState 输入的 IN，输出的是 OUT。

### 状态清理

如果状态不断累计的话，势必会造成内存和效率问题，所以状态的正确清理非常重要。可以在RichXXXFunction中通过Timer定期清理State，也可以使用Flink提供的TTL State。

## Flink Time

Process Time 是通过直接去调用本地机器的时间，而 Event Time 则是根据每一条处理记录所携带的时间戳来判定。

### watermark

Event Time 因为是绑定在每一条的记录上的，由于网络延迟、程序内部逻辑、或者其他一些分布式系统的原因，数据的时间可能会存在一定程度的乱序,解决办法是在整个时间序列里插入一些类似于标志位的一些**特殊的处理数据**，这些特殊的处理数据叫做watermark。一个 watermark 本质上就代表了这个 watermark 所包含的 timestamp 数值，表示以后到来的数据已经再也没有小于或等于这个时间的了。

### Timer Service

Timer Service可以基于Processing Time或者Event Time设置定时器，可以用于消息的延迟处理／状态的定时清理等操作。

## Flink窗口

### 窗口分类

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/(null)-20211228161646979.(null))

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/(null)-20211228161642720.(null))

Flink支持如下窗口计算：



• tumbling window(窗口间的元素无重复）

• sliding window（窗口间的元素可能重复）

• session window

• global window 其中最常用的是tumbling window 和 sliding window。

### 窗口计算

• ReduceFunction（两个输入消息计算得到一个输出消息）

• AggregateFunction（输入消息不断更新Accumulator，两个Accumulator可以合并，Accumulator

计算得到输出）

• FoldFunction（特例化的AggregateFunction，Accumulator和输出类型相同）

• ProcessWindowFunction（不建议使用！会占用较多内存）

## 数据序列化

目前 Java 生态圈提供了众多的序列化框架：Java serialization, Kryo, Apache Avro 等等。但是

**Flink 实现了自己的序列化框架。因为在 Flink 中处理的数据流通常是同一类型，由于数据集对象的类型固定，对于数据集可以只保存一份对象 Schema 信息，节省大量的存储空间。同时，对于固定大小的类型，也可通过固定的偏移位置存取。当我们需要访问某个对象成员变量的时候，通过定制的序列化工具，并不需要反序列化整个 Java 对象，而是可以直接通过偏移量，只是反序列化特定的对象成员变量。如果对象的成员变量较多时，能够大大减少 Java 对象的创建开销，以及内存数据的拷贝大小。**

![](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/-20211228163014355-20211228163030155)



## 最佳实践

### 四个步骤到生产环境

1. 明确定义 **Flink** 算子的最大并发度

可以通过 setMaxParallelism(int maxParallelism) 来手动地设定作业或具体算子的最大并发。



任何进入生产的作业都应该指定最大并发数。但是，一定要仔细考虑后再决定该值的大小。因为一旦设置了最大并发度（无论是手动设置，还是默认设置），之后就无法再对该值做更新。



最大并发度的取值建议设定一个足够高的值以满足应用未来的可扩展性和可用性，同时，又要选一个相对较低的值以避免影响应用程序整体的性能。



1. 为 **Flink** 算子指定唯一用户**ID**（**UUID**）

Flink 算子的 UUID 可以通过 uid(String uid) 方法指定。



算子 UUID 使得 Flink 有效地将算子的状态从 savepoint 映射到作业修改后（拓扑图可能也有改变）的正确的算子上



建议每个算子都指定上 UUID。



1.  充分考虑 **Flink** 程序的状态后端

对于生产用例来说，强烈建议使用 **RocksDB** 状态后端，因为这是目前唯一一种支持大型状态和异步操作（如快照过程）的状态后端，异步操作能使 Flink 不阻塞正常数据流的处理的情况下做快照操作。



1. 配置 **JobManager** 的高可用性（**HA**）

说明：由于HA依赖较高版本Zookeeper，目前公司内部Flink暂未开启HA，但是开启了Yarn Application 自动重试；当JobManager发生异常后，Yarn会自动重新拉起，Flink作业依然可以从之前的状态快速恢复。

### batch输出

我们可以通过Window来实现这一需求，Window自带的State，可以很好地实现缓存数据功能，并且状态的维护清理不需要用户操心 用户需要关心的主要是两个点：

1. 如何缓存一定数量之后触发发送；

2. 如何延迟一定时间之后触发发送；



可以通过自定义Trigger，实现根据消息的的数量以及延迟来确定发送时机
