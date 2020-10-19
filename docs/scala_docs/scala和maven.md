# 简介

maven是一个基于插件的框架，使得新增库和模块到已有项目非常简单

 Scala Maven Plugin ([GitHub repo](https://github.com/davidB/scala-maven-plugin), [website](https://davidb.github.io/scala-maven-plugin)) 目前是scala项目的主导插件，它包含了scala,因此可以不用安装scala，来编译项目。这个插件用于在maven里编译、测试、运行scala代码的。

有了这个插件，idea才可以选择创建scala类

# 使用示例

```xml
<plugin>
				<groupId>net.alchim31.maven</groupId>
				<artifactId>scala-maven-plugin</artifactId>
				<version>3.2.2</version>
				<executions>
					<execution>
					<id>compile-scala</id>
					<phase>compile</phase>
						<goals>
							<goal>add-source</goal>
							<goal>compile</goal>
						</goals>
					</execution>

					<execution>
						<id>test-compile-scala</id>
						<phase>test-compile</phase>
						<goals>
							<goal>add-source</goal>
							<goal>testCompile</goal>
						</goals>
					</execution>
				</executions>
				<configuration>
					<recompileMode>incremental</recompileMode>
					<scalaVersion>${scala.version}</scalaVersion>
					<args>
						<arg>-deprecation</arg>
					</args>
					<jvmArgs>
						<jvmArg>-Xms64m</jvmArg>
						<jvmArg>-Xmx1024m</jvmArg>
					</jvmArgs>
				</configuration>
			</plugin>
```



翻译自https://docs.scala-lang.org/tutorials/scala-with-maven.html

