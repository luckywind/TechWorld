当我们仅仅执行一个简单动作时，TaskExecutor还是略显麻烦。Spring的异步方法就是解决这个问题的。

为实现在其他线程中执行某个方法，只需要给那个方法加上@Async注解即可。

异步方法有两种模式：

1. 返回void

```java
 @Async
    @Transactional
    public void printEmployees() {
        List<Employee> employees = entityManager.createQuery("SELECT e FROM Employee e").getResultList();
        employees.stream().forEach(e->System.out.println(e.getEmail()));
    }
```

2. 有返回值模式

```java
 @Async
    @Transactional
    public CompletableFuture<List<Employee>> fetchEmployess() {
        List<Employee> employees = entityManager.createQuery("SELECT e FROM Employee e").getResultList();
        return CompletableFuture.completedFuture(employees);
    }
```

需要注意的是，@Async方法如果使用this调用则是无效的。另外，异步方法必须是public的。

除此之外，我们还需要开启Spring的异步方法执行能力，这只需在任意一个配置类上加上注解@EnableAsync即可。

```java
package com.gkatzioura.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Created by gkatzioura on 4/26/17.
 */
@Configuration
@EnableAsync
public class ThreadConfig {

    @Bean
    public TaskExecutor threadPoolTaskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setThreadNamePrefix("sgfgd");
        executor.initialize();

        return executor;
    }

}
```

下一个问题是，怎么声明异步方法使用的资源和线程池，[官方文档](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/scheduling/annotation/EnableAsync.html)

默认，Spring会搜索一个关联的线程池定义：

1. 唯一的TaskEcecutor bean
2. 名称为taskExecutor的Executor bean
3. 如果上面两种都没找到，则使用SimpleAsyncTaskExecutor处理异步方法调用

然而，我们有时不想所有任务都使用同一个线程池，我们需要给@Async注解传递一个executor的名字，例如：

```java
@Configuration
@EnableAsync
public class ThreadConfig {

    @Bean(name = "specificTaskExecutor")
    public TaskExecutor specificTaskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.initialize();
        return executor;
    }

}
```

然后方法就可以设置qualifier值以决定使用的executor

```java
@Async("specificTaskExecutor")
public void runFromAnotherThreadPool() {
    System.out.println("You function code here");
}
```

