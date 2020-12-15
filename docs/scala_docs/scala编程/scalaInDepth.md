# 规则

REPL解决饥饿解析：

1. 容器对象

2. :paste

表达式与语句：
表达式返回值，语句只执行而不返回值
不要使用return

可变性：
面向表达式编程混合可变性、或者修改对象状态的能力时，更有趣，因为利用可变对象的代码往往是命令式的。命令式代码通常是语句而不是表达式，对象创建时有状态。执行语句修改对象状态。

不可变的优势：

1. 对象相等。默认，scala使用对象地址和哈希值判断对象是否相等。

对比Java,scala利用java.lang.Object相同的equals和hashCode，但是scala还抽象一些原语使得它们成为完整的对象。编译器会自动装箱、拆箱。这些原语对象都是scala.AnyVal的子类，而继承java.lang.Object的“标准”对象都是scala.AnyRef的子类。AnyRef可以认为是java.lang.Object的别名。因为AnyRef定义了hashCode和equals，scala提供##(hashCode)和==对于AnyRef和AnyVal都可以使用。

   hashCode和equals应该匹配。x==y 一定保证hashCode相同，即x.##==y.##.
hashMap使用对象插入时的hash值，且不会随着对象的更新而更新(对象更新了，在hashMap中就找不到它了)。因此在实现equals方法时，严格遵守：

1. equals => 相同的hashCode
2. 对象的hashCode不会发生变化
3. 如果对象发送到其他JVM,需要使用它们都能访问的属性判断相等性。

## None代替null

scala.Opthon可以当作something/nothing的一个容器，有两个子类：Some和None，Some代表一个对象的容器，None代表一个空容器。java中习惯使用null初始化对象，scala中可以使用Option.

Option伴生对象提供工厂方法把null变成Option, 其他java对象变成Some。

```scala
scala> var x : Option[String] = Option(null)
        x: Option[String] = None
scala> x = Option("Initialized")      // Option.apply(“Initialized”) 
x: Option[String] = Some(Initialized)
```

### 高级Option技巧

Option最大的特点是可以当作集合使用，即可以执行map/flatMap/foreach等。

1. 创建或者返回默认

   ```scala
    def getTemporaryDirectory(tmpArg: Option[String]): java.io.File = {
     
   tmpArg.map(name => new java.io.File(name)).    //Create if defined 
       filter(_.isDirectory).   								//Only directories
       getOrElse(new java.io.File(            //Specify default
         System.getProperty("java.io.tmpdir")))
         }
   ```

2. 根据变量是否初始化执行代码块

   ```scala
   val username: Option[String] = ...
   for(uname <- username) {
      println("User: " + uname)
   }
   ```

3. 





