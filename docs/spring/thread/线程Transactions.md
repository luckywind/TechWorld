spring的事务信息存储在ThreadLocal变量中，因此，这些变量特定于单个线程上正在进行的事务。

当涉及由单个线程运行的动作时，事务将在分层调用的Spring组件之间传播。

因此，如果使用@Transactional带注释的服务生成了一个线程，则事务将不会从@Transactional服务传播到新创建的线程。结果将是一个错误，指示缺少事务。

通过查看事务 [documentation](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/transaction/annotation/Transactional.html)，我们可以看到更多事务传播类型，默认的传播模式是`REQUIRED`. 因此，使用@Transactional注解的方法将会创建一个新的事务，并传播给当前线程所调用的其他服务。

```java
@Async
@Transactional
public void executeTransactionally() {
    System.out.println("Execute a transaction from the new thread");
}
```

从Runnable类的run函数调用的方法也是如此。尽管异步的使用非常简单，但是在后台，它会将调用包装在Runnable中，然后将其分派给执行者。

总是，在spring中使用线程事务时，需要多加关注，事务不能在线程之间传播。确保@Async和@Transactional方法必须是public的。