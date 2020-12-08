Scala中的 apply 方法有着不同的含义, 对于函数来说apply方法意味着调用function本身；scala中函数也是对象, 每一个对象都是scala.FunctionN(1-22)的实例, 其中N是函数参数的数量。

但是如果每次调用方法对象都要通过FunctionN.apply(x, y...), 就会略显啰嗦, Scala提供一种模仿函数调用的格式来调用函数对象: f(3)


Iterator迭代器的几个子trait包括Seq, Set, Map都继承PartialFunction并实现了apply方法, 不同的是实现的方式不一样

```scala
val list1=List.apply(1,2,3,4)  //创建
Seq(1,2,3).apply(1)     //检索  输出2
Set(1,2,3).apply(1)     //判断是否存在，输出true
Map("china"->"beijing","US"->"Washington").apply("US")  //查找  输出Washington
```



