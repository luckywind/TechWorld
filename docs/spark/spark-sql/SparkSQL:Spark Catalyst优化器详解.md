SparkSQL Catalyst优化器详解

# 前言

Spark SQL 是 Spark 最新且技术最复杂的组件之一。它同时支持 SQL 查询和新的 DataFrame API。Spark SQL 的核心是 Catalyst 优化器。其可扩展设计有两个目的：首先，能够非常容易地为 Spark SQL 添加新的优化技术和特性，尤其是为了应对我们遇到的大数据中的各种问题（例如：半结构化数据和高级分析）；其次，作为SparkSQL的使用者可以扩展自定义的优化器。例如，为数据源添加特定的规则从而使过滤或聚合操作下推到外部的存储系统，或者支持新的数据类型。Catalyst 同时支持基于规则和基于成本的优化（CBO）。

# Catalyst优化器

Catalyst 核心是树和操作树的规则的一个通用库。在框架的顶层，我们构建了专门用于关系型查询处理的库（例如，表达式，逻辑查询计划），以及处理查询执行的不同阶段的几组规则：分析，逻辑优化，物理计划和将部分查询编译为 Java 字节码的代码生成。

##  树

Catalyst 主要的数据类型是由节点对象构成的树。每个节点由一个节点类型和零到多个子节点组成。节点类型在 Scala 中被定义为 TreeNode 类的子类。这些对象是不可变的，可以使用函数式的转换对其进行操作，我们将在下一小节继续讨论。

举个简单的例子，假设我们有以下三个节点类型，可以用更简化的表达式表示为：

- Literal(value: Int)：代表常量
- Attribute(name: String)：代表输入一行数据的一个属性，例如：“x”
- Add(left: TreeNode, right: TreeNode)：对两个表达式加和

这些类构建成树；例如，表达式 x+(1+2)，可以在 Scala 代码中表示为：

```scala
Add(Attribute(x), Add(Literal(1), Literal(1)))
```

 ![Deep Dive into Spark SQL’s Catalyst Optimizer](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/spark_catalyst-iteblog.png)

## 规则

规则用于对树进行操作，其实际上是一个将一棵树转换为另外一棵树的方法。虽然规则可以在其输入树上运行任意的代码（假定该树只是一个 Scala 对象），但最常见的方式是使用一组模式匹配函数，找到并替换特定结构的子树。

模式匹配是许多函数式编程语言的特性，允许从代数数据类型的嵌套结构中进行值提取。在 Catalyst中，树提供的转换方法可以递归地应用模式匹配函数到树的所有节点。例如，我们可以实现一个常量之间叠加操作的规则：

```scala
tree.transform {
  case Add(Literal(c1), Literal(c2)) => Literal(c1+c2)
}
```

将其应用于树`x+(1+2)`将产生新树`x+3`。`case`这里的关键字是Scala的标准模式匹配语法，可用于匹配对象的类型，也可以为提取的值（例如这里的`c1`以及`c2`）指定名称。

传递给transform的模式匹配表达式是一个偏函数，这意味着它只需要匹配所有可能的输入树的子集。只要它通过模式匹配语法匹配到一颗子树，就会执行相应case语句后的逻辑对树进行转换。注意，这个偏函数可以很方便的匹配多个模式：

```scala
tree.transform {
 
  case Add(Literal(c1), Literal(c2)) => Literal(c1+c2)
 
  case Add(left, Literal(0)) => left
 
  case Add(Literal(0), right) => right
 
}
```

随着规则的执行，树可能会不断的发生变化，从而会出现新的模式，规则可能需要执行多次才能完全转换一棵树。Catalyst 将规则分成批次，执行各个批次直到达到一个固定的点，即应用规则之后树不再更新为止，或者达到指定的次数。

## 代码生成

