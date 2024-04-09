# debug

debug窗口可以输入表达式并回车计算， 也可以在这里修改值，也可以把表达式加入watch

# 自定义代码模版

## 文件头

1. 新建一个custom模版组，并在该模版组里新建模版
2. 选择上下文， 这里选择Java，这样只在Java代码里有效
3. 贴入模版代码，类名可以起一个变量，`$className$`
4. 编辑变量，Expression选择className()，会自动给你替换类名
5. 勾选静态导入

![image-20200802225850777](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/007S8ZIlly1ghcv1difmej315v0u07ri.jpg)

## logger

```java
private static final Logger logger = LoggerFactory.getLogger($className$.class);
```

## scala文件

![image-20220406102059026](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220406102059026.png)



再创建scala文件时，初始内容就是模版里的代码了

```scala
#if ((${PACKAGE_NAME} && ${PACKAGE_NAME} != ""))package ${PACKAGE_NAME} #end
import com.xiaomi.bigdata.oneid.social.base.common.SocialConstants
import com.xiaomi.bigdata.oneid.social.base.util.{ArgumentParser, DateUtils, PathUtils}
import com.xiaomi.bigdata.oneid.util.{OneIdType, VertexProcessor}
import com.xiaomi.id.mapping.crypto.Crypto
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.slf4j.LoggerFactory
import com.xiaomi.bigdata.oneid.social.base.util.HDFSUtils.{OutputOverwriteParquetWrapper, SparkContextThriftFileWrapper}
import org.apache.spark.rdd.RDD
#parse("File Header.java")
object ${NAME} {
val defaultPartion = 200
  val defaultShufflePartion = 500
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName)
  def main(args: Array[String]): Unit = {
    val sc = SparkContext.getOrCreate()
    val spark = SparkSession
      .builder()
      .appName(this.getClass.getSimpleName)
      .enableHiveSupport()
      .getOrCreate()
    
    import spark.sqlContext.implicits._
    import org.apache.spark.sql.functions._
    val params = ArgumentParser.parse(args)
    val partition: Int = params.getOrElse("partition",defaultPartion).toString.toInt
    val shufflePartition: Int = params.getOrElse("shuffle_partition",defaultShufflePartion).toString.toInt
    val day: String = params.getOrElse("day",20991231).toString
    logger.info(s"params=${params.toString}")
    val in:String= params.get("in").get.toString
    val out:String= params.get("out").get.toString
  }
}
```



# 从命令行打开项目

idea每次打开项目要用鼠标点击好几次，甚是麻烦。于是google到了如下方法：

Tools->Create Command-line Launcher

![image-20200727195159666](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20200727195159666.png)

然后cd 到项目根目录,一个命令打开项目！简直爽爆！

```shell
idea .
```



新版idea这么做：

```shell
cat /usr/local/bin/idea
#!/bin/sh
open -na "IntelliJ IDEA.app" --args "$@"

chmod u+x /usr/local/bin/idea
```





# idea运行跳过错误

Run-Edit Configurations -Before launch里面，把Build换成Build ,no error check就可以了。如果提示找不到主类，可先执行mvn clean compile。

# 展开目录

![image-20210731221941738](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210731221941738.png)

# object in compiler mirror not found

