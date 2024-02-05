Spark插件框架详解

# 简介

Apache Spark是大数据处理领域最常用的计算引擎之一，支持批/流计算、SQL分析、机器学习、图计算等计算范式，以其强大的容错能力、可扩展性、函数式API、多语言支持（SQL、Python、Java、Scala、R）等特性在大数据计算领域被广泛使用。尤其是Spark3.0带来的插件框架大大提升了Spark的可扩展性。

# 插件框架设计

## 基本架构

基本架构如下图所示，在Executor和Driver初始化的时候，会加载用户指定的插件, 创建DriverPluginContainer和ExecutorPluginContainer。

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/clip_image001.png" alt="image-20231127111119290" style="zoom:50%;" />

 

## API介绍 

1. PluginContainer作为一个接口类，提供了shutdown(),registerMetrics(), onTaskStart(), onTaskSucceed(), onTaskFailed等方法，分别在一个task初始化、成功、失败等情况下插件相应的实现接口的方法被调用。其中，registerMetrics()作用是将相应的插件的相关信息注册到Spark的信息统计中，相应的数据等会在Spark UI中被使用。

2. SparkPlugin接口定义了获取自定义DriverPlugin和ExecutorPlugin的接口，相当于把DriverPlugin和ExecutorPlugin组合在一起了，在实现Spark的插件时，只需要继承并实现SparkPlugin即可。

3. DriverPlugin定义了init(),receive(),shutdown()和registerMetrics()接口。其中DriverPlugin的init()方法和shutdown()方法分别是在Spark driver初始化阶段和结束阶段被调用的，可以实现Driver的自定义。receive方法用于接受来自Executors的消息；registerMetrics方法用于跟踪Driver端的自定义指标。

4. ExecutorPlugin定义了init()和shutdown()两个方法，可以定义Executor的初始化以及关闭过程

 

[When and How to extend Apache Spark?](https://medium.com/@kkyon/when-and-how-to-extend-apache-spark-5196f350eb3f)

# 插件框架的一些应用场景

事实上，通过 Spark Plugin框架，用户可以编写自定义插件实现自定义Driver和Executor。例如

•     扩展Spark SQL的Catalyst优化器，应用自定义的优化规则

•     编写自定义数据源插件，以支持特定的数据格式

•     支持自定义的指标 用户可以写代码进行自定义指标的编写，而这些指标和Spark的指标结合, 这样就能通过Sinks进行收集

•     通过Executor 插件和 Driver插件可以实现Executors和Driver的交互

## 自定义Metrics

监控Spark作业的metrics是一件非常有意义的事情，目前Spark收集了非常多的metrics并在日志或者web UI上进行展示，但是在一些场景上这些是不够的：

1. 以定制化的方式实时获取Spark metrics
2. 创建新的metrics，Spark提供的metrics大多是关于JVM以及Spark Core的，缺乏数据相关的统计信息

[简单例子](https://blog.madhukaraphatak.com/spark-plugin-part-4)

[自定义各种类型的metrics](https://metrics.dropwizard.io/3.1.0/getting-started/)

## 自定义Shuffle Manager

Spark的shuffle默认使用本地磁盘，这有时是不合适的， 尤其是无法给Executor分配比较大的磁盘存储的时候。

通过插件框架，我们可以切换到自己的Shuffle服务以及存储后端。

## 自定义SQL引擎

Spark SQL 在数据开发领域的应用非常广泛，但是对它的优化不是一件容易的事，原因是Catalyst优化器的复杂性，以及扩展的难度。好在Spark2.2版本后，Catalyst新增了许多扩展点，通过插件框架和Catalyst扩展点，我们可以实现以下这些场景：

1. 在语句执行前检查表或者列的权限
2. 语法规范检查，拒绝不合理的SQL查询，例如“select *”
3. 自定义查询优化
4. 自定义的SQL函数、表达式等

## Driver和Executor的RPC通信

Spark 插件框架允许Driver和Executor使用 RPC 消息进行通信。该功能可用于发送状态或向Driver查询某些配置的详细信息。



# 总结

本文我们介绍了Spark 插件框架的设计和一些应用场景，这些功能将会让Spark的应用方向以及优化方向提供了更多的可能性。