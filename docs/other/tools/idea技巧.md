# 自定义代码模版

## 文件头

1. 新建一个custom模版组，并在该模版组里新建模版
2. 选择上下文， 这里选择Java，这样只在Java代码里有效
3. 贴入模版代码，类名可以起一个变量，`$className$`
4. 编辑变量，Expression选择className()，会自动给你替换类名
5. 勾选静态导入

![image-20200802225850777](https://tva1.sinaimg.cn/large/007S8ZIlly1ghcv1difmej315v0u07ri.jpg)

## logger

```java
private static final Logger logger = LoggerFactory.getLogger($className$.class);
```

## scala文件

![image-20220406102059026](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220406102059026.png)



再创建scala文件时，初始内容就是模版里的代码了



# 从命令行打开项目

idea每次打开项目要用鼠标点击好几次，甚是麻烦。于是google到了如下方法：

Tools->Create Command-line Launcher

![image-20200727195159666](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20200727195159666.png)

然后cd 到项目根目录,一个命令打开项目！简直爽爆！

```shell
idea .
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
