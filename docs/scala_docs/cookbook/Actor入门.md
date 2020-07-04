Akka模型使用Actor实现

# 什么是Actor

1. 是消息并发模型
2. 实现并行编程的功能，基于事件模型的并发机制
3. 使scala更容易实现多线程应用的开发

通过复制不可变状态的资源的一个副本，在基于Actor的消息发送、接收机制进行并行编程。

避免锁和共享资源。

Actor方法执行顺序：

1. 调用start()方法启动Actor
2. 执行act()方法
3. 向Actor发送消息

发送消息的方式

1. ！ 发送异步消息，没有返回值
2. ！？发送同步消息，等待返回值
3. ！！发送异步消息，返回值是Future[Any]

掌握；

1. 创建Actor
2. Actor的消息接收和发送
3. 用Actor并发编程实现WordCount

从scala2.11开始Actor已经废弃了，如果非要用，需要导入scala-actors依赖

```xml
<dependency>
  <groupId>org.scala-lang</groupId>
  <artifactId>scala-actors</artifactId>
  <version>2.11.7</version>
</dependency>
```

