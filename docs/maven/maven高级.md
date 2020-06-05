# maven多模块

## 搭建

建一个工程，把src目录删除，作为父模块，它的package是pom类型

新建它的子模块，只要添加到当前模块即可，把Parent置为None。

## 构建

命令

mvn clean install <参数>

```shell
Options:
 -am,--also-make                        同时构建依赖
 -amd,--also-make-dependents            同时构建上层模块(依赖这些模块的模块)
 -B,--batch-mode                        Run in non-interactive (batch)
                                        mode (disables output color)
 -b,--builder <arg>                     The id of the build strategy to
                                        use
 -C,--strict-checksums                  Fail the build if checksums don't
                                        match
 -c,--lax-checksums                     Warn if checksums don't match
 -cpu,--check-plugin-updates            Ineffective, only kept for
                                        backward compatibility
 -D,--define <arg>                      Define a system property
 -e,--errors                            Produce execution error messages
 -emp,--encrypt-master-password <arg>   Encrypt master security password
 -ep,--encrypt-password <arg>           Encrypt server password
 -f,--file <arg>                        Force the use of an alternate POM
                                        file (or directory with pom.xml)
 -fae,--fail-at-end                     Only fail the build afterwards;
                                        allow all non-impacted builds to
                                        continue
 -ff,--fail-fast                        Stop at first failure in
                                        reactorized builds
 -fn,--fail-never                       NEVER fail the build, regardless
                                        of project result
 -gs,--global-settings <arg>            Alternate path for the global
                                        settings file
 -gt,--global-toolchains <arg>          Alternate path for the global
                                        toolchains file
 -h,--help                              Display help information
 -l,--log-file <arg>                    Log file where all build output
                                        will go (disables output color)
 -llr,--legacy-local-repository         Use Maven 2 Legacy Local
                                        Repository behaviour, ie no use of
                                        _remote.repositories. Can also be
                                        activated by using
                                        -Dmaven.legacyLocalRepo=true
 -N,--non-recursive                     Do not recurse into sub-projects
 -npr,--no-plugin-registry              Ineffective, only kept for
                                        backward compatibility
 -npu,--no-plugin-updates               Ineffective, only kept for
                                        backward compatibility
 -nsu,--no-snapshot-updates             Suppress SNAPSHOT updates
 -ntp,--no-transfer-progress            Do not display transfer progress
                                        when downloading or uploading
 -o,--offline                           Work offline
 -P,--activate-profiles <arg>           Comma-delimited list of profiles
                                        to activate
 -pl,--projects <arg>                   Comma-delimited list of specified
                                        reactor projects to build instead
                                        of all projects. A project can be
                                        specified by [groupId]:artifactId
                                        or by its relative path
 -q,--quiet                             Quiet output - only show errors
 -rf,--resume-from <arg>                从指定模块重新提交反应堆
 -s,--settings <arg>                    Alternate path for the user
                                        settings file
 -t,--toolchains <arg>                  Alternate path for the user
                                        toolchains file
 -T,--threads <arg>                     Thread count, for instance 2.0C
                                        where C is core multiplied
 -U,--update-snapshots                  Forces a check for missing
                                        releases and updated snapshots on
                                        remote repositories
 -up,--update-plugins                   Ineffective, only kept for
                                        backward compatibility
 -v,--version                           Display version information
 -V,--show-version                      Display version information
                                        WITHOUT stopping build
 -X,--debug                             Produce execution debug output
```



### 参数

```shell
-am --also-make 同时构建所列模块的依赖模块；
-amd -also-make-dependents 同时构建依赖于所列模块的模块；
-pl --projects <arg> 构建制定的模块，模块间用逗号分隔；
-rf -resume-from <arg> 从指定的模块恢复反应堆。
```



#### -pl,--projects <arg>

project location

构件指定的模块，arg表示多个模块，之间用逗号分开，模块有两种写法

