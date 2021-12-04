[hive执行计划](https://mp.weixin.qq.com/s/ixTprq2dJgWlOzMR3kBssA)

Hive SQL的执行计划描述SQL实际执行的整体轮廓，通过执行计划能了解SQL程序在转换成相应计算引擎的执行逻辑，掌握了执行逻辑也就能更好地把握程序出现的瓶颈点，从而能够实现更有针对性的优化。此外还能帮助开发者识别看似等价的SQL其实是不等价的，看似不等价的SQL其实是等价的SQL。**可以说执行计划是打开SQL优化大门的一把钥匙**。

要想学SQL执行计划，就需要学习查看执行计划的命令：`explain`，在查询语句的SQL前面加上关键字explain是查看执行计划的基本方法。

学会explain，能够给我们工作中使用hive带来极大的便利！

# 查看SQL的执行计划

Hive提供的执行计划目前可以查看的信息有以下几种：

- **explain**：查看执行计划的基本信息；
- **explain dependency**：dependency在explain语句中使用会产生有关计划中输入的额外信息。它显示了输入的各种属性；
- **explain authorization**：查看SQL操作相关权限的信息；
- **explain vectorization**：查看SQL的向量化描述信息，显示为什么未对Map和Reduce进行矢量化。从 Hive 2.3.0 开始支持；
- **explain analyze**：用实际的行数注释计划。从 Hive 2.2.0 开始支持；
- **explain cbo**：输出由Calcite优化器生成的计划。CBO 从 Hive 4.0.0 版本开始支持；
- **explain locks**：这对于了解系统将获得哪些锁以运行指定的查询很有用。LOCKS 从 Hive 3.2.0 开始支持；
- **explain ast**：输出查询的抽象语法树。AST 在 Hive 2.1.0 版本删除了，存在bug，转储AST可能会导致OOM错误，将在4.0.0版本修复；
- **explain extended**：加上 extended 可以输出有关计划的额外信息。这通常是物理信息，例如文件名，这些额外信息对我们用处不大；

## explain

**Hive提供了explain命令来展示一个查询的执行计划**，这个执行计划对于我们了解底层原理，Hive 调优，排查数据倾斜等很有帮助。

```sql
explain select sum(id) from test1;	
STAGE DEPENDENCIES:
  Stage-1 is a root stage
  Stage-0 depends on stages: Stage-1

STAGE PLANS:
  Stage: Stage-1
    Map Reduce
      Map Operator Tree:
          TableScan
            alias: test1
            Statistics: Num rows: 6 Data size: 75 Basic stats: COMPLETE Column stats: NONE
            Select Operator
              expressions: id (type: int)
              outputColumnNames: id
              Statistics: Num rows: 6 Data size: 75 Basic stats: COMPLETE Column stats: NONE
              Group By Operator
                aggregations: sum(id)
                mode: hash
                outputColumnNames: _col0
                Statistics: Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: NONE
                Reduce Output Operator
                  sort order:
                  Statistics: Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: NONE
                  value expressions: _col0 (type: bigint)
      Reduce Operator Tree:
        Group By Operator
          aggregations: sum(VALUE._col0)
          mode: mergepartial
          outputColumnNames: _col0
          Statistics: Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: NONE
          File Output Operator
            compressed: false
            Statistics: Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: NONE
            table:
                input format: org.apache.hadoop.mapred.SequenceFileInputFormat
                output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat
                serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe

  Stage: Stage-0
    Fetch Operator
      limit: -1
      Processor Tree:
        ListSink
```

> **一个HIVE查询被转换为一个由一个或多个stage组成的序列（有向无环图DAG）。这些stage可以是MapReduce stage，也可以是负责元数据存储的stage，也可以是负责文件系统的操作（比如移动和重命名）的stage**。

看第二部分 stage plan，里面有一个 Map Reduce，一个MR的执行计划分为两个部分：

1. Map Operator Tree：MAP端的执行计划树
2. Reduce Operator Tree：Reduce端的执行计划树

这两个执行计划树里面包含这条sql语句的 operator：

1. **TableScan：表扫描操作**，map端第一个操作肯定是加载表，所以就是表扫描操作，常见的属性：

2. - alias：表名称
   - Statistics：表统计信息，包含表中数据条数，数据大小等

3. **Select Operator：选取操作**，常见的属性 ：

4. - expressions：需要的字段名称及字段类型
   - outputColumnNames：输出的列名称
   - Statistics：表统计信息，包含表中数据条数，数据大小等

5. **Group By Operator：分组聚合操作**，常见的属性：

6. - aggregations：显示聚合函数信息
   - mode：聚合模式，值有 hash：随机聚合，就是hash partition；partial：局部聚合；final：最终聚合
   - keys：分组的字段，如果没有分组，则没有此字段
   - outputColumnNames：聚合之后输出列名
   - Statistics：表统计信息，包含分组聚合之后的数据条数，数据大小等

7. **Reduce Output Operator：输出到reduce操作**，常见属性：

8. - sort order：值为空 不排序；值为 + 正序排序，值为 - 倒序排序；值为 +-  排序的列为两列，第一列为正序，第二列为倒序

9. **Filter Operator：过滤操作**，常见的属性：

10. - **predicate：过滤条件**，如sql语句中的where id>=1，则此处显示(id >= 1)

11. **Map Join Operator：join 操作**，常见的属性：

12. - condition map：join方式 ，如Inner Join 0 to 1 Left Outer Join0 to 2
    - keys: join 的条件字段
    - outputColumnNames：join 完成之后输出的字段
    - Statistics：join 完成之后生成的数据条数，大小等

13. **File Output Operator：文件输出操作**，常见的属性

14. - compressed：是否压缩
    - table：表的信息，包含输入输出文件格式化方式，序列化方式等

15. **Fetch Operator 客户端获取数据操作**，常见的属性：

16. - limit，值为 -1 表示不限制条数，其他值为限制的条数



## explain dependency的用法

explain dependency的使用场景有两个：

- **场景一**：快速排除。快速排除因为读取不到相应分区的数据而导致任务数据输出异常。例如，在一个以天分区的任务中，上游任务因为生产过程不可控因素出现异常或者空跑，导致下游任务引发异常。通过这种方式，可以快速查看SQL读取的分区是否出现异常。
- **场景二**：理清表的输入，帮助理解程序的运行，特别是有助于理解有多重子查询，多表连接的依赖输入。
- 识别SQL读取数据范围的差别