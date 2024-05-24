# 实战springboot整合quartz，job-store-type使用jdbc

## 配置

在配置文件中定义quartz的一些属性，

1. job-store-type: JDBC  把job以及触发等信息保存到数据库
2. initialize-schema: NEVER  第一次运行时可以改为always，等表初始化完后，再改为NEVER
3. jobStore.dataSource需要指定一个数据源，这个可以放到其他环境配置文件中配置，这里直接引用

```yaml
spring:
#    main:
#        allow-bean-definition-overriding: true
    profiles:
        active: dev #spring.profiles.active#
    quartz:
        properties:
            org:
                quartz:
                    scheduler:
                        instanceName: quartzScheduler
                        instanceId: AUTO
                    jobStore:
                        class: org.quartz.impl.jdbcjobstore.JobStoreTX
                        driverDelegateClass: org.quartz.impl.jdbcjobstore.StdJDBCDelegate
                        tablePrefix: QRTZ_
                        isClustered: false
                        clusterCheckinInterval: 10000
                        useProperties: false
                        dataSource: quartz_datasource
                    threadPool:
                        class: org.quartz.simpl.SimpleThreadPool
                        threadCount: 300
                        threadPriority: 5
                        threadsInheritContextClassLoaderOfInitializingThread: true
        #数据库方式
        job-store-type: JDBC
        wait-for-jobs-to-complete-on-shutdown: true
        #初始化表结构
        jdbc:
            initialize-schema: NEVER

```

第一次初始化数据库后，会自动生成如下这些表

![image-20200523121723519](https://tva1.sinaimg.cn/large/007S8ZIlly1gf29i1vwobj30cu0c0mye.jpg)

## Bean配置

```java
package com.cxf.springbootquartzdemo.config;

import org.apache.commons.lang3.ArrayUtils;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.*;

import javax.sql.DataSource;
import java.util.Calendar;
import java.util.Properties;

@Configuration
public class QuartzConfig {
    private static final Logger log = LoggerFactory.getLogger(QuartzConfig.class);
    private ApplicationContext applicationContext;
    private DataSource dataSource;

    public QuartzConfig(ApplicationContext applicationContext, DataSource dataSource) {
        this.applicationContext = applicationContext;
        this.dataSource = dataSource;
    }

    @Bean
    public SpringBeanJobFactory springBeanJobFactory() {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    @Bean
    public SchedulerFactoryBean scheduler(Trigger... triggers) {
        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        schedulerFactory.setOverwriteExistingJobs(true);
        schedulerFactory.setAutoStartup(true);
        schedulerFactory.setDataSource(dataSource);
        schedulerFactory.setJobFactory(springBeanJobFactory());
        schedulerFactory.setWaitForJobsToCompleteOnShutdown(true);

        if (ArrayUtils.isNotEmpty(triggers)) {
            schedulerFactory.setTriggers(triggers);
        }

        return schedulerFactory;
    }
}

```

## job

只要实现Job接口即可

```java
/**
 * Job 的实例要到该执行它们的时候才会实例化出来。每次 Job 被执行，一个新的 Job 实例会被创建。
 * 其中暗含的意思就是你的 Job 不必担心线程安全性，因为同一时刻仅有一个线程去执行给定 Job 类的实例，甚至是并发执行同一 Job 也是如此。
 * @DisallowConcurrentExecution 保证上一个任务执行完后，再去执行下一个任务
 */
@DisallowConcurrentExecution
public class PrintHello implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        System.out.println("hello"+ LocalDateTime.now());
    }
}

```

## 调度

把trigger和JobDetail绑定起来即可

```java
@Configuration
public class QuartzSubmitJobs {
    private static final String CRON_EVERY_FIVE_MINUTES = "0 0/5 * ? * * *";

    @Bean(name = "printHello")
    public JobDetailFactoryBean jobMemberStats() {
        return QuartzConfig.createJobDetail(PrintHello.class, "printHello Job");
    }

    @Bean(name = "printHelloTrigger")
    public SimpleTriggerFactoryBean triggerMemberStats(@Qualifier("printHello") JobDetail jobDetail) {
        return QuartzConfig.createTrigger(jobDetail, 60000, "printHello Trigger");
    }
}

```

## 完整代码

springboot-quartz-demo

