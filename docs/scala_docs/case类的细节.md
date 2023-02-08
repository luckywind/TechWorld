*本质上case class是个语法糖，对你的类构造参数增加了getter访问，还有toString, hashCode, equals 等方法； 最重要的是帮你实现了一个伴生对象，这个伴生对象里定义了*

- apply方法：意味着你不需要使用new关键字就能创建该类对象

- unapply方法：可以通过模式匹配获取类属性



