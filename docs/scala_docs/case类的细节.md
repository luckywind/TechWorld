*本质上case class是个语法糖，对你的类构造参数增加了getter访问，还有toString, hashCode, equals 等方法； 最重要的是帮你实现了一个伴生对象，这个伴生对象里定义了*

- apply方法：意味着你不需要使用new关键字就能创建该类对象

- unapply方法：可以通过模式匹配获取类属性



# 另一种定义方式

```scala
case class Person( lastname: String )( firstname: String, birthYear: Int )
```

[case class](https://blog.csdn.net/hellojoy/article/details/81034528)

上文提到的所有 case class 的特性在这种定义方式下只作用于第一个参数列表中的参数（比如在参数前自动加 val，模式匹配，copy 支持等等），第二个及之后的参数列表中的参数和普通的 class 参数列表参数无异。
