# Hello world



```shell
 ~/code/open/spark/spark/hello
$ mkdir hello
$ cd hello
$ echo 'object Hi { def main(args: Array[String]) = println("Hi!") }' > hw.scala
$ sbt
...
> run
...
Hi!
```

在这个例子中，sbt 完全按照约定工作。sbt 将会自动找到以下内容：

- 项目根目录下的源文件
- `src/main/scala` 或 `src/main/java` 中的源文件
- `src/test/scala` 或 `src/test/java` 中的测试文件
- `src/main/resources` 或 `src/test/resources` 中的数据文件
- `lib` 中的 jar 文件

默认情况下，sbt 会用和启动自身相同版本的 Scala 来构建项目。 你可以通过执行 `sbt run` 来运行项目或者通过 `sbt console` 进入 [Scala REPL](https://www.scala-lang.org/node/2097)。`sbt console` 已经帮你 设置好项目的 classpath，所以你可以根据项目的代码尝试实际的 Scala 示例。

## 构建定义

基本的构建设置都放在项目根目录的 `build.sbt` 文件里，如果你准备将你的项目打包成一个 jar 包，在 `build.sbt` 中至少要写上 name 和 version。

## 设置sbt版本

可以通过创建 `hello/project/build.properties` 文件强制指定一个版本的 sbt。在这个文件里，编写如下内容来强制使用 1.3.4：

```
sbt.version=1.3.4
```

# 目录结构

## 源文件

 sbt 和 [Maven](https://maven.apache.org/) 的默认的源文件的目录结构是一样的（所有的路径都是相对于基础目录的）：

```shell
src/
  main/
    resources/
       <files to include in main jar here>
    scala/
       <main Scala sources>
    scala-2.12/
       <main Scala 2.12 specific sources>
    java/
       <main Java sources>
  test/
    resources
       <files to include in test jar here>
    scala/
       <test Scala sources>
    scala-2.12/
       <test Scala 2.12 specific sources>
    java/
       <test Java sources>
```

`src/` 中其他的目录将被忽略。而且，所有的隐藏目录也会被忽略

## 构建定义文件

```shell
build.sbt
project/
  Build.scala
```

指定sbt版本，可以让使用不同sbt版本的人获得相同的结果，只需在project/build.properties里指定即可，如果本地不存在会自动下载。如果不指定，则sbt会任意指定一个版本，强烈建议指定版本。

### 什么是构建定义？

构建定义是定义在build.sbt中的，它包含子项目集合。

例如，build.sbt中定义当前目录下的子项目：

```scala
lazy val root = (project in file("."))
  .settings(
    name := "Hello",
    scalaVersion := "2.12.7"
  )
```

每个子项目都是通过key-value对配置。例如，name指定项目名称。

### Build.sbt语法

Key-value对称为setting/task表达式，使用bulld.sbt DSL定义

```scala
ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.12.10"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "hello"
  )
```

DSL结构

![image-20200923143530697](https://gitee.com/luckywind/PigGo/raw/master/image/image-20200923143530697.png)

key是SettingKey[T],TaskKey[T]或者InputKey[T]的实例。

可以使用val /lazy val /def修饰， object和class不允许用在build.sbt中，但可以用在project/目录下的scala源文件中。

## Key

### 类型

有三种key

- `SettingKey[T]`: 其值只会计算一次 (the value is computed when loading the subproject, and kept around).
- `TaskKey[T]`:其值是task,会重复计算
- `InputKey[T]`: 其值是有命令行参数的task

### 内置key

内置key就是Keys这个object的字段，build.sbt隐式`import sbt.Keys._`， 因此sot.Keys.name可以使用name代替。

### 自定义Key

settingKey,taskKey和inputKey都有自己的定义方法，每个方法都需要key的值的类型和一个描述，key的名称是key赋给的那个变量名。例如

```scala
lazy val hello = taskKey[Unit]("An example task")
```

taskKey的名称是hello，它的值类型是Unit。 所有这些定义都是在settings之前执行。

### Task 和Setting keys

TaskKey[T]用于定义一个task, task其实是compile/package等操作。它们可以返回Unit或者一个值，例如，package是一个TaskKey[File]，它的值是它创建的jar文件 。

在sbt交互环境下，compile命令会重新运行一次相关的task。

## 定义task和settings

:= 可以把值赋给一个setting， 可以把一个计算赋给一个task。 setting只会在项目加载时执行一次，task可以多次执行。例如前面的hello task

```scala
lazy val hello = taskKey[Unit]("An example task")

lazy val root = (project in file("."))
  .settings(
    hello := { println("Hello!") }
  )
```

setting不能依赖一个task,因为setting只会在项目加载时计算一次。

## sbt shell中使用key

sbt shell里使用task的名称来执行task，例如compile执行编译任务。使用setting的名称来打印它的值。执行任务不会打印任务的值，可以使用show taskName查看task的返回值。

还可以使用inspect  keyName查看key的说明

## Build.sbt中的imports

默认会引入如下

```scala
import sbt._
import Keys._
```

## Bare .sbt构建定义

就是可以直接写settings，而不需要放到.settings(...)调用里，这称为bare 风格

```scala
ThisBuild / version := "1.0"
ThisBuild / scalaVersion := "2.12.10"
```

## 添加依赖

有两个办法，一是把jar丢到lib/下（未管理的依赖），另一个是添加到依赖：

```scala
val derby = "org.apache.derby" % "derby" % "10.4.1.3"

ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.12.10"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "Hello",
    libraryDependencies += derby
  )
```

这里的% 是用于构造ivy坐标。

# 运行

## 交互模式

`sbt`命令会进入交互模式

## 批处理模式

以空格为分隔符指定参数。对于接受参数的 sbt 命令，将命令和参数用引号引起来一起传给 sbt。例如：

```shell
$ sbt clean compile "testOnly TestA TestB"
```

## 持续构建和测试

在命令前面加上前缀 `~` 后，每当有一个或多个源文件发生变化时就会自动运行该命令。例如，在交互模式下尝试：

```shell
> ~ compile
```

## 常用命令

| `clean`       | 删除所有生成的文件 （在 `target` 目录下）。                  |
| ------------- | ------------------------------------------------------------ |
| `compile`     | 编译源文件（在 `src/main/scala` 和 `src/main/java` 目录下）。 |
| `test`        | 编译和运行所有测试。                                         |
| `console`     | 进入到一个包含所有编译的文件和所有依赖的 classpath 的 Scala 解析器。输入 `:quit`， Ctrl+D （Unix），或者 Ctrl+Z （Windows） 返回到 sbt。 |
| `run <参数>*` | 在和 sbt 所处的同一个虚拟机上执行项目的 main class。         |
| `package`     | 将 `src/main/resources` 下的文件和 `src/main/scala` 以及 `src/main/java` 中编译出来的 class 文件打包成一个 jar 文件。 |
| `help <命令>` | 显示指定的命令的详细帮助信息。如果没有指定命令，会显示所有命令的简介。 |
| `reload`      | 重新加载构建定义（`build.sbt`， `project/*.scala`， `project/*.sbt` 这些文件中定义的内容)。在修改了构建定义文件之后需要重新加载。 |

