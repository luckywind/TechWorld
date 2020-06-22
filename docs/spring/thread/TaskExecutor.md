Spring提供TaskExecutor作为executors抽象，解决的问题：

1. 线程由Spring管理
2. 可以使用应用中的其他组件

TaskExecutor接口等价于 java.util.concurrent.Executor接口，有一些默认的实现。

在Spring应用中，提供一个TaskExecutor实现，我们就可以把它注入到bean中，并且管理线程。

```java
package com.gkatzioura.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import java.util.List;
/**
 * Created by gkatzioura on 4/26/17.
 */
@Service
public class AsynchronousService {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private TaskExecutor taskExecutor;
    public void executeAsynchronously() {
        taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                //TODO add long running task
            }
        });
    }
}
```

第一步就是添加TaskExecutor配置

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
public class ThreadConfig {
    @Bean
    public TaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setThreadNamePrefix("default_task_executor_thread");
        executor.initialize();
        return executor;
    }
}
```

一旦配置了executor，就可以把它注入到spring组件中，用于提交Runnable作业。

因为我们的异步代码可能需要与其他组件交互，并且注入了其他组件，一个比较好的方式是创建一个prototype Runnable实例

```java
package com.gkatzioura;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
/**
 * Created by gkatzioura on 10/18/17.
 */
@Component
@Scope("prototype")
public class MyThread implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyThread.class);
    @Override
    public void run() {
        LOGGER.info("Called from thread");
    }
}
```

然后，我们就可以把executor注入到service中，并用它来执行Runable实例

```java
package com.gkatzioura.service;
import com.gkatzioura.MyThread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import java.util.List;
/**
 * Created by gkatzioura on 4/26/17.
 */
@Service
public class AsynchronousService {
    @Autowired
    private TaskExecutor taskExecutor;
    @Autowired
    private ApplicationContext applicationContext;
    public void executeAsynchronously() {
        MyThread myThread = applicationContext.getBean(MyThread.class);
        taskExecutor.execute(myThread);
    }
}
```

