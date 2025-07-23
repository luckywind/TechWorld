# Table /SQL

```xml
负责 Table API 和下层 DataStream API 的连接支持
<dependency>
   <groupId>org.apache.flink</groupId>
   <artifactId>flink-table-api-scala-bridge_${scala.binary.version}</artifactId>
   <version>${flink.version}</version>
</dependency>
IDE里需要：它是 Table API 的核心组件，负责提供 运行时环境，并生成程序的执行计划
<dependency>
   <groupId>org.apache.flink</groupId>
   <artifactId>flink-table-planner-blink_${scala.binary.version}</artifactId>
   <version>${flink.version}</version>
</dependency>
另外，如果想实现自定义的数据格式来做序列化，可以引入下面的依赖:
<dependency>
   <groupId>org.apache.flink</groupId>
<artifactId>flink-table-common</artifactId>
   <version>${flink.version}</version>
</dependency>
```

实例

```scala
   // 获取流执行环境
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    // 读取数据源
    val eventStream = env
      .fromElements(
        Event("Alice", "./home", 1000L),
        Event("Bob", "./cart", 1000L),
        Event("Alice", "./prod?id=1", 5 * 1000L),
        Event("Cary", "./home", 60 * 1000L),
        Event("Bob", "./prod?id=3", 90 * 1000L),
        Event("Alice", "./prod?id=7", 105 * 1000L)
      )
    // 获取表环境
    val tableEnv = StreamTableEnvironment.create(env)
    // 将数据流转换成表
    val eventTable = tableEnv.fromDataStream(eventStream)
    // 用执行 SQL 的方式提取数据
    val visitTable = tableEnv.sqlQuery("select url, user from " + eventTable) 
// 将表转换成数据流，打印输出
    tableEnv.toDataStream(visitTable).print() 
// 执行程序
    env.execute()

+I[./home, Alice]
+I[./cart, Bob]
+I[./prod?id=1, Alice]
+I[./home, Cary]
+I[./prod?id=3, Bob]
+I[./prod?id=7, Alice]

 // 用 Table API 方式提取数据
val clickTable2 = eventTable.select($("url"), $("user"))
// 这里的$符号是 Table API 中定义的“表达式”类 Expressions 中的一个静态方法，传入一 个字段名称，就可以指代数据中对应字段，这个方法需要使用如下的方式进行手动导入。
import org.apache.flink.table.api.Expressions.$
```

## 程序架构

```scala
// 创建表环境
val tableEnv = ...;
// 创建输入表，连接外部系统读取数据
tableEnv.executeSql("CREATE TEMPORARY TABLE inputTable ... WITH ( 'connector' = ... )")
// 注册一个表，连接到外部系统，用于输出
tableEnv.executeSql("CREATE TEMPORARY TABLE outputTable ... WITH ( 'connector' = ... )")

// 执行 SQL 对表进行查询转换，得到一个新的表
val table1 = tableEnv.sqlQuery("SELECT ... FROM inputTable... ")
// 使用 Table API 对表进行查询转换，得到一个新的表
val table2 = tableEnv.from("inputTable").select(...)

// 将得到的结果写入输出表
val tableResult = table1.executeInsert("outputTable")
```

## 基本API

### 流式表环境

```scala
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment
val env = StreamExecutionEnvironment.getExecutionEnvironment
val tableEnv = StreamTableEnvironment.create(env)
```

### 创建表

1. **连接器表**

   最直观的创建表的方式，就是通过连接器(connector)连接到一个外部系统，然后定义出 对应的表结构。executeSql()方法执行一个DDL语句

   ```scala 
    tableEnv.executeSql("create [temporary] table MyTable ....  with ('connector'=...)")
   ```

   

2. **虚拟视图**

```scala
val newTable = tableEnv.sqlQuery("SELECT ... FROM MyTable... ")
tableEnv.createTemporaryView("NewTable", newTable)
```

### 查询

#### 执行sql进行查询

```scala
// 创建表环境
val tableEnv = ...
// 创建表
tableEnv.executeSql("CREATE TABLE EventTable ... WITH ( 'connector' = ... )")
// 查询用户 Alice 的点击事件，并提取表中前两个字段 
val aliceVisitTable = tableEnv.sqlQuery(
   "SELECT user, url " +
   "FROM EventTable " +
   "WHERE user = 'Alice' "
)
```

