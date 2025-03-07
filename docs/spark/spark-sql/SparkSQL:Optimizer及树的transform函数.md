# 简介

Catalyst optimizer是SparkSQL的核心，它利用高级编程语言特性(例如scala的模式匹配、quasi quotes(准引用))以一个新奇的方式构建了一个可扩展的查询优化器，以如下两个关键目的作为设计目标：

1. 易于给SparkSQL添加优化技术和特性
2. 允许外部开发者扩展优化器

![Catalyst Optimizer Diagram](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/Catalyst-Optimizer-diagram.png)

Catalyst包含表达以及应用规则来操作树的通用库。框架上层，有关系查询特定的库(例如表达式、逻辑查询计划)以及一些规则集来处理查询计划的不同阶段：解析、逻辑优化、物理计划和代码生成。catalyst还提供一些公共扩展点，包括扩展数据源和自定义类型，同时Catalyst支持基于规则和基于成本的优化。

Optimizer的主要职责是将Analyzer输出的Resolved Logical Plan根据不同的优化策略Batch，来对语法树进行优化。Optimizer的工作方式其实类似Analyzer，因为它们都继承自RuleExecutor[LogicalPlan]，都是执行一系列的Batch操作。

## Trees

Catalyst里主要的数据类型是由节点构成的树，每个节点有一个节点类型和0个或多个孩子。新的节点类型是TreeNode的子类，这些对象都是不可变的，且可用下文描述的transformations来操作。

假如对于一个非常简单的表达式语言，我们有如下三个节点类：

- `Literal(value: Int)`: 常量
- `Attribute(name: String):` 输入行的一个属性, e.g.,“x”
- `Add(left: TreeNode, right: TreeNode):` 两个表达式的和

例如，表达式x+(1+2)可以这么表示：

```
Add(Attribute(x), Add(Literal(1), Literal(2)))
```

## Rules

树可以使用规则来操作，规则就是把一个树转成另一个树的函数，规则可以对输入树执行任意代码，通常是应用一系列模式匹配函数来查找/替换特定结构的子树。

模式匹配是很多函数式编程语言的一个特性，允许从代数数据结构中国抽取值。 Catalyst中，树提供一个可以递归地在所有节点上应用模式匹配的函数transform。例如，我们可以实现一个规则来折叠两个常量的加操作：

```
tree.transform {
  case Add(Literal(c1), Literal(c2)) => Literal(c1+c2)
}
```

树x+(1+2)应用这个规则后就变成x+3了。

传给transform的模式匹配表达式是一个偏函数，意思是它只需要匹配所有可能的情况中的子集即可。可以调用一个transform来匹配多种模式，从而非常简洁的实现同时完成多种转换操作：

```scala
case Add(Literal(c1), Literal(c2)) => Literal(c1+c2)
case Add(left, Literal(0)) => left
case Add(Literal(0), right) => right
}
```

Catalyst把规则分成batch组,执行每个batch直到达到一个固定点，即树不再发生变化时。                                      

最后，规则条件和规则体可以包含任意代码

## Spark SQL中的使用

我们在四个阶段使用了 Catalyst 通用树转换操作框架

1. 分析：语法树和元数据 *（Catalog）*  绑定，得到*Resolved Logical Plan（Analyzed Logical Plan）*
2. 逻辑优化：使用*RBO*（逻辑优化规则）和*CBO*（成本优化规则）进行优化，得到新的语法树
3. 物理计划：将*Logical Plan* 转换为多个*Physical Plans* ，使用*Cost Model* 选择最佳的*Physical Plans*
4. 代码生成：编译部分查询为*Java* 字节码

# Optimizer:常量折叠

## 前置函数

### TreeNode.transformDownWithPruning

<font color=red>核心逻辑，rule按照前序应用到所有子节点后返回节点副本</font>

cond:一个表示是否遍历该节点子树的lambda表达式，如果cond.apply返回true则遍历该子树，false跳过该子树