```
-pl 模块1相对路径 [,模块2相对路径] [,模块n相对路径]
-pl [模块1的groupId]:模块1的artifactId [,[模块2的groupId]:模块2的artifactId] [,[模块n的groupId]:模块n的artifactId]
```

例如

```shell
mvn clean install -pl b2b-account
mvn clean install -pl b2b-account/b2b-account-api
mvn clean install -pl b2b-account/b2b-account-api,b2b-account/b2b-account-service
mvn clean install -pl :b2b-account-api,b2b-order/b2b-order-api
mvn clean install -pl :b2b-account-api,:b2b-order-service
```

#### -rf,--resume-from <arg>

从指定的模块恢复反应堆，也就是从指定模块开始构建

#### -am,--also-make

同时构建所列模块的依赖模块

#### -amd,--also-make-dependents

同时构件依赖于所列模块的模块,和上面那个相反！

### 总结

1. 需要掌握`mvn`命令中`-pl、-am、-amd、-rf`的各种用法
2. 注意`-pl`后面的参数的写法：模块相对路径、或者[groupId].artifactId

# 依赖管理

## 依赖范围

<img src="../../../../Library/Application Support/typora-user-images/image-20191228000526164.png" alt="image-20191228000526164" style="zoom:50%;" />

## 依赖的传递

假设A依赖于B，B依赖于C，我们说A对于B是第一直接依赖，B对于C是第二直接依赖，而A对于C是传递性依赖，而第一直接依赖的scope和第二直接依赖的scope决定了传递依赖的范围，即决定了A对于C的scope的值。

下面我们用表格来列一下这种依赖的效果，表格最左边一列表示第一直接依赖（即A->B的scope的值）,而表格中的第一行表示第二直接依赖（即B->C的scope的值），行列交叉的值显示的是A对于C最后产生的依赖效果。

![image-20191228105627626](../../../../Library/Application Support/typora-user-images/image-20191228105627626.png)

解释一下：

1. 比如A->B的scope是`compile`，而B->C的scope是`test`，那么按照上面表格中，对应第2行第3列的值`-`，那么A对于C是没有依赖的，A对C的依赖没有从B->C传递过来，所以A中是无法使用C的
2. 比如A->B的scope是`compile`，而B->C的scope是`runtime`，那么按照上面表格中，对应第2行第5列的值为`runtime`，那么A对于C是的依赖范围是`runtime`，表示A只有在运行的时候C才会被添加到A的classpath中，即对A进行运行打包的时候，C会被打包到A的包中
3. 大家仔细看一下，上面的表格是有规律的，当B->C依赖是compile的时候（表中第2列），那么A->C的依赖范围和A->B的sope是一样的；当B->C的依赖是test时（表中第3列），那么B->C的依赖无法传递给A；当B->C的依赖是provided（表第4列），只传递A->C的scope为provided的情况，其他情况B->C的依赖无法传递给A；当B->C的依赖是runtime（表第5列），那么C按照B->C的scope传递给A

## 依赖调节

现实中可能存在这样的情况，A->B->C->Y(1.0)，A->D->Y(2.0)，此时Y出现了2个版本，1.0和2.0，此时maven会选择Y的哪个版本？

解决这种问题，maven有2个原则：

#### 路径最近原则

上面`A->B->C->Y(1.0)，A->D->Y(2.0)`，Y的2.0版本距离A更近一些，所以maven会选择2.0。

但是如果出现了路径是一样的，如：`A->B->Y(1.0)，A->D->Y(2.0)`，此时maven又如何选择呢？

#### 最先声明原则

如果出现了路径一样的，此时会看A的pom.xml中所依赖的B、D在`dependencies`中的位置，谁的声明在最前面，就以谁的为主，比如`A->B`在前面，那么最后Y会选择1.0版本。

## 排除依赖

