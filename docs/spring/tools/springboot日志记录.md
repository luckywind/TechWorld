[集成lombok记录日志](https://www.baeldung.com/spring-boot-logging)

# 零配置

springboot2.x会自动引入spring-jcl模块，该模块包含依赖Apatch Commons Logging。当我们使用springboot的starters时，这些都自动引入了。所以我们不需要任何配置，springboot应用就可以打印出日志，默认是info级别，但是我们可以通过命令行传入--debug或者--trace改变默认级别。

```shell
java -jar target/spring-boot-logging-0.0.1-SNAPSHOT.jar --trace
```

或者可以在application.properties里添加

```properties
logging.level.root=WARN
logging.level.com.baeldung=TRACE
```

# 自定义Logback配置

当classpath中有以下任何一个文件时，springboot会自动加载以覆盖默认日志配置

- *logback-spring.xml*
- *logback.xml*
- *logback-spring.groovy*
- *logback.groovy*

例如，logback-spring.xml: 滚动记录日志文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
 
    <property name="LOGS" value="./logs" />
 
    <appender name="Console"
        class="ch.qos.logback.core.ConsoleAppender">
        <layout class="ch.qos.logback.classic.PatternLayout">
            <Pattern>
                %black(%d{ISO8601}) %highlight(%-5level) [%blue(%t)] %yellow(%C{1.}): %msg%n%throwable
            </Pattern>
        </layout>
    </appender>
 
    <appender name="RollingFile"
        class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOGS}/spring-boot-logger.log</file>
        <encoder
            class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <Pattern>%d %p %C{1.} [%t] %m%n</Pattern>
        </encoder>
 
        <rollingPolicy
            class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- rollover daily and when the file reaches 10 MegaBytes -->
            <fileNamePattern>${LOGS}/archived/spring-boot-logger-%d{yyyy-MM-dd}.%i.log
            </fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy
                class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>10MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
    </appender>
     
    <!-- LOG everything at INFO level -->
    <root level="info">
        <appender-ref ref="RollingFile" />
        <appender-ref ref="Console" />
    </root>
 
    <!-- LOG "com.baeldung*" at TRACE level -->
    <logger name="com.baeldung" level="trace" additivity="false">
        <appender-ref ref="RollingFile" />
        <appender-ref ref="Console" />
    </logger>
 
</configuration>
```

# 使用Log4j2代替logback

 Apache Commons Logging是核心API，Logback是默认实现。但我们也可以切换其他日志库： 

例如，log4j2,我们需要排除掉spring-boot-starter-logging，并引入spring-boot-starter-log4j2

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
```



同时提供log4j2.xml或者log4j2-spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout
                pattern="%style{%d{ISO8601}}{black} %highlight{%-5level }[%style{%t}{bright,blue}] %style{%C{1.}}{bright,yellow}: %msg%n%throwable" />
        </Console>
 
        <RollingFile name="RollingFile"
            fileName="./logs/spring-boot-logger-log4j2.log"
            filePattern="./logs/$${date:yyyy-MM}/spring-boot-logger-log4j2-%d{-dd-MMMM-yyyy}-%i.log.gz">
            <PatternLayout>
                <pattern>%d %p %C{1.} [%t] %m%n</pattern>
            </PatternLayout>
            <Policies>
                <!-- rollover on startup, daily and when the file reaches 
                    10 MegaBytes -->
                <OnStartupTriggeringPolicy />
                <SizeBasedTriggeringPolicy
                    size="10 MB" />
                <TimeBasedTriggeringPolicy />
            </Policies>
        </RollingFile>
    </Appenders>
 
    <Loggers>
        <!-- LOG everything at INFO level -->
        <Root level="info">
            <AppenderRef ref="Console" />
            <AppenderRef ref="RollingFile" />
        </Root>
 
        <!-- LOG "com.baeldung*" at TRACE level -->
        <Logger name="com.baeldung" level="trace"></Logger>
    </Loggers>
 
</Configuration>
```



除了xml配置，Log4j2还可以使用yaml[和json配置](https://docs.spring.io/spring-boot/docs/current/reference/html/howto-logging.html#howto-configure-log4j-for-logging-yaml-or-json-config)

# lombok使用

上面的例子里，我们都需要声明一个log ger实例，使用lombok可以减少这部分代码

## 引入依赖

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.4</version>
    <scope>provided</scope>
</dependency>
```

## *@Slf4j* 和 *@CommonsLog*

*org.slf4j.Logger* 用于添加SLF4J实例

*org.apache.commons.logging.Log* 用于添加*org.apache.commons.logging.Log* 实例

```java
@RestController
@Slf4j
public class LombokLoggingController {
  
    @RequestMapping("/lombok")
    public String index() {
        log.trace("A TRACE Message");
        log.debug("A DEBUG Message");
        log.info("An INFO Message");
        log.warn("A WARN Message");
        log.error("An ERROR Message");
  
        return "Howdy! Check out the Logs to see the output...";
    }
}
```

@Slf4j自动添加了一个log实例

零配置情况下，将会使用默认的Logback实现。当配置了Log4j2配置文件时会使用Log4j2实现

## @Log4j2

可以使用@Log4j2直接使用Log4j2

```java
@RestController
@Log4j2
public class LombokLoggingController {
  
    @RequestMapping("/lombok")
    public String index() {
        log.trace("A TRACE Message");
        log.debug("A DEBUG Message");
        log.info("An INFO Message");
        log.warn("A WARN Message");
        log.error("An ERROR Message");
  
        return "Howdy! Check out the Logs to see the output...";
    }
}   
```