```scala
def transformDownWithPruning(cond: TreePatternBits => Boolean,
  ruleId: RuleId = UnknownRuleId)(rule: PartialFunction[BaseType, BaseType])
: BaseType 
= {
     //（一）是否应用规则
    if (!cond.apply(this) || isRuleIneffective(ruleId)) {
      return this
    }
    // withOrigin是用于错误跟踪的，对于理解这里可忽略
    val afterRule = CurrentOrigin.withOrigin(origin) {
      //（二）应用规则，如果规则没处理，则原样返回
      rule.applyOrElse(this, identity[BaseType])
    }

    // Check if unchanged and then possibly return old copy to avoid gc churn.
    if (this fastEquals afterRule) {
      //（三）如果plan没有替换为一个新的plan(内存地址未发生变化)，则递归对子节点应用规则
      val rewritten_plan = mapChildren(_.transformDownWithPruning(cond, ruleId)(rule))
      //  3.1 如果没有变化，则标记当前rule无效，并返回原计划
      if (this eq rewritten_plan) {
        markRuleAsIneffective(ruleId)
        this
      } else {
      // 3.2 否则，返回新的计划  
        rewritten_plan
      }
    } else {
      // 查询计划被换为一个新的了，需要把tags同步过去，并递归应用规则
      afterRule.copyTagsFrom(this)
      afterRule.mapChildren(_.transformDownWithPruning(cond, ruleId)(rule))
    }
  }
```

注意，LogicalPlan继承了AnalysisHelper接口，AnalysisHelper也有这个方法，只是这个方法会继续调这里

1. 规则是否有影响？即是否应用规则？  如果ruleId是UnknownRuleId，则一定应用

```scala
  protected def isRuleIneffective(ruleId : RuleId): Boolean = {
    if (ruleId eq UnknownRuleId) {
      return false
    }
    ineffectiveRules.get(ruleId.id)
  }
```

2. CurrentOrigin

官方注释： 给TreeNode提供一个请求origin context的位置，例如，当前解析的代码是哪一行

这是一个thread-local变量，在plan/expression中跟踪原始SQL行位置，通常设置CurrentOrigin、创建TreeNode实例，最后reset CurrentOrigin

```scala
case class Origin(
  line: Option[Int] = None,
  startPosition: Option[Int] = None,
  startIndex: Option[Int] = None,
  stopIndex: Option[Int] = None,
  sqlText: Option[String] = None,
  objectType: Option[String] = None,
  objectName: Option[String] = None)


object CurrentOrigin {
  private val value = new ThreadLocal[Origin]() {
    override def initialValue: Origin = Origin()
  }

  def get: Origin = value.get()
  def set(o: Origin): Unit = value.set(o)

  def reset(): Unit = value.set(Origin())

  def setPosition(line: Int, start: Int): Unit = {
    value.set(
      value.get.copy(line = Some(line), startPosition = Some(start)))
  }

  def withOrigin[A](o: Origin)(f: => A): A = {
    // remember the previous one so it can be reset to this
    // this way withOrigin can be recursive
    val previous = get
    set(o)
    val ret = try f finally { set(previous) }
    ret
  }
}
```



### TreeNode.transformWithPruning

<font color=red>这个函数是规则应用的入口，规则需要提供cond函数、ruleId和一个偏函数(通常是case语句构成的模式匹配)   </font>

内部直接调用了transformDownWithPruning

```scala
def transformWithPruning(cond: TreePatternBits => Boolean,
  ruleId: RuleId = UnknownRuleId)(rule: PartialFunction[BaseType, BaseType])
: BaseType = {
  transformDownWithPruning(cond, ruleId)(rule)
}
```



### QueryPlan.transformExpressionsDownWithPruning

<font color=red>对当前算子的所有表达式应用规则，即和算子一样执行TreeNode.transformDownWithPruning(cond, ruleId)(rule)</font>