#### 调用Table API查询

```scala
//先获取table对象
val eventTable = tableEnv.from("EventTable")
//调用api
val maryClickTable = eventTable
       .where($("user").isEqual("Alice"))
       .select($("url"), $("user"))
```



### 输出表

executeInsert()方法

```scala
// 注册表，用于输出数据到外部系统
tableEnv.executeSql("CREATE TABLE OutputTable ... WITH ( 'connector' = ... )")
// 经过查询转换，得到结果表 
val result = ...
// 将结果表写入已注册的输出表中 
result.executeInsert("OutputTable")
```

### 表和流的转换

#### 表转流

- <font color=red>调用 toDataStream()方法</font>
  这种简单的结果不会变的查询可以直接输出

```scala
    val eventTable = tableEnv.fromDataStream(eventStream)//流转表
    // 用执行 SQL 的方式提取数据
    val visitTable = tableEnv.sqlQuery("select url, user from " + eventTable) //表
     tableEnv.toDataStream(visitTable).print() // 表转流
```

但是如果，查询结果会随时间变化，则不支持输出：

```scala
    val cntTable=tableEnv.sqlQuery(
      s"""
        |select user,count(1) cnt from ${eventTable} group by user
        |""".stripMargin)

    tableEnv.toDataStream(cntTable)
        .print()
```

因为，group by user后，user只能有一条数据，不同时间统计的结果可能会变化，所以Sink操作（print也是一个Sink操作）不允许。要想输出，则可使用toChangelogStream

- <font color=red>有更新的表调用 toChangelogStream()方法进行Sink</font>

对于有更新操作的表， 记录它的"更新日志"，这样所有的更新操作就变成了一条更新日志的流，这样就可以转换成流打印输出了，  这种流的Sink操作不受限制

#### 流转表

- <font color=red>**调用 fromDataStream()方法**</font>

```scala
//可选择提取流中部分字段
val eventTable2 = tableEnv.fromDataStream(eventStream, $("timestamp").as("ts"), $("url"))


// 将数据流转换成动态表，动态表只有一个字段，重命名为 myLong
val table = tableEnv.fromDataStream(stream, $("myLong"))
```

- <font color=red>**调用 createTemporaryView()方法**</font>
  这种好处是可在sql中直接使用表名

```scala
 tableEnv.createTemporaryView("EventTable", eventStream,
$("timestamp").as("ts"),$("url"));
```

- **调用 fromChangelogStream ()方法**

可以将一个更新日志流转换成表。这

个方法要求流中的数据类型只能是 Row，而且每一个数据都需要指定当前行的更新类型 (RowKind);所以一般是由连接器帮我们实现的，直接应用比较少见

#### 支持的数据类型

1. 原生类型
2. tuple
3. case class
4. Row
   <font color=red>Row是更通用的数据类型，Row 类型还附加了一个属性 RowKind，用来表示当前行在更新操作中的类型。这样， Row 就可以用来表示更新日志流(changelog stream)中的数据，从而架起了 Flink 中流和表的 转换桥梁。所以在更新日志流中，元素的类型必须是 Row，而且需要调用 ofKind()方法来指定更新类 型</font>

#### 动态表转流

**(1)仅追加(append-only)流**

**(2)撤回(retract)流**

撤回流是包含两类消息的流，添加(add)消息和撤回(retract)消息。

具体的编码规则是:INSERT 插入操作编码为 add 消息;DELETE 删除操作编码为 retract 消息;而 UPDATE 更新操作则编码为被更改行的 retract 消息，和更新后行(新行)的 add 消 息。这样，我们可以通过编码后的消息指明所有的增删改操作，一个动态表就可以转换为撤回 流了。

![image-20250723100947199](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250723100947199.png)

**(3)更新插入(upsert)流**

更新插入流中只包含两种类型的消息:更新插入(upsert)消息和删除(delete)消息。

对于更新插入流来说，INSERT插入操作和 UPDATE 更新操作，统一被编码为 upsert 消息

既然更新插入流中不区分插入(insert)和更新(update)，那我们自然会想到一个问题: 如果希望更新一行数据时，怎么保证最后做的操作不是插入呢?

## 流处理中的表

### 动态表和持续查询

- **动态表**： 随着新数据的加入，基于某个sql查询的到的表就会不断变化，称为动态表，是Flink Sql中的核心概念。

  思想： 数据库中的表是一系列Insert/update/delete的更新日志流执行的结果，基于某一时刻的快照读取更新日志流就可以得到最终结果。

