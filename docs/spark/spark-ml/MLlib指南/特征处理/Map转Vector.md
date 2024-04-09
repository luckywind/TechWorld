没有内置的函数可完成这个任务，可用udf实现：
```scala
val toVector = udf((m: Map[String, Double]) => Vectors.dense(m.values.toArray).toSparse)
```

注意，要求value是Double类型



把value放到一个数组，然后丢掉0

```scala
  private val m = Map("k1" -> 1.0, "k2" -> 2.0)
  println(m.values.toArray.mkString(","))

  private val dense: linalg.Vector = Vectors.dense(m.values.toArray)
  println(dense.toString)
  println(dense.toSparse)
1.0,2.0
[1.0,2.0]
(2,[0,1],[1.0,2.0])
```

