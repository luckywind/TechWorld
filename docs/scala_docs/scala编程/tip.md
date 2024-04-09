Scala中的 apply 方法有着不同的含义, 对于函数来说apply方法意味着调用function本身；scala中函数也是对象, 每一个对象都是scala.FunctionN(1-22)的实例, 其中N是函数参数的数量。

但是如果每次调用方法对象都要通过FunctionN.apply(x, y...), 就会略显啰嗦, Scala提供一种模仿函数调用的格式来调用函数对象: f(3)


Iterator迭代器的几个子trait包括Seq, Set, Map都继承PartialFunction并实现了apply方法, 不同的是实现的方式不一样

```scala
val list1=List.apply(1,2,3,4)  //创建
Seq(1,2,3).apply(1)     //检索  输出2
Set(1,2,3).apply(1)     //判断是否存在，输出true
Map("china"->"beijing","US"->"Washington").apply("US")  //查找  输出Washington
```



# 日期范围

```scala
    def loadXActiveBetween(curStart: DateTime, curEnd: DateTime):RDD[DwmDvcOtDeviceActiveChain] = {
        val days = Days.daysBetween(curStart,curEnd).getDays
        val rddList = (0 until days).map(curStart.plusDays(_).toString(DateUtils.DATE_FORMAT)).map(loadXActive(_)).filter(_!=null)
        rddList.reduce(_ union _)
    }
```



# while循环

```scala
    var real="-2621139991315662769"
    var uuid=""
    import scala.util.control._
    val loop = new Breaks;
    loop.breakable {
      while (true) {
        uuid = calUuid(OneIdType.MID.getValue, "16953773").toString
        println(uuid)
        if (!uuid.eq(real)) {
          loop.break()
          println(uuid)
        }
      }
    }
```

# 查看scala源码

[参考](https://stackoverflow.com/questions/13520532/attaching-sources-in-intellij-idea-for-scala-project)

![image-20211206222614684](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20211206222614684.png)