```xml
<dependency>
    <groupId>com.javacode2018</groupId>
    <artifactId>B</artifactId>
    <version>1.0</version>
    <exclusions>
        <exclusion>
            <groupId>com.javacode2018</groupId>
            <artifactId>C</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

上面使用使用exclusions元素排除了B->C依赖的传递，也就是B->C不会被传递到A中。

exclusions中可以有多个`exclusion`元素，可以排除一个或者多个依赖的传递，声明exclusion时只需要写上groupId、artifactId就可以了，version可以省略。

# 生命周期和插件

我们开发一个项目的时候，通常有这些环节：创建项目、编写代码、清理已编译的代码、编译代码、执行单元测试、打包、集成测试、验证、部署、生成站点等，这些环节组成了项目的生命周期，这些过程也叫做项目的**构建过程**

## 生命周期

maven中定义的3套生命周期：

1. **clean生命周期**
2. **default生命周期**
3. **site生命周期**

上面这3套生命周期是相互独立的，没有依赖关系的，而每套生命周期中有多个阶段，每套中的多个阶段是有先后顺序的，并且后面的阶段依赖于前面的阶段，而用户可以直接使用`mvn`命令来调用这些阶段去完成项目生命周期中具体的操作，命令是：

```
mvn 生命周期阶段
```

### clean生命周期

clean生命周期的目的是清理项目，它包含三个阶段：

| 生命周期阶段 | 描述                                  |
| :----------- | :------------------------------------ |
| pre-clean    | 执行一些需要在clean之前完成的工作     |
| clean        | 移除所有上一次构建生成的文件          |
| post-clean   | 执行一些需要在clean之后立刻完成的工作 |

### default生命周期

这个是maven主要的生命周期，主要被用于构建应用，包含了23个阶段。

| 生命周期阶段            | 描述                                                         |
| :---------------------- | :----------------------------------------------------------- |
| validate                | 校验：校验项目是否正确并且所有必要的信息可以完成项目的构建过程。 |
| initialize              | 初始化：初始化构建状态，比如设置属性值。                     |
| generate-sources        | 生成源代码：生成包含在编译阶段中的任何源代码。               |
| process-sources         | 处理源代码：处理源代码，比如说，过滤任意值。                 |
| generate-resources      | 生成资源文件：生成将会包含在项目包中的资源文件。             |
| process-resources       | 编译：复制和处理资源到目标目录，为打包阶段最好准备。         |
| compile                 | 处理类文件：编译项目的源代码。                               |
| process-classes         | 处理类文件：处理编译生成的文件，比如说对Java class文件做字节码改善优化。 |
| generate-test-sources   | 生成测试源代码：生成包含在编译阶段中的任何测试源代码。       |
| process-test-sources    | 处理测试源代码：处理测试源代码，比如说，过滤任意值。         |
| generate-test-resources | 生成测试源文件：为测试创建资源文件。                         |
| process-test-resources  | 处理测试源文件：复制和处理测试资源到目标目录。               |
| test-compile            | 编译测试源码：编译测试源代码到测试目标目录.                  |
| process-test-classes    | 处理测试类文件：处理测试源码编译生成的文件。                 |
| test                    | 测试：使用合适的单元测试框架运行测试（Juint是其中之一）。    |
| prepare-package         | 准备打包：在实际打包之前，执行任何的必要的操作为打包做准备。 |
| package                 | 打包：将编译后的代码打包成可分发格式的文件，比如JAR、WAR或者EAR文件。 |
| pre-integration-test    | 集成测试前：在执行集成测试前进行必要的动作。比如说，搭建需要的环境。 |
| integration-test        | 集成测试：处理和部署项目到可以运行集成测试环境中。           |
| post-integration-test   | 集成测试后：在执行集成测试完成后进行必要的动作。比如说，清理集成测试环境。 |
| verify                  | 验证：运行任意的检查来验证项目包有效且达到质量标准。         |
| install                 | 安装：安装项目包到本地仓库，这样项目包可以用作其他本地项目的依赖。 |
| deploy                  | 部署：将最终的项目包复制到远程仓库中与其他开发者和项目共享。 |

### site生命周期

site生命周期的目的是建立和发布项目站点，Maven能够基于pom.xml所包含的信息，自动生成一个友好的站点，方便团队交流和发布项目信息。主要包含以下4个阶段：

| 阶段        | 描述                                                       |
| :---------- | :--------------------------------------------------------- |
| pre-site    | 执行一些需要在生成站点文档之前完成的工作                   |
| site        | 生成项目的站点文档                                         |
| post-site   | 执行一些需要在生成站点文档之后完成的工作，并且为部署做准备 |
| site-deploy | 将生成的站点文档部署到特定的服务器上                       |

### mvn命令和生命周期

从命令行执行maven任务的最主要方式就是调用maven生命周期的阶段，需要注意的是，每套生命周期是相互独立的，但是每套生命周期中阶段是有前后依赖关系的，执行某个的时候，会按序先执行其前面所有的。

mvn执行阶段的命令格式是：

```
mvn 阶段1 [阶段2] [阶段n]
```

> 多个阶段的名称之间用空格隔开。

### 举个复杂的例子：

```shell
mvn clean install
```

这个命令中执行了两个阶段：`clean`和`install`，从上面3个生命周期的阶段列表中找一下，可以看出`clean`位于`clean`生命周期的表格中，`install`位于`default`生命周期的表格中，所以这个命令会先从`clean`生命周期中的`pre-clean`阶段开始执行一直到`clean`生命周期的`clean`阶段；然后会继续从`default`生命周期的`validate`阶段开始执行一直到default生命周期的`install`阶段。

这里面包含了清理上次构建的结果，编译代码，测试，打包，将打好的包安装到本地仓库。

## maven插件

每个阶段具体做的事情是由maven插件来完成的。`mvn 阶段`明明执行的是阶段，但是实际输出中确实插件在干活，那么阶段是如何和插件关联起来的呢？

maven中的插件就相当于一些工具，比如编译代码的工具，运行测试用例的工具，打包代码的工具，将代码上传到本地仓库的工具，将代码部署到远程仓库的工具等等，这些都是maven中的插件。

插件可以通过`mvn`命令的方式调用直接运行，或者将插件和maven生命周期的阶段进行绑定，然后通过`mvn 阶段`的方式执行阶段的时候，会自动执行和这些阶段绑定的插件。

### 插件目标

maven中的插件以jar的方式存在于仓库中，和其他构件是一样的，也是通过坐标进行访问，每个插件中可能为了代码可以重用，一个插件可能包含了多个功能，比如编译代码的插件，可以编译源代码、也可以编译测试代码；**插件中的每个功能就叫做插件的目标（Plugin Goal），每个插件中可能包含一个或者多个插件目标（Plugin Goal）**。

#### 目标参数

插件目标是用来执行任务的，那么执行任务肯定是有参数配的，这些就是目标的参数，每个插件目标对应于java中的一个类，参数就对应于这个类中的属性。

#### 列出插件所有目标

```
mvn 插件goupId:插件artifactId[:插件version]:help
mvn 插件前缀:help
```

插件版本可省略

#### 列出目标参数

```
mvn 插件goupId:插件artifactId[:插件version]:help -Dgoal=目标名称 -Ddetail
mvn 插件前缀:help -Dgoal=目标名称 -Ddetail
```

> 上面命令中的`-Ddetail`用户输出目标详细的参数列表信息，如果没有这个，目标的参数列表不会输出出来

例如

```shell
mvn org.apache.maven.plugins:maven-surefire-plugin:help -Dgoal=test -Ddetail
```

```xml
    skip (Default: false)
      Set this to 'true' to bypass unit tests entirely. Its use is NOT
      RECOMMENDED, especially if you enable it using the 'maven.test.skip'
      property, because maven.test.skip disables both running the tests and
      compiling the tests. Consider using the skipTests parameter instead.
			注意：这个参数传递时使用-Dmaven.test.skip

    skipExec (Default: null)
      Deprecated. Use skipTests instead.

      This old parameter is just like skipTests, but bound to the old property
      'maven.test.skip.exec'.

    skipTests (Default: false)
      Set this to 'true' to skip running tests, but still compile them. Its use
      is NOT RECOMMENDED, but quite convenient on occasion.


