Scala的List总是不可变的，这和Java不同，因为Scala的List是设计给函数式风格的编程使用的

# List类型特点

scala里的List一点都不像Java里的List，scala里的List是不可变的，因此它的大小和元素都不可变。使用链表结构实现，通常使用head,tail和isEmpty方法，大多数操作都涉及到递归(把list分割为head和tail)

1. List类型是不可变列表，代表有序集合。
2. 该类有两个样例类scala.Nil和scala.::实现了抽象isEmpty,head和tail
3. 实现了后入先出(LIFO)栈式获取模式

## 创建List

创建List的八种方式



```scala
//1  head连接list的方式，最后必须是Nil
scala> val list = 1 :: 2 :: 3 :: Nil
list: List[Int] = List(1, 2, 3)
//2
scala> val list = List(1, 2, 3)
list: List[Int] = List(1, 2, 3)
//3a
scala> val x=List(1,2.0,33D,4000L)
x: List[Double] = List(1.0, 2.0, 33.0, 4000.0)
//3b
scala> val x = List[Number](1, 2.0, 33D, 4000L)
x: List[Number] = List(1, 2.0, 33.0, 4000)
//4
scala>  val x = List.range(1, 10)
x: List[Int] = List(1, 2, 3, 4, 5, 6, 7, 8, 9)
//5
scala> val x = List.range(0, 10, 2)
x: List[Int] = List(0, 2, 4, 6, 8)
//6
scala> val x = List.fill(3)("foo")
x: List[String] = List(foo, foo, foo)
//7
scala> val x = List.tabulate(5)(n => n * n)
x: List[Int] = List(0, 1, 4, 9, 16)
//8   很多集合类型都有这个方法
scala> val x = collection.mutable.ListBuffer(1, 2, 3).toList
x: List[Int] = List(1, 2, 3)

scala> "foo".toList
res4: List[Char] = List(f, o, o)
```

## 创建可变list

**使用ListBuffer，在需要的时候再转成List**

**ListBuffer适合在经常变化的场景下使用，大多数操作都是线性的，但是当使用下标索引元素时最好使用ArrayBuffer**

```scala
import scala.collection.mutable.ListBuffer
var fruits = new ListBuffer[String]()
// 添加一个元素
fruits += "Apple"
fruits += "Banana"
fruits += "Orange"
// 添加多个元素
fruits += ("Strawberry", "Kiwi", "Pineapple")
// 删除一个元素
fruits -= "Apple"
// 删除多个元素
fruits -= ("Banana", "Orange")
// 删除一个序列
fruits --= Seq("Kiwi", "Pineapple")
// 最后转成List
val fruitsList = fruits.toList
```

## list添加元素

**因为list是不可变的，所以我们没法真正的往List里添加元素，只能说在现有List上增加元素获取一个新的List**

使用::操作符在现有List前面添加元素

```scala
scala> val x = List(2)
x: List[Int] = List(2)
scala> val y = 1 :: x  //添加后赋值给新的变量，因为x不可变，当然也可以把x声明为var，从而可以还赋值给x
y: List[Int] = List(1, 2)
```

小提示： 通常以:结尾的运算符都是右结合的

除了::还有其他一些运算符可以实现LIst的添加

```scala
scala> val x = List(2)
x: List[Int] = List(2)
scala> val y = 1 :: x   //前面添加元素
y: List[Int] = List(1, 2)
scala> val y = x :+ 2   //后面添加元素
y: List[Int] = List(1, 2)

```

## 删除元素

因为list不可变，我们同样无法从list中真正删除元素，也是返回一个新的list

```scala
scala> var x = List(5, 1, 4, 3, 2)
x: List[Int] = List(5, 1, 4, 3, 2)
scala> x = x.filter(_ > 2) //x使用var修饰，所以可以还赋值给它
x: List[Int] = List(5, 4, 3)
```

但是如果经常修改元素，我们最好使用ListBuffer

