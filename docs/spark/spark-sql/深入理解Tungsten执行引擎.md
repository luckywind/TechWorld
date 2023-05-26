[参考](https://www.databricks.com/blog/2016/05/23/apache-spark-as-a-compiler-joining-a-billion-rows-per-second-on-a-laptop.html)

大量cpu时间花在如下的无用功上：

1. 虚函数调用
2. 读写中间结果到缓存/内存上

Tungsten 运行时优化子节码、把整个查询折叠为一个函数，消除虚函数调用，中间数据使用寄存器。这种策略就是全代码生成，大大提升了CPU效率。

# Volcano Iterator Model

![Volcano Iterator Model](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/volcano-iterator-model.png)

一个query包含多个算子，每个算子提供一个next()接口，每次返回一个tuple, 例如，Filter算子的代码：

```scala
class Filter(child: Operator, predicate: (Row => Boolean))
extends Operator {
def next(): Row = {
var current = child.next()
while (current != null && !predicate(current)) {
current = child.next()
}
return current
}
}
```

这个接口允许执行引擎优雅的组合任意算子而不担心类型问题，因此火山模型成为一个标准的数据库系统。

## 与手写代码对比

假如我们用代码写上面那个sql的逻辑，性能会有多大差距呢？ 

```scala
var count = 0
for (ss_item_sk in store_sales) {
if (ss_item_sk == 1000) {
count += 1
}
}
```

性能对比

![Volcano vs hand-written code](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/volcano-vs-hand-written-code-1024x397.png)

显然手写代码快得多，为什么？

1. 没有虚函数调用
   火山模型中，处理一个tuple需要调用next()至少一次，这个函数被编译器实现为一个虚函数(通过vtable).手写代码没有函数调用。 虽然虚函数经过了很多优化，但是仍然消耗多个cpu指令，况且被大量调用
2. 中间数据
   火山模型中，每次算子传递一个tuple到另一个算子时，需要把tuple放到内存(函数栈)；而手写代码，JVM实际上把数据放到了CPU寄存器。CPU从内存获取数据要比从寄存器获取慢得多。
3. Loop unrolling(循环展开) and SIMD(单指令多数据流)
   编译器通常自动展开循环、甚至生成SIMD指令，用一个指令完成多个tuple的处理。CPU特性例如pipeline、prefetching以及指令重排，使得执行循环非常高效。 然而，编译器和CPU对火山模型依赖的复杂函数调用图效率低

# Whole-stage Code Generation

目标就是自动生成手写代码，具体就是这些算子在运行时生成代码并折叠成一个函数。例如上面的查询，是一个stage，spark将生成下面的代码：

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/whole-stage-code-generation-model.png)

```scala
spark.range(1000).filter("id > 100").selectExpr("sum(id)").explain()

== Physical Plan ==
*Aggregate(functions=[sum(id#201L)])
+- Exchange SinglePartition, None
   +- *Aggregate(functions=[sum(id#201L)])
      +- *Filter (id#201L > 100)
         +- *Range 0, 1, 3, 1000, [id#201L]
```

从物理计划上看，Range/Filter/Aggregate都启用了代码生成，Exchange因为要跨网络发送数据没有生成代码。





# 向量化

​        全代码生成对大量大数据量的简单谓词操作非常高效，但是对复杂算子(csv解析，parquet解压缩)或者集成三方组件的代码无法折叠成一个函数。

​        为了提升这些场景的性能，就需要向量化技术， 主要思想是不再逐条处理数据，而是引擎把一批数据打包为columnar格式，每个算子只在batch内执行一个简单的循环。这些简单循环会启用前面提到的编译器和CPU的优化。

例如，一个三列的表，下面展示了行格式和列格式的内存布局

![Memory layout in row and column formats](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/memory-layout-in-row-and-column-formats.png)

# Tungsten项目

三大优化

1.  内存管理和二进制处理
   