```scala
def transformExpressionsDownWithPruning(cond: TreePatternBits => Boolean,
  ruleId: RuleId = UnknownRuleId)(rule: PartialFunction[Expression, Expression])
: this.type = {
  mapExpressions(_.transformDownWithPruning(cond, ruleId)(rule))
}
```



## ConstantFolding

调用计划树的transformWithPruning函数，传入应用条件函数、ruleId以及一个偏函数

```scala
def apply(plan: LogicalPlan): LogicalPlan =   
plan.transformWithPruning(AlwaysProcess.fn, ruleId) {//按前序顺序对所有子节点做下面的事情：
   //只有一个case匹配逻辑计划，即只对逻辑计划应用规则：
  case q: LogicalPlan => q.transformExpressionsDownWithPruning( //对当前算子下的所有表达式执行规则
    AlwaysProcess.fn, ruleId) 
     {
       // 一个小tric： literal直接跳过，否则会执行到下面的case导致无用的eval新建一个Literal值
      case l: Literal => l
      //size函数，计算array长度
      case Size(c: CreateArray, _) if c.children.forall(hasNoSideEffect) =>
      Literal(c.children.length)
     //size函数，计算map大小
      case Size(c: CreateMap, _) if c.children.forall(hasNoSideEffect) =>
      Literal(c.children.length / 2)

      // 对可折叠表达式进行计算
      case e if e.foldable => Literal.create(e.eval(EmptyRow), e.dataType)
      }
}
```

foldable是所有表达式都有的一个属性，折叠规则：

1. Coalesce在所有子节点可折叠的条件下可折叠
2. BinaryExpression在所有子节点可折叠的条件下可折叠
3. Not/IsNull/IsNotNull 在子节点可折叠的条件下可折叠
4. Cast /  UnaryMinus 在子节点可折叠的条件下可折叠



由于常量都可以直接计算，即可折叠，它的计算也很简单，就是把Litera把value返回即可。

```scala
case class Literal{
override def eval(input: InternalRow): Any = value }
```



### 案例

以这条sql为例

```scala
spark.sql("select cast('2000-08-23' as date) from warehouse").collect()
```

常量折叠前的计划,是有cast运算的

```
Project [cast(2000-08-23 as date) AS CAST(2000-08-23 AS DATE)#28]
+- Relation [w_warehouse_sk#0,w_warehouse_id#1,w_warehouse_name#2,w_warehouse_sq_ft#3,w_street_number#4,w_street_name#5,w_street_type#6,w_suite_number#7,w_city#8,w_county#9,w_state#10,w_zip#11,w_country#12,w_gmt_offset#13] parquet
```

Cast的子节点"2000-08-23"是常量是可折叠的，如何确定的呢？

Cast 继承了CastBase, CastBase继承了UnaryExpression，  UnaryExpression对foldable的实现就是调用child.foldable

```scala
abstract class UnaryExpression extends Expression with UnaryLike[Expression] {

  override def foldable: Boolean = child.foldable
  override def nullable: Boolean = child.nullable
  
  override def eval(input: InternalRow): Any = {
    val value = child.eval(input)
    if (value == null) {
      null
    } else {
      nullSafeEval(value)
    }
  }
  ... ...
}
```

应用完常量折叠后的计划,已经没有cast表达式了

```
Project [2000-08-23 AS CAST(2000-08-23 AS DATE)#28]
+- Relation [w_warehouse_sk#0,w_warehouse_id#1,w_warehouse_name#2,w_warehouse_sq_ft#3,w_street_number#4,w_street_name#5,w_street_type#6,w_suite_number#7,w_city#8,w_county#9,w_state#10,w_zip#11,w_country#12,w_gmt_offset#13] parquet
```

注意，Relation算子的表达式是一系列AttributeReference，继承自Unevaluable，foldable是false，不可折叠。