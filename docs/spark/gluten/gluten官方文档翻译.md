# 介绍

## 简介

- 问题： Spark社区需要解决不断出现的性能优化需求，Spark2使用CodeGen代替了火山模型获得了2倍的性能提升，但自此之后性能提升效果非常缓慢。 与此同时像clickhouse/Arrow/Velos之类的SQL引擎在不断的研发，通过使用native 实现、列式数据向量化处理，这些库的性能超出了Spark基于JVM的SQL引擎，然而这些库只运行在单节点。
- gluten的方案：把SparkSQL与native库"连起来"。兼具Spark的可扩展性和native库的性能优势。

Gluten设计的基本原则是尽可能重用Spark的控制流和JVM代码，但是把计算敏感的数据处理部分卸载到native 代码。例如：

- 把Spark的CodeGen物理计划转化为Substrait计划，然后发送给native
- 卸载性能敏感的数据处理到native库
- 定义清晰的JNI接口给native库使用
- 方便切换native引擎
- 重用Spark分布式控制流
- 管理JVM和native的数据共享
- 扩展支持native加速器

## 架构

使用Spark3.0的列式API，native库需要返回Columnar Batch给Spark。我们对每个native backend包装Columnar Batch。 Gluten的c++代码使用Arrow的数据格式作为基本的数据格式，因此返回给Spark JVM的数据是ArrowColumnarBatch。

![image-20231101110008914](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231101110008914.png)

关键的组件

- Plan转化： Query plan conversion which convert Spark’s physical plan into substrait plan in each stage.
- Spark的统一内存管理也控制native内存分配： Unified memory management in Spark is used to control the native memory allocation as well
- 列shuffle直接shuffle列数据，重用spark core的shuffle 服务:  Columnar shuffle is used to shuffle columnar data directly. The shuffle service still reuses the one in Spark core. The exchange operator is reimplemented to support columnar data format
- 支持falback, native实现C2R/R2C:  For unsupported operators or functions Gluten fallback the operator to Vanilla Spark. There are C2R and R2C converter to convert the columnar data and Spark’s internal row data. Both C2R and R2C are implemented natively as well
- 收集并展示native metrics:   Metrics are very important to get insight of Spark’s execution, identify the issues or bottlenecks. Gluten collects the metrics from native library and shows in Spark UI.
- 支持多版本Spark :  Shim layer is used to support multiple releases of Spark. Gluten only plans to support the latest 2-3 spark stable releases, with no plans to add support on older spark releases. Current support is on Spark 3.2 and Spark 3.3.



# 使用

只支持centos7/8 and ubuntu20.04/22.04

```shell
spark-shell \
 --master yarn --deploy-mode client \
 --conf spark.plugins=io.glutenproject.GlutenPlugin \
 --conf spark.memory.offHeap.enabled=true \
 --conf spark.memory.offHeap.size=20g \
 --conf spark.shuffle.manager=org.apache.spark.shuffle.sort.ColumnarShuffleManager \
 --jars https://github.com/oap-project/gluten/releases/download/v1.0.0/gluten-velox-bundle-spark3.2_2.12-ubuntu_20.04-1.0.0.jar
```



## 编译

### Prerequisite

velox  使用scripts/setup-xxx.sh安装依赖，但是Arrow的依赖没有安装，且Velox编译需要ninja。

```shell
## run as root
## install gcc and libraries to build arrow
apt-get update && apt-get install -y sudo locales wget tar tzdata git ccache cmake ninja-build build-essential llvm-11-dev clang-11 libiberty-dev libdwarf-dev libre2-dev libz-dev libssl-dev libboost-all-dev libcurl4-openssl-dev openjdk-8-jdk maven

export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

```



### with velox backend

[参考](https://github.com/oap-project/gluten/blob/main/docs/get-started/Velox.md)

推荐使用buildbundle-veloxbe.sh构建gluten， [Gluten Usage](https://github.com/oap-project/gluten/blob/main/docs/get-started/GlutenUsage.md)列出了参数和默认值

```shell
cd /path/to/gluten

## The script builds two jars for spark 3.2.2 and 3.3.1.
./dev/buildbundle-veloxbe.sh

#结束后，需要重新编译gluten 
#skip building arrow, velox and protobuf.
./dev/buildbundle-veloxbe.sh --skip_build_ep=ON --build_protobuf=OFF
```

#### 分别编译velox和Arrow

/path/to/gluten/ep/build-xxx/src下面的get_xxx.sh和build_xxx.sh用于分别编译velox和Arrow，可以使用自定义源。

在执行下面脚本前，最好使用export NUM_THREADS=4防止OOM

```shell
## fetch Arrow and compile
cd /path/to/gluten/ep/build-arrow/src/
## you could use custom ep location by --arrow_home=custom_path, make sure specify --arrow_home in build_arrow.sh too.
./get_arrow.sh
./build_arrow.sh

## 拉取并编译velox
cd /path/to/gluten/ep/build-velox/src/
## you could use custom ep location by --velox_home=custom_path, make sure specify --velox_home in build_velox.sh too.
./get_velox.sh
## make sure specify --arrow_home or --velox_home if you have specified it in get_xxx.sh.
./build_velox.sh

## compile Gluten cpp module
cd /path/to/gluten/cpp
## if you use custom velox_home or arrow_home, make sure specified here by --arrow_home or --velox_home 
./compile.sh --build_velox_backend=ON

## compile Gluten java module and create package jar
cd /path/to/gluten
# For spark3.2.x
mvn clean package -Pbackends-velox -Prss -Pspark-3.2 -DskipTests
# For spark3.3.x
mvn clean package -Pbackends-velox -Prss -Pspark-3.3 -DskipTests
```

### 依赖库部署

配置enable_vcpkg=ON， 依赖库将被静态链接到libvelox.so和libgluten.so，这些so被打进了gluten-jar，因此只需要把gluten-jar加到spark.<driver|executor>.extraClassPath, spark会把依赖部署到每个节点。

配置enable_vcpkg=OFF，脚本将把依赖安装到系统并打进另一个包gluten-package--SNAPSHOT.jar。这样我们需要把这个jar加到extraClassPath，并且设置spark.gluten.loadLibFromJar=true。 或者我们手动部署依赖也是可行的。 



```shell
export gluten_jvm_jar = /PATH/TO/GLUTEN/backends-velox/target/<gluten-jar>
spark-shell 
  --master yarn --deploy-mode client \
  --conf spark.plugins=io.glutenproject.GlutenPlugin \
  --conf spark.memory.offHeap.enabled=true \
  --conf spark.memory.offHeap.size=20g \
  --conf spark.driver.extraClassPath=${gluten_jvm_jar} \
  --conf spark.executor.extraClassPath=${gluten_jvm_jar} \
  --conf spark.shuffle.manager=org.apache.spark.shuffle.sort.ColumnarShuffleManager
  ...
```

## Gluten UI

gluten提供了两个事件

- GlutenBuildInfoEvent： 采集构建信息
- GlutenPlanFallbackEvent：回退信息

我们可以注册SparkListener处理这两个事件

把gluten-ui jar 放到$SPARK_HOME/jars后重启HistoryServer，也可以从HistoryServer看到gluten UI.

# 开发

gluten作为一个桥梁角色，是Spark和Native执行库的中间层，负责验证Spark Plan的算子是否可以由native引擎做。如果可以，则把SparkPlan转为Substrait plan, 然后发送给native引擎。

Java/Scala代码用于验证、转换执行计划

C++ 代码把Subtrait计划和数据源作为输入，把Subtrait计划转化为相应的backend计划。 JNI是从Java调用C++的编程技术，所有的JNI接口都定义在jni/JniWrapper.cc文件里。

## [debug](https://oap-project.github.io/gluten/developers/HowTo.html)

### debugC++

使用GDB通过benchmark来debug C++， 先生成示例文件，示例文件包含：

- 包含json格式的Substrait plan的文件
- Parquet格式的输入文件

请使用一下步骤

1. build Velox and Gluten CPP

   ```shell
   gluten_home/dev/builddeps-veloxbe.sh --build_tests=ON --build_benchmarks=ON --build_type=Debug
   ```

   生成可执行文件gluten_home/cpp/build/velox/benchmarks/generic_benchmark

2. build Gluten and generate the example files

   ```shell
   cd gluten_home
   mvn clean package -Pspark-3.2 -Pbackends-velox -Prss
   mvn test -Pspark-3.2 -Pbackends-velox -Prss -pl backends-velox -am -DtagsToInclude="io.glutenproject.tags.GenerateExample" -Dtest=none -DfailIfNoTests=false -Darrow.version=11.0.0-gluten -Dexec.skip
   ```

   在gluten_home/backends-velox下生成示例文件

3. now, run benchmarks with GDB

   ```shell
   cd gluten_home/cpp/build/velox/benchmarks/
   gdb generic_benchmark
   ```

   - 当GDB成功加载generic_benchmark，可以使用命令b man在main函数里设置断点，命令r运行
   - 使用命令`p 变量名`查看变量，也可以用命令`n`来逐行执行，命令`s`来进入函数
   - 实际上可以使用gdb的任何命令调试generic_benchmark

4. `gdb-tui` is a valuable feature and is worth trying. You can get more help from the online docs. [gdb-tui](https://sourceware.org/gdb/onlinedocs/gdb/TUI.html)

5. 可以根据指定的json格式的plan和输入文件开始generic_benchmark

   - 默认使用gluten_home/backends-velox/generated-native-benchmark目录下的example.json, example_lineitem + example_orders
   - 可以编辑example.json自定义substrait plan或者指定其他目录下的文件

6. get more detail information about benchmarks from [MicroBenchmarks](https://oap-project.github.io/gluten/developers/MicroBenchmarks.html)

## java/scala开发

idea中远程debug

### 设置gluten项目

- Make sure you have compiled gluten.
- Load the gluten by File->Open, select <gluten_home/pom.xml>.
- Activate your profiles such as , and Reload Maven Project, you will find all your need modules have been activated.
- Create breakpoint and debug as you wish, maybe you can try `CTRL+N` to find `TestOperator` to start your test.

Gw-123123