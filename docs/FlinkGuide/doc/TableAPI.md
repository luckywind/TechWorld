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
// 查询用户 Alice 的点击事件，并提取表中前两个字段 val aliceVisitTable = tableEnv.sqlQuery(
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
// 经过查询转换，得到结果表 val result = ...
// 将结果表写入已注册的输出表中 result.executeInsert("OutputTable")
```

### 表和流的转换

#### 表转流

- 调用 toDataStream()方法
  这种简单的结果不会变的查询可以直接输出

```scala
    val eventTable = tableEnv.fromDataStream(eventStream)
    // 用执行 SQL 的方式提取数据
    val visitTable = tableEnv.sqlQuery("select url, user from " + eventTable) // 将表转换成数据流，打印输出
     tableEnv.toDataStream(visitTable).print() // 执行程序
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

因为，group by user后，user只能有一条数据，不同时间统计的结果可能会变化，所以Sink操作不允许。要想输出，则可使用toChangelogStream

- 调用 toChangelogStream()方法

这种流的Sink操作不受限制

#### 流转表

- **调用 fromDataStream()方法**

```scala
//可选择提取流中部分字段
val eventTable2 = tableEnv.fromDataStream(eventStream, $("timestamp").as("ts"), $("url"))


// 将数据流转换成动态表，动态表只有一个字段，重命名为 myLong
val table = tableEnv.fromDataStream(stream, $("myLong"))
```

- **调用 createTemporaryView()方法**
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
   Row是更通用的数据类型，Row 类型还附加了一个属性 RowKind，用来表示当前行在更新操作中的类型。这样， Row 就可以用来表示更新日志流(changelog stream)中的数据，从而架起了 Flink 中流和表的 转换桥梁。所以在更新日志流中，元素的类型必须是 Row，而且需要调用 ofKind()方法来指定更新类 型

#### 动态表转流

**(1)仅追加(append-only)流**

**(2)撤回(retract)流**

撤回流是包含两类消息的流，添加(add)消息和撤回(retract)消息。

具体的编码规则是:INSERT 插入操作编码为 add 消息;DELETE 删除操作编码为 retract 消息;而 UPDATE 更新操作则编码为被更改行的 retract 消息，和更新后行(新行)的 add 消 息。这样，我们可以通过编码后的消息指明所有的增删改操作，一个动态表就可以转换为撤回 流了。

**(3)更新插入(upsert)流**

更新插入流中只包含两种类型的消息:更新插入(upsert)消息和删除(delete)消息。

对于更新插入流来说，INSERT插入操作和 UPDATE 更新操作，统一被编码为 upsert 消息

既然更新插入流中不区分插入(insert)和更新(update)，那我们自然会想到一个问题: 如果希望更新一行数据时，怎么保证最后做的操作不是插入呢?

## 流处理中的表

### 动态表

- 动态表： 随着新数据的加入，基于某个sql查询的到的表就会不断变化，称为动态表，是Flink Sql中的核心概念。

- 思想： 数据库中的表是一系列Insert/update/delete的更新日志流执行的结果，基于某一时刻的快照读取更新日志流就可以得到最终结果。

- 动态查询： 对动态表定义的查询操作，都是持续查询;而持续查询的结果也会是一个动态表。

### 流转动态表

流其实是一个insert-only的更新日志

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
// 将数据流转换成表
tableEnv.createTemporaryView("EventTable", eventStream, $("user"), $("url"), $("timestamp").as("ts"))
// 统计每个用户的点击次数
val urlCountTable = tableEnv.sqlQuery("SELECT user, COUNT(url) as cnt FROM EventTable GROUP BY user")
// 将表转换成数据流，在控制台打印输出 
tableEnv.toChangelogStream(urlCountTable).print("count")
// 执行程序 
env.execute()
```

### 持续查询

1. **更新查询**

随着新数据的到来，查询可能会得到新的数据，也可能会更新之前的查询结果，也就是说持续查询的到的更新日志流包含了insert和update两种操作，如果要转成流，就只能调用toChangelogStream()方法。

```scala
 val urlCountTable = tableEnv.sqlQuery("SELECT user, COUNT(url) as cnt FROM
EventTable GROUP BY user")
```

2. **追加查询**

查询只会追加结果，也就是一个insert流，这种查询称为追加查询，转换成 DataStream 调用方法 没有限制，可以直接用 toDataStream()，也可以像更新查询一样调用 toChangelogStream()。

```scala
val aliceVisitTable = tableEnv.sqlQuery("SELECT url, user FROM EventTable WHERE
user = 'Alice'")
```

### 动态表转流

动态表的持续更改需要编码后通知外部系统要进行的操作，Flink API和SQL支持三种编码方式:

1. 仅追加(append-only)流:  只有insert操作
2. 撤回(retract)流: 
   1. add： insert操作
   2. retract：delete;  update被编码为一个retract和一个add消息
3. 更新插入(upsert)流
   1. upsert
   2. delete 

## 时间属性和窗口

### 事件时间

事件时间属性可以在创建表 DDL 中定义，也可以在数据流和表的转换中定义。

1. DDL中定义

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

2. 数据流转为表时定义

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

### 窗口

使用窗口表值函数(结果是一个Table)来定义窗口：

⚫ 滚动窗口(Tumbling Windows);
 ⚫ 滑动窗口(Hop Windows，跳跃窗口);
 ⚫ 累积窗口(Cumulate Windows);
 ⚫ 会话窗口(Session Windows，目前尚未完全支持)。

返回值中，除去原始表中的所有列，还增加了用来描述窗口的额外 3 个列:“窗口起始点”(window_start)、“窗口结束点”(window_end)、“窗口时间”(window_time)

```sql
-- 滚动窗口
TUMBLE(TABLE EventTable, DESCRIPTOR(ts), INTERVAL '1' HOUR)
-- 滑动窗口
HOP(TABLE EventTable, DESCRIPTOR(ts), INTERVAL '5' MINUTES, INTERVAL '1' HOURS));
-- 累积窗口
CUMULATE(TABLE EventTable, DESCRIPTOR(ts), INTERVAL '1' HOURS, INTERVAL '1' DAYS))
```

## 聚合

GROUP BY 后面的则是窗口新增的字段 window_start 和 window_end

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

