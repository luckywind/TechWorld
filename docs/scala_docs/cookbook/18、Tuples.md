scala中，tuple也是值，只不过它包含了几个元素。 scala使用一系列类，Tuple2,Tuple3,....Tuple22来表示Tuple

# 使用tuple

```scala
val ingredient = ("Sugar" , 25) //声明tuple
println(ingredient._1) // Sugar
println(ingredient._2) // 25
```

## 模式匹配

```scala
scala> val ingredient = ("Sugar" , 25)
ingredient: (String, Int) = (Sugar,25)

scala> println(ingredient._1)
Sugar

scala> val (name, quantity) = ingredient
name: String = Sugar
quantity: Int = 25
```

```scala
val planets =
  List(("Mercury", 57.9), ("Venus", 108.2), ("Earth", 149.6),
       ("Mars", 227.9), ("Jupiter", 778.3))
planets.foreach{
  case ("Earth", distance) =>
    println(s"Our planet is $distance million kilometers from the sun")
  case _ =>
}
```

For循环中

```scala
val numPairs = List((2, 5), (3, -7), (20, 56))
for ((a, b) <- numPairs) {
  println(a * b)
}
```

## tuples和case class

case class的每个字段都有个名字，比tuple可读性强