```



#### 插件传参

有两种传参方式：

1. 使用-D目标参数=x x x
2. 在pom.xml的properties中指定

```xml
<maven.test.skip>true</maven.test.skip>
```

#### 插件前缀

运行插件的时候，可以通过指定插件坐标的方式运行，但是插件的坐标信息过于复杂，也不方便写和记忆，所以maven中给插件定义了一些简捷的插件前缀，可以通过插件前缀来运行指定的插件。

可以通过下面命令查看到插件的前缀：

```shell
mvn help:describe -Dplugin=插件goupId:插件artifactId[:插件version]
例如：
mvn help:describe -Dplugin=org.apache.maven.plugins:maven-surefire-plugin
```

有了插件前缀，我们就不需要写插件的坐标了

```shell
mvn surefire:test
```

上面的help:正是`maven-help-plugin`插件的功能，`help`是插件的前缀

## 插件和生命周期阶段绑定

maven只是定义了生命周期中的阶段，而没有定义每个阶段中具体的实现，这些实现是由插件的目标来完成的，所以需要将阶段和插件目标进行绑定，来让插件目标帮助生命周期的阶段做具体的工作，生命周期中的每个阶段支持绑定多个插件的多个目标。

**当我们将生命周期中的阶段和插件的目标进行绑定的时候，执行`mvn 阶段`就可以执行和这些阶段绑定的`插件目标`。**

### 内置绑定

#### clean生命周期的默认绑定

| 生命周期阶段 | 插件:目标                |
| :----------- | :----------------------- |
| pre-clean    |                          |
| clean        | maven-clean-plugin:clean |
| post-clean   |                          |

#### default生命周期的默认绑定

| 生命周期阶段           | 插件:目标                            | 执行任务                       |
| :--------------------- | :----------------------------------- | :----------------------------- |
| process-resources      | maven-resources-plugin:resources     | 复制主资源文件至主输出目录     |
| compile                | maven-compiler-plugin:compile        | 编译主代码至主输出目录         |
| process-test-resources | maven-resources-plugin:testResources | 复制测试资源文件至测试输出目录 |
| test-compile           | maven-compiler-plugin:testCompile    | 编译测试代码至测试输出目录     |
| test                   | maven-surefile-plugin:test           | 执行测试用例                   |
| package                | maven-jar-plugin:jar                 | 创建项目jar包                  |
| install                | maven-install-plugin:install         | 将输出构件安装到本地仓库       |
| deploy                 | maven-deploy-plugin:deploy           | 将输出的构件部署到远程仓库     |

#### site生命周期阶段的默认绑定

| 生命周期阶段 | 插件:目标                |
| :----------- | :----------------------- |
| pre-site     |                          |
| site         | maven-site-plugin:site   |
| post-site    |                          |
| site-deploy  | maven-site-plugin:deploy |

### 自定义绑定

除了默认绑定的一些操作，我们自己也可以将一些阶段绑定到指定的插件目标上来完成一些操作，这种自定义绑定让maven项目在构件的过程中可以执行更多更丰富的操作。

常见的一个案例是：创建项目的源码jar包，将其安装到仓库中，内置插件绑定关系中没有涉及到这一步的任务，所以需要用户自己配置。

插件`maven-source-plugin`的`jar-no-fork`可以帮助我们完成该任务，我们将这个目标绑定在`default`生命周期的`verify`阶段上面，这个阶段没有任何默认绑定，`verify`是在测试完成之后并将构件安装到本地仓库之前执行的阶段，在这个阶段我们生成源码，配置如下：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-source-plugin</artifactId>
            <version>3.2.0</version>
            <executions>
                <!-- 使用插件需要执行的任务 -->
                <execution>
                    <!-- 任务id -->
                    <id>attach-source</id>
                    <!-- 任务中插件的目标，可以指定多个 -->
                    <goals>
                        <goal>jar-no-fork</goal>
                    </goals>
                    <!-- 绑定的阶段 -->
                    <phase>verify</phase>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

注意上面配置的`attach-source`，后面输出中会有。

id：任务的id，需唯一，如果不指定，默认为`default`。

每个插件的配置在pom.xml的`plugins`元素中只能写一次，否则会有警告。

有些插件的目标默认会绑定到一些生命周期的阶段中，那么如果刚好插件默认绑定的阶段和上面配置的一致，那么上面`phase`元素可以不写了，那么怎么查看插件的默认绑定呢？

```shell
mvn help:describe -Dplugin=插件goupId:插件artifactId[:插件version] -Dgoal=目标名称 -Ddetail
mvn help:describe -Dplugin=插件前缀 -Dgoal=目标名称 -Ddetail
```

例如：

```shell
mvn help:describe -Dplugin=source -Dgoal=jar-no-fork -Ddetail
[INFO] Scanning for projects...
[INFO]
[INFO] -------------------< com.javacode2018:maven-chat06 >--------------------
[INFO] Building maven-chat06 1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] --- maven-help-plugin:3.2.0:describe (default-cli) @ maven-chat06 ---
[INFO] Mojo: 'source:jar-no-fork'
source:jar-no-fork
  Description: This goal bundles all the sources into a jar archive. This
    goal functions the same as the jar goal but does not fork the build and is
    suitable for attaching to the build lifecycle.
  Implementation: org.apache.maven.plugins.source.SourceJarNoForkMojo
  Language: java
  Bound to phase: package 绑定到package阶段
