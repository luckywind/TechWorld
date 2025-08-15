# jvm-1.8

参考https://maven.apache.org/plugins/maven-compiler-plugin/examples/set-compiler-source-and-target.html
用途：
指定编译器版本
javac接受-source和-target参数，编译插件也可以配置在编译期间提供这些选项
例如，要使用java 8的语言特性(-source 1.8)并希望编译后的类适配JVM1.8(-target 1.8)，可以添加两个属性：

```xml
  <properties>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
  </properties>
```
或者直接配置插件
```xml
 <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.10.1</version>
        <configuration>
          <source>1.8</source>
          <target>1.8</target>
        </configuration>
      </plugin>
```



但是这个没有解决我的问题

这是因为maven的pom.xml中的插件有-nobootcp这个参数:

```xml
		<plugin>
				<groupId>net.alchim31.maven</groupId>
				<artifactId>scala-maven-plugin</artifactId>
				<version>3.2.2</version>
				<executions>
					<execution>
						<goals>
							<goal>compile</goal>
							<goal>testCompile</goal>
						</goals>
					</execution>
				</executions>
				<configuration>
					<args>
						<arg>-nobootcp</arg>
					</args>
					<addScalacArgs>-target:jvm-1.8</addScalacArgs>
				</configuration>
			</plugin>
```

这会出现在idea的编译参数里

![image-20220812192159249](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220812192159249.png)

解决办法就是pom里把这个配置去掉

> -nobootcp：  Do not use the boot classpath for the scala jars.



## 这两个参数作用
javac提供的这两个参数是用来解决JAVA版本的兼容问题的
例如 Java9里新增的List.of()方法
```java
public class TestForSourceAndTarget {
    public static void main(String[] args) {
        System.out.println(List.of("Hello", "Baeldung"));
    }
}
```
如果想兼容JAVA8，就需要提供这俩参数：
/jdk9path/bin/javac TestForSourceAndTarget.java -source 8 -target 8
编译没问题，但是运行会报错，方法不存在！

理想情况下，JAVA应该在编译器就抛出错误，我们必须通过-Xbootclasspath 指定交叉编译时的JAVA路径
/jdk9path/bin/javac TestForSourceAndTarget.java -source 8 -target 8 -Xbootclasspath ${jdk8path}/jre/lib/rt.jar
这样在编译期间就会报错



## source选项
-source指定编译器接受的JAVA源代码版本
/jdk9path/bin/javac TestForSourceAndTarget.java -source 8 -target 8
如果没有这个参数，编译器会基于当前使用的JAVA版本编译源代码，这里例子如果不传-source，编译器就会依据JAVA9的规范编译源代码
-source=8还意味着不能使用JAVA9特有的API
## target选项
target指定输出的class文件的JAVA版本，target不能低于source
target=8意味着class文件必须在JAVA8及以上才能运行