- **持续查询**： 对动态表定义的查询操作，都是持续查询;而持续查询的结果也会是一个动态表。



![image-20230102164912099](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230102164912099.png)

​       动态表可以像静态的批处理表一样进行查询操作。由于数据在不断变化，因此基于它定义 的 SQL 查询也不可能执行一次就得到最终结果。这样一来，我们对动态表的查询也就永远不 会停止，<font color=red>一直在随着新数据的到来而继续执行</font>。这样的查询就被称作“持续查询”(Continuous Query)。对动态表定义的查询操作，都是持续查询;而持续查询的结果也会是一个动态表。

持续查询的步骤如下:
 (1)流(stream)被转换为动态表(dynamic table); 

(2)对动态表进行持续查询(continuous query)，生成新的动态表; 

(3)生成的动态表被转换成流。

<font color=red>这样，只要 API 将流和动态表的转换封装起来，我们就可以直接在数据流上执行 SQL 查询，用处理表的方式来做流处理了。</font>



### 流转动态表

流其实是一个insert-only的更新日志, 我们要做的就是把一个只有插入操作的更新日志构建一个表

```scala
// 获取流环境
val env = StreamExecutionEnvironment.getExecutionEnvironment env.setParallelism(1)
// 读取数据源
val eventStream = env
             .fromElements(
                   Event("Alice", "./home", 1000L),
                    Event("Bob", "./cart", 1000L),
                    Event("Alice", "./prod?id=1", 5 * 1000L),
                    Event("Cary", "./home", 60 * 1000L),
                    Event("Bob", "./prod?id=3", 90 * 1000L),
                    Event("Alice", "./prod?id=7", 105 * 1000L)
)
// 获取表环境
val tableEnv = StreamTableEnvironment.create(env)
// 将数据流转换成动态表
tableEnv.createTemporaryView("EventTable", eventStream, $("user"), $("url"), $("timestamp").as("ts"))
// 统计每个用户的点击次数
val urlCountTable = tableEnv.sqlQuery("SELECT user, COUNT(url) as cnt FROM EventTable GROUP BY user")
// 将表转换成数据流，在控制台打印输出 
tableEnv.toChangelogStream(urlCountTable).print("count")
// 执行程序 
env.execute()
```

### 用SQL持续查询

1. **更新查询**

随着新数据的到来，查询可能会得到新的数据，也可能会更新之前的查询结果，也就是说持续查询的更新日志流包含了insert和update两种操作，如果要转成流，就只能调用toChangelogStream()方法。

```scala
 val urlCountTable = tableEnv.sqlQuery("SELECT user, COUNT(url) as cnt FROM
EventTable GROUP BY user")
```

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230102170112673.png" alt="image-20230102170112673" style="zoom: 50%;" />

具体步骤解释如下:
 (1)当查询启动时，原始动态表 EventTable 为空;
 (2)当第一行 Alice 的点击数据插入 EventTable 表时，查询开始计算结果表，urlCountTable中插入一行数据[Alice，1]。
 (3)当第二行 Bob 点击数据插入 EventTable 表时，查询将更新结果表并插入新行[Bob，1]。
 (4)第三行数据到来，同样是 Alice 的点击事件，这时不会插入新行，而是生成一个针对已有行的更新操作。这样，结果表中第一行[Alice，1]就更新为[Alice，2]。 (5)当第四行 Cary 的点击数据插入到 EventTable 表时，查询将第三行[Cary，1]插入到结果表中。

2. **追加查询**

​       查询只会追加结果，也就是一个insert流，这种查询称为**追加查询**，转换成 DataStream 调用方法 没有限制，可以直接用 toDataStream()，也可以像更新查询一样调用 toChangelogStream()。

典型的就是窗口聚合，窗口结束时间作为一个聚合维度，那么查询结果就只有追加， 没有更新。

```scala
val aliceVisitTable = tableEnv.sqlQuery("SELECT url, user FROM EventTable WHERE
user = 'Alice'")
```

```scala
   val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    // 读取数据源，并分配时间戳、生成水位线
    val eventStream = env
      .fromElements(
        Event("Alice", "./home", 1000L),
        Event("Bob", "./cart", 1000L),
        Event("Alice", "./prod?id=1", 25 * 60 * 1000L),
        Event("Alice", "./prod?id=4", 55 * 60 * 1000L),
        Event("Bob", "./prod?id=5", 3600 * 1000L + 60 * 1000L),
        Event("Cary", "./home", 3600 * 1000L + 30 * 60 * 1000L),
        Event("Cary", "./prod?id=7", 3600 * 1000L + 59 * 60 * 1000L)
      )
      .assignAscendingTimestamps(_.timestamp) // 创建表环境
    val tableEnv = StreamTableEnvironment.create(env) // 将数据流转换成表，并指定时间属性
    val eventTable = tableEnv.fromDataStream(
      eventStream,
      $("user"),
      $("url"), $("timestamp").rowtime().as("ts")
      // 将 timestamp 指定为事件时间，并命名为 ts
    )
    // 为方便在 SQL 中引用，在环境中注册表 EventTable
    tableEnv.createTemporaryView("EventTable", eventTable); // 设置 1 小时滚动窗口，执行 SQL 统计查询
    val result = tableEnv
      .sqlQuery(
        "SELECT " +
          "user, " +
          "window_end AS endT, " + // 窗口结束时间
           "COUNT(url) AS cnt " + // 统计 url 访问次数
           "FROM TABLE( " +
          "TUMBLE( TABLE EventTable, " + // 1 小时滚动窗口
           "DESCRIPTOR(ts), " +
          "INTERVAL '1' HOUR)) " +
          "GROUP BY user, window_start, window_end "
      )
    tableEnv.toDataStream(result).print()
    env.execute()
输出
+I[Alice, 1970-01-01T01:00, 3]
+I[Bob, 1970-01-01T01:00, 1]
+I[Bob, 1970-01-01T02:00, 1]
+I[Cary, 1970-01-01T02:00, 2]
```



3. 查询限制

实际应用中，一些持续查询会因为代价太高而受到限制：

- 状态太大
- 新数据带来大量更新

### 动态表转流

**与关系型数据库中的表一样**，动态表也可以通过插入(insert)、更新(update)和删除(delete) 操作，进行持续的更改。

动态表的持续更改需要编码后通知外部系统要进行的操作，Flink API和SQL支持三种编码方式:

1. 仅追加(append-only)流:  只有insert操作

2. 撤回(retract)流: 
   1. add： insert操作
   2. retract：delete;  update被编码为一个retract和一个add消息
   
3. 更新插入(upsert)流
   1. upsert： INSERT插入操作和 UPDATE 更新操作，统一被编码为 upsert 消息
   2. delete 
   
   > 更新插入流不区分更新和插入，所以动态表中必须有唯一的键，来确定是更新还是插入。

## 时间属性和窗口

谓的时间属性(time attributes)，其实就是每个表模式结构(schema)的一部分。 它可以在创建表的 DDL 里直接定义为一个字段，也可以在流转换成表时定义。一旦定义了时 间属性，它就可以作为一个普通字段引用，并且可以在基于时间的操作中使用。

- 时间属性的数据类型为 TIMESTAMP，它的行为类似于常规时间戳，可以直接访问并且进 行计算。
- 窗口表值函数滚动窗口(TUMBLE)、滑动窗口(HOP)、累积窗口(CUMULATE) 额外返回三个列：
  - window_start
  - window_end
  - window_time=window_end - 1ms :  相当于窗口的最大时间戳
- 

### 事件时间

事件时间属性可以在创建表 DDL 中定义，也可以在数据流和表的转换中定义。

1. **DDL中定义**
   通过WATERMARK语句定义水位线生成表达式

```sql
CREATE TABLE EventTable(
  user STRING,
  url STRING,
  ts TIMESTAMP(3),
  --  ts 字段定义为事件时间属性，而且基于 ts 设置了 5 秒的水位线延迟
  WATERMARK FOR ts AS ts - INTERVAL '5' SECOND
) WITH ( ...
);
```

Flink 中支持的事件时间属性数据类型必须为 TIMESTAMP 或者 TIMESTAMP_LTZ

```sql
CREATE TABLE events (
user STRING,
  url STRING,
  ts BIGINT,
  ts_ltz AS TO_TIMESTAMP_LTZ(ts, 3),-- 把毫秒数转成TIMESTAMP_LTZ
  WATERMARK FOR ts_ltz AS time_ltz - INTERVAL '5' SECOND
) WITH ( ...
);
```

2. **数据流转为表时定义**

```scala
// 方法一:
// 流中数据类型为二元组 Tuple2，包含两个字段;需要自定义提取时间戳并生成水位线 
val stream = inputStream.assignTimestampsAndWatermarks(...)
// 声明一个额外的逻辑字段作为事件时间属性
val table = tEnv.fromDataStream(stream, $("user"), $("url"), $("ts").rowtime())
// 方法二:
// 流中数据类型为三元组 Tuple3，最后一个字段就是事件时间戳
val stream = inputStream.assignTimestampsAndWatermarks(...)
// 不再声明额外字段，直接用最后一个字段作为事件时间属性
val table = tEnv.fromDataStream(stream, $("user"), $("url"), $("ts").rowtime())
```

### 处理时间

相比之下处理时间就比较简单了，它就是我们的系统时间，使用时不需要提取时间戳 (timestamp)和生成水位线(watermark)。因此在定义处理时间属性时，必须要额外声明一个 字段，专门用来保存当前的处理时间。

1. 在创建表的DDL中定义
   <font color=red>通过PROCTIME()函数指定当前的处理时间属性。</font>

```sql
CREATE TABLE EventTable(
  user STRING,
  url STRING,
  ts AS PROCTIME()
) WITH ( ...
);
```

2. 在数据流转换为表时定义

```sql
val stream = ...
// 声明一个额外的字段作为处理时间属性字段
val table = tEnv.fromDataStream(stream, $("user"), $("url"), $("ts").proctime())
```







### 窗口

有了时间属性，接下来就可以定义窗口进行计算了。我们知道，窗口可以将无界流切割成 大小有限的“桶”(bucket)来做计算，通过截取有限数据集来处理无限的流数据。

1. 分组窗口（老版本）

   > 分组窗口的功能比较有限，只支持窗口聚合，所以目前已经处于弃用(deprecated)的状 态。

   <font color=red>TUMBLE()、HOP()、SESSION()，传入时间属性字段、窗口大小等参数就可以了</font>。以滚动窗口为例:

   ```sql
   TUMBLE(ts, INTERVAL '1' HOUR)
   ```

   在进行窗口计算时，分组窗口是将窗口本身当作一个字段对数据进行分组的，可以对组内 的数据进行聚合。基本使用方式如下:

   ```scala
   val result = tableEnv.sqlQuery(
                     "SELECT " +
                         "user, " +
                         "TUMBLE_END(ts, INTERVAL '1' HOUR) as endT, " +
                         "COUNT(url) AS cnt " +
   "FROM EventTable " +
   "GROUP BY " + // 使用窗口和用户名进行分组
   "user, " +
   "TUMBLE(ts, INTERVAL '1' HOUR)" // 定义 1 小时滚动窗口
    ) 
   ```

   

2. 窗口表值函数(新版本)

使用窗口表值函数(结果是一个Table)来定义窗口：

⚫ 滚动窗口(Tumbling Windows);
 ⚫ 滑动窗口(Hop Windows，跳跃窗口);
 ⚫ 累积窗口(Cumulate Windows);

> 与滑动窗口不同的是，在一个统计周期内，我们会多次输出统计值，它们应该是不断叠 加累积的。

 ⚫ 会话窗口(Session Windows，目前尚未完全支持)。

<font color=red>返回值中，除去原始表中的所有列，还增加了用来描述窗口的额外 3 个列:“窗口起始点”(window_start)、“窗口结束点”(window_end)、“窗口时间”(window_time)</font>

```sql
-- 滚动窗口
TUMBLE(TABLE EventTable, DESCRIPTOR(ts), INTERVAL '1' HOUR)
-- 滑动窗口
HOP(TABLE EventTable, DESCRIPTOR(ts), INTERVAL '5' MINUTES, INTERVAL '1' HOURS));
		 参数依次为：     表、 时间、步长、窗口大小
-- 累积窗口
CUMULATE(TABLE EventTable, DESCRIPTOR(ts), INTERVAL '1' HOURS, INTERVAL '1' DAYS))
		 参数依次为：     表、 时间、累积步长、统计周期
```



例如一个累积窗口：