```



# Pom.xml的配置

## 插件目标共享参数配置

`build->plugins->plugin`中配置：

```
<!-- 插件参数配置，对插件中所有的目标起效 -->
<configuration>
    <目标参数名>参数值</目标参数名>
</configuration>
```

> `configuration`节点下配置目标参数的值，节点名称为目标的参数名称，上面这种配置对当前插件的所有目标起效，也就是说这个插件中所有的目标共享此参数配置。

例如：跳过测试

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>2.12.4</version>
            <!-- 插件参数配置，对插件中所有的目标起效 -->
            <configuration>
                <skip>true</skip>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## 插件目标参数配置

`project->build->plugins->plugin->executions->execution`元素中进行配置，如下：

```
<!-- 这个地方配置只对当前任务有效 -->
<configuration>
    <目标参数名>参数值</目标参数名>
</configuration>
```

> 上面这种配置常用于自定义插件绑定，只对当前任务有效。

例如：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>2.12.4</version>
            <executions>
                <execution>
                    <goals>
                        <goal>test</goal>
                        <goal>help</goal>
                    </goals>
                    <phase>pre-clean</phase>
                    <!-- 这个地方配置只对当前任务有效 -->
                    <configuration>
                        <skip>true</skip>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>

```

上面自定义了一个绑定，在clean周期的`pre-clean`阶段绑定了插件`maven-surefire-plugin`的两个目标`test和help`，`execution`元素没有指定`id`，所以默认id是`default`。

运行 `mvn pre-clean`测试效果



# 未完待续

https://mp.weixin.qq.com/s?__biz=MzA5MTkxMDQ4MQ==&mid=2648933601&idx=1&sn=b3263c2c02029521609abc248c5d3233&chksm=88621cdfbf1595c91ee4d5067ddbb2cbde5ef6ed78ef0c68d0e058c685626300ec64bf4366ae&token=1776250768&lang=zh_CN&scene=21#wechat_redirect