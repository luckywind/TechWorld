scala里默认引入了`java.lang._`和`scala._`,另外scala.Predef对象也被引入以支持隐式转换。

# 使用{}打包

scala太灵活了，一个文件里可以声明多个包，包之间可以嵌套

```scala
 // 包含一个类的包
package orderentry {
class Foo { override def toString = "I am orderentry.Foo" }
}
    // 包嵌套
package customers {
    class Foo { override def toString = "I am customers.Foo" }
    package database {
    // this Foo is different than customers.Foo or orderentry.Foo
    class Foo { override def toString = "I am customers.database.Foo" }
    } 
}
    // 测试对象
object PackageTests extends App { 
  println(new orderentry.Foo)
  println(new customers.Foo)
println(new customers.database.Foo)
}
```

# Import重命名

使用=>符号重命名

```scala
import java.util.{ArrayList => JavaList}
```

# 静态导入

静态导入后，可以直接调用方法而无需使用类名

```scala
import java.lang.Math._
val a = sin(0)
a: Double = 0.0
```