```sql
SELECT
 uid, window_end AS endT, COUNT(url) AS cnt
FROM TABLE(
  CUMULATE(
    TABLE eventTable,
    DESCRIPTOR(et),
    INTERVAL '30' MINUTE,
    INTERVAL '1' HOUR
  )
)
GROUP BY uid, window_start, window_end
```





## 聚合

Flink 中的 SQL 是流处理与标准 SQL 结合的产物，所以聚合查询也可以分成两种

### 分组聚合

就是普通的SQL聚合

### 窗口聚合

窗口本身返回的是就是一张表，所以窗口会出现在 FROM 后面，GROUP BY 后面的则是窗口新增的字段 window_start 和 window_end

```sql
val result = tableEnv.sqlQuery(
                    "SELECT " +
                       "user, " +
                       "window_end AS endT, " +
                       "COUNT(url) AS cnt " +
                    "FROM TABLE( " +

        "TUMBLE( TABLE EventTable, " +
        "DESCRIPTOR(ts), " +
        "INTERVAL '1' HOUR)) " +
"GROUP BY user, window_start, window_end "
)
```

## 函数

### udf

```sql
-- 注册函数
tableEnv.createTemporarySystemFunction("MyFunction", classOf[MyFunction])
-- 使用
tableEnv.from("MyTable").select(call("MyFunction", $("myField")))
tableEnv.sqlQuery("SELECT MyFunction(myField) FROM MyTable")
 
```

## 连接到外部系统

到控制台

```sql
CREATE TABLE ResultTable (
   user STRING,
cnt BIGINT
WITH (
   'connector' = 'print'
);
```



到文件系统

```sql
CREATE TABLE MyTable (
  column_name1 INT,
  column_name2 STRING,
  ...
  part_name1 INT,
  part_name2 STRING
) PARTITIONED BY (part_name1, part_name2) WITH (
'connector' = 'filesystem', -- 连接器类型 'path' = '...', -- 文件路径
'format' = '...' -- 文件格式
)
```

### hive

Flink 与 Hive 的集成比较特别。Flink 提供了“Hive 目录”(HiveCatalog)功能，允许使用 Hive 的“元存储”(Metastore)来管理 Flink 的元数据

注意只有 Blink 的计划器(planner)提供了 Hive 集成的支持， 所以需要在使用 Flink SQL 时选择 Blink planner。

1. 连接hive

```scala
val settings = EnvironmentSettings.newInstance.useBlinkPlanner.build()
val tableEnv = TableEnvironment.create(settings)
val name = "myhive"
val defaultDatabase = "mydatabase" 
val hiveConfDir = "/opt/hive-conf"
// 创建一个 HiveCatalog，并在表环境中注册
val hive = new HiveCatalog(name, defaultDatabase, hiveConfDir) 
tableEnv.registerCatalog("myhive", hive)
// 使用 HiveCatalog 作为当前会话的 catalog 
tableEnv.useCatalog("myhive")
```

也可以在sql客户端连接

```sql
Flink SQL> create catalog myhive with ('type' = 'hive', 'hive-conf-dir' =
'/opt/hive-conf');
[INFO] Execute statement succeed.
Flink SQL> use catalog myhive;
[INFO] Execute statement succeed.
```

### 读写hive表

```sql
-- 设置 SQL 方言为 hive，创建 Hive 表 
SET table.sql-dialect=hive; 
CREATE TABLE hive_table (
  user_id STRING,
  order_amount DOUBLE
) PARTITIONED BY (dt STRING, hr STRING) STORED AS parquet TBLPROPERTIES (
  'partition.time-extractor.timestamp-pattern'='$dt $hr:00:00',
  'sink.partition-commit.trigger'='partition-time',
  'sink.partition-commit.delay'='1 h',
  'sink.partition-commit.policy.kind'='metastore,success-file'
);
-- 设置 SQL 方言为 default，创建 Kafka 表 
SET table.sql-dialect=default; 
CREATE TABLE kafka_table (
  user_id STRING,
  order_amount DOUBLE,
  log_ts TIMESTAMP(3),
  WATERMARK FOR log_ts AS log_ts - INTERVAL '5' SECOND
) WITH (...);
– 定义水位线
-- 将 Kafka 中读取的数据经转换后写入 Hive
INSERT INTO TABLE hive_table
SELECT user_id, order_amount, DATE_FORMAT(log_ts, 'yyyy-MM-dd'),
 DATE_FORMAT(log_ts, 'HH')
FROM kafka_table;
```