```scala
Error:scalac: Error: object scala.$less$colon$less in compiler mirror not found.
scala.reflect.internal.MissingRequirementError: object scala.$less$colon$less in compiler mirror not found.
	at scala.reflect.internal.MissingRequirementError$.notFound(MissingRequirementError.scala:24)
	at scala.reflect.internal.Mirrors$RootsBase.$anonfun$getModuleOrClass$6(Mirrors.scala:66)
	at scala.reflect.internal.Mirrors$RootsBase.getModuleByName(Mirrors.scala:66)
	at scala.reflect.internal.Mirrors$RootsBase.getRequiredModule(Mirrors.scala:163)
	at scala.reflect.internal.Mirrors$RootsBase.requiredModule(Mirrors.scala:173)
	at scala.reflect.internal.Definitions$DefinitionsClass$RunDefinitions.SubTypeModule$lzycompute(Definitions.scala:1672)
	at scala.reflect.internal.Definitions$DefinitionsClass$RunDefinitions.SubTypeModule(Definitions.scala:1672)
	at scala.reflect.internal.Definitions$DefinitionsClass$RunDefinitions.SubType_refl$lzycompute(Definitions.scala:1673)
	at scala.reflect.internal.Definitions$DefinitionsClass$RunDefinitions.SubType_refl(Definitions.scala:1673)
	at scala.tools.nsc.typechecker.Implicits$ImplicitSearch$ImplicitComputation.isIneligible(Implicits.scala:1022)
	at scala.tools.nsc.typechecker.Implicits$ImplicitSearch$ImplicitComputation.survives(Implicits.scala:1029)
	at scala.tools.nsc.typechecker.Implicits$ImplicitSearch$ImplicitComputation.eligibleNew(Implicits.scala:1116)
	at scala.tools.nsc.typechecker.Implicits$ImplicitSearch$ImplicitComputation.<init>(Implicits.scala:1171)
	at scala.tools.nsc.typechecker.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1305)
	at scala.tools.nsc.typechecker.Implicits$ImplicitSearch.bestImplicit(Implicits.scala:1704)
	at scala.tools.nsc.typechecker.Implicits.inferImplicit1(Implicits.scala:112)
	at scala.tools.nsc.typechecker.Implicits.inferImplicit(Implicits.scala:91)
	at scala.tools.nsc.typechecker.Implicits.inferImplicit$(Implicits.scala:88)
	at scala.tools.nsc.Global$$anon$5.inferImplicit(Global.scala:483)
	at scala.tools.nsc.typechecker.Implicits.inferImplicitView(Implicits.scala:50)
	at scala.tools.nsc.typechecker.Implicits.inferImplicitView$(Implicits.scala:49)
	at scala.tools.nsc.Global$$anon$5.inferImplicitView(Global.scala:483)
	at scala.tools.nsc.typechecker.Typers$Typer.inferView(Typers.scala:336)
	at scala.tools.nsc.typechecker.Typers$Typer.viewExists(Typers.scala:306)
	at scala.tools.nsc.typechecker.Typers$Typer$$anon$1.$anonfun$isCoercible$1(Typers.scala:219)
	at scala.runtime.java8.JFunction0$mcZ$sp.apply(JFunction0$mcZ$sp.scala:17)
	at scala.reflect.internal.tpe.TypeConstraints$UndoLog.undo(TypeConstraints.scala:68)
	at scala.tools.nsc.typechecker.Typers$Typer$$anon$1.isCoercible(Typers.scala:219)
	at scala.tools.nsc.typechecker.Infer$Inferencer.isCompatible(Infer.scala:345)
	at scala.tools.nsc.typechecker.Infer$Inferencer.$anonfun$isCompatibleArgs$1(Infer.scala:353)
	at scala.tools.nsc.typechecker.Infer$Inferencer.$anonfun$isCompatibleArgs$1$adapted(Infer.scala:353)
	at scala.collection.immutabl
```



网上普遍说是jdk的问题，切换成jdk8就解决，还有说是maven依赖问题，删除重新下载就解决。但两种方法我都尝试了，依然没有解决。

最后，在Project Structure->Global Libraries里通过maven下载的scala全删了， 使用brew下载了scala2.11.12（brew install scala@2.11）并选择它，就ok了。scala-2.13是不行的，可能是2.13依赖了更高版本的jdk。

遇到问题，先clean，再reimport,   scala sdk选择时，要继续定位到lib目录，不包含idea目录

# 查看所有子类

command+alt+B

![image-20210709225127815](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210709225127815.png)

# 快速定位文件在左侧文件夹的位置

点击左侧雷达

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/20200227145007996.png)

# spark