```scala
scala> import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ListBuffer

scala> val x = ListBuffer(1, 2, 3, 4, 5, 6, 7, 8, 9)
x: scala.collection.mutable.ListBuffer[Int] = ListBuffer(1, 2, 3, 4, 5, 6, 7, 8, 9)

scala> x-=5  //按值删除一个
res12: x.type = ListBuffer(1, 2, 3, 4, 6, 7, 8, 9)

scala> x -= (2, 3) //按值删除两个
res13: x.type = ListBuffer(1, 4, 6, 7, 8, 9)

scala> x.remove(0) //按下标删除
res14: Int = 1

scala> x
res15: scala.collection.mutable.ListBuffer[Int] = ListBuffer(4, 6, 7, 8, 9)

scala> x.remove(1, 3) //从开始位置删除多个

scala> x
res17: scala.collection.mutable.ListBuffer[Int] = ListBuffer(4, 9)
```

## list合并

可以使用++, concat和:::合并两个list

```scala
scala> val a=List(1,2,3)
scala> val b=List(4,5,6)
scala> val c=a ++ b
scala> val c=a ::: b
scala> val c= List.concat(a,b)

```

## 使用Stream，懒惰版的List

Stream用法和List类似，只是它使用 #::构造而不是::

```scala
val stream = 1 #:: 2 #:: 3 #:: Stream.empty
```

## Demo

```scala
(一)声明
scala> val fruit=List("Apple","Banana","Orange")
fruit: List[String] = List(Apple, Banana, Orange)
//和下面的等效
scala> val fruit=List.apply("Apple","Banana","Orange")
fruit: List[String] = List(Apple, Banana, Orange)
（二）元素访问
scala>fruit(0)
res2: String = Apple
//不可以更改,即使把fruit声明为var.   fruit(0)="xxx"会报错
（三）遍历List , List嵌套
scala> val list = List(List(1, 2, 3), List("adfa", "asdfa", "asdf"))
list: List[List[Any]] = List(List(1, 2, 3), List(adfa, asdfa, asdf))

scala> for(i <- list; from=i; j<-from)println(j)
1
2
3
adfa
asdfa
asdf

```

# List具有递归结构

List具有协变性

即对于类型S和T，如果S是T的子类型，则List[S]也是List[T]的子类型。

空的List,其类行为Nothing,Nothing在Scala的继承层次中的最底层

# List常用构造方法

```scala
scala> val nums = 1 :: (2:: (3:: (4 :: Nil)))
nums: List[Int] = List(1, 2, 3, 4)
//::操作符的优先级从右至左，上面和下面等效
scala> val nums = 1::2::3::4::Nil
nums: List[Int] = List(1, 2, 3, 4)

```

# List常用操作

```scala
nums.isEmpty
nums.head
nums.tail.head  //取第二个元素
nums.tail.tail.head //取第三个元素
nums.head::(3::nums.tail)  //在第二个位置插入一个元素
//连接操作
List(1, 2, 3):::List(4, 5, 6)
nums.init  //去除最后一个元素
nums.last  //最后一个元素
nums.reverse   //反转
nums drop 3  //丢弃前3个 nums.drop(3)
nums take 1  //获取前n个 nums.take(1)
nums.splitAt(2)//分割列表
nums.toArray  //转成数组
nums.toString
nums.mkString
//zip操作
scala> val nums=List(1,2,3,4)
nums: List[Int] = List(1, 2, 3, 4)
scala> val chars=List('1','2','3','4')
chars: List[Char] = List(1, 2, 3, 4)
scala> nums zip chars
res2: List[(Int, Char)] = List((1,1), (2,2), (3,3), (4,4))


```

# List伴生对象方法

```scala
scala>  List.apply(1, 2, 3)
res3: List[Int] = List(1, 2, 3)

scala> List.range(2, 6)
res4: List[Int] = List(2, 3, 4, 5)

scala> List.range(2, 6,2)
res5: List[Int] = List(2, 4)

scala> List.range(2, 6,-1)
res6: List[Int] = List()
```

