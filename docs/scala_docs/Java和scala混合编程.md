在pom.xml里加入依赖

```xml
    <dependency>
      <groupId>org.scala-lang</groupId>
      <artifactId>scala-library</artifactId>
      <version>${scala.verison}</version>
    </dependency>
```

其次就是三个插件

1. scala-maven-plugin用于编译scala代码
2. maven-compiler-plugin用于编译Java代码
3. maven-assembly-plugin用于打包

只是要注意： scala插件一定要在Java插件前面，即先编译scala再编译java。否则，会出现找不到scala类的情况。

```xml
  <!-- This plugin compiles Scala files -->
      <plugin>
        <groupId>net.alchim31.maven</groupId>
        <artifactId>scala-maven-plugin</artifactId>
        <executions>
          <execution>
            <id>scala-compile-first</id>
            <phase>process-resources</phase>
            <goals>
              <goal>add-source</goal>
              <goal>compile</goal>
            </goals>
          </execution>
          <execution>
            <id>scala-test-compile</id>
            <phase>process-test-resources</phase>
            <goals>
              <goal>testCompile</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <scalaVersion>${scala.version}</scalaVersion>
        </configuration>
      </plugin>

      <!-- This plugin compiles Java files -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <source>${java.version}</source>
          <target>${java.version}</target>
        </configuration>
        <executions>
          <execution>
            <phase>compile</phase>
            <goals>
              <goal>compile</goal>
            </goals>
          </execution>
        </executions>
      </plugin>

      <!-- This plugin adds all dependencies to JAR file during 'package' command.
      Pay EXTRA attention to the 'mainClass' tag.
      You have to set name of class with entry point to program ('main' method) -->
      <plugin>
        <artifactId>maven-assembly-plugin</artifactId>
        <configuration>
          <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
          </descriptorRefs>
          <archive>
            <manifest>
              <mainClass>com.cxf.Hello</mainClass>
            </manifest>
          </archive>
        </configuration>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>single</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
```

