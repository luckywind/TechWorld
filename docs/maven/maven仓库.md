[原文](https://mp.weixin.qq.com/s?__biz=MzA5MTkxMDQ4MQ==&mid=2648933541&idx=1&sn=8617c73b82d8aa4517a6357261a882b4&chksm=88621c9bbf15958d96ec7ebd83571245e765f1df07ac7dca028e93a656c2e91d943dc7dab08f&token=428347706&lang=zh_CN&scene=21#wechat_redirect)

本篇探讨7个问题:

1. maven是如何找到我们依赖的jar的
2. 什么是仓库
3. 仓库的分类
4. 各种类型仓库详解
5. maven中远程仓库配置详解
6. 关于构件版本问题说明
7. 构件文件的布局

maven采用引用的方式将依赖的jar引入进来，不对真实的jar进行拷贝，但是打包的时候，运行需要用到的jar都会被拷贝到安装包中。

# 仓库
在 Maven 中，任何一个依赖、插件或者项目构建的输出，都可以称之为构件。
在 Maven 中，仓库是一个位置，这个位置是用来存放各种第三方构件的，所有maven项目可以共享这个仓库中的构件。
Maven 仓库能帮助我们管理构件（主要是jar包），它就是放置所有jar文件（jar、war、zip、pom等等）的地方。

## 仓库的分类

主要分为2大类：

1. **本地仓库**
2. **远程仓库**

而远程仓库又分为：中央仓库、私服、其他公共远程仓库

### 本地仓库

当maven根据坐标寻找构件的时候，会首先查看本地仓库，如果本地仓库存在，则直接使用；如果本地不存在，maven会去远程仓库中查找，如果找到了，会将其下载到本地仓库中进行使用，如果本地和远程仓库都没有找到构件，maven会报错，构件只有在本地仓库中存在了，才能够被maven项目使用。

默认情况下，maven本地仓库默认地址是`~/.m2/respository`目录，这个默认我们也可以在`~/.m2/settings.xml`文件中进行修改：

```
<localRepository>本地仓库地址</localRepository>
```

当我们使用maven的时候，依赖的构件都会从远程仓库下载到本地仓库目录中。

将maven安装目录中的`settings.xml`拷贝到`~/.m2`中进行编辑，这个是用户级别的，只会影响当前用户。

### 远程仓库

#### 中央仓库

中央仓库有几个特点：

1. 中央仓库是由maven官方社区提供给大家使用的
2. 不需要我们手动去配置，maven内部集成好了
3. 使用中央仓库时，机器必须是联网状态，需要可以访问中央仓库的地址

中央仓库还为我们提供了一个检索构件的站点：

[检索jar包](https://search.maven.org/)

#### 私服

**总体上来说私服有以下好处：**

1. 加速maven构件的下载速度
2. 节省宽带
3. 方便部署自己的构件以供他人使用
4. 提高maven的稳定性，中央仓库需要本机能够访问外网，而如果采用私服的方式，只需要本机可以访问内网私服就可以了

#### 其他远程仓库

中央仓库是在国外的，访问速度不是特别快，所以有很多比较大的公司做了一些好事，自己搭建了一些maven仓库服务器，公开出来给其他开发者使用，比如像阿里、网易等等，他们对外提供了一些maven仓库给全球开发者使用，在国内的访问速度相对于maven中央仓库来说还是快了不少。

还有一些公司比较牛，只在自己公开的仓库中发布构件，这种情况如果要使用他们的构件时，需要去访问他们提供的远程仓库地址。

## 构建文件的布局

```shell
[artifactId][-verion][-classifier].[type]
```

例如fastjson构建：

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.62</version>
</dependency>
```

fastjson-1.2.62.jar信息如下：

```
artifactId为fastjson
version为1.2.62
classifier为空
type没有指定，默认为jar
```

所以构件文件名称为`fastjson-1.2.62.jar`。

## 配置远程仓库

### 方式一

```xml
<project>
    <repositories>
        <repository>
            <id>aliyun-releases</id>
            <url>https://maven.aliyun.com/repository/public</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>
</project>
```

在repositories元素下，可以使用repository子元素声明一个或者多个远程仓库。

repository元素说明：

- id：远程仓库的一个标识，中央仓库的id是`central`，所以添加远程仓库的时候，id不要和中央仓库的id重复，会把中央仓库的覆盖掉
- url：远程仓库地址
- releases：主要用来配置是否需要从这个远程仓库下载稳定版本构建
- snapshots：主要用来配置是否需要从这个远程仓库下载快照版本构建

releases和snapshots中有个`enabled`属性，是个boolean值，默认为true，表示是否需要从这个远程仓库中下载稳定版本或者快照版本的构建，一般使用第三方的仓库，都是下载稳定版本的构建。

快照版本的构建以`-SNAPSHOT`结尾，稳定版没有这个标识。

##### 示例

来感受一下pom方式配置远程仓库的效果。

文本编辑器打开`maven-chat03/pom.xml`，将下面内容贴进去：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.javacode2018</groupId>
    <artifactId>maven-chat03</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.62</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>2.2.1.RELEASE</version>
        </dependency>
    </dependencies>

    <repositories>
        <repository>
            <id>aliyun-releases</id>
            <url>https://maven.aliyun.com/repository/public</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>

</project>
```

pom中配置远程仓库的方式只对当前项目起效，如果我们需要对所有项目起效，我们可以下面的方式2，向下看。

### 方式二：镜像

如果仓库X可以提供仓库Y所有的内容，那么我们就可以认为X是Y的一个镜像，通俗点说，可以从Y获取的构件都可以从他的镜像中进行获取。

可以采用镜像的方式配置远程仓库，镜像在`settings.xml`中进行配置，对所有使用该配置的maven项目起效，配置方式如下：

```xml
<mirror>
  <id>mirrorId</id>
  <mirrorOf>repositoryId</mirrorOf>
  <name>Human Readable Name for this Mirror.</name>
  <url>http://my.repository.com/repo/path</url>
</mirror>
```

mirrors元素下面可以有多个mirror元素，每个mirror元素表示一个远程镜像，元素说明：

- id：镜像的id，是一个标识
- name：镜像的名称，这个相当于一个描述信息，方便大家查看
- url：镜像对应的远程仓库的地址
- mirrorOf：指定哪些远程仓库的id使用这个镜像，这个对应pom.xml文件中repository元素的id，就是表示这个镜像是给哪些pom.xml文章中的远程仓库使用的，这里面需要列出远程仓库的id，多个之间用逗号隔开，`*`表示给所有远程仓库做镜像

这里主要对mirrorOf再做一下说明，上面我们在项目中定义远程仓库的时候，pom.xml文件的repository元素中有个id，这个id就是远程仓库的id，而mirrorOf就是用来配置哪些远程仓库会走这个镜像去下载构件。

mirrorOf的配置有以下几种:

```
<mirrorOf>*</mirrorOf> 
```

> 上面匹配所有远程仓库id，这些远程仓库都会走这个镜像下载构件

```
<mirrorOf>远程仓库1的id,远程仓库2的id</mirrorOf> 
```

> 上面匹配指定的仓库，这些指定的仓库会走这个镜像下载构件

```
<mirrorOf>*,! repo1</mirrorOf> 
```

> 上面匹配所有远程仓库，repo1除外，使用感叹号将仓库从匹配中移除。

#### 示例

修改~/.m2/settings.xml，加入镜像配置，如下：

```xml
<mirrors>
    <mirror>
        <id>mirror-aliyun-releases</id>
        <mirrorOf>*</mirrorOf>
        <name>阿里云maven镜像</name>
        <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
</mirrors>
```

关于镜像一个比较常用的用法是结合私服一起使用，由于私服可以代理所有远程仓库（包含中央仓库），因此对于maven用来来说，只需通过访问一个私服就可以间接访问所有外部远程仓库了，这块后面我们会在私服中做具体说明。

# 优先级

总结Maven 远程仓库优先级了。

主要有以下几点：
1.从日志信息我们得出这几种maven仓库的优先级别为

> 本地仓库 > 私服 （profile）> 远程仓库（repository）和 镜像 （mirror） > 中央仓库 （central）

2.镜像是一个特殊的配置，其实镜像等同与远程仓库，没有匹配远程仓库的镜像就毫无作用（如 foo2）。
3.总结上面所说的，Maven 仓库的优先级就是 **私服和远程仓库** 的对比，没有其它的仓库类型。为什么这么说是因为，镜像等同远程，而中央其实也是 maven super xml 配置的一个repository 的一个而且。所以 maven 仓库真正的优先级为

> 本地仓库 > 私服（profile）> 远程仓库（repository）