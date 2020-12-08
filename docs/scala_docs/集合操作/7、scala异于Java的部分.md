[原文](https://www.jianshu.com/p/116e26e668df)

1. List一旦被定义，其值就不能变化，因此声明时需要初始化。注意：**这里说的值不能变是指所指向的对象地址不能变，对象本省的值是可以发生变化！！**
2. Set是不重复元素的容器，按照插入顺序排序。 有可变版本和不可变版本，默认不可变版本。不可变指的是对象所开辟的空间不可变，添加了元素后相当于重新生成了新的Set在新的空间
3. Map默认不可变
4. Iterator   不是一个集合，但提供一个访问集合的方法，两个基本操作：next和hasNext

```scala
while循环来访问：
val iter = Iterator("Hadoop", "Spark")
while (iter.hasNext) {
    println(iter.next())
}

for循环来访问：
val iter = Iterator("Hadoop", "Spark")
for (elem <- iter) {
    println(elem)
}
```

Iterator继承自Iterable类，其类提供两个方法返回一个迭代器（这个返回的迭代器不是单个元素，而是原容器元素的全部子序列）：grouped和sliding。
 groubed返回元素的增量分块；
 sliding返回一个滑动元素的窗口。

```scala
val list1 = List(1,2,3,4,5)
val list_g = list1 grouped 3
list_g.next()
    -->输出：List(1，2，3)
list_g.next()
    -->输出：List(4，5)

val list_s = list1 sliding 3
list_s.next()
    -->输出：List(1，2，3)
list_s.next()
    -->输出：List(2，3，4)
```

5. Array/ArrayBuffer

Array定义的数组属于定长数组，一旦初始化，不可改变。
Scala中可以不指明初始化数组的类型，Scala可以根据数据元素自行推断元素类型

ArrayBuffer是变长数组

