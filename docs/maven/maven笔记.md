多模块工程构建时，可以单独构建某一个模块，但前提是其依赖模块已安装到本地

```shell
-am --also-make 同时构建所列模块的依赖模块；
-amd -also-make-dependents 同时构建依赖于所列模块的模块；
-pl --projects <arg> 构建制定的模块，模块间用逗号分隔；
-rf -resume-from <arg> 从指定的模块恢复反应堆。
```

# 普通maven工程多环境配置

## 方法一

### 原则

1. 创建主配置文件，里面是需要的配置项，不过属性值采用 @xxx@形式书写
2. 创建不同环境的值文件，里面是需要动态加载到主配置文件的具体值
3. 在 pom.xml 中配置 profile

### 配置文件布局

```shell
resources目录放入主配置文件application.properties
src/main下创建properties目录放入环境配置文件
```



<img src="https://tva1.sinaimg.cn/large/007S8ZIlgy1ggo812gi5bj30nm0cmjsq.jpg" alt="image-20200528002238044" style="zoom:50%;" />

application.properties内容

```properties
application.name=@application.name@
```

application-dev.properties内容

```properties
application.name=application-dev
```

application-prod.properties内容

```properties
application.name=application-prod
```

### pom

```xml
 <build>
        <filters>
            <!-- 这里的文件名必须与多环境配置文件的文件名相同, ${env} 会动态获取不同环境 -->
            <!-- 假如激活 dev 环境, 这时对应的文件就是 src/main/properties/application-dev.properties -->
            <filter>src/main/properties/application-${env}.properties</filter>
        </filters>
        <resources>
            <resource>
                <!-- 可以理解为真正的配置文件所在的目录 -->
                <directory>src/main/resources</directory>
                <!-- 是否替换资源中的属性, 设置为 true 才能实现动态替换 -->
                <filtering>true</filtering>
            </resource>
        </resources>
    </build>
    <profiles>
        <!-- 环境一 -->
        <profile>
            <!-- 使用 mvn package 打包时, -P 指定 profile 的输入值就是此 id -->
            <!-- id 可以随便命名, 不能重复, 可以与 env 相同, 这里演示特意与 env 不同 -->
            <id>develop</id>
            <properties>
                <!-- env 必须与文件的后缀一致(application-${env}.properties) -->
                <!-- 其中 env 这个标签也可以自定义, 没有强制要求必须是 env,
                     但必须与上面 application-${env}.properties 的 ${} 里的值一致 -->
                <env>dev</env>
            </properties>
            <!-- 不指定环境则默认 dev 环境, 可以放在任一个环境下, 但不能在多个环境中指定 -->
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
        </profile>

        <!-- 环境二 -->
        <profile>
            <id>product</id>
            <properties>
                <env>prod</env>
            </properties>
        </profile>
    </profiles>
```



### 打包时指定环境

mvn clean package -Pproduct

mvn clean package -Pdevelop

完整源码见https://github.com/luckywind/TechWorld/blob/master/code/boot/springboot-profile/

## 方法二

使用maven插件完成，不同环境的配置文件单独放到一个资源文件夹下

下图配置了三个环境dev、prod和stage

![image-20200705220202612](https://tva1.sinaimg.cn/large/007S8ZIlgy1ggo80z5l58j30d808k0t6.jpg)

然后配置maven插件，根据构建时传入的参数决定把哪个环境下的配置文件拷贝到资源文件夹根目录。运行时参数通过-Dspring.profiles.active=dev指定，使用maven打包时使用-Pdev指定

```xml
    <profiles>
        <profile>
            <id>dev</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-antrun-plugin</artifactId>
                        <version>1.1</version>
                        <executions>
                            <execution>
                                <id>compile</id>
                                <phase>compile</phase>
                                <configuration>
                                    <tasks>
                                        <echo>${project.build.outputDirectory}</echo>
                                        <copy file="src/main/resources/dev/iauth.properties"
                                              tofile="${project.build.outputDirectory}/iauth.properties"
                                              overwrite="true"/>
                                        <copy file="src/main/resources/dev/zookeeper.properties"
                                              tofile="${project.build.outputDirectory}/zookeeper.properties"
                                              overwrite="true"/>
                                        <copy file="src/main/resources/dev/talos.properties"
                                              tofile="${project.build.outputDirectory}/talos.properties"
                                              overwrite="true"/>
                                        <copy file="src/main/resources/dev/logback.xml"
                                              tofile="${project.build.outputDirectory}/logback.xml"
                                              overwrite="true"/>
                                    </tasks>
                                </configuration>
                                <goals>
                                    <goal>run</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
        <profile>
            <id>stage</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-antrun-plugin</artifactId>
                        <version>1.1</version>
                        <executions>
                            <execution>
                                <id>compile</id>
                                <phase>compile</phase>
                                <configuration>
                                    <tasks>
                                        <echo>${project.build.outputDirectory}</echo>
                                        <copy file="src/main/resources/dev/iauth.properties"
                                              tofile="${project.build.outputDirectory}/iauth.properties"
                                              overwrite="true"/>
                                        <copy file="src/main/resources/dev/zookeeper.properties"
                                              tofile="${project.build.outputDirectory}/zookeeper.properties"
                                              overwrite="true"/>
                                        <copy file="src/main/resources/dev/talos.properties"
                                              tofile="${project.build.outputDirectory}/talos.properties"
                                              overwrite="true"/>
                                        <copy file="src/main/resources/dev/logback.xml"
                                              tofile="${project.build.outputDirectory}/logback.xml"
                                              overwrite="true"/>
                                    </tasks>
                                </configuration>
                                <goals>
                                    <goal>run</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
        <profile>
            <id>prod</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-antrun-plugin</artifactId>
                        <version>1.1</version>
                        <executions>
                            <execution>
                                <id>compile</id>
                                <phase>compile</phase>
                                <configuration>
                                    <tasks>
                                        <echo>${project.build.outputDirectory}</echo>
                                        <copy file="src/main/resources/prod/iauth.properties"
                                              tofile="${project.build.outputDirectory}/iauth.properties"
                                              overwrite="true"/>
                                        <copy file="src/main/resources/prod/zookeeper.properties"
                                              tofile="${project.build.outputDirectory}/zookeeper.properties"
                                              overwrite="true"/>
                                        <copy file="src/main/resources/prod/talos.properties"
                                              tofile="${project.build.outputDirectory}/talos.properties"
                                              overwrite="true"/>
                                        <copy file="src/main/resources/prod/logback.xml"
                                              tofile="${project.build.outputDirectory}/logback.xml"
                                              overwrite="true"/>
                                    </tasks>
                                </configuration>
                                <goals>
                                    <goal>run</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
```



# springboot实现多环境切换

## 方法一

https://blog.csdn.net/top_code/article/details/78570047

### pom文件

需要切换环境的模块pom加入如下配置，定义几个环境，可指定默认环境

```xml
  <profiles>
        <!-- 开发环境 -->
        <profile>
            <id>dev</id>
            <properties>
                <env>dev</env><!-- 之前写的@env@就是通过这里的配置切换环境 -->
            </properties>
            <activation>
                <activeByDefault>true</activeByDefault><!-- 指定缺省环境 -->
            </activation>
        </profile>
        <!-- 测试环境 -->
        <profile>
            <id>test</id>
            <properties>
                <env>test</env>
            </properties>
        </profile>
        <!-- 生产环境 -->
        <profile>
            <id>prod</id>
            <properties>
                <env>prod</env>
            </properties>
        </profile>
    </profiles>

```

可以发现 Spring Boot 的 pom 文件不需要配置 `build` 标签就可以工作，比普通 Maven 工程更友好

### yaml文件

三个配置文件，application-dev.yml、application-prod.yml和主配置文件

主配置文件application.yaml这么配置, active: @env@

```yaml
spring:
  profiles:
    active: @env@
```

### 开发中切换不同环境

在idea的侧边栏可以找到Profiles，想启用哪个环境，勾选即可，默认勾选dev

![image-20200527235516266](https://tva1.sinaimg.cn/large/007S8ZIlgy1ggo80r0e82j30e209k3z2.jpg)



### 运行时指定环境

```shell
mvn clean package打包
```

直接执行java -jar module-web-0.0.1-SNAPSHOT.jar默认使用dev环境

参数--spring.profiles.active=prod可以切换到prod环境

## 方法二

yaml配置文件

```yaml
spring:
    profiles:
        active:  #spring.profiles.active#
```

运行时通过参数-Dspring.profiles.active=dev指定环境（这个参数要写到jar包前面），maven打包使用-Pprod 指定环境

# 多模块切换环境

其实就结合上面普通maven工程和springboot工程的方法二的配置方式就可以完成。

一般我们在sparingboot中使用多模块，springboot模块作为主模块，其配置文件通过#spring.profiles.active#接收传入的环境参数

```yaml
spring:
    profiles:
        active:  #spring.profiles.active#
```

运行时通过参数-Dspring.profiles.active=dev指定环境（这个参数要写到jar包前面），maven打包使用-Pprod 指定环境。

这个参数会传递给主模块依赖的字模块中，子模块可以使用maven插件完成,例如，这里配置一个dev环境，启用它时，会把相应环境目录下的配置文件拷贝到资源目录的根目录

```xml
 <profiles>
        <profile>
            <id>dev</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-antrun-plugin</artifactId>
                        <version>1.1</version>
                        <executions>
                            <execution>
                                <id>compile</id>
                                <phase>compile</phase>
                                <configuration>
                                    <tasks>
                                        <echo>${project.build.outputDirectory}</echo>
                                        <copy file="src/main/resources/dev/iauth.properties"
                                              tofile="${project.build.outputDirectory}/iauth.properties"
                                              overwrite="true"/>
                                        <copy file="src/main/resources/dev/zookeeper.properties"
                                              tofile="${project.build.outputDirectory}/zookeeper.properties"
                                              overwrite="true"/>
                                        <copy file="src/main/resources/dev/talos.properties"
                                              tofile="${project.build.outputDirectory}/talos.properties"
                                              overwrite="true"/>
                                        <copy file="src/main/resources/dev/logback.xml"
                                              tofile="${project.build.outputDirectory}/logback.xml"
                                              overwrite="true"/>
                                    </tasks>
                                </configuration>
                                <goals>
                                    <goal>run</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
     </profiles>
```



# 总结

通过实践

1. 普通maven项目，打包时可以通过参数切换不同环境

1. springboot项目打包和运行时都可以通过参数自由切换多环境

本文完整源码见https://github.com/luckywind/TechWorld/blob/master/code/boot/springboot-profile/

# 无法解析一个provided依赖

 Could not resolve following dependencies: [org.apache.logging.log4j:log4j-core:jar:2.11.1 (provided)]

这时去看下pom.xml哪个依赖的范围是provided， 找到它后，把无法解析的那个依赖给排除掉即可