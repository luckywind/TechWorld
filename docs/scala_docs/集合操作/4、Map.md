# Map

scala的Map和Java的Map也不像，scala的Map仍然是不可变

## 创建Map

创建Map不需要import引入，因为Predef对象为不可变的Map定义了别名

```scala
type Map[A, +B] = immutable.Map[A, B]
val Map = immutable.Map
```

所以可以直接使用：

```scala
scala> val states = Map("AL" -> "Alabama", "AK" -> "Alaska")
states: scala.collection.immutable.Map[String,String] = Map(AL -> Alabama, AK -> Alaska)
```

创建可变Map

```scala
var states = collection.mutable.Map[String, String]()
states += ("AL" -> "Alabama")
```
当然我们也可以给类型起一个别名，来方便的创建可变Map

```scala
import scala.collection.mutable.{Map => MMap}
object Test extends App {
val m = MMap(1 -> 'a')
for((k,v) <- m) println(s"$k, $v")
}
```

## 选择一个map实现

scala有丰富的map类型， 我们还可以使用Java中的map类，如果我们不关心排序和插入顺序，仅仅想要一个简单的map，那就可以选择默认的不可变map或者可变map。但是如果需要有序的Map，使用SortedMap

```scala
scala> val grades = SortedMap("Kim" -> 90,
     | "AL"->95,
     | "Emily"->91)
grades: scala.collection.SortedMap[String,Int] = TreeMap(AL -> 95, Emily -> 91, Kim -> 90)

```

LinkedHashMap或者ListMap可以记住插入顺序，scala只有可变版本的LinkedHashMap,底层使用Hash表实现，而ListMap使用List结构实现。

```scala

scala>  import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.LinkedHashMap

scala> var states = LinkedHashMap("IL" -> "Illinois")
states: scala.collection.mutable.LinkedHashMap[String,String] = LinkedHashMap(IL -> Illinois)

scala> states += ("KY" -> "Kentucky")
res27: scala.collection.mutable.LinkedHashMap[String,String] = LinkedHashMap(IL -> Illinois, KY -> Kentucky)

scala> states += ("TX" -> "Texas")
res28: scala.collection.mutable.LinkedHashMap[String,String] = LinkedHashMap(IL -> Illinois, KY -> Kentucky, TX -> Texas)
```



ListMap有可变和不可变两个版本

```scala
scala> import scala.collection.mutable.ListMap
import scala.collection.mutable.ListMap

scala> var states = ListMap("IL" -> "Illinois")
                    ^
       warning: object ListMap in package mutable is deprecated (since 2.13.0): Use an immutable.ListMap assigned to a var instead of mutable.ListMap
states: scala.collection.mutable.ListMap[String,String] = ListMap(IL -> Illinois)

scala> states += ("KY" -> "Kentucky") //注意哦，从头部插入的
res29: scala.collection.mutable.ListMap[String,String] = ListMap(KY -> Kentucky, IL -> Illinois)
```

## 可变map的增删改

```scala
//构造
scala> var states = scala.collection.mutable.Map[String, String]()
states: scala.collection.mutable.Map[String,String] = HashMap()
//增
scala> states("AK") = "Alaska"

scala>  states += ("AL" -> "Alabama")
res31: scala.collection.mutable.Map[String,String] = HashMap(AK -> Alaska, AL -> Alabama)

scala> states ++= List("CA" -> "California", "CO" -> "Colorado")
res32: scala.collection.mutable.Map[String,String] = HashMap(AK -> Alaska, AL -> Alabama, CO -> Colorado, CA -> California)
//删
scala> states -= "AR"
res33: scala.collection.mutable.Map[String,String] = HashMap(AK -> Alaska, AL -> Alabama, CO -> Colorado, CA -> California)
//改
scala> states("AK") = "Alaska, A Really Big State"
```

除此之外，还可以使用方法来操作map，例如，put、reatin、remove和clear。

## 不可变map的增删改

和可变的用法类似，只是如果是不可变变量，修改map后，需要赋值给一个新的变量，这里就不举例子了

## 获取map值

