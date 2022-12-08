[参考](https://zhuanlan.zhihu.com/p/367590611)

[Spark Sql 执行流程源码阅读笔记](https://cloud.tencent.com/developer/article/1820090)

[spark源码分析-Catalyst流程解析(1)](https://blog.csdn.net/strawhat2416/article/details/120158313?spm=1001.2101.3001.6650.7&utm_medium=distribute.pc_relevant.none-task-blog-2%7Edefault%7EBlogCommendFromBaidu%7ERate-7-120158313-blog-114988789.pc_relevant_3mothn_strategy_recovery&depth_1-utm_source=distribute.pc_relevant.none-task-blog-2%7Edefault%7EBlogCommendFromBaidu%7ERate-7-120158313-blog-114988789.pc_relevant_3mothn_strategy_recovery&utm_relevant_index=12), 2,3,4也值得看一看

[spark系列15:catalyst使用介绍与演示](https://hero78.blog.csdn.net/article/details/114988789)

基于spark3.0.2



一些视频

[A Deep Dive into Query Execution Engine of Spark SQL](https://www.databricks.com/session/a-deep-dive-into-query-execution-engine-of-spark-sql)

[A Deep Dive into Spark SQL’s Catalyst Optimizer](https://www.databricks.com/session/a-deep-dive-into-spark-sqls-catalyst-optimizer)





# 整个流程

![image-20221206212959602](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221206212959602.png)



![在这里插入图片描述](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwMDMxMjIx,size_16,color_FFFFFF,t_70.png)

> [参考](https://blog.csdn.net/qq_30031221/article/details/109222355)

主要步骤：

1. 输入sql，dataFrame或者dataSet

2. 经过Catalyst过程，生成最终我们得到的最优的物理执行计划
   1. parser阶段

- 主要是通过Antlr4解析SqlBase.g4 ，所有spark支持的语法方式都是定义在sqlBase.g4里面了，生成了我们的语法解析器SqlBaseLexer.java和词法解析器SqlBaseParser.java

- parse阶段 --> antlr4 —> 解析 —> SqlBase.g4 —> 语法解析器SqlBaseLexer.java + 词法解析器SqlBaseParser.java

  2. analyzer阶段

- 使用基于Rule的规则解析以及Session Catalog来实现函数资源信息和元数据管理信息
- Analyzer 阶段 --> 使用 --> Rule + Session Catalog --> 多个rule --> 组成一个batch --> session CataLog --> 保存函数资源信息以及元数据信息等
    3. optimizer阶段


optimizer调优阶段 --> 基于规则的RBO优化rule-based optimizer --> 谓词下推 + 列剪枝 + 常量替换 + 常量累加

​		 4. planner阶段

生成多个物理计划 --> 经过Cost Model进行最优选择 --> 基于代价的CBO优化 --> 最终选定得到的最优物理执行计划
    5. 选定最终的物理计划，准备执行

最终选定的最优物理执行计划 --> 准备生成代码去开始执行
3. 将最终得到的物理执行计划进行代码生成，提交代码去执行我们的最终任务















## SparkSession.sql

```scala
def sql(sqlText: String): DataFrame = withActive {
  // 用于跟踪查询计划的执行，例如：查询计划要执行哪些Rule、跟踪记录各个阶段执行的时间等。
  val tracker = new QueryPlanningTracker
  // 调用measurePhase统计解析执行计划的时间。
  // 这是一个高阶函数：def measurePhase[T](phase: String)(f: => T):
  // 执行一个操作，并计算其执行的时间。
  val plan = tracker.measurePhase(QueryPlanningTracker.PARSING) {
    // SessionState的SQL Parser负责解析SQL，并生成解析的执行计划
    // 接口定义为：def parsePlan(sqlText: String): LogicalPlan
    sessionState.sqlParser.parsePlan(sqlText)
  }
  // 生成物理执行计划并生成DataSet（就是DataFrame）
  Dataset.ofRows(self, plan, tracker)
}
```

这里的sqlParser是SparkSqlParser，为什么是SparkSqlParser,在类BaseSessionStateBuilder里（详细的流程后面单独写）



#### SessionState组件

这个类保存了一个SparkSession的所有状态：

- conf：Spark SQL配置
- functionRegistry：函数注册
- udfRegistration：UDF注册
- catalogBuilder：构建Catalog
- sqlParser：SQL解析器
- analyzerBuilder：分析后的执行计划构建器
- optimizerBuilder：优化后的执行计划构建器
- streamingQueryManagerBuilder：Streaming查询管理构建器
- listenerManager：监听器管理器
- resourceLoaderBuilder：资源加载构建器
- createQueryExecution：create语句执行器
- columnarRules：自定义规则实现，在执行计划中实现运算符实现
- queryStagePrepRules：查询阶段预处理规则
- analyzer：分析器
- optimizer：优化器
- resourceLoader：资源加载器
- streamingQueryManager：流查询管理器
- catalogManager：Catalog管理器

## 编译器Parser

### SparkSqlParser获取逻辑计划的核心类

![在这里插入图片描述](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwMDMxMjIx,size_16,color_FFFFFF,t_70-20221208093427147.png)

```scala
/**
 * Concrete parser for Spark SQL statements.
 */
class SparkSqlParser extends AbstractSqlParser {
  val astBuilder = new SparkSqlAstBuilder()

  private val substitutor = new VariableSubstitution()

  protected override def parse[T](command: String)(toResult: SqlBaseParser => T): T = {
    //parse方法也交给父类
    super.parse(substitutor.substitute(command))(toResult)
  }
}
```

我们发现SparkSqlParser并没有定义parsePlan方法，它实际上继承自父类AbstractSqlParser：

```scala
//  abstract class AbstractSqlParser extends ParserInterface with SQLConfHelper with Logging {
  /** Creates LogicalPlan for a given SQL string. */
  override def parsePlan(sqlText: String): LogicalPlan = parse(sqlText) { parser =>
    //astBuilder = SparkSqlParser  解析语法树， 即把Tree节点转换成LogicalPlan
    astBuilder.visitSingleStatement(parser.singleStatement()) match {
      case plan: LogicalPlan => plan
      case _ =>
        val position = Origin(None, None)
        throw new ParseException(Option(sqlText), "Unsupported SQL statement", position, position)
    }
  }
```

parsePlan其实就是调用了一下parse，传入一个sql语句和一个返回LogicalPlan的函数；后面会看到:

1. parse函数里，使用antlr4对SQL 进行词法分析并构建<font color=red>语法树</font>，会创建两个Java类
   - **词法解析器SqlBaseLexer.java**
   - 语法解析器SqlBaseParser.java。

2. parsePlan的函数体使用AstBuilder将语法树转换成为LogicalPlan，这个LogicalPlan也被称为<font color=red>Unresolved LogicalPlan</font>。

重点看parse的实现，它如何处理这个sql

#### parse得到语法树

注意，这是一个柯里化函数，有两个参数列表，第一个参数是sql语句，第二个参数是一个函数

```scala
    protected def parse[T](command: String)(toResult: SqlBaseParser => T): T = {
    logDebug(s"Parsing command: $command")
    // 创建词法分析器
    val lexer = new SqlBaseLexer(new UpperCaseCharStream(CharStreams.fromString(command)))
    lexer.removeErrorListeners()
    lexer.addErrorListener(ParseErrorListener)

    val tokenStream = new CommonTokenStream(lexer)
      // 创建语法解析器
    val parser = new SqlBaseParser(tokenStream)
    parser.addParseListener(PostProcessor)
    parser.removeErrorListeners()
    parser.addErrorListener(ParseErrorListener)
    parser.legacy_setops_precedence_enbled = conf.setOpsPrecedenceEnforced
    parser.legacy_exponent_literal_as_decimal_enabled = conf.exponentLiteralAsDecimalEnabled
      // 是否开启标准SQL
    parser.SQL_standard_keyword_behavior = conf.ansiEnabled

    try {
      try {
        // first, try parsing with potentially faster SLL mode
        parser.getInterpreter.setPredictionMode(PredictionMode.SLL)
        // 执行语法解析
        toResult(parser)
      }
      catch {
        case e: ParseCancellationException =>
          // if we fail, parse with LL mode
          tokenStream.seek(0) // rewind input stream
          parser.reset()

          // Try Again.
          parser.getInterpreter.setPredictionMode(PredictionMode.LL)
          toResult(parser)
      }
    }
    catch {
      case e: ParseException if e.command.isDefined =>
        throw e
      case e: ParseException =>
        throw e.withCommand(command)
      case e: AnalysisException =>
        val position = Origin(e.line, e.startPosition)
        throw new ParseException(Option(command), e.message, position, position)
    }
  }
```

#### astBuilder.visitSingleStatement的未解析的逻辑计划

通过解析整个语法树，sql转换成逻辑执行计划，当前生成的逻辑执行计划还是Unresolved Logical Plan。

```scala
  override def visitSingleStatement(ctx: SingleStatementContext): LogicalPlan = withOrigin(ctx) {
    visit(ctx.statement).asInstanceOf[LogicalPlan]
  }
// AbstractParseTreeVisitor
	public T visit(ParseTree tree) {
		return tree.accept(this);
	}
```

#### Tree

Tree是Catalyst执行计划表示的数据结构。LogicalPlans，Expressions和Pysical Operators都可以使用Tree来表示。Tree具备一些Scala Collection的操作能力和树遍历能力。
Tree有三种

UnaryNode：一元节点，即只有一个子节点 ag: Project
BinaryNode：二元节点，即有左右子节点的二叉节点 ag : Join
LeafNode：叶子节点，没有子节点的节点 ag: HiveTableRelation
Tree有两个子类继承体系，即QueryPlan和Expression
QueryPlan下面的两个子类分别是LogicalPlan（逻辑执行计划）和SparkPlan（物理执行计划）
Expression是表达式体系，是指不需要执行引擎计算，而可以直接计算或处理的节点



### LogicPlan

我们跟踪解析到的逻辑计划

![image-20221206205039004](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221206205039004.png)

好，拿到逻辑计划后，如何得到Dataset呢？

## Analyzer:AnalyzedLogicalPlan生成

<font color=red>该逻辑算子树中未被解析的有 UnresolvedRelation和 UnresolvedAttribute两种对象。实际上， Analyzer所起到的主要作用就是将这两种节点或表达式解析成有类型的(Typed)对象。 在此过 程中，需要用到Catalog的相关信息</font>。

<font color=red>逻辑算子树的解析是 一 个不断的迭代过程 。 实际上，用户可以通过参数( spark.sq!.optimizer.max.Iterations)设定 RuleExecutor迭代的轮数，默认配置为 50轮，对于某些嵌套较深的特殊 SQL，可以适当地增加 轮数。</font>

### catalog体系与Rule体系

- **catalog体系**
  在Spark SQL体系中，Catalog主要用于各种函数资源信息和元数据信息的统一管理。其实现以SessionCatalog为主体

- **Rule体系**
  在 Unresolved LogicalPlan 逻辑算子树的操作(如绑定、解析、优化 等 )中，主要方法都是 基于规则( Rule)的，通过 Scala语言模式匹配机制进行树结构的转换或节点改写。**RuleExecutor**把这些规则打包成了一个Batch序列，每个Batch里都是相似的一些Rule。
  <font color=red>这些规则大概都是干啥的呢？</font> 比较多，这里举几个例子：with子句优化，遇到with节点时，将子LogicalPlan替换成解析后的CTE，其作用是将多个LogicPlan合并成一个LogicPlan；spark2.0中支持group by列索引，就是通过规则把索引替换成表达式映射到对应的列。

  > with语句的存在，导致了一个sql会产生多个逻辑计划，但是通过规则可以合并

### Analyzed LogicalPlan 生成过程

- 在sql解析parse阶段，生成了很多未解析出来的有些关键字，这些都是属于 Unresolved LogicalPlan解析的部分。 Unresolved LogicalPlan仅仅是一种数据结构，不包含任何数据信息，例如不知道数据源，数据类型，不同的列来自哪张表等等。
- Analyzer 阶段会使用事先定义好的 Rule 以及 SessionCatalog 等信息对 Unresolved LogicalPlan 进行 transform。SessionCatalog 主要用于各种函数资源信息和元数据信息（数据库、数据表、数据视图、数据分区与函数等）的统一管理。而Rule 是定义在 Analyzer 里面的，具体的类的路径如下：org.apache.spark.sql.catalyst.analysis.Analyzer

在 QueryExecution 类中可以看到，触发 Analyzer 执行的是 execute 方法，即父类 RuleExecutor 中

的 execute 方法，该方法会循环地调用规则对逻辑算子树进行分析 。

```scala


具体的rule规则定义如下：
 lazy val batches: Seq[Batch] = Seq(
    Batch("Hints", fixedPoint,
      new ResolveHints.ResolveBroadcastHints(conf),
      ResolveHints.RemoveAllHints),
    Batch("Simple Sanity Check", Once,
      LookupFunctions),
    Batch("Substitution", fixedPoint,
      CTESubstitution,
      WindowsSubstitution,
      EliminateUnions,
      new SubstituteUnresolvedOrdinals(conf)),

```



- 多个性质类似的 Rule 组成一个 Batch，而多个 Batch 构成一个 batches。这些 batches 会由 RuleExecutor 执行，先按一个一个 Batch 顺序执行，然后对 Batch 里面的每个 Rule 顺序执行。每个 Batch 的执行策略决定会执行一次（Once）或多次（FixedPoint，由 spark.sql.optimizer.maxIterations 参数决定），执行过程如下：

![在这里插入图片描述](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwMDMxMjIx,size_16,color_FFFFFF,t_70-20221208113438292.png)



- 总结来看Analyzed Logical Plan主要就是干了一些这些事情

  1、确定最终返回字段名称以及返回类型：

  2、确定聚合函数

  3、确定表当中获取的查询字段

  4、确定过滤条件

  5、确定join方式

  6、确定表当中的数据来源以及分区个数



## Optimizer逻辑优化阶段

### Optimizer规则体系

这个阶段的优化器主要是基于规则的（Rule-based Optimizer，简称 RBO），而绝大部分的规则都是启发式规则，也就是基于直观或经验而得出的规则。

Optimizer 同样继承自 RuleExecutor 类，parkOptimizer继承自Optimizer，Optimizer本身定 义了 12 个规则 Batch，在 SparkOptimizer 类中又添加了 4 个 Batch。

![image-20221208174518505](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221208174518505.png)

优化规则举例：

1. 取消子查询别名，一旦Analyzer阶段结束，子查询对应的SubqueryAlias节点就可以删除了(替换为其子节点)
2. 表达式替换，这条规则通常用来对其他类型的数据库提供兼容 的能力，例如，可以用“ coalesce”来替换支持“ nvl”的 表达式。
3. 当前时间表达式替换，当前时间只会计算一次，然后替换到其他函数中，避免不一致。
4. 重写distinct操作。
5. 算子下推、算子组合、常量折叠

### Optimized LogicalPlan 的生成过程

在QueryExecution中， Optimizer会对传入的AnalyzedLogicalPlan执行execute方 法，启动优化过程 。

与前文介绍绑定逻辑计划阶段类似，这个阶段所有的规则也是实现 Rule 抽象类，多个规则组成一个 Batch，多个 Batch 组成一个 batches，同样也是在 RuleExecutor 中进行执行

1.  谓词下推
2.  列裁剪
3.  常量替换
4.  常量累加

## 物理计划Physical Plan生成

在此阶段， Spark SQL 会对 生成的逻辑算子树进行进一步处理，得到物理算子树，并将 LogicPlan 节点及其所包含的各种 信息映射成 Spark Core 计算模型的元素，如 RDD、 Transformation 和 Action 等，以支持其提交 执行。

分为三个阶段：

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221208183550787.png" alt="image-20221208183550787" style="zoom:50%;" />

**(1)由 SparkPlanner 将各种物理计划策略( Strategy)作用于对应的 LogicalPlan 节点上，生 成 SparkPlan列表(注: 一个 LogicalPlan可能产生多种 SparkPlan)。**

**(2)选取最佳的 SparkPlan，在 Spark2.l 版本中的实现较为简单，在候选列表中直接用 next() 方法获取第一个。**

**(3)提交前进行准备工作，进行一些分区排序方面的处理，确保 SparkPlan各节点能够正确 执行，这一步通过 prepareForExecution()方法调用若干规则(Rule)进行转换。**

### SparkPlan

<font color=red>在物理算子树 中，叶子类 型的 SparkPlan节点负责从无到有地创建RDD，每个非叶子类型的 SparkPlan节点等价于在 RDD 上进行一次 Transformation</font>

SparkPlan 的主要功能可以划分为 3 大块

1. 每个 SparkPlan 节点必不可少地 会记录其元数据( Metadata)与指标( Metric)信息，这些信息以 Key-Value 的形式保存在 Map 数 据结构中，统称为 SparkPlan 的 Metadata与 Metric体系 

2. 在对 RDD 进行 Transformation操 作时，会涉及数据分区( Partitioning)与排序( Ordering)的处理，称为 SparkPlan 的 Partitioning与 Ordering体系

3. SparkPlan作为物理计划，支持提交到 SparkCore去执行，即 SparkPlan 的执行操作部分，以 execute 和 executeBroadcast 方法为主。

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221208190753724.png" alt="image-20221208190753724" style="zoom:50%;" />

1. **LeafExecNode 类型**
   负责对初始RDD的创建，RangeExec、HiveTableScanExec、FileSourceScanExec

2. **UnaryExecNode 类型**

   只 包含 1 个子节点，主要是对 RDD进行转换操作

   - ProjectExec 和 FilterExec 分别对子节点产生的 RDD 进行列剪裁与行过滤操作
   - Exchange负责对数据进行重分区
   - SampleExec对输入 RDD 中的数据进行采样
   - SortExec按照一 定条件对输入 RDD 中数据进行排序
   -  WholeStageCodegenExec 类型的 SparkPlan 将生成的代码 整合成单个 Java 函数 

3. **BinaryExecNode类型**
    具有两个子节点，主要支持Join执行计划。

### SparkPlan生成

在 Spark SQL 中，当逻辑计划处理完毕后，会构造 SparkPlanner 并执行 plan() 方法对 LogicalPlan进行处理，得到对应的物理计划 。 实际上， 一个逻辑计划可能会对应多个物理计划， 因此， SparkPlanner得到的是一个物理计划的列表 (Iterator[SparkPlan])。

- 一个逻辑计划（Logical Plan）经过一系列的策略处理之后，得到多个物理计划（Physical Plans），物理计划在 Spark 是由 SparkPlan 实现的。多个物理计划再经过代价模型（Cost Model）得到选择后的物理计划（Selected Physical Plan），整个过程如下所示：
  ![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/20201022150611475.png)

- Cost Model 对应的就是基于代价的优化（Cost-based Optimizations，CBO，主要由华为的大佬们实现的，详见 SPARK-16026 ），核心思想是计算每个物理计划的代价，然后得到最优的物理计划。但是在目前最新版的 Spark 2.4.3，这一部分并没有实现，直接返回多个物理计划列表的第一个作为最优的物理计划

![image-20221208192029225](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221208192029225.png)

#### Strategy策略体系

 Strategy都实现了 apply 方法，将传入的 LogicalPlan 转换为 SparkPlan 的 列表，是生成物理算子树的基础。

一对一转换比较直观，例如Sort对应SortExec，Union对应UnionExec等。多对一转换称为逻辑算子树的模式匹配：

- ExtractEquiJoinKeys:针对具有相等条件的 Join操作的算子集合，提取出其中的 Join条件、

  左子节点和右子节点等信息 。

- ExtractFiltersAndinnerJoins: 收集 Inner类型 Join操作中的过滤条件，目前仅支持对左子

  树进行处理 。

- PhysicalAggregation:针对聚合操作，提取出聚合算子中的各个部分，并对一些表达式进行初步的转换

-  PhysicalOperation:匹配逻辑算子树中 的 Project和Filter等节点，返回投影列、过滤条件集合和子节点 。

## 执行前的准备

在 QueryExection 中，最后阶段由 prepareforExecution 方法对传入的 SparkPlan 进行处理而 生成 executedPlan，处理过程仍然基于若干规则(如表 6.3 所示)，主要包括对 Python 中 UDF 的 提取、子查询的计划生成等 。



## 代码生成阶段

- 从以上多个过程执行完成之后,最终我们得到的**物理执行计划**，这个物理执行计划标明了整个的代码执行过程当中
  - 执行过程
  - 数据字段以及字段类型，
  - 数据源的位置
- 然得到了物理执行计划，但是这个物理执行计划想要被执行，最终还是得要生成**完整的代码**，底层还是基于**sparkRDD去进行处理的**

### Tungsten 代码生成

分为三个部分：

- 表达式代码生成（expression codegen）
- 全阶段代码生成（Whole-stage Code Generation）
- 加速序列化和反序列化（speed up serialization/deserialization）

1. 表达式代码生成
   表达式代码生成主要是想解决大量虚函数调用（Virtual Function Calls），泛化的代价等

表达式代码生成的基类是 org.apache.spark.sql.catalyst.expressions.codegen.CodeGenerator，其下有七个子类：
![在这里插入图片描述](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMwMDMxMjIx,size_16,color_FFFFFF,t_70-20221208114404708.png)

2. 全阶段代码生成
   通过引入全阶段代码生成，大大减少了虚函数的调用，减少了 CPU 的调用，使得 SQL 的执行速度有很大提升。

   全阶段代码生成（Whole-stage Code Generation），用来将多个处理逻辑整合到单个代码模块中，其中也会用到上面的表达式代码生成。和前面介绍的表达式代码生成不一样，这个是对整个 SQL 过程进行代码生成，前面的表达式代码生成仅对于表达式的。
   

3. 加速序列化和反序列化

**代码生成是在 Driver 端进行的，而代码编译是在 Executor 端进行的。**

### Dataset.ofRows 

```scala
def ofRows(sparkSession: SparkSession, logicalPlan: LogicalPlan, tracker: QueryPlanningTracker)
: DataFrame = sparkSession.withActive {
  // QueryExecution是为Spark执行关系型查询的主要工作流，它包含了所有的执行计划。
  val qe = new QueryExecution(sparkSession, logicalPlan, tracker)
  // 解析逻辑执行计划
  qe.assertAnalyzed()
  // 创建DataSet，获取解析后的逻辑执行计划对应的schema，这个schema其实就是DataSet要使用的schema。
  new Dataset[Row](qe, RowEncoder(qe.analyzed.schema))
}
```

![image-20221206205836607](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221206205836607.png)

到此所有的计划都已经生成好了，DataSet也构建好了，就等着调度执行了。到此，所有的操作都是在Driver端执行的。

#### QueryExecution

其构造里

```scala
class QueryExecution(
    val sparkSession: SparkSession,
    val logical: LogicalPlan,
    val tracker: QueryPlanningTracker = new QueryPlanningTracker) extends Logging {

  val id: Long = QueryExecution.nextExecutionId

  // TODO: Move the planner an optimizer into here from SessionState.
  protected def planner = sparkSession.sessionState.planner

  def assertAnalyzed(): Unit = analyzed

  def assertSupported(): Unit = {
    if (sparkSession.sessionState.conf.isUnsupportedOperationCheckEnabled) {
      UnsupportedOperationChecker.checkForBatch(analyzed)
    }
  }
// 1. 分析执行器  Analyzer继承了RuleExecutor
  lazy val analyzed: LogicalPlan = executePhase(QueryPlanningTracker.ANALYSIS) {
    // We can't clone `logical` here, which will reset the `_analyzed` flag.
    sparkSession.sessionState.analyzer.executeAndCheck(logical, tracker)
  }

  lazy val withCachedData: LogicalPlan = sparkSession.withActive {
    assertAnalyzed()
    assertSupported()
    // clone the plan to avoid sharing the plan instance between different stages like analyzing,
    // optimizing and planning.
    sparkSession.sharedState.cacheManager.useCachedData(analyzed.clone())
  }
//2. 优化执行器
  lazy val optimizedPlan: LogicalPlan = executePhase(QueryPlanningTracker.OPTIMIZATION) {
    // clone the plan to avoid sharing the plan instance between different stages like analyzing,
    // optimizing and planning.
    val plan = sparkSession.sessionState.optimizer.executeAndTrack(withCachedData.clone(), tracker)
    // We do not want optimized plans to be re-analyzed as literals that have been constant folded
    // and such can cause issues during analysis. While `clone` should maintain the `analyzed` state
    // of the LogicalPlan, we set the plan as analyzed here as well out of paranoia.
    plan.setAnalyzed()
    plan
  }

  private def assertOptimized(): Unit = optimizedPlan

  // 3. 生成物理执行计划
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

  // executedPlan should not be used to initialize any SparkPlan. It should be
  // only used for execution.
  lazy val executedPlan: SparkPlan = {
    // We need to materialize the optimizedPlan here, before tracking the planning phase, to ensure
    // that the optimization time is not counted as part of the planning phase.
    assertOptimized()
    executePhase(QueryPlanningTracker.PLANNING) {
      // clone the plan to avoid sharing the plan instance between different stages like analyzing,
      // optimizing and planning.
      //4. 物理执行计划执行之前的初始化工作
      QueryExecution.prepareForExecution(preparations, sparkPlan.clone())
    }
  }
```

#### RuleExecutor

Rule是一系列的规则的接口类，比如分析器的各种分析规则，优化器的各种规则，执行器的各种规则等等。
RuleExecutor 就是各种不同Rule匹配逻辑执行计划树结构并且生成新的树结构的生成器。

**在RuleExecutor的实现子类（如Analyzer和Optimizer）中会定义Batch(每个Batch代表着一套规则)，Once(只执行一次的策略)和FixedPoint(执行固定次数的策略)。
executo()的执行逻辑就是批量根据策略调用Rule的apply方法。**

#### Analyzer







### 解析逻辑执行计划

```scala
def executeAndCheck(plan: LogicalPlan, tracker: QueryPlanningTracker): LogicalPlan = {
    // 因为要解析整棵语法树，需要递归检查执行计划是否已经完成解析。其实就是递归的出口
    if (plan.analyzed) return plan
    AnalysisHelper.markInAnalyzer {
      // 执行解析，此处是调用父类RuleExecutor对应的executeAndTrack方法
      val analyzed = executeAndTrack(plan, tracker)
      try {
        checkAnalysis(analyzed)
        analyzed
      } catch {
        case e: AnalysisException =>
          val ae = new AnalysisException(e.message, e.line, e.startPosition, Option(analyzed))
          ae.setStackTrace(e.getStackTrace)
          throw ae
      }
    }
  }
```



















### join源码剖析

[参考](https://cloud.tencent.com/developer/article/2008744)

#### 基本概念

在 Spark SQL 中，参与 Join 操作的两张表分别被称为流式表（StreamTable）和构件表（BuildTable），不同表的角色在 Spark SQL 中会通过一定的策略进行设定。通常来讲，系统会将大表设置为 StreamTable，小表设置为 BuildTable。流式表的迭代器为 streamIter，构建表的迭代器为 buildIter。遍历 streamIter 的每一条记录，然后在 buildIter 中查找匹配的记录。这个查找过程称为 build 过程。每次 build 操作的结果为一条 `JoinedRow(A, B)`，其中 A 来自 streamedIter，B 来自 buildIter。

#### 物理计划选取顺序

Join 物理执行计划的选取在 JoinSelection 中进行，其主要逻辑如下：

**如果是一个等值 join（equi-join）且包含 join hint，我们依次查看 join hint：**

1.  `broadcast hint`：如果 join 类型支持，使用 broadcast hash join。如果 left 和 right 都有 broadcast hint，选择 size 较小的一侧（基于统计数据）进行 broadcast
2.  `sort merge hint`：如果 join keys 是可排序的，使用 sort merge join。
3.  `shuffle hash hint`：如果 join 类型支持，如果 left 和 right 都设置了 shuffle hash hints，选择 size 较小的一侧作为 build side
4.  `shuffle replicate NL hint`：如果 join type 为 inner like，使用 cartesian product join（笛卡尔积）

JoinSelection 通过 `ExtractEquiJoinKeys` 来判断是否为等值 Join 并提取相关信息：

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/1620.png" alt="img" style="zoom: 67%;" />

##### 等值join

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/1620-20221206203811021.png)

##### 非等值join

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/1620-20221206203834366.png)