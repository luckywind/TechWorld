[SparkSQL源码解析系列一](https://zhuanlan.zhihu.com/p/367590611)

[是时候学习真正的 spark 技术了](https://mp.weixin.qq.com/s/awT4aawtTIkNKGI_2zn5NA)

[InfoQ Sparksql内核剖析](https://xie.infoq.cn/article/2a70e9fb993bed9bc9ed02c46)

[是时候学习真正的 spark 技术了](https://mp.weixin.qq.com/s/awT4aawtTIkNKGI_2zn5NA)写的不错 https://tech.xiaomi.com/#/pc/article-detail?id=15736

[spark-sql源码剖析和最佳实践精品系列教程](http://spark.coolplayer.net/?page_id=240)

[The Internals of Spark SQL Online Book](https://github.com/japila-books/spark-sql-internals)

[你真的懂Spark SQL原理吗？——SparkSQL源码解析系列](https://zhuanlan.zhihu.com/p/367590611)

[SparkSQL YYDS](https://cloud.tencent.com/developer/article/1922956?from=article.detail.2176233&areaSource=106000.2&traceId=cMcXnRvRpDNrcIPuB8htW)

[一文了解函数式查询优化器Spark SQL Catalyst](https://cloud.tencent.com/developer/article/1638045#:~:text=Catalyst%E5%B7%A5%E4%BD%9C%E6%B5%81%E7%A8%8B%201%20Parser%EF%BC%8C%E5%88%A9%E7%94%A8ANTLR%E5%B0%86sparkSql%E5%AD%97%E7%AC%A6%E4%B8%B2%E8%A7%A3%E6%9E%90%E4%B8%BA%E6%8A%BD%E8%B1%A1%E8%AF%AD%E6%B3%95%E6%A0%91AST%EF%BC%8C%E7%A7%B0%E4%B8%BAunresolved%20logical%20plan%2FULP%202,Analyzer%EF%BC%8C%E5%80%9F%E5%8A%A9%E4%BA%8E%E6%95%B0%E6%8D%AE%E5%85%83%E6%95%B0%E6%8D%AEcatalog%E5%B0%86ULP%E8%A7%A3%E6%9E%90%E4%B8%BAlogical%20plan%2FLP%203%20Optimizer%EF%BC%8C%E6%A0%B9%E6%8D%AE%E5%90%84%E7%A7%8DRBO%EF%BC%8CCBO%E4%BC%98%E5%8C%96%E7%AD%96%E7%95%A5%E5%BE%97%E5%88%B0optimized%20logical%20plan%2FOLP%EF%BC%8C%E4%B8%BB%E8%A6%81%E6%98%AF%E5%AF%B9Logical%20Plan%E8%BF%9B%E8%A1%8C%E5%89%AA%E6%9E%9D%EF%BC%8C%E5%90%88%E5%B9%B6%E7%AD%89%E6%93%8D%E4%BD%9C%EF%BC%8C%E8%BF%9B%E8%80%8C%E5%88%A0%E9%99%A4%E6%8E%89%E4%B8%80%E4%BA%9B%E6%97%A0%E7%94%A8%E8%AE%A1%E7%AE%97%EF%BC%8C%E6%88%96%E5%AF%B9%E4%B8%80%E4%BA%9B%E8%AE%A1%E7%AE%97%E7%9A%84%E5%A4%9A%E4%B8%AA%E6%AD%A5%E9%AA%A4%E8%BF%9B%E8%A1%8C%E5%90%88%E5%B9%B6)

[一条 SQL 在 Apache Spark 之旅（下） 全阶段代码生成](https://blog.51cto.com/u_15127589/2678553#:~:text=%E5%85%A8%E9%98%B6%E6%AE%B5%E4%BB%A3%E7%A0%81%E7%94%9F%E6%88%90%E9%83%BD%E6%98%AF%E7%BB%A7%E6%89%BF%E8%87%AA%20org.apache.spark.sql.execution.BufferedRowIterator%20%E7%9A%84%EF%BC%8C%E7%94%9F%E6%88%90%E7%9A%84%E4%BB%A3%E7%A0%81%E9%9C%80%E8%A6%81%E5%AE%9E%E7%8E%B0%20processNext%20%28%29,%E6%96%B9%E6%B3%95%EF%BC%8C%E8%BF%99%E4%B8%AA%E6%96%B9%E6%B3%95%E4%BC%9A%E5%9C%A8%20org.apache.spark.sql.execution.WholeStageCodegenExec%20%E9%87%8C%E9%9D%A2%E7%9A%84%20doExecute%20%E6%96%B9%E6%B3%95%E9%87%8C%E9%9D%A2%E8%A2%AB%E8%B0%83%E7%94%A8%E3%80%82)

[深入分析Spark SQL Catalyst Analyzer](https://zhuanlan.zhihu.com/p/558758701)

# 通过案例研究源码

```scala
 Seq((0,"xiaozhang",10),
    (1,"xiaohong",11),
    (2,"xiaoli",12)).toDF("id","name","age").createTempView("stu")

    Seq((0,"chinese",80),(0,"math",100),(0,"english",99),
      (1,"chinese",40),(1,"math",50),(1,"english",60),
      (0,"chinese",70),(0,"math",80),(0,"english",90)
    ).toDF("id","xueke","score").createTempView("score")

    val res: DataFrame = spark.sql(
      """
        |
        |select
        |sum(v),name
        |from
        |(
        |select stu.id,100+10+score.score as v,
        |name
        |from stu join score
        |where stu.id=score.id and stu.age>=11
        |)tmp
        |group by name
        |""".stripMargin)
    val queryExecution: QueryExecution =res.queryExecution

    println(queryExecution)
```



SparkSQL Catalyst解析流程图

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/ab8bdf5c6b7dd28842ac64256a66346c.png)

1. SQL 语句经过 **Antlr4** 解析，生成 **Unresolved Logical Plan**
2. **analyzer** 与 **catalog** 进行绑定，生成 **Logical Plan**
3. **optimizer** 对 **Logical Plan** 优化,生成 **Optimized LogicalPlan**
4. **SparkPlan** 将 **Optimized LogicalPlan** 转换成 **Physical Plan**
5. **prepareForExecution** 方法将 **Physical Plan** 转换成 **executed Physical Plan**
6. **execute**()执行可执行物理计划，得到 **RDD**



该sql语句的执行计划生成逻辑：

```scala
(1)经过Parser有了抽象语法树
== Parsed Logical Plan ==
'Aggregate ['name], [unresolvedalias('sum('v), None), 'name]
+- 'SubqueryAlias `tmp`
   +- 'Project ['stu.id, ((100 + 10) + 'score.score) AS v#26, 'name]
      +- 'Filter (('stu.id = 'score.id) && ('stu.age >= 11))
         +- 'Join Inner
            :- 'UnresolvedRelation `stu`
            +- 'UnresolvedRelation `score`

(2)解析逻辑计划， 到这里表变成了LocalRelation,所有的列都有一个编号及类型信息
== Analyzed Logical Plan ==
sum(v): bigint, name: string
Aggregate [name#8], [sum(cast(v#26 as bigint)) AS sum(v)#28L, name#8]
+- SubqueryAlias `tmp`
   +- Project [id#7, ((100 + 10) + score#22) AS v#26, name#8]
      +- Filter ((id#7 = id#20) && (age#9 >= 11))
         +- Join Inner
            :- SubqueryAlias `stu`
            :  +- Project [_1#3 AS id#7, _2#4 AS name#8, _3#5 AS age#9]
            :     +- LocalRelation [_1#3, _2#4, _3#5]
            +- SubqueryAlias `score`
               +- Project [_1#16 AS id#20, _2#17 AS xueke#21, _3#18 AS score#22]
                  +- LocalRelation [_1#16, _2#17, _3#18]
（3）逻辑优化
== Optimized Logical Plan ==
Aggregate [name#8], [sum(cast(v#26 as bigint)) AS sum(v)#28L, name#8]
+- Project [(110 + score#22) AS v#26, name#8]					//100+10变成了110
   +- Join Inner, (id#7 = id#20)
      :- LocalRelation [id#7, name#8]
      +- LocalRelation [id#20, score#22]

（4）物理计划
== Physical Plan ==
*(2) HashAggregate(keys=[name#8], functions=[sum(cast(v#26 as bigint))], output=[sum(v)#28L, name#8])  //整体聚合
+- Exchange hashpartitioning(name#8, 200)   //这一步保证相同key shuffle到相同分区
   +- *(1) HashAggregate(keys=[name#8], functions=[partial_sum(cast(v#26 as bigint))], output=[name#8, sum#32L]) //局部聚合
      +- *(1) Project [(110 + score#22) AS v#26, name#8]
         +- *(1) BroadcastHashJoin [id#7], [id#20], Inner, BuildLeft
            :- BroadcastExchange HashedRelationBroadcastMode(List(cast(input[0, int, false] as bigint)))//Exchange 用来在节点间交换数据
            :  +- LocalTableScan [id#7, name#8]
            +- LocalTableScan [id#20, score#22]
```

## 先看逻辑计划，可形象表示如下

这里的节点有三类：

1. UnaryNode: 一元节点，只有一个孩子，例如Filter节点
2. LeafNode:叶子结点，例如两个表产生的UnresolvedRelation节点
3. BinaryNode：有两个孩子的节点，例如Join节点

这三类都是LogicalPlan类型的，可以理解为各种操作的Operator

![图片](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/640-20220319165918381.jpeg)

![Operator](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/640-20220319170745211.jpeg )

这些 operator 组成的抽象语法树就是整个 Catatyst 优化的基础，Catatyst 优化器会在这个树上面进行各种折腾，把树上面的节点挪来挪去来进行优化。

## analyzer解析抽象语法树

现在经过 Parser 有了抽象语法树，但是并不知道 score，sum 这些东西是啥，所以就需要 analyer 来定位, analyzer 会把 AST 上所有 Unresolved 的东西都转变为 resolved 状态，sparksql 有很多resolve 规则，都很好理解，例如

1.  ResolverRelations 就是解析表（列）的基本类型等信息，
2. ResolveFuncions 就是解析出来函数的基本信息，比如例子中的sum 函数，
3. ResolveReferences 可能不太好理解，我们在 sql 语句中使用的字段比如 Select name 中的 name 对应一个变量， 这个变量在解析表的时候就作为一个变量（Attribute 类型）存在了，那么 Select 对应的 Project 节点中对应的相同的变量就变成了一个引用，他们有相同的 ID，所以经过 ResolveReferences 处理后，就变成了 AttributeReference 类型  ，保证在最后真正加载数据的时候他们被赋予相同的值，就跟我们写代码的时候定义一个变量一样，这些 Rule 就反复作用在节点上，直到树节点趋于稳定。

解析完成后，AST就变成了：

![图片](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/640-20220319172104378.jpeg)

## 逻辑优化

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/640-20220319172330430.jpeg" alt="图片" style="zoom:67%;" />![图片](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/640-20220319172330430.jpeg)

sparksql 中的逻辑优化种类繁多，spark sql 中的 Catalyst 框架大部分逻辑是在一个 Tree 类型的数据结构上做各种折腾

## 物理计划

做完逻辑优化，毕竟只是抽象的逻辑层，还需要先转换为物理执行计划，将逻辑上可行的执行计划变为 Spark 可以真正执行的计划,即 SparkPlan：

这部分源码文件org/apache/spark/sql/execution/QueryExecution.scala

```scala
lazy val sparkPlan: SparkPlan = {
    // We need to materialize the optimizedPlan here because sparkPlan is also tracked under
    // the planning phase
    assertOptimized()
    executePhase(QueryPlanningTracker.PLANNING) {
      // Clone the logical plan here, in case the planner rules change the states of the logical
      // plan.
      QueryExecution.createSparkPlan(sparkSession, planner, optimizedPlan.clone())
    }
  }

/**
   * Transform a [[LogicalPlan]] into a [[SparkPlan]].
   *
   * Note that the returned physical plan still needs to be prepared for execution.
   */
  def createSparkPlan(
      sparkSession: SparkSession,
      planner: SparkPlanner,
      plan: LogicalPlan): SparkPlan = {
    // TODO: We use next(), i.e. take the first plan returned by the planner, here for now,
    //       but we will implement to choose the best plan.
    planner.plan(ReturnAnswer(plan)).next()
  }
```



这里 SparkSQL 在真正执行时，会调用 prepareForExecution 将 sparkPlan 转换成 executedPlan，并在 sparkPlan 中执行过程中，如果出现 stage 分区规则不同时插入 Shuffle 操作以及进行一些数据格式转换操作等等。

```scala
  private[execution] def prepareForExecution(
      preparations: Seq[Rule[SparkPlan]],
      plan: SparkPlan): SparkPlan = {
    val planChangeLogger = new PlanChangeLogger[SparkPlan]()
    val preparedPlan = preparations.foldLeft(plan) { case (sp, rule) =>
      val result = rule.apply(sp)
      planChangeLogger.logRule(rule.ruleName, sp, result)
      result
    }
    planChangeLogger.logBatch("Preparations", plan, preparedPlan)
    preparedPlan
  }

```

这里preparations是一个Seq[Rule[SparkPlan]] 规则序列：

这些规则用于：

1. 确保子查询是计划好的
2. 数据分区和排序是正确的
3. 插入全阶段代码生成
4. 重用exchanges和子查询

```scala
  private[execution] def preparations(
      sparkSession: SparkSession,
      adaptiveExecutionRule: Option[InsertAdaptiveSparkPlan] = None,
      subquery: Boolean): Seq[Rule[SparkPlan]] = {
    // `AdaptiveSparkPlanExec` is a leaf node. If inserted, all the following rules will be no-op
    // as the original plan is hidden behind `AdaptiveSparkPlanExec`.
    adaptiveExecutionRule.toSeq ++
    Seq(
      CoalesceBucketsInJoin,
      PlanDynamicPruningFilters(sparkSession),
      PlanSubqueries(sparkSession),
      RemoveRedundantProjects,
      EnsureRequirements(),
      // `ReplaceHashWithSortAgg` needs to be added after `EnsureRequirements` to guarantee the
      // sort order of each node is checked to be valid.
      ReplaceHashWithSortAgg,
      // `RemoveRedundantSorts` needs to be added after `EnsureRequirements` to guarantee the same
      // number of partitions when instantiating PartitioningCollection.
      RemoveRedundantSorts,
      DisableUnnecessaryBucketedScan,
      ApplyColumnarRulesAndInsertTransitions(
        sparkSession.sessionState.columnarRules, outputsColumnar = false),
      CollapseCodegenStages()) ++
      (if (subquery) {
        Nil
      } else {
        Seq(ReuseExchangeAndSubquery)
      })
  }
```



spark sql 把逻辑节点转换为了相应的物理节点， 比如 Join 算子，Spark 根据不同场景为该算子制定了不同的算法策略，有BroadcastHashJoin、ShuffleHashJoin 以及 SortMergeJoin 等.

spark sql 中 join 操作根据各种条件选择不同的 join 策略，分为 BroadcastHashJoin， SortMergeJoin， ShuffleHashJoin。



- BroadcastHashJoin：spark 如果判断一张表存储空间小于 broadcast 阈值时（Spark 中使用参数 spark.sql.autoBroadcastJoinThreshold 来控制选择 BroadcastHashJoin 的阈值，默认是 10MB），就是把小表广播到 Executor， 然后把小表放在一个 hash 表中作为查找表，通过一个 map 操作就可以完成 join 操作了，避免了性能代码比较大的 shuffle 操作，不过要注意， BroadcastHashJoin 不支持 full outer join， 对于 right outer join， broadcast 左表，对于 left outer join，left semi join，left anti join ，broadcast 右表， 对于 inner join，那个表小就 broadcast 哪个。



- SortMergeJoin：如果两个表的数据都很大，比较适合使用 SortMergeJoin， SortMergeJoin 使用shuffle 操作把相同 key 的记录 shuffle 到一个分区里面，然后两张表都是已经排过序的，进行 sort merge 操作，代价也可以接受。



- ShuffleHashJoin：就是在 shuffle 过程中不排序了，把查找表放在hash表中来进行查找 join，那什么时候会进行 ShuffleHashJoin 呢？查找表的大小需要超过 spark.sql.autoBroadcastJoinThreshold 值，不然就使用  BroadcastHashJoin 了，每个分区的平均大小不能超过  spark.sql.autoBroadcastJoinThreshold ，这样保证查找表可以放在内存中不 OOM， 还有一个条件是 大表是小表的 3 倍以上，这样才能发挥这种 Join 的好处。



上面提到 AST 上面的节点已经转换为了物理节点，这些物理节点最终从头节点递归调用 execute 方法，里面会在 child 生成的 RDD 上调用 transform操作就会产生一个串起来的 RDD 链

![image-20220319174054530](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220319174054530.png)

1. 最终执行时被Exchange(shuffle)分成了两个stage
2. 数据在一个一个的 plan 中流转，然后每个 plan 里面表达式都会对数据进行处理，就相当于经过了一个个小函数的调用处理，这里面就有大量的函数调用开销，那么我们是不是可以把这些小函数内联一下，当成一个大函数，WholeStageCodegen 就是干这事的。最终执行计划每个节点前面有个 * 号，说明WholeStageCodegen被启用
3. Exchange 算子并没有实现整段代码生成，因为它需要通过网络发送数据。

