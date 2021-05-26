# 包

隐式导入了以下包：

```scala
package java.lang 
package scala  
scala.Predef 对象
```

需要声明引入的包

***1.scala.collection\*** （该包和其子包 包含了 Scala 集合框架）

```
scala.collection.immutable —— 不可变的顺序数据结构，如Vector，List，Range，HashMap或HashSet
scala.collection.mutable —— 可变的顺序数据结构，如ArrayBuffer，StringBuilder，HashMap或HashSet
scala.collection.concurrent —— 可变的并发数据结构，如TrieMap
scala.collection.parallel.immutable —— 不可变的并行数据结构，如ParVector，ParRange，ParHashMap或ParHashSet
scala.collection.parallel.mutable —— 可变的并行数据结构，如ParArray，ParHashMap，ParTrieMap或ParHashSet
```

***2.scala.concurrent\***—— 并行编程的基础类例如 Futures 和 Promises

***3.scala.io\*** —— 输入和输出操作
***4.scala.math\***—— 基本数学函数和附加数字类型，如BigInt和BigDecimal
***5.scala.sys\*** —— 与其他进程和操作系统进行交互
***6.scala.util.matching\*** ——常规操作包

# 集合

默认情况下，Scala 一直采用不可变集合类。另外如果你写迭代，你也会得到一个不可变的迭代集合类，这是由于这些类在从scala中导入的时候都是默认绑定的。为了得到可变的默认版本，你需要显式的声明`collection.mutable.Set`或`collection.mutable.Iterable`

一个有用的约定，如果你想要同时使用可变和不可变集合类，只导入collection.mutable包即可。
import scala.collection.mutable

要使用可变Set,只需要用mutable.Set即可。

collection.generic包包含了用于实现集合的构建块，用户只需要特殊情况下才引用它。

scala有类型别名规则，因此可以简单引用，例如List

```scala
scala.conllection.immutable.List //定义所在位置
scala.List
List  //因为scala._总是被自动引入
```



## 不可变集合

蓝色是特质，黑色是实现
粗黑线 是默认实现关系

![collections-immutable-diagram](https://gitee.com/luckywind/PigGo/raw/master/image/collections-immutable-diagram.svg)

## 可变集合

![image-20210204145818824](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210204145818824.png)

## Traversable特质

Traversable（遍历）是容器(collection)类的最高级别特性，它唯一的抽象操作是foreach:

```scala
def foreach[U](f: Elem => U)
```

1. 通过par方法得到并行集合
2. Traversable可以直接实例化一个对象，内部其实使用了伴生对象的apply方法

# scala容器类体系结构

[官网](https://docs.scala-lang.org/zh-cn/overviews/core/architecture-of-scala-collections.html)

scala容器框架的设计原则目标就是尽量避免重复，在尽可能少的地方定义操作。设计中使用的方法是，在 Collection 模板中实现大部分的操作，这样就可以灵活的从独立的基类和实现中继承。后面的部分，我们会来详细阐述框架的各组成部分：模板(templates)、类(classes)以及trait

## Builders

几乎所有的 Collection 操作都由遍历器（traversals）和构建器 （builders）来完成.

traversal的foreach方法负责遍历， builder负责构建容器。

```scala
package scala.collection.mutable

class Builder[-Elem, +To] {
  def +=(elem: Elem): this.type   // 添加元素
  def result(): To          	//返回一个集合
  def clear(): Unit           //重置成空状态
  def mapResult[NewTo](f: To => NewTo): Builder[Elem, NewTo] = ...
  //一个builder可以使用其他的builder来组合一个容器的元素，但是如果想要把其他builder返回的结果进行转换，例如，转成另一种类型，就需要使用Builder类的mapResult方法
}
```

## 分解通用操作

### TraversableLike类概述

```scala
package scala.collection

class TraversableLike[+Elem, +Repr] {
  def newBuilder: Builder[Elem, Repr] // deferred
  def foreach[U](f: Elem => U) // deferred
          ...
  def filter(p: Elem => Boolean): Repr = {
    val b = newBuilder
    foreach { elem => if (p(elem)) b += elem }
    b.result
  }
}
```

Collection库重构的主要设计目标是在拥有自然类型的同时又尽可能的共享代码实现。Scala的Collection 遵从“结果类型相同”的原则：只要可能，容器上的转换方法最后都会生成相同类型的Collection。例如，过滤操作对各种Collection类型都应该产生相同类型的实例。在List上应用过滤器应该获得List，在Map上应用过滤器，应该获得Map

Scala的 Collection 库通过在 trait 实现中使用通用的构建器（builders）和遍历器（traversals）来避免代码重复、实现“结果类型相同”的原则。这些Trait的名字都有Like后缀。例如：IndexedSeqLike 是 IndexedSeq 的 trait 实现，再如，TraversableLike 是 Traversable 的 trait 实现。和普通的 Collection 只有一个类型参数不同，trait实现有两个类型参数。他们不仅参数化了容器的成员类型，也参数化了 Collection 所代表的类型，就像下面的 Seq[I] 或 List[T]。下面是TraversableLike开头的描述：

```scala
trait TraversableLike[+Elem, +Repr] { ... }
```

