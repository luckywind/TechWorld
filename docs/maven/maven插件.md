1. 每个插件会有一个或者多个目标，每个目标对应一个任务
2. 调用目标的方式

- 绑定到生命周期，执行周期即可
- 命令行直接指定要执行的目标，例如： **mvn archetype:generate**

插件列表：

1. http://maven.apache.org/plugins/index.html
2. http://mojo.codehaus.org/plugins.html

# 常用插件

## maven-antrun-plugin

1. 可以直接在该插件的配置以 Ant 的方式编写 Target，然后交给该插件的 run 目标去执行
2. run目标通常与生命周期绑定

## maven-archetype-plugin

generate 目标，该目标使用交互式的方式提示用户输入必要的信息以创建项目

mvn archetype:generate

## maven-assembly-plugin

1. maven-assembly-plugin 的用途是制作项目分发包，具体打包哪些文件高度可控
2. 使用`assembly.xml`的元数据文件来表述打包

## maven-dependency-plugin

最大的用途就是分析项目依赖

1. **dependency:list**能够列出项目最终解析到的依赖列表
2. **dependency:tree**能进一步的描绘项目依赖树
3. **dependency:analyze**可以告诉你项目依赖潜在的问题，如果你有直接使用到的却未声明的依赖，该目标就会发出警告
4. **dependency:copy-dependencies**能将项目依赖从本地 Maven 仓库复制到某个特定的文件夹下面

## maven-enforcer-plugin

## maven-help-plugin

1. 最简单的**help:system**可以打印所有可用的环境变量和 Java 系统属性

## maven-resources-plugin

默认的主资源文件目录是`src/main/resources`，这个插件可以添加额外的资源文件

## maven-surefire-plugin

测试插件

 **mvn test -Dtest=FooTest** 这样一条命令的效果是仅运行FooTest测试类

## exec-maven-plugin

[参考](https://www.cnblogs.com/zz0412/tag/Maven/)

## maven-shade-plugin

[官网](http://maven.apache.org/plugins/maven-shade-plugin/index.html)

[入门指南](https://www.jianshu.com/p/7a0e20b30401)

[使用指南](https://developer.aliyun.com/article/632130)

### Why?

通过 maven-shade-plugin 生成一个 uber-jar，它包含所有的依赖 jar 包。

### Goals

| Goal                                                         | Description                                                  |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [shade:help](https://links.jianshu.com/go?to=http%3A%2F%2Fmaven.apache.org%2Fplugins%2Fmaven-shade-plugin%2Fhelp-mojo.html) | mvn shade:help -Ddetail=true -Dgoal=<goal-name> 查看参数详情 |
| [shade:shade](https://links.jianshu.com/go?to=http%3A%2F%2Fmaven.apache.org%2Fplugins%2Fmaven-shade-plugin%2Fshade-mojo.html) | Mojo that performs shading delegating to the Shader component. |



### 使用

1. maven-shade-plugin 将 goal shade:shade 绑定到 phase package 上。

```xml
 <build>
     <plugins>
         <plugin>
             <groupId>org.apache.maven.plugins</groupId>
             <artifactId>maven-shade-plugin</artifactId>
             <version>2.4.3</version>
             <configuration>
                <!-- put your configurations here -->
             </configuration>
             <executions>
                 <execution>
                     <phase>package</phase>
                     <goals>
                        <goal>shade</goal>
                     </goals>
                 </execution>
             </executions>
         </plugin>
     </plugins>
 </build>
```

#### 例子

##### 选择jar

```xml
<build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>2.4.3</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <artifactSet>
                <excludes>
                  <exclude>classworlds:classworlds</exclude>
                  <exclude>junit:junit</exclude>
                  <exclude>jmock:*</exclude>
                  <exclude>*:xml-apis</exclude>
                  <exclude>org.apache.maven:lib:tests</exclude>
                  <exclude>log4j:log4j:jar:</exclude>
                </excludes>
              </artifactSet>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
</build>
```

##### 将依赖的某个jar内部的资源include/exclude掉

```xml
<build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>2.4.3</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <filters>
                <filter>
                  <artifact>junit:junit</artifact>
                  <includes>
                    <include>junit/framework/**</include>
                    <include>org/junit/**</include>
                  </includes>
                  <excludes>
                    <exclude>org/junit/experimental/**</exclude>
                    <exclude>org/junit/runners/**</exclude>
                  </excludes>
                </filter>
                <filter>
                  <artifact>*:*</artifact>
                  <excludes>
                    <exclude>META-INF/*.SF</exclude>
                    <exclude>META-INF/*.DSA</exclude>
                    <exclude>META-INF/*.RSA</exclude>
                  </excludes>
                </filter>
              </filters>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
</build>
```

##### 自动 排除不使用的类

```xml
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>2.4.3</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <minimizeJar>true</minimizeJar>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

##### 默认会生成一个Jar包和一个以 "-shaded"为结尾的uber-jar包，可以通过配置来指定uber-jar的后缀名。

```xml
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>2.4.3</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <shadedArtifactAttached>true</shadedArtifactAttached>
              <shadedClassifierName>jackofall</shadedClassifierName> <!-- Any name that makes sense -->
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

##### 可执行jar

```xml
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>2.4.3</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <transformers>
                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <mainClass>org.sonatype.haven.HavenCli</mainClass>
                </transformer>
              </transformers>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```