[如何使用IDEA远程调试spark](https://support.huaweicloud.com/devg3-mrs/mrs_07_410141.html)

# 时序图

[参考](https://www.bilibili.com/video/BV1LL4y1P7JC/?spm_id_from=333.337.search-card.all.click&vd_source=fa2aaef8ece31d2c310d46092c301b46)

用Sequence Diagram 自动生成 UML文件，然后在IDEA中打开\编辑，PlantUML插件实时查看

## /opt/local/bin/dot找不到

老版本mac不能使用brew了， 使用conda install graphviz安装后，把可执行文件dot放到指定位置，idea提示dot崩溃了

[参考这里](https://plantuml.com/zh/graphviz-dot)，



# 破解&远程开发

破解： https://www.exception.site/essay/how-to-free-use-intellij-idea-2019-3

[破解到2099](http://www.itzoo.net/idea/intellij-idea-2023-2-latest-crack-tutorial-activation-2099.html)

远程开发：https://cloud.tencent.com/developer/article/1979328

## 破解

重装了idea，但是打开时提示“应用程序“IntelliJ IDEA”无法打开”

为了查看具体是什么错误，直接执行可执行文件：

```shell
/Applications/IntelliJ\ IDEA.app/Contents/MacOS/idea
2023-04-19 15:29:29.458 idea[3899:50432] allVms required 1.8*,1.8+
2023-04-19 15:29:29.460 idea[3899:50435] Current Directory: /Users/chengxingfu/Downloads
2023-04-19 15:29:29.460 idea[3899:50435] parseVMOptions: IDEA_VM_OPTIONS = (null)
2023-04-19 15:29:29.460 idea[3899:50435] fullFileName is: /Applications/IntelliJ IDEA.app/Contents/bin/idea.vmoptions
2023-04-19 15:29:29.460 idea[3899:50435] fullFileName exists: /Applications/IntelliJ IDEA.app/Contents/bin/idea.vmoptions
2023-04-19 15:29:29.460 idea[3899:50435] parseVMOptions: /Applications/IntelliJ IDEA.app/Contents/bin/idea.vmoptions
2023-04-19 15:29:29.460 idea[3899:50435] parseVMOptions: /Applications/IntelliJ IDEA.app.vmoptions
2023-04-19 15:29:29.463 idea[3899:50435] parseVMOptions: /Users/chengxingfu/Library/Application Support/JetBrains/IntelliJIdea2023.1/idea.vmoptions
2023-04-19 15:29:29.463 idea[3899:50435] parseVMOptions: platform=18 user=19 file=/Users/chengxingfu/Library/Application Support/JetBrains/IntelliJIdea2023.1/idea.vmoptions
OpenJDK 64-Bit Server VM warning: Options -Xverify:none and -noverify were deprecated in JDK 13 and will likely be removed in a future release.
Error opening zip file or JAR manifest missing : /Applications/IntelliJ IDEA.app/Contents/cxf/jetbrainsCrack.jar
Error occurred during initialization of VM
agent library failed to init: instrument
```

发现是/Users/chengxingfu/Library/Application Support/JetBrains/IntelliJIdea2023.1/idea.vmoptions这个文件配置了一个jar，而增额jar不存在jetbrainsCrack.jar

删除配置文件中的-javaagent:/Applications/IntelliJ IDEA.app/Contents/cxf/jetbrainsCrack.jar，再次启动又报错：

```shell
Internal error. Please refer to https://jb.gg/ide/critical-startup-errors

com.intellij.diagnostic.PluginException: Fatal error initializing 'com.github.izhangzhihao.rainbow.brackets.component.RainbowComponent
            ' [Plugin: izhangzhihao.rainbow.brackets]
    at com.intellij.serviceContainer.ComponentManagerImpl.handleInitComponentError$intellij_platform_serviceContainer(ComponentManagerImpl.kt:612)
    at com.intellij.serviceContainer.ComponentManagerImpl.registerComponents(ComponentManagerImpl.kt:416)
    at com.intellij.serviceContainer.ComponentManagerImpl.access$registerComponents(ComponentManagerImpl.kt:69)
    at com.intellij.serviceContainer.ComponentManagerImpl.registerComponents(ComponentManagerImpl.kt:294)
    at com.intellij.openapi.client.ClientAwareComponentManager.registerComponents(ClientAwareComponentManager.kt:46)
    at com.intellij.idea.ApplicationLoader.doInitApplication(ApplicationLoader.kt:86)
    at com.intellij.idea.ApplicationLoader.access$doInitApplication(ApplicationLoader.kt:1)
    at com.intellij.idea.ApplicationLoader$doInitApplication$1.invokeSuspend(ApplicationLoader.kt)
    at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
    at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
    at kotlinx.coroutines.EventLoopImplBase.processNextEvent(EventLoop.common.kt:284)
    at kotlinx.coroutines.BlockingCoroutine.joinBlocking(Builders.kt:85)
    at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking(Builders.kt:59)
    at kotlinx.coroutines.BuildersKt.runBlocking(Unknown Source)
    at com.intellij.idea.ApplicationLoader.initApplication(ApplicationLoader.kt:63)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
    at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
    at java.base/java.lang.reflect.Method.invoke(Method.java:568)
    at com.intellij.idea.a.T(a.java:73)
    at com.intellij.ide.a.k.a_.T(a_.java:139)
    at com.intellij.ide.a.k.a0.a(a0.java:195)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
    at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
    at java.base/java.lang.reflect.Method.invoke(Method.java:568)
    at com.intellij.idea.MainImpl.start(MainImpl.kt:62)
    at com.intellij.idea.StartupUtil$startApplication$7.invokeSuspend(StartupUtil.kt:297)
    at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
    at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
    at kotlinx.coroutines.EventLoopImplBase.processNextEvent(EventLoop.common.kt:284)
    at kotlinx.coroutines.BlockingCoroutine.joinBlocking(Builders.kt:85)
    at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking(Builders.kt:59)
    at kotlinx.coroutines.BuildersKt.runBlocking(Unknown Source)
    at com.intellij.idea.Main.main(Main.kt:40)
Caused by: java.lang.ClassNotFoundException: com.github.izhangzhihao.rainbow.brackets.component.RainbowComponent
             PluginClassLoader(plugin=PluginDescriptor(name=Rainbow Brackets, id=izhangzhihao.rainbow.brackets, descriptorPath=plugin.xml, path=~/Library/Application Support/JetBrains/IntelliJIdea2023.1/plugins/intellij-rainbow-brackets, version=5.26, package=null, isBundled=false), packagePrefix=null, instanceId=293, state=active)
    at com.intellij.ide.plugins.cl.PluginClassLoader.loadClass(PluginClassLoader.kt:150)
    at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:520)
    at com.intellij.serviceContainer.ComponentManagerImpl.registerComponent(ComponentManagerImpl.kt:536)
    at com.intellij.serviceContainer.ComponentManagerImpl.registerComponents(ComponentManagerImpl.kt:408)
    ... 33 more

-----
Your JRE: 17.0.6+10-b829.5 x86_64 (JetBrains s.r.o.)
/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home
```

看起来是/Library/Application Support/JetBrains/IntelliJIdea2023.1/plugins/intellij-rainbow-brackets这个插件兼容性问题，把它移走。再次启动就ok了。

![image-20230419154033009](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230419154033009.png)

```shell
cp -r  jetbra  /Applications/IntelliJIdeaCrack/jetbra
cd /Applications/IntelliJIdeaCrack/jetbra
sudo bash install.sh #如果报错sed: RE error: illegal byte sequence， 可先执行export LC_ALL='C'
```

执行完脚本，输出：done. the "kill Dock" command can fix the crash issue.

重启电脑，打开idea，输入激活码

```shell
`6G5NXCPJZB-eyJsaWNlbnNlSWQiOiI2RzVOWENQSlpCIiwibGljZW5zZWVOYW1lIjoic2lnbnVwIHNjb290ZXIiLCJhc3NpZ25lZU5hbWUiOiIiLCJhc3NpZ25lZUVtYWlsIjoiIiwibGljZW5zZVJlc3RyaWN0aW9uIjoiIiwiY2hlY2tDb25jdXJyZW50VXNlIjpmYWxzZSwicHJvZHVjdHMiOlt7ImNvZGUiOiJQU0kiLCJmYWxsYmFja0RhdGUiOiIyMDI1LTA4LTAxIiwicGFpZFVwVG8iOiIyMDI1LTA4LTAxIiwiZXh0ZW5kZWQiOnRydWV9LHsiY29kZSI6IlBEQiIsImZhbGxiYWNrRGF0ZSI6IjIwMjUtMDgtMDEiLCJwYWlkVXBUbyI6IjIwMjUtMDgtMDEiLCJleHRlbmRlZCI6dHJ1ZX0seyJjb2RlIjoiSUkiLCJmYWxsYmFja0RhdGUiOiIyMDI1LTA4LTAxIiwicGFpZFVwVG8iOiIyMDI1LTA4LTAxIiwiZXh0ZW5kZWQiOmZhbHNlfSx7ImNvZGUiOiJQUEMiLCJmYWxsYmFja0RhdGUiOiIyMDI1LTA4LTAxIiwicGFpZFVwVG8iOiIyMDI1LTA4LTAxIiwiZXh0ZW5kZWQiOnRydWV9LHsiY29kZSI6IlBHTyIsImZhbGxiYWNrRGF0ZSI6IjIwMjUtMDgtMDEiLCJwYWlkVXBUbyI6IjIwMjUtMDgtMDEiLCJleHRlbmRlZCI6dHJ1ZX0seyJjb2RlIjoiUFNXIiwiZmFsbGJhY2tEYXRlIjoiMjAyNS0wOC0wMSIsInBhaWRVcFRvIjoiMjAyNS0wOC0wMSIsImV4dGVuZGVkIjp0cnVlfSx7ImNvZGUiOiJQV1MiLCJmYWxsYmFja0RhdGUiOiIyMDI1LTA4LTAxIiwicGFpZFVwVG8iOiIyMDI1LTA4LTAxIiwiZXh0ZW5kZWQiOnRydWV9LHsiY29kZSI6IlBQUyIsImZhbGxiYWNrRGF0ZSI6IjIwMjUtMDgtMDEiLCJwYWlkVXBUbyI6IjIwMjUtMDgtMDEiLCJleHRlbmRlZCI6dHJ1ZX0seyJjb2RlIjoiUFJCIiwiZmFsbGJhY2tEYXRlIjoiMjAyNS0wOC0wMSIsInBhaWRVcFRvIjoiMjAyNS0wOC0wMSIsImV4dGVuZGVkIjp0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`
```



## 远程开发

[连接Gateway](https://www.jetbrains.com/help/idea/remote-development-a.html#gateway)

连接服务器，会在服务器/root/.cache/JetBrains/RemoteDev这个目录下安装Remote Server,并启动.

本地启动一个JetBrains Client

![image-20230419162330887](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230419162330887.png)

打开即可看到项目代码

![image-20230419162840356](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230419162840356.png)

只是需要重新安装插件



### JetBrains Gateway﻿

一个连接远程服务的轻量启动器，下载需要的组件、并在JetBrain Client中打开项目

#### 首次启动（在服务器安装idea）

Remote Development里选SSH,连接服务器即可

#### stop

点击Running图标右边的三个点，选择Stop IDE Backend

#### 手动启动remote  IDE并连接

一个connection对应一个project，如果要打开一个新的project，需要新建一个connection，主要流程如下：

- Start a backend project in the remote IDE.

- Select one of the connection links generated by the backend.

- Open the link on your local machine.

连接remote IDE:
服务器上执行命令：

```shell
remote-dev-server.sh run /path_to_project/ 
--ssh-link-host host_server_address
--ssh-link-user remote_side_user
--ssh-link-port ssh_connection_port
这三个参数都有默认值，可不用指定
```

成功启动会生成三个链接：

Join link

Http link: 展示欢迎页，调起Gateway

Gateway link: 直接调起Gateway

这三个链接可以复制到浏览器里打开，也可以直接在Gageway里打开



#### 下载Backend

1. 自动下载，需要能访问jetbrain的官网

   ```
   https://code-with-me.jetbrains.com
   https://download.jetbrains.com
   https://download-cf.jetbrains.com
   https://cache-redirector.jetbrains.com
   ```

   

2. Use download link
3. 本地上传，需要事先下载.tar.gz安装包



#### 打开新项目失败

This IDE build has expired. Provide another build or select 'JetBrains Installer' from the installation options to install the latest version.



## 问题

### preferences设置页没有响应，可以搜索idea can't open preferences。[找到的](https://intellij-support.jetbrains.com/hc/en-us/community/posts/360000387760-2018-2-Can-t-open-Settings)

有人说是插件导致的，可以关闭所有项目后，在welcome页面disable一些可疑插件，再次打开就行了。

我禁用了easy Code

### JetBrains Client启动失败

```shell
~/Library/Logs/JetBrains/IntelliJIdea2023.1
```

/Users/chengxingfu/Library/Logs/JetBrains/IntelliJIdea2023.1/gateway/下查看最新日志

CodeWithMeClientDownloader - GUEST OUTPUT: LSOpenURLsWithRole() failed for the application /Users/chengxingfu/Library/Caches/JetBrains/JetBrainsClientDist/JetBrainsClient-232.8660.142.sit-49b2997ced.ide.d/JetBrains Client.app with error -10825.

搜索一番发现可能是因为操作系统不兼容，于是降低Client版本

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230724154349709.png" alt="image-20230724154349709" style="zoom:50%;" />

### host unreachable

退出Gateaway后，出现Host unreachable, 服务器上没有idea进程了，且出现Input/output error

### 找不到sun.misc

Java抛异常： java.nio.DirectByteBuffer.<init>(long, int) not available   

[程序包sun.misc找不到](https://github.com/airlift/slice/issues/106)，JDK11丢弃了sun.misc,修改为jdk8后解决了这个问题



Modules->Dependencies->Module SDK改成java8

### illegal cyclic reference involving trait Iterable

scala和spark的版本不兼容，scala-2.13换成scala-2.12.4就成了

### Failed to exec spawn helper

[参考](https://blog.csdn.net/wbkys/article/details/129370160)修改launch.sh,并重启BackEnd解决了这个问题

#### is already defined as

在Modules的Sources里把多余的Content Root删掉

![image-20230519152451581](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230519152451581.png)



实在不行，

1. 调整包的属性，就是Mark as SourceCode等等
2. invalid cache and restart
3. 删除.idea 目录重新导入项目

#### scalac  sdk in module 

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230420151118730.png" alt="image-20230420151118730" style="zoom:50%;" />



#### Service 'sparkDriver' could not bind on a random free port. You may check whether configuring an appropriate binding address.

```java
23/04/20 15:12:33 WARN Utils: Service 'sparkDriver' could not bind on a random free port. You may check whether configuring an appropriate binding address.
23/04/20 15:12:33 WARN Utils: Service 'sparkDriver' could not bind on a random free port. You may check whether configuring an appropriate binding address.
23/04/20 15:12:33 ERROR SparkContext: Error initializing SparkContext.
java.net.BindException: 无法指定被请求的地址: Service 'sparkDriver' failed after 16 retries (on a random free port)! Consider explicitly setting the appropriate binding address for the service 'sparkDriver' (for example spark.driver.bindAddress for SparkDriver) to the correct binding address.
        at java.base/sun.nio.ch.Net.bind0(Native Method)
        at java.base/sun.nio.ch.Net.bind(Net.java:459)
        at java.base/sun.nio.ch.Net.bind(Net.java:448)
        at java.base/sun.nio.ch.ServerSocketChannelImpl.bind(ServerSocketChannelImpl.java:227)
        at io.netty.channel.socket.nio.NioServerSocketChannel.doBind(NioServerSocketChannel.java:134)
        at io.netty.channel.AbstractChannel$AbstractUnsafe.bind(AbstractChannel.java:562)
        at io.netty.channel.DefaultChannelPipeline$HeadContext.bind(DefaultChannelPipeline.java:1334)
        at io.netty.channel.AbstractChannelHandlerContext.invokeBind(AbstractChannelHandlerContext.java:506)
        at io.netty.channel.AbstractChannelHandlerContext.bind(AbstractChannelHandlerContext.java:491)
        at io.netty.channel.DefaultChannelPipeline.bind(DefaultChannelPipeline.java:973)
        at io.netty.channel.AbstractChannel.bind(AbstractChannel.java:260)
        at io.netty.bootstrap.AbstractBootstrap$2.run(AbstractBootstrap.java:356)
        at io.netty.util.concurrent.AbstractEventExecutor.safeExecute(AbstractEventExecutor.java:164)
        at io.netty.util.concurrent.SingleThreadEventExecutor.runAllTasks(SingleThreadEventExecutor.java:469)
        at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:500)
        at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:986)
        at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
        at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
        at java.base/java.lang.Thread.run(Thread.java:834)
```

这个是地址绑定错误，本机配置的HOSTNAME是node28161,但是shell中被改为了master

[root@master conf]# cat /etc/sysconfig/network

Created by anaconda

HOSTNAME=node28161

此时，只要执行hostname  node28161改为一致就行了

### java.lang.NoSuchMethodError: java.nio.ByteBuffer.rewind()Ljava/nio/ByteBuffer;

yarn使用的jdk版本过低。修改yarn-env.sh中的JAVA_HOME即可， 我是修改了软连接，指向jdk-11

向集群提交，写本地文件是，其实写到了集群的每个节点上

### 无效的发行版

settings里检查java compiler

### 单测包括provide依赖

到Modify options列表里去选择

![image-20230513171819716](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230513171819716.png)
