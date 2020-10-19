# 依赖

[参考](http://tengj.top/2017/04/05/springboot7/)

假如maven依赖中添加了spring-boot-starter-logging：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-logging</artifactId>
</dependency>
```

Spring Boot应用将自动使用logback作为应用日志框架,但是呢，实际开发中我们不需要直接添加该依赖，你会发现spring-boot-starter其中包含了 spring-boot-starter-logging

# 自定义日志配置

根据不同的日志系统，你可以按如下规则组织配置文件名，就能被正确加载：

- Logback：`logback-spring.xml`, `logback-spring.groovy`, `logback.xml`, `logback.groovy`
- Log4j：`log4j-spring.properties`, `log4j-spring.xml`, `log4j.properties`, `log4j.xml`
- Log4j2：`log4j2-spring.xml`, `log4j2.xml`
- JDK (Java Util Logging)：`logging.properties`

一般我们用默认的Logback, 把`logback-spring.xml`放到resource目录下即可，内容可以基于下面的修改

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration  scan="true" scanPeriod="60 seconds" debug="false">
    <contextName>logback</contextName>  可有可无
    <property name="log.path" value="/Users/cxf/Documents/log" />
    <!--输出到控制台-->
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
       <!-- <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>-->
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %contextName [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!--输出到文件-->
    <appender name="file" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${log.path}/logback.%d{yyyy-MM-dd}.log</fileNamePattern>
        </rollingPolicy>
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %contextName [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="info">  console和file两个appender会添加到root这个logger
        <appender-ref ref="console" />
        <appender-ref ref="file" />
    </root>

    <!-- 将控制controller包下的所有类的日志的打印，但是并没用设置打印级别，所以继承他的上级的日志级别“info”；
          没有设置addtivity，默认为true，将此loger的打印信息向上级(即root)传递，root接到信息交给appender处理；
          没有设置appender，此loger本身不打印任何信息。 -->
    <logger name="com.dudu.controller"/>
    <!--控制com.dudu.controller.LearnController类的日志打印，打印级别为“WARN”;
        additivity属性为false，表示此loger的打印信息不再向上级传递;
        指定了名字为“console”的appender; 打印日志到控制台-->
    <logger name="com.dudu.controller.LearnController" level="WARN" additivity="false">
        <appender-ref ref="console"/>
    </logger>
</configuration>
```

## 根节点configuration包含的属性

- scan:当此属性设置为`true`时，配置文件如果发生改变，将会被重新加载，默认值为`true`。
- scanPeriod:设置监测配置文件是否有修改的时间间隔，如果没有给出时间单位，默认单位是毫秒。当`scan`为`true`时，此属性生效。默认的时间间隔为1分钟。
- debug:当此属性设置为`true`时，将打印出`logback`内部日志信息，实时查看`logback`运行状态。默认值为`false`。

根节点`<configuration>`有5个子节点，下面一一会详细介绍。

### 子节点root

root节点是必选节点，用来指定最基础的日志输出级别，只有一个level属性。

level:用来设置打印级别，大小写无关：TRACE, DEBUG, INFO, WARN, ERROR, ALL 和 OFF，不能设置为INHERITED或者同义词NULL。
 默认是DEBUG。
 可以包含零个或多个元素，标识这个appender将会添加到这个loger。

### 子节点context设置上下文名称

每个logger都关联到logger上下文，默认上下文名称为“default”。但可以使用设置成其他名字，用于区分不同应用程序的记录。一旦设置，不能修改,可以通过`%contextName`来打印日志上下文名称，一般来说我们不用这个属性，可有可无。

### 子节点property设置变量

通过定义的值会被插入到logger上下文中。定义变量后，可以使“${}”来使用变量。

可以通过application.yml传递参数过来？

### 子节点appender

appender用来格式化日志输出节点，有俩个属性name和class，class用来指定哪种输出策略，常用就是控制台输出策略和文件输出策略。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" scanPeriod="60 seconds" debug="false">

    <contextName>logback-demo</contextName>

    <!--输出到控制台 ConsoleAppender-->
    <appender name="consoleLog1" class="ch.qos.logback.core.ConsoleAppender">
        <!--展示格式 layout-->
        <layout class="ch.qos.logback.classic.PatternLayout">  控制台打印用layout
            <pattern>%d -1 %msg%n</pattern>
        </layout>
    </appender>

    <!--输出到控制台 ConsoleAppender-->
    <appender name="consoleLog2" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>																							输出到文件建议用encoder
            <pattern>%d -2 %msg%n</pattern>
        </encoder>
    </appender>

    <!--指定最基础的日志输出级别-->
    <root level="INFO">
        <!--appender将会添加到这个loger-->
        <appender-ref ref="consoleLog1"/>
        <appender-ref ref="consoleLog2"/>
    </root>

</configuration>
```

%d{HH: mm:ss.SSS}——日志输出时间

%thread——输出日志的进程名字，这在Web应用以及异步任务处理中很有用

%-5level——日志级别，并且使用5个字符靠左对齐

%logger{36}——日志输出者的名字

%msg——日志消息

%n——平台的换行符
 ThresholdFilter为系统定义的拦截器，例如我们用ThresholdFilter来过滤掉ERROR级别以下的日志不输出到文件中。如果不用记得注释掉，不然你控制台会发现没日志~

#### 输出到文件

```xml
<appender name="fileInfoLog" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <!--如果只是想要 Info 级别的日志，只是过滤 info 还是会输出 Error 日志，因为 Error 的级别高，
    所以我们使用下面的策略，可以避免输出 Error 的日志-->
    <filter class="ch.qos.logback.classic.filter.LevelFilter">
        <!--过滤 Error-->
        <level>ERROR</level>
        <!--匹配到就禁止-->
        <onMatch>DENY</onMatch>
        <!--没有匹配到就允许-->
        <onMismatch>ACCEPT</onMismatch>
    </filter>
    <!--日志名称，如果没有File 属性，那么只会使用FileNamePattern的文件路径规则
        如果同时有<File>和<FileNamePattern>，那么当天日志是<File>，明天会自动把今天
        的日志改名为今天的日期。即，<File> 的日志都是当天的。
    -->
    <File>${logback.logdir}/info.${logback.appname}.log</File>
    <!--滚动策略，按照时间滚动 TimeBasedRollingPolicy-->
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <!--文件路径,定义了日志的切分方式——把每一天的日志归档到一个文件中,以防止日志填满整个磁盘空间-->
        <FileNamePattern>${logback.logdir}/info.${logback.appname}.%d{yyyy-MM-dd}.log</FileNamePattern>
        <!--只保留最近90天的日志-->
        <maxHistory>90</maxHistory>
        <!--用来指定日志文件的上限大小，那么到了这个值，就会删除旧的日志-->
        <!--<totalSizeCap>1GB</totalSizeCap>-->
    </rollingPolicy>
    <!--日志输出编码格式化-->
    <encoder>
        <charset>UTF-8</charset>
        <pattern>%d [%thread] %-5level %logger{36} %line - %msg%n</pattern>
    </encoder>
</appender>


<appender name="fileErrorLog" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <!--如果只是想要 Error 级别的日志，那么需要过滤一下，默认是 info 级别的，ThresholdFilter-->
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
        <level>Error</level>
    </filter>
    <!--日志名称，如果没有File 属性，那么只会使用FileNamePattern的文件路径规则
        如果同时有<File>和<FileNamePattern>，那么当天日志是<File>，明天会自动把今天
        的日志改名为今天的日期。即，<File> 的日志都是当天的。
    -->
    <File>${logback.logdir}/error.${logback.appname}.log</File>
    <!--滚动策略，按照时间滚动 TimeBasedRollingPolicy-->
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <!--文件路径,定义了日志的切分方式——把每一天的日志归档到一个文件中,以防止日志填满整个磁盘空间-->
        <FileNamePattern>${logback.logdir}/error.${logback.appname}.%d{yyyy-MM-dd}.log</FileNamePattern>
        <!--只保留最近90天的日志-->
        <maxHistory>90</maxHistory>
        <!--用来指定日志文件的上限大小，那么到了这个值，就会删除旧的日志-->
        <!--<totalSizeCap>1GB</totalSizeCap>-->
    </rollingPolicy>
    <!--日志输出编码格式化-->
    <encoder>
        <charset>UTF-8</charset>
        <pattern>%d [%thread] %-5level %logger{36} %line - %msg%n</pattern>
    </encoder>
</appender>
```

### 子节点loger

`<logger>`用来设置某一个包或者具体的某一个类的日志打印级别、以及指定`<appender>`。`<logger>`仅有一个`name`属性，一个可选的`level`和一个可选的`addtivity`属性。

- `name`:用来指定受此loger约束的某一个包或者具体的某一个类。
- `level`:用来设置打印级别，大小写无关：TRACE, DEBUG, INFO, WARN, ERROR, ALL 和 OFF，还有一个特俗值INHERITED或者同义词NULL，代表强制执行上级的级别。如果未设置此属性，那么当前loger将会继承上级的级别。
- `addtivity`:是否向上级loger传递打印信息。默认是true。

logger接收到消息后，交给appender处理， logger是有层级关系的，可以向上级传递消息，而自己不打印日志，也可以把日志交给配置的appender处理。

# 多环境日志输出

springProfile标签用在configuration下面，用于决定哪段配置有效

```xml
<configuration>
    ...
    
    <!-- 测试环境+开发环境. 多个使用逗号隔开. -->
    <springProfile name="test,dev">
        <logger name="com.example.demo.controller" level="DEBUG" additivity="false">
            <appender-ref ref="consoleLog"/>
        </logger>
    </springProfile>
    
    <!-- 生产环境. -->
    <springProfile name="prod">
        <logger name="com.example.demo.controller" level="INFO" additivity="false">
            <appender-ref ref="consoleLog"/>
        </logger>
    </springProfile>
</configuration>
```

application.yml增加环境选择的配置active:dev

```yml
server:
  port: 9010

spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:mysql://localhost:3306/test?characterEncoding=utf8
    username: root
    password: root

mybatis:
    type-aliases-package: org.larry.springboot.entity
    mapper-locations: classpath:mapper/**/*.xml
    check-config-location: true
```

## 通过yml传参数

例如，自定义日志路径

application.yml增加相关自定义配置

```yml
logback:
  logdir: /Users/inke/dev/log/tomcat/sell
  appname: sell
```

在`logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" scanPeriod="60 seconds" debug="false">

    <!--application.yml 传递参数，不能使用logback 自带的<property>标签 -->
    <springProperty scope="context" name="appname" source="logback.appname"/>
    <springProperty scope="context" name="logdir" source="logback.logdir"/>

    <contextName>${appname}</contextName>

    <!--输出到控制台 ConsoleAppender-->
    <appender name="consoleLog" class="ch.qos.logback.core.ConsoleAppender">
        <!--展示格式 layout-->
        <layout class="ch.qos.logback.classic.PatternLayout">
            <pattern>
                <pattern>%d{HH:mm:ss.SSS} %contextName [%thread] %-5level %logger{36} - %msg%n</pattern>
            </pattern>
        </layout>
    </appender>
...
...
```

