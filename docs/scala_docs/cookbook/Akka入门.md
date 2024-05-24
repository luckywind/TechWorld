# Akka简介

Spark的RPC是通过Akka类库实现的，Akka是scala语言开发，基于Actor的并发模型实现。Actor是一个封装了状态和行为的对象，Actor之间可以通过交换消息的方式进行通信。每个Actor都有自己的收件箱，通过Actor能够简化锁及线程管理。

## Actor特性

1. 提供了一种高级抽象，能够简化在并发/并行应用场景下的编程开发
2. 提供了异步非阻塞的、高性能的事件驱动编程模型
3. 超级轻量级事件处理

在Akka中，负责通信，在Actor中有一些重要的声明周期方法：

1. p reStart()方法，仅在构造后执行一次
2. receive()

## ActorSystem

ActorSystem是一个重量级结构，他需要分配多个线程，所以实际应用中AtorSystem是一个单例对象，它可以创建很多Acgtor.





![image-20200107213200257](../../../../../Library/Application Support/typora-user-images/image-20200107213200257.png)

Worker通过ActorSystem创建一个Actor(worker)，该worker Actor把封装在case class里的消息序列化后发送给Master，Master接收到消息后返序列化，并发送给master Actor, master Actor再使用模式匹配，执行相应的逻辑并把执行结果发送给worker Actor.