```scala
val states = Map("AL" -> "Alabama", "AK" -> "Alaska", "AZ" -> "Arizona")
val az = states("AZ")
val states = Map("AL" -> "Alabama").withDefaultValue("Not found")//不存在时提供默认值而不是报错
```

## 遍历map

```scala
scala> val ratings = Map("Lady in the Water"-> 3.0,
     | "Snakes on a Plane"-> 4.0,
     | "You, Me and Dupree"-> 3.5)
ratings: scala.collection.immutable.Map[String,Double] = Map(Lady in the Water -> 3.0, Snakes on a Plane -> 4.0, You, Me and Dupree -> 3.5)
//for循环
scala> for ((k,v) <- ratings) println(s"key: $k, value: $v")
key: Lady in the Water, value: 3.0
key: Snakes on a Plane, value: 4.0
key: You, Me and Dupree, value: 3.5
//模式匹配
scala> ratings.foreach {
     | case(movie, rating) => println(s"key: $movie, value: $rating")
     | }
key: Lady in the Water, value: 3.0
key: Snakes on a Plane, value: 4.0
key: You, Me and Dupree, value: 3.5
//Tuple方式获取
scala> ratings.foreach(x => println(s"key: ${x._1}, value: ${x._2}"))
key: Lady in the Water, value: 3.0
key: Snakes on a Plane, value: 4.0
key: You, Me and Dupree, value: 3.5
//遍历key
scala> ratings.keys.foreach((movie) => println(movie))
Lady in the Water
Snakes on a Plane
You, Me and Dupree
```

## 获取key或者value集合

```scala
scala> val states = Map("AK" -> "Alaska", "AL" -> "Alabama", "AR" -> "Arkansas")
states: scala.collection.immutable.Map[String,String] = Map(AK -> Alaska, AL -> Alabama, AR -> Arkansas)
//获取key集合
scala>  states.keySet
res41: scala.collection.immutable.Set[String] = Set(AK, AL, AR)

scala> states.keys
res42: Iterable[String] = Set(AK, AL, AR)

scala> states.keysIterator
res43: Iterator[String] = <iterator>
```



## key的存在性检测

```scala
//方式一 
if (states.contains("FOO")) println("Found foo") else println("No foo")
//方式二
states.valuesIterator.exists(_.contains("ucky"))
```



## map过滤和转换

```scala
scala>  var x = collection.mutable.Map(1 -> "a", 2 -> "b", 3 -> "c")
x: scala.collection.mutable.Map[Int,String] = HashMap(1 -> a, 2 -> b, 3 -> c)
//对于可变map直接使用filterInPlace方法
scala> x.filterInPlace((k,v) => k > 1)
res45: scala.collection.mutable.Map[Int,String] = HashMap(2 -> b, 3 -> c)
//使用mapValuesInPlace 转换map
scala> x.mapValuesInPlace((k,v) => v.toUpperCase)
res47: scala.collection.mutable.Map[Int,String] = HashMap(2 -> B, 3 -> C)
```

## map排序

```scala
scala> val grades=Map("Kim" ->90,"Al"->85)
grades: scala.collection.immutable.Map[String,Int] = Map(Kim -> 90, Al -> 85)

scala> import scala.collection.immutable.ListMap
import scala.collection.immutable.ListMap

scala> ListMap(grades.toSeq.sortBy(_._1):_*)
res50: scala.collection.immutable.ListMap[String,Int] = ListMap(Al -> 85, Kim -> 90)
//增序排序
scala> ListMap(grades.toSeq.sortWith(_._1 < _._1):_*)
res51: scala.collection.immutable.ListMap[String,Int] = ListMap(Al -> 85, Kim -> 90)

//按value排序
scala> ListMap(grades.toSeq.sortBy(_._2):_*)
res52: scala.collection.immutable.ListMap[String,Int] = ListMap(Al -> 85, Kim -> 90)

scala> ListMap(grades.toSeq.sortWith(_._2 < _._2):_*)
res53: scala.collection.immutable.ListMap[String,Int] = ListMap(Al -> 85, Kim -> 90)
```

## 最大值

```scala

scala>grades.max  //默认取key最大的对
res54: (String, Int) = (Kim,90)

scala>  grades.valuesIterator.max  //最大的值
res55: Int = 90
```

