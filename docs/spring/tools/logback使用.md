[参考](https://juejin.im/post/5b51f85c5188251af91a7525)

```xml
<configuration scan="true" scanPeriod="60 seconds" debug="false">  
 1. scan=true会自动检测配置的变动，scanPeriod是检测周期
 2. debug打印logback内部信息，默认false  
    <property name="glmapper-name" value="glmapper-demo" /> 
   property定义变量，可以在上下文中引用
   <contextName>${glmapper-name}</contextName> 
    每个logger都关联到logger上下文，默认上下文名称为“default”，用于区分不同程序的记录
    <appender>
        //负责写日志的组件
    </appender>   
    
    <logger>
        //用来设置某一个包或者具体的某一个类的日志打印级别以及指定appender
    </logger>
    
    <root>             
       //根logger，也是一种logger，且只有一个level属性
    </root>  
</configuration>  

```

# 知识点

1. root是根logger, 是一个特殊的logger,不能有name和additivity属性，只有一个level

2. appender是一个日志打印组件，必须用某个logger的appender-ref指定它，它才会生效

   它让我们的应用知道怎么打、打到哪里、打印成什么样。logger是告诉应用哪些可以这么打

## appender

- 属性

1. name
2. class

- 种类

1. ConsoleAppender：把日志添加到控制台

2. FileAppender：把日志添加到文件

3. RollingFileAppender：滚动记录文件，先将日志记录到指定文件，当符合某个条件时，将日志记录到其他文件。它是FileAppender的子类

- 子标签-指定是否追加打印

```xml
<append>true</append>
```

## logger

```xml
<logger name="com.glmapper.spring.boot.controller"
        level="${logging.level}" additivity="false">
    <appender-ref ref="GLMAPPER-LOGGERONE" />
</logger>
```

1. name 指定受此logger约束的包或者类
2. Additivity：是否向上级logger传递打印信息，默认true

# 案例

## 打印控制台

```xml
<configuration>
    <!-- 默认的控制台日志输出，一般生产环境都是后台启动，这个没太大作用 -->
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <Pattern>%d{HH:mm:ss.SSS} %-5level %logger{80} - %msg%n</Pattern>
        </encoder>
    </appender>

    <root level="info">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>

```

## 只输出到文件

```xml
<configuration>
    <!-- 属性文件:在properties文件中找到对应的配置项 -->
    <springProperty scope="context" name="logging.path"  source="logging.path"/>
    <springProperty scope="context" name="logging.level" source="logging.level.com.glmapper.spring.boot"/>
    <!-- 默认的控制台日志输出，一般生产环境都是后台启动，这个没太大作用 -->
    <appender name="STDOUT"
        class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <Pattern>%d{HH:mm:ss.SSS} %-5level %logger{80} - %msg%n</Pattern>
        </encoder>
    </appender>
    
    <appender name="GLMAPPER-LOGGERONE"
    class="ch.qos.logback.core.rolling.RollingFileAppender">
        <append>true</append> //是否追加打印
       //filter对日志进行过滤
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>${logging.level}</level>
        </filter>
      //file指定文件路径
        <file>
            ${logging.path}/glmapper-spring-boot/glmapper-loggerone.log
        </file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <FileNamePattern>${logging.path}/glmapper-spring-boot/glmapper-loggerone.log.%d{yyyy-MM-dd}</FileNamePattern>
            <MaxHistory>30</MaxHistory>
        </rollingPolicy>
      //encoder 对日志进行格式化
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <root level="info">
        <appender-ref ref="GLMAPPER-LOGGERONE"/>
    </root>
</configuration>

```



## 打印指定包下的日志

```xml
<configuration>
    <!-- 属性文件:在properties文件中找到对应的配置项 -->
    <springProperty scope="context" name="logging.path"  source="logging.path"/>
    <springProperty scope="context" name="logging.level" source="logging.level.com.glmapper.spring.boot"/>
    <!-- 默认的控制台日志输出，一般生产环境都是后台启动，这个没太大作用 -->
    <appender name="STDOUT"
              class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <Pattern>%d{HH:mm:ss.SSS} %-5level %logger{80} - %msg%n</Pattern>
        </encoder>
    </appender>

    <appender name="GLMAPPER-LOGGERONE"
              class="ch.qos.logback.core.rolling.RollingFileAppender">
        <append>true</append>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>${logging.level}</level>
        </filter>
        <file>
            ${logging.path}/glmapper-spring-boot/glmapper-loggerone.log
        </file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <FileNamePattern>${logging.path}/glmapper-spring-boot/glmapper-loggerone.log.%d{yyyy-MM-dd}</FileNamePattern>
            <MaxHistory>30</MaxHistory>
        </rollingPolicy>
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

<!--    该logger负责打印指定包下的日志-->
    <logger name="com.glmapper.spring.boot.controller" level="${logging.level}"
            additivity="false">
        <appender-ref ref="GLMAPPER-LOGGERONE" />
    </logger>

    <root level="${logging.level}">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>

```



