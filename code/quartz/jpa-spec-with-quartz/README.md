# jpa-spec-with-quartz

> The original jpa-spec repository was created in order to provide sample data to be
> used with a DZone article designed to communicate the benefits of Spring Data, 
> JPA and Specifications. 
> 
> This repository, jpa-spec-with-quartz, is a clone of jpa-spec with Quartz scheduler 
> functionality being added to the project, to demonstrate using the `spring-boot-starter-quartz` 
> implementation by Spring. 
> 
> It will be featured in the following DZone article:
> 
> [Adding Quartz to Spring Boot](https://dzone.com/articles/adding-quartz-to-spring-boot)

## Using This Repository

The repository is a Spring Boot application, which can be executed by running the `JpaSpecWithQuartzApplication` files in the `com.gitlab.johnjvester.jpaspec` package.  

When the application starts, an in-memory H2 database will be created and initialized with some default data.  The inserts for this data can be found in the `src\main\resources\data.sql` file.

## Quartz Scheduler

Quartz version 2.3.0 is currently being utilized via spring-boot integration and the following dependency:
```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-quartz</artifactId>
</dependency>
```

### Jobs & Triggers

Quartz utilizes Jobs or (JobDetails) to reference a Java-class to execute.  Those Jobs (or JobDetails) are used by a Trigger (simple or cron-based) to determine how the job will be scheduled.

Currently, all Job classes are located in the `com.gitlab.johnjvester.jpaspec.jobs` package.  The `com.gitlab.johnjvester.jpaspec.config.QuartzSubmitJobs` class is used to configure the JobDetail and Trigger objects, 
using the spring-boot version's `JobDetailFactoryBean`, `CronTriggerFactoryBean` and `SimpleTriggerFactoryBean` objects.

The jobs which interact with Quartz can be found in the `com.gitlab.johnjvester.jpaspec.jobs` package.  These jobs will call a class located in the `com.gitlab.johnjvester.jpaspec.service` package. 

### Quartz Configuration Classes

The `com.gitlab.johnjvester.jpaspec.config.QuartzConfig` class configures the Quartz `SchedulerFactoryBean` and leverages `@Bean` technology via the `com.gitlab.johnjvester.jpaspec.config.AutowiringSpringBeanJobFactory` class (created initially by [David Kiss](https://github.com/davidkiss)).

For this example, the properties for the Quartz scheduler are set in the `com.gitlab.johnjvester.jpaspec.config.QuartzConfig` class.  This information could easily be externalized into one of the many configuration options available to Spring Boot applications.

## More Information

See the following links for additional information:

* [My original jpa-spec repository](https://gitlab.com/johnjvester/jpa-spec)
* [Spring Boot Features (Quartz Scheduler)](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-quartz.html)

Made with ♥ by johnjvester.