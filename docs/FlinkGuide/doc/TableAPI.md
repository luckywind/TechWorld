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
    val visitTable = tableEnv.sqlQuery("select url, user from " + eventTable) // 将表转换成数据流，打印输出
    tableEnv.toDataStream(visitTable).print() // 执行程序
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

1. 连接器表

   最直观的创建表的方式，就是通过连接器(connector)连接到一个外部系统，然后定义出 对应的表结构。executeSql()方法执行一个DDL语句

   ```scala 
    tableEnv.executeSql("create [temporary] table MyTable ....  with ('connector'=...)")
   ```

   

2. 虚拟视图

```scala
val newTable = tableEnv.sqlQuery("SELECT ... FROM MyTable... ")
tableEnv.createTemporaryView("NewTable", newTable)
```

### 查询

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