[参考](http://allaboutscala.com/scala-cheatsheet/#scalatest-introduction)

ScalaTest是scala生态里的单元测试框架，[官方文档](https://www.scalatest.org/)

# maven依赖

```xml
<dependency>
  <groupId>org.scalactic</groupId>
  <artifactId>scalactic_2.13</artifactId>
  <version>3.2.0</version>
</dependency>
<dependency>
  <groupId>org.scalatest</groupId>
  <artifactId>scalatest_2.13</artifactId>
  <version>3.2.0</version>
  <scope>test</scope>
</dependency>
```

# 使用

1. 核心概念是 套件：多个测试用例的集合



三个步骤

1. [Select your testing styles](https://www.scalatest.org/user_guide/selecting_a_style)
2. [Define your base classes](https://www.scalatest.org/user_guide/defining_base_classes)
3. [Start writing tests](https://www.scalatest.org/user_guide/writing_your_first_test)

## 选一个测试风格

其实测试风格只影响到测试的声明方式，其他都是一样的。推荐FlatSpec风格，它和Junit一样没有嵌套测试。

它的风格就是“X should Y”, "A must B"等

例如：

```scala
import org.scalatest.flatspec.AnyFlatSpec
class SetSpec extends AnyFlatSpec {
  "An empty Set" should "have size 0" in {
    assert(Set.empty.size == 0)
  }
  it should "produce NoSuchElementException when head is invoked" in {
    assertThrows[NoSuchElementException] {
      Set.empty.head
    }
  }
}
```

## 写法

1. 定义一个测试类，继承样式类，例如AnyFlatSpec

```scala
import org.scalatest.flatspec.AnyFlatSpec

class FirstSpec extends AnyFlatSpec {
  // tests go here...
}
```

2. 每一个测试包含一句话以及一个代码块，这句话描述了期望的结果。这句话由一个主语、一个动词(should、must、can)和描述组成。 如果对这个主语有多个测试，后面的测试可以使用it代替

```scala
import collection.mutable.Stack
import org.scalatest.flatspec.AnyFlatSpec

class StackSpec extends AnyFlatSpec {

  "A Stack" should "pop values in last-in-first-out order" in {
    val stack = new Stack[Int]
    stack.push(1)
    stack.push(2)
    assert(stack.pop() === 2)
    assert(stack.pop() === 1)
  }

  it should "throw NoSuchElementException if an empty stack is popped" in {
    val emptyStack = new Stack[String]
    assertThrows[NoSuchElementException] {
      emptyStack.pop()
    }
  }
}
```



