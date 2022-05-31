[参考](https://www.baeldung.com/maven-archetype)

[创建原型](https://maven.apache.org/guides/mini/guide-creating-archetypes.html)

blog posts: [one](http://blogs.jetbrains.com/idea/2009/10/new-packaging-configurations/) and [two](http://blogs.jetbrains.com/idea/2009/10/update-a-running-javaee-application/)：

Artifact:将项目中的东西组合在一起的一个过程,就是如何打包。



# 使用archetype快速创建项目

set `groupId` to `com.company`, 

`artifactId` to `project`, 

`version` to `1.0` and 

`package` to `com.company.project`

```shell
mvn archetype:generate
```



Maven 原型是一种项目的抽象，可以实例化为具体的定制 Maven 项目。简而言之，它是一个模板项目模板，从中创建其他项目

# 原型

![image-20210731214431466](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210731214431466.png)

## Maven Archetype 描述文件

位于jar包的META-INF/maven目录下

```xml
<archetype-descriptor
  ...
  name="custom-archetype">
<!--提供一些参数，可选择默认值-->
    <requiredProperties> 
        <requiredProperty key="foo">
            <defaultValue>bar</defaultValue>
        </requiredProperty>
    </requiredProperties>
<!--并且，通过在 fileSet 中使用 packaged="true"，我们表示所选文件将被添加到包属性指定的文件夹层次结构中。-->
    <fileSets>
        <fileSet filtered="true" packaged="true">
            <directory>src/main/java</directory>
            <includes>
                <include>**/*.java</include>
            </includes>
        </fileSet>
    </fileSets>
<!--创建多个模块-->
    <modules>
        <module name="sub-module"></module>
    </modules>

</archetype-descriptor>
```

# 创建一个原型

原型Archetype是一个特殊的maven项目，包含以下：

- *src/main/resources/archetype-resources* 项目resources
- *src/main/resources/META-INF/maven/archetype[-metadata].xml*: 描述文件

要手动创建原型，我们可以从一个新创建的 Maven 项目开始，然后我们可以添加上面提到的资源。
或者，我们可以使用 archetype-maven-plugin 生成它，然后自定义 archetype-resources 目录和 archetype-metadata.xml 文件的内容。

生成一个原型：

```shell
mvn archetype:generate -B -DarchetypeArtifactId=maven-archetype-archetype \
  -DarchetypeGroupId=maven-archetype \
  -DgroupId=com.cxf \
  -DartifactId=test-archetype
```

也可以从一个已有项目生成：

```shell
mvn archetype:create-from-project
```



## 初始化一个原型项目

```shell
mvn archetype:generate\
  -DgroupId=com.cxf\
  -DartifactId=my-archetype\
  -DarchetypeGroupId=org.apache.maven.archetypes\
  -DarchetypeArtifactId=maven-archetype-archetype
```



## 修改archetype-resources目录以及原型描述文件

archetype-resources目录是我们的一些项目文件，随意写。

原型描述文件位于src/main/resources/META-INF/maven/archetype-metadata.xml或者archetype.xml

[可参考](https://maven.apache.org/archetype/archetype-models/archetype-descriptor/archetype-descriptor.html)

```xml
<archetype-descriptor
  xmlns="http://maven.apache.org/plugins/maven-archetype-plugin/archetype-descriptor/1.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/plugins/maven-archetype-plugin/archetype-descriptor/1.1.0 https://maven.apache.org/xsd/archetype-descriptor-1.1.0.xsd"
  name="my-archetype">
  <!-- 注意这个name和项目的artifactId要一样-->
  <fileSets>
    <fileSet filtered="true" packaged="true">
      <directory>src/main/java</directory>
    </fileSet>
    <fileSet>
      <directory>src/test/java</directory>
    </fileSet>
  </fileSets>
</archetype-descriptor>
```



## 添加pom文件

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
 
  <!-- 注意模版的写法-->
  <groupId>${groupId}</groupId>
  <artifactId>${artifactId}</artifactId>
  <version>${version}</version>
  <packaging>jar</packaging>
  <name>${artifactId}</name>
  
    <url>http://www.myorganization.org</url>
 
    <dependencies>
        <dependency>
                <groupId>junit</groupId>
                <artifactId>junit</artifactId>
                 <version>4.12</version>
                <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

修改完后，文件结构如下：

![image-20210801000630406](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210801000630406.png)



## 构建原型

```shell
mvn clean install 
```

这会在target目录下生成相关文件

# 使用自定义的原型创建项目

注意，原型的坐标一定要写对

groupId一般是和原型一样

```shell
mvn archetype:generate \
  -DarchetypeGroupId=com.cxf \
  -DarchetypeArtifactId=my-archetype \
  -DarchetypeVersion=1.0-SNAPSHOT \
  -DgroupId=com.cxf \
  -DartifactId=my-project \
  -DinteractiveMode=false
```

# 描述文件写法

[可参考](https://maven.apache.org/archetype/archetype-models/archetype-descriptor/archetype-descriptor.html)

```xml
<archetype-descriptor xmlns="https://maven.apache.org/plugins/maven-archetype-plugin/archetype-descriptor/1.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="https://maven.apache.org/plugins/maven-archetype-plugin/archetype-descriptor/1.1.0 http://maven.apache.org/xsd/archetype-descriptor-1.1.0.xsd"
  name=.. partial=.. >
  <!--
name : 原型的名称
partial: 原型是否代表整个项目，默认false
--->
  <requiredProperties>  
<!--一些属性,工程里可用${keya}引用
-->
    <requiredProperty key=.. >
      <defaultValue/>
      <validationRegex/>  <!--属性校验正则表达式-->
    </requiredProperty>
  </requiredProperties>
 
  <fileSets>
    <!--
filtered: boolean,false文件直接拷贝,模版变量不被替换，true时被替换！！！
packaged: boolean,false文件直接拷贝
encoding: string，文件过滤所用编码
-->
    <fileSet filtered=.. packaged=.. encoding=.. >
      <directory/>
      <includes/>
      <excludes/>
    </fileSet>
  </fileSets>
 
  <modules>
    <module id=.. dir=.. name=.. >
 
      <fileSets>
        <fileSet filtered=.. packaged=.. encoding=.. >
          <directory/>
          <includes/>
          <excludes/>
        </fileSet>
      </fileSets>
 
      <modules>
        <module>...recursion...<module>
      </modules>
    </module>
  </modules>
</archetype-descriptor>
```

