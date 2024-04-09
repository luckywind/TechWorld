[参考](https://www.codenong.com/cs105935880/)

RDDOperationScope.withScopeU(body)的中就是用到了柯里化的技术，他的作用就是把公共部分(函数体)抽出来封装成方法（这里的公共部分我的理解就是sc，也就是sparkCont）, 把非公共部分通过函数值传进来（这里就是body的内容了），为什么要这么做呢：
withScope是最近的发现版中新增加的一个模块，它是用来做DAG可视化的（DAG visualization on SparkUI）
以前的sparkUI中只有stage的执行情况，也就是说我们不可以看到上个RDD到下个RDD的具体信息。于是为了在
sparkUI中能展示更多的信息。所以把所有创建的RDD的方法都包裹起来，同时用RDDOperationScope 记录 RDD 的操作历史和关联，就能达成目标。





```scala
object TestWithScope {
    def withScope(func: => String) = {
        println("withscope")
        func
    }

    def bar(foo: String) = withScope {
        println("Bar: " + foo)
        "BBBB"
    }

    def main(args: Array[String]): Unit = {
        println(bar("AAAA"));
    }
}
```