​        在sparkSQL当中，spark2.0版本之前使用的是基于Volcano Iterator Model（参见 [《Volcano-An Extensible and Parallel Query Evaluation System》](https://link.juejin.cn?target=http%3A%2F%2Fpaperhub.s3.amazonaws.com%2Fdace52a42c07f7f8348b08dc2b186061.pdf)） 来实现sql的解析的，这个是由 Goetz Graefe 在 1993 年提出的。



Volcano Iterator Model 的优点是抽象起来很简单，很容易实现，而且可以通过任意组合算子来表达复杂的查询。但是缺点也很明显，存在大量的虚函数调用，会引起 CPU 的中断，最终影响了执行效率。[Databricks](https://link.juejin.cn?target=https%3A%2F%2Fdatabricks.com%2Fblog%2F2016%2F05%2F23%2Fapache-spark-as-a-compiler-joining-a-billion-rows-per-second-on-a-laptop.html)对比过使用 Volcano Iterator Model 和手写代码的执行效率，结果发现手写的代码执行效率要高出十倍。![Volcano vs hand-written code](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/volcano-vs-hand-written-code-1024x397.png)

Catalyst 依赖于 Scala 语言特定的属性 Quasiquotes 使得代码生成更加简单。Quasiquotes 允许在 Scala 语言中使用编程的方式构建抽象语法树（ASTs），然后可以在运行时提供给 Scala 编译器生成字节码。我们使用 Calalyst 将 SQL 表达式的树转换为 Scala 代码的 AST 评估表达式，然后编译并运行生成的代码。

Quasiquotes 会在编译时进行类型检查以确保只有合适的 ASTs 或者字面量能够被替换，这比字符串连接更有用，而且是直接生成 Scala AST 树而不是在运行时运行 Scala 解析器。此外，由于每个节点代码的生成规则不需要知晓其子节点是如何构建的，因此它们是高度可组合的。最后，如果 Catalyst 缺少表达式级别的优化，Scala 编译器会对代码进行进一步的优化。下图展示了 Quasiquotes 生成的代码性能近似于手动优化的程序性能。

![Screen-Shot-2015-04-12-at-8](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/Screen-Shot-2015-04-12-at-8.45.27-AM-300x129.png)






# Spark SQL对Catalyst的使用

一条SQL语句生成执行引擎可识别的程序，就离不开解析（Parser）、优化（Optimizer）、执行（Execution）这三大过程，而这些过程都是由Catalyst的规则完成的，每个过程可能会包含多个批次的规则。

下图展示了SparkSQL的工作原理。

![image-20231122160518813](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231122160518813.png)

 

一条由用户输入的SQL，到真实可调度执行的RDD DAG任务，需要经历以下五个阶段：

•     Parser阶段：将SparkSql字符串解析为一个抽象语法树/AST

•     Analyzer阶段：该阶段会遍历整个AST，并对AST上的每个节点进行数据类型的绑定以及函数绑定，然后根据元数据信息Catalog对数据表中的字段进行解析

•     Optimizer阶段：该阶段是Catalyst的核心，主要分为RBO和CBO两种优化策略，其中RBO是基于规则优化，CBO是基于代价优化

•     SparkPlanner阶段：优化后的逻辑执行计划OptimizedLogicalPlan依然是逻辑的，并不能被Spark系统理解，此时需要将OptimizedLogicalPlan转换成Physical plan（物理计划）

•     Query Execution阶段: 主要是执行前的一些Preparations优化，比如AQE, 列式执行支持，Exchange Reuse, CodeGen stages合并等

上述五个阶段中，除了Parser (由Antlr实现)，其他的每个阶段都是由一系列规则(Rule)构成，且这些规则被组织成多个batch，每个batch可以看作是一个子阶段，其执行策略不尽相同，有的只会执行一遍，有的会迭代执行直到满足一定条件。所谓的规则是一个模式匹配语句和相应替换逻辑，通过模式匹配获取满足规则的节点，并进行计划树的等价替换。

值得一提的是，在Spark2.2版本中，Catalyst优化器引入了新的扩展点，使得用户可以在Spark session中自定义自己的Parser，Analyzer，Optimizer以及Physical Planning Stragegy Rule，接下来我们再来介绍一下Spark Catalyst扩展点。

# 扩展Catalyst优化器

## 注册自定义规则

Spark Catalyst的扩展点在SPARK-18127中被引入，Spark用户可以在SQL处理的各个阶段扩展查询优化规则，例如改变查询计划，甚至引入新的优化策略。SparkSessionExtensions保存了所有用户自定义的扩展规则，自定义规则保存在成员变量中，对于不同阶段的自定义规则，SparkSessionExtensions提供了不同的接口。

- injectParser – 添加parser自定义规则，parser负责SQL解析

- Analyzer阶段有三个拓展点，分别的用处为
  - injectResolutionRule：可以在这鉴权，检查元信息
  - injectPostHocResolutionRule：添加Analyzer自定义规则到Resolution子阶段
  - injectCheckRule：检查是否还有未解析的节点

- injectOptimizerRule – 添加Optimizer自定义规则，Optimizer负责逻辑执行计划的优化。 

- injectPlannerStrategy – 添加Planner Strategy自定义规则，Planner负责根据Strategy生成物理执行计划。 

在 Spark 3.0 之后，又额外提供了一些其他拓展点

- injectColumnar：添加列式计算相关规则

- injectFunction：添加UDF相关规则

- injectQueryStagePrepRule：添加自适应执行相关规则

通过 Catalyst 扩展点，用户可以根据自己的需求来优化 Spark SQL 查询。尤其是Spark3.0之后，加入了对列式计算的支持，Query Execution模块在优化物理计划时会对物理计划应用一个名称为ColumnarRule的规则以支持列式计算，这个规则会逐个应用用户通过injectColumnar扩展点添加的自定义的规则。

当然，目前Catalyst也有一些限制，Catalyst 拓展只能在 Catalyst 框架下进行，具体表现为用户自定义规则的被限制在固定的位置、无法修改内置的规则等。

## 配置自定义规则

首先编写自定义的规则或者策略，其次需要写一个扩展类，在扩展类的apply方法中，通过SparkSessionExtensions提供的不同接口注册我们实现的规则或者策略，最后通过一个配置指定该类的完整类名，具体参数名为spark.sql.extensions。

#  总结

Catalyst优化器是SparkSQL的核心组件，它的优劣是SparkSQL执行性能的关键因素。  本文首先介绍了Catalyst优化器，然后介绍了SparkSQL是如何使用Catalyst优化器的，最后介绍了Catalyst提供的一些扩展点。Spark2.2引入的扩展点，使得用户可以在Spark session中自定义自己的Parser，Analyzer，Optimizer以及Physical Planning Stragegy Rule。Spark Catalyst高度的可扩展性使得我们可以非常方便的定制适合自己实际使用场景的SQL引擎，拓展了更多的可能性。

 

[如何扩展Spark Catalyst,抓取spark sql 语句，通过listenerBus发送sql event以及编写自定义的Spark SQL引擎](https://www.cnblogs.com/laoqing/p/16351482.html)