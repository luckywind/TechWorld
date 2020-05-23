# spring quartz调度支持



​		spring-boot-starter-quartz这个starter让springboot可以很方便的使用quartz调度。 一个Scheduler实例通过SchedulerFactoryBean抽象会被自动配置起来。

​		下面这些类型的Bean会自动和Scheduler建立联系：

- JobDetail: 定义了一个Job，可以使用JobBuilder API很方便的构造这个对象
- Calendar
- Trigger: 定义了Job触发的时机

默认使用基于内存的JobStore。也可以配置基于jdbc的store： 

```
spring.quartz.job-store-type=jdbc
```

当使用jdbc store时，schema可以这么初始化

```
spring.quartz.jdbc.initialize-schema=true
```

当然也可以自定义初始化脚本，通过属性spring.quartz.jdbc.schema指定

quartz scheduler配置可以使用属性`spring.quartz.properties.*`自定义，也可以使用SchedulerFactoryBeanCustomizer通过编程方式自定义

job可以定义setter方法来注入属性，或者其他bean

```java
public class SampleJob extends QuartzJobBean {

	private MyService myService;
	private String name;

	// Inject "MyService" bean
	public void setMyService(MyService myService) { ... }

	// Inject the "name" job data property
	public void setName(String name) { ... }

	@Override
	protected void executeInternal(JobExecutionContext context)
			throws JobExecutionException {
		...
	}

}
```



# springboot添加quartz调度实战

## 依赖

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

## 配置

添加项目中创建的quartz Job的自动织入

```java
public final class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory implements ApplicationContextAware {
    private transient AutowireCapableBeanFactory beanFactory;
    @Override
    public void setApplicationContext(final ApplicationContext context) {
        beanFactory = context.getAutowireCapableBeanFactory();
    }
    @Override
    protected Object createJobInstance(final TriggerFiredBundle bundle) throws Exception {
        final Object job = super.createJobInstance(bundle);
        beanFactory.autowireBean(job);
        return job;
    }
}
```

添加基础配置,实际开发中应该把sheduler方法中的属性放到配置文件，这里简化了

```java
@Configuration
public class QuartzConfig {
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
        Properties properties = new Properties();
        properties.setProperty("org.quartz.scheduler.instanceName", "MyInstanceName");
        properties.setProperty("org.quartz.scheduler.instanceId", "Instance1");
        schedulerFactory.setOverwriteExistingJobs(true);
        schedulerFactory.setAutoStartup(true);
        schedulerFactory.setQuartzProperties(properties);
        schedulerFactory.setDataSource(dataSource);
        schedulerFactory.setJobFactory(springBeanJobFactory());
        schedulerFactory.setWaitForJobsToCompleteOnShutdown(true);
        if (ArrayUtils.isNotEmpty(triggers)) {
            schedulerFactory.setTriggers(triggers);
        }
        return schedulerFactory;
    }
  //下面的静态方法用于提供一个编程的方式调度job和trigger
  static SimpleTriggerFactoryBean createTrigger(JobDetail jobDetail, long pollFrequencyMs, String triggerName) {
        log.debug("createTrigger(jobDetail={}, pollFrequencyMs={}, triggerName={})", jobDetail.toString(), pollFrequencyMs, triggerName);
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(jobDetail);
        factoryBean.setStartDelay(0L);
        factoryBean.setRepeatInterval(pollFrequencyMs);
        factoryBean.setName(triggerName);
        factoryBean.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        factoryBean.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
        return factoryBean;
    }
    static CronTriggerFactoryBean createCronTrigger(JobDetail jobDetail, String cronExpression, String triggerName) {
        log.debug("createCronTrigger(jobDetail={}, cronExpression={}, triggerName={})", jobDetail.toString(), cronExpression, triggerName);
        // To fix an issue with time-based cron jobs
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        CronTriggerFactoryBean factoryBean = new CronTriggerFactoryBean();
        factoryBean.setJobDetail(jobDetail);
        factoryBean.setCronExpression(cronExpression);
        factoryBean.setStartTime(calendar.getTime());
        factoryBean.setStartDelay(0L);
        factoryBean.setName(triggerName);
        factoryBean.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        return factoryBean;
    }
    static JobDetailFactoryBean createJobDetail(Class jobClass, String jobName) {
        log.debug("createJobDetail(jobClass={}, jobName={})", jobClass.getName(), jobName);
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setName(jobName);
        factoryBean.setJobClass(jobClass);
        factoryBean.setDurability(true);
        return factoryBean;
    }
  
}
```



## 创建services

此时，基本的quartz调度器已经可以在springboot应用里运行job了，下一步创建一些service来运行调度器

MemberService类里写一个方法memberStats

```java
public void memberStats() {
  List<Member> members = memberRepository.findAll();
  int activeCount = 0;
  int inactiveCount = 0;
  int registeredForClassesCount = 0;
  int notRegisteredForClassesCount = 0;
  for (Member member : members) {
    if (member.isActive()) {
      activeCount++;
      if (CollectionUtils.isNotEmpty(member.getMemberClasses())) {
        registeredForClassesCount++;
      } else {
        notRegisteredForClassesCount++;
      }
    } else {
      inactiveCount++;
    }
  }
  log.info("Member Statics:");
  log.info("==============");
  log.info("Active member count: {}", activeCount);
  log.info(" - Registered for Classes count: {}", registeredForClassesCount);
  log.info(" - Not registered for Classes count: {}", notRegisteredForClassesCount);
  log.info("Inactive member count: {}", inactiveCount);
  log.info("==========================");
}

```



MemberClassService类里写一个方法 `classStats()`

```java
public void classStats() {
  List<MemberClass> memberClasses = classRepository.findAll();
  Map<String, Integer> memberClassesMap = memberClasses
    .stream()
    .collect(Collectors.toMap(MemberClass::getName, c -> 0));
  List<Member> members = memberRepository.findAll();
  for (Member member : members) {
    if (CollectionUtils.isNotEmpty(member.getMemberClasses())) {
      for (MemberClass memberClass : member.getMemberClasses()) {
        memberClassesMap.merge(memberClass.getName(), 1, Integer::sum);
      }
    }
  }
  log.info("Class Statics:");
  log.info("=============");
  memberClassesMap.forEach((k,v) -> log.info("{}: {}", k, v));
  log.info("==========================");
}

```

## 创建Job

job用来调用service里的代码，对于MemberService，我们创建MemberStatsJob类：

```java
@Slf4j
@Component
@DisallowConcurrentExecution
public class MemberStatsJob implements Job {
    @Autowired
    private MemberService memberService;
    @Override
    public void execute(JobExecutionContext context) {
        log.info("Job ** {} ** starting @ {}", context.getJobDetail().getKey().getName(), context.getFireTime());
        memberService.memberStats();
        log.info("Job ** {} ** completed.  Next job scheduled @ {}", context.getJobDetail().getKey().getName(), context.getNextFireTime());
    }
}
```

对于MemberClassService类，我们创建MemberClassStatsJob类

```java
@Slf4j
@Component
@DisallowConcurrentExecution
public class MemberClassStatsJob implements Job {
    @Autowired
    MemberClassService memberClassService;
    @Override
    public void execute(JobExecutionContext context) {
        log.info("Job ** {} ** starting @ {}", context.getJobDetail().getKey().getName(), context.getFireTime());
        memberClassService.classStats();
        log.info("Job ** {} ** completed.  Next job scheduled @ {}", context.getJobDetail().getKey().getName(), context.getNextFireTime());
    }
}
```

## 调度Job

本项目想实现springboot启动时调度所有job,因此，创建了QuartzSubmitJobs类，包含四个方法，其中两个用于创建job,两个创建对应的trigger

```java
@Configuration
public class QuartzSubmitJobs {
    private static final String CRON_EVERY_FIVE_MINUTES = "0 0/5 * ? * * *";
    @Bean(name = "memberStats")
    public JobDetailFactoryBean jobMemberStats() {
        return QuartzConfig.createJobDetail(MemberStatsJob.class, "Member Statistics Job");
    }
    @Bean(name = "memberStatsTrigger")
    public SimpleTriggerFactoryBean triggerMemberStats(@Qualifier("memberStats") JobDetail jobDetail) {
        return QuartzConfig.createTrigger(jobDetail, 60000, "Member Statistics Trigger");
    }
    @Bean(name = "memberClassStats")
    public JobDetailFactoryBean jobMemberClassStats() {
        return QuartzConfig.createJobDetail(MemberClassStatsJob.class, "Class Statistics Job");
    }
    @Bean(name = "memberClassStatsTrigger")
    public CronTriggerFactoryBean triggerMemberClassStats(@Qualifier("memberClassStats") JobDetail jobDetail) {
        return QuartzConfig.createCronTrigger(jobDetail, CRON_EVERY_FIVE_MINUTES, "Class Statistics Trigger");
    }
}
```

## 启动项目

可以看到quartz调度的初始化

```java
2019-07-14 14:36:51.651  org.quartz.impl.StdSchedulerFactory      : Quartz scheduler 'MyInstanceName' initialized from an externally provided properties instance.
2019-07-14 14:36:51.651  org.quartz.impl.StdSchedulerFactory      : Quartz scheduler version: 2.3.0
2019-07-14 14:36:51.651  org.quartz.core.QuartzScheduler          : JobFactory set to: com.gitlab.johnjvester.jpaspec.config.AutowiringSpringBeanJobFactory@79ecc507
2019-07-14 14:36:51.851  o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService 'applicationTaskExecutor'
2019-07-14 14:36:51.901  aWebConfiguration$JpaWebMvcConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2019-07-14 14:36:52.051  o.s.s.quartz.SchedulerFactoryBean        : Starting Quartz Scheduler now
2019-07-14 14:36:52.054  o.s.s.quartz.LocalDataSourceJobStore     : Freed 0 triggers from 'acquired' / 'blocked' state.
2019-07-14 14:36:52.056  o.s.s.quartz.LocalDataSourceJobStore     : Recovering 0 jobs that were in-progress at the time of the last shut-down.
2019-07-14 14:36:52.056  o.s.s.quartz.LocalDataSourceJobStore     : Recovery complete.
2019-07-14 14:36:52.056  o.s.s.quartz.LocalDataSourceJobStore     : Removed 0 'complete' triggers.
2019-07-14 14:36:52.058  o.s.s.quartz.LocalDataSourceJobStore     : Removed 0 stale fired job entries.
2019-07-14 14:36:52.058  org.quartz.core.QuartzScheduler          : Scheduler MyInstanceName_$_Instance1 
```

项目源码见项目:jpa-spec-with-quartz

[原文](https://dzone.com/articles/adding-quartz-to-spring-boot)