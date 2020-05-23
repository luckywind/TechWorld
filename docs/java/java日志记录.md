

# 日志实现

## Jdk Logging

这个用作开发测试还行，但是不适合直接用在生产环境。

```java
package com.rambo.tools.log;
import java.util.logging.Logger;
public class JdkLog {
    public static void main(String[] args) {
        Logger logger = Logger.getGlobal();
        logger.info("start process...");
        logger.warning("memory is running out...");
        logger.fine("ignored.");
        logger.severe("process will be terminated...");
    }
}

```

## log4j

### 依赖包

- log4j-api-2.x.jar
- log4j-core-2.x.jar
- log4j-jcl-2.x.jar

### 配置文件

可选log4j.properties或者log4j2.xml，例如：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
	<Properties>
        <!-- 定义日志格式 -->
		<Property name="log.pattern">%d{MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36}%n%msg%n%n</Property>
        <!-- 定义文件名变量 -->
		<Property name="file.err.filename">log/err.log</Property>
		<Property name="file.err.pattern">log/err.%i.log.gz</Property>
	</Properties>
    <!-- 定义Appender，即目的地 -->
	<Appenders>
        <!-- 定义输出到屏幕 -->
		<Console name="console" target="SYSTEM_OUT">
            <!-- 日志格式引用上面定义的log.pattern -->
			<PatternLayout pattern="${log.pattern}" />
		</Console>
        <!-- 定义输出到文件,文件名引用上面定义的file.err.filename -->
		<RollingFile name="err" bufferedIO="true" fileName="${file.err.filename}" filePattern="${file.err.pattern}">
			<PatternLayout pattern="${log.pattern}" />
			<Policies>
                <!-- 根据文件大小自动切割日志 -->
				<SizeBasedTriggeringPolicy size="1 MB" />
			</Policies>
            <!-- 保留最近10份 -->
			<DefaultRolloverStrategy max="10" />
		</RollingFile>
	</Appenders>
	<Loggers>
		<Root level="info">
            <!-- 对info级别的日志，输出到console -->
			<AppenderRef ref="console" level="info" />
            <!-- 对error级别的日志，输出到err，即上面定义的RollingFile -->
			<AppenderRef ref="err" level="error" />
		</Root>
	</Loggers>
</Configuration>
```

## Logback

### Logback组件：

Logback 分为三个模块：logback-core，logback-classic，logback-access

logback-core 是核心；

logback-classic 改善了 log4j，且自身实现了 SLF4J API，所以即使用 Logback 你仍然可以使用其他的日志实现，如原始的 Log4J，java.util.logging 等；

logback-access 让你方便的访问日志信息，如通过 http 的方式。

值得注意的是logback-classic依赖了logback-access和slf4j-api，配合slf4j使用时，如果不需要通过http等方式访问日志，我们只要引入logback-core 和logback-classic就可以了

## log4j2

`Log4j2`是`Log4j`的升级版，与之前的版本`Log4j 1.x`相比、有重大的改进，在修正了`Logback`固有的架构问题的同时，改进了许多`Logback`所具有的功能。

# 日志框架

通常不直接使用日志实现来记录日志，而要配合日志框架使用。好处是可以自由切换日志实现。

## Commons Logging框架

默认情况下，Commons Logging自动搜索并使用Log4j（Log4j是另一个流行的日志系统），如果没有找到Log4j，再使用JDK Logging。

### 依赖

```xml
        <dependency>
            <groupId>log4j</groupId>
            <artifactId>log4j</artifactId>
            <version>1.2.17</version>
        </dependency>
        <dependency>
            <groupId>commons-logging</groupId>
            <artifactId>commons-logging</artifactId>
            <version>1.2</version>
        </dependency>
```

### 配置

```properties
### set log levels ###
log4j.rootLogger = debug , stdout , D , E

### 输出到控制台 ###
log4j.appender.stdout = org.apache.log4j.ConsoleAppender
log4j.appender.stdout.Target = System.out
## 输出INFO级别以上的日志
log4j.appender.stdout.Threshold = INFO
log4j.appender.stdout.layout = org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern = %d{ABSOLUTE} %5p %c{1}:%L - %m%n

### 输出到日志文件 ###
log4j.appender.D = org.apache.log4j.DailyRollingFileAppender
log4j.appender.D.File = /Users/chengxingfu/code/tools/jtool/logs/log.log
log4j.appender.D.Append = true
## 输出DEBUG级别以上的日志
log4j.appender.D.Threshold = DEBUG
log4j.appender.D.layout = org.apache.log4j.PatternLayout
log4j.appender.D.layout.ConversionPattern = %-d{yyyy-MM-dd HH:mm:ss} [ %t:%r ] - [ %p ] %m%n

### 保存异常信息到单独文件 ###
log4j.appender.E = org.apache.log4j.DailyRollingFileAppender
## 异常日志文件名
log4j.appender.E.File = /Users/chengxingfu/code/tools/jtool/logs/error.log
log4j.appender.E.Append = true
## 只输出ERROR级别以上的日志!!!
log4j.appender.E.Threshold = ERROR
log4j.appender.E.layout = org.apache.log4j.PatternLayout
log4j.appender.E.layout.ConversionPattern = %-d{yyyy-MM-dd HH:mm:ss} [ %t:%r ] - [ %p ] %m%n
```

### demo

```java
package com.rambo.tools.log;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CommonLog4j {
    static final Log log = LogFactory.getLog(CommonLog4j.class);//注意Log和LogFactory两个类

    public void foo() {
        log.info("foo");
        log.error("error info ");
    }
    public static void main(String[] args) {
        CommonLog4j commonLog4j = new CommonLog4j();
        log.info("calling foo...");
        commonLog4j.foo();
    }
}

```



## SLF4J框架

其实SLF4J类似于Commons Logging，也是一个日志接口，而Logback类似于Log4j，是一个日志的实现。

这一对组合的好处时，支持日志的模板记录方法

```java
int score = 99;
p.setScore(score);
logger.info("Set score {} for Person {} ok.", score, p.getName());
```



### 用法

```java
package com.rambo.tools.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jLogback {
    static final Logger logger = LoggerFactory.getLogger(Slf4jLogback.class);

    public static void main(String[] args) {
        logger.info("info ");
        logger.error("error ");
    }
}

```

对比一下Commons Logging和SLF4J的接口：

| Commons Logging                       | SLF4J                   |
| :------------------------------------ | :---------------------- |
| org.apache.commons.logging.Log        | org.slf4j.Logger        |
| org.apache.commons.logging.LogFactory | org.slf4j.LoggerFactory |

不同之处就是Log变成了Logger，LogFactory变成了LoggerFactory。

### 依赖

上面提到了，logback-classic依赖了logback-core和slf4j-api，配合slf4j使用时，我们只要引入logback-core 和logback-classic就可以了，这个其实可以通过maven helper查看看出来：

<img src="https://tva1.sinaimg.cn/large/007S8ZIlly1gelh2y9foxj30ui0jkabr.jpg" alt="image-20200508234506799" style="zoom:50%;" />

```xml
			<!-- logback-core基础模块 -->
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-core</artifactId>
            <version>1.2.3</version>
        </dependency>
        <!-- slf4j 接口实现模块 -->
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.2.3</version>
        </dependency>
```

### 配置文件

配置文件查找规则：

**1.** 如果指定了 **logback.configurationFile** 属性的值，则使用该属性指定的文件作为配置文件

**2.** 类路径下寻找 **logback.groovy** 文件

**3.** 类路径下寻找 **logback-test.xml** 文件

**4.** 类路径下寻找 **logback.xml** 文件

这里我们使用Logback.xml   **注意文件名**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

	<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
		<encoder>
			<pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
		</encoder>
	</appender>

	<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
		<encoder>
			<pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
			<charset>utf-8</charset>
		</encoder>
		<file>log/output.log</file>
		<rollingPolicy class="ch.qos.logback.core.rolling.FixedWindowRollingPolicy">
			<fileNamePattern>log/output.log.%i</fileNamePattern>
		</rollingPolicy>
		<triggeringPolicy class="ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy">
			<MaxFileSize>1MB</MaxFileSize>
		</triggeringPolicy>
	</appender>

	<root level="INFO">
		<appender-ref ref="CONSOLE" />
		<appender-ref ref="FILE" />
	</root>
</configuration>
```

# 总结

在实际使用中，一般选择`slf4j+Log4j2`或者`slf4j+logback`。 本文暂且给出`slf4j+logback`的使用方法。

在使用`slf4j+logback`时，如果是spring项目，要注意排除它自带的日志框架和日志实现，防止冲突。

如`spring-core`里面就集成了日志框架`commons-logging`

可以这么排除

```xml
<dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-core</artifactId>
            <version>${spring.version}</version>
            <!--因为使用了sl4j，所以去掉commons-logging-->
            <exclusions>
                <exclusion>
                    <groupId>commons-logging</groupId>
                    <artifactId>commons-logging</artifactId>
                </exclusion>
            </exclusions>
</dependency>
```

但spring本身日志就使用的`commons-logging`，仅仅去掉就会使其不能正常工作，还需要添加`commons logging`到`slf4j`的桥接器`jcl-over-slf4j`，如下在项目中添加该依赖：

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>jcl-over-slf4j</artifactId>
    <version>${jcl.over.slf4j.version}</version>
</dependency>
```

如果有直接使用`log4j`的组件，也要将`log4j`排除掉，同时添加`log4j-over-slf4`。

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>log4j-over-slf4j</artifactId>
</dependency>
```

