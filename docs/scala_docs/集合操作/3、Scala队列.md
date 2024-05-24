介绍：队列是一个有序列表，在底层用数组或是链表来实现，输入、输出遵循先进先出的原则

在Scala中，有可变和不可变，一般来说，在开发中通常用可变集合的队列

```scala
scala> import scala.collection.mutable
import scala.collection.mutable

scala> val q1= new mutable.Queue[Int]     //创建队列
q1: scala.collection.mutable.Queue[Int] = Queue()

scala> q1+=9                             //单元素入队
res10: q1.type = Queue(9)

scala> q1++=List(1,2,3)                 //列表入队
res11: q1.type = Queue(9, 1, 2, 3)

scala> val  q2 = q1.dequeue()           //出队
q2: Int = 9

scala> q1
res12: scala.collection.mutable.Queue[Int] = Queue(1, 2, 3)

scala> q1.enqueue(88,99,100)            //批量入队
res13: q1.type = Queue(1, 2, 3, 88, 99, 100)

scala> 
```

