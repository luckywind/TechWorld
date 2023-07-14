原文链接：https://blog.csdn.net/misayaaaaa/article/details/123245066

[参考链接](https://zhuanlan.zhihu.com/p/336693158)

[Understanding Spark’s Logical and Physical Plan in layman’s term](https://blog.knoldus.com/understanding-sparks-logical-and-physical-plan-in-laymans-term/)



# 使用

从3.0开始，explain方法有一个新的参数mode，该参数可以指定以什么样的格式展示执行计划：

- explain(mode=”simple”)：只展示物理执行计划。
- explain(mode=”extended”)：展示物理执行计划和逻辑执行计划。
- explain(mode=”codegen”) ：展示要Codegen生成的可执行Java代码。
- <font color=red>explain(mode=”cost”)：展示优化后的逻辑执行计划以及相关的统计。</font>
- explain(mode=”formatted”)：以分隔的方式输出，它会输出更易读的物理执行计划，并展示每个节点的详细信息。

# 阅读

```sql
val testsql =
    """
      |with t as (
      |select concat(uid,'_dddd')uid
      |from userTable
      |
      |)
      |select t1.uid
      |from
      |   (
      |   select * from t where uid >'label_1'
      |   )as t1
      |   left join
      |   (
      |   select * from t where uid='label_1'
      |   )as t2 on t1.uid = t2.uid
      |
    """.stripMargin
    
  val df = sql(s"$testsql")
  df.explain(true)
```

物理计划：

```oxygene
== Physical Plan ==
*(5) Project [uid#50]
+- SortMergeJoin [uid#50], [uid#54], LeftOuter
   :- *(2) Sort [uid#50 ASC NULLS FIRST], false, 0
   :  +- Exchange hashpartitioning(uid#50, 5)
   :     +- *(1) Project [concat(_1#14, _dddd) AS uid#50]
   :        +- *(1) Filter (concat(_1#14, _dddd) > label_1)
   :           +- LocalTableScan [_1#14, _2#15, _3#16, _4#17, _5#18, _6#19]
   +- *(4) Sort [uid#54 ASC NULLS FIRST], false, 0
      +- Exchange hashpartitioning(uid#54, 5)
         +- *(3) Project [concat(_1#14, _dddd) AS uid#54]
            +- *(3) Filter (concat(_1#14, _dddd) = label_1)
               +- LocalTableScan [_1#14, _2#15, _3#16, _4#17, _5#18, _6#19]
```

说明：

```fsharp
1. `*`代表当前节点可以动态生成代码
2. 括号中的数字，代表的是codegen stage id,即代码生成的阶段id。
    2.1 codegen stage的划分：
        相邻节点(父子节点)且节点支持codegen ，会被划分到同一个 codegen stage,直到不支持codegen的节点。
        eg:Filter 、Project 节点支持，Exchange 节点不支持，那么Filter 、Project就会被划分到同一个stage中，
    2.2 如何判断节点是否支持 codegen: 
        查看节点源代码是否 with CodegenSupport 且 def supportCodegen: Boolean = true 
    
3. 相同的codegen stage id 代码会被折叠在一起，减少函数的调用。
4. #数字，代表字段id, id相同的代表同一个字段。
```

## 常用节点及其含义

1. Project  
    含义：投影 
    源代码类：ProjectExec 
    产生原因: select 
    CodegenSupport : true 
    explain 输出解释：Project [投影字段#投影字段的id,..]
2. Filter 
    含义：过滤条件
    源代码类：FilterExec 
    产生原因: where 
    CodegenSupport : true 
    explain 输出解释：Filter(条件表达式)
3. Exchange 
    含义：shuffle节点
    源代码类：ShuffleExchangeExec 
    产生原因: group by 、order by 、join 等都有可能产生。在EnsureRequirements中根据父节点的requiredChildDistribution(要求子节点输出分布)和子节点的outputPartitioning(输出分布)决定是否插入Exchange节点
    CodegenSupport : false 
    explain 输出解释：**Exchange 分区方式(分区key, 分区数)**
4. Sort 
    含义：排序 
    源代码类：SortExec 
    **产生原因：order by, group by(sort aggregate)** 
    ,join(sortMergeJoin)。在EnsureRequirements中根据父节点的requiredChildOrdering(要求子节点排序情况)和子节点的outputOrdering(排序情况)决定是否插入Sort节点 
    CodegenSupport : true 
    explain 输出解释：Sort [排序字段#id  排序方向(ASC|DESC)  null值在前还是在后(NULLS FIRST:null 排在前边, NULLS LAST:null 排在后边) ]
5. SortMergeJoin 
    含义：排序方式进行join
    源代码类：SortMergeJoinExec
    产生原因：join (条件不满足使用 broadcastJoin 、shuffledHashJoin 时使用 sortmergejoin)
    CodegenSupport : false
    explain 输出解释：SortMergeJoin  join的条件  join类型
6. BroadcastHashJoin
    含义：广播方式进行join
    源代码类：BroadcastHashJoinExec
    产生原因：join(使用hint 、一侧表小于广播阈值)
    CodegenSupport :true
    explain 输出解释：BroadcastHashJoin  **join的条件  join类型  build侧**（BuildRight:右表broadcast, BuildLeft :左表broadcast）
7. ShuffledHashJoin
    含义：先shuffle再hashjoin
    源代码类：ShuffledHashJoinExec
    产生原因：join(条件不满足BroadcastHashJoin，且表总大小/partition个数 小于 brocast阈值，且spark.sql.join.preferSortMergeJoin=false （默认等于true）)
    CodegenSupport :false
    explain 输出解释：ShuffledHashJoin  join的条件  join类型
8. HashAggregate
    含义：hash 方式进行聚合
    源代码类：HashAggregateExec
    产生原因：group by , 聚合函数(count,sum), distinct
    CodegenSupport :true
    explain 输出解释: HashAggregate(聚合的key, 聚合函数, 输出)
9. SortAggregate
    含义：sort 方式进行聚合
    源代码类：SortAggregateExec
    产生原因：group by , 聚合函数(count,sum), distinct
    CodegenSupport :false
    explain 输出解释: SortAggregate(聚合的key, 聚合函数, 输出)
10. ObjectHashAggregate
     含义：hash 方式进行聚合, 与HashAggregate有所区别
     源代码类：ObjectHashAggregateExec
     产生原因：group by , 聚合函数(count,sum), distinct
     CodegenSupport :false
     explain 输出解释: ObjectHashAggregate(聚合的key, 聚合函数, 输出)
11. Window
     含义：窗口操作
     源代码类:WindowExec
     产生原因：使用了over 从句
     CodegenSupport :false
     explain 输出解释: Window [窗口函数(eg row_number ) 窗口定义(partition order ), 窗框范围]



