[官方文档](https://spark.apache.org/docs/latest/running-on-yarn.html)

# YARN上启动Spark

确保HADOOP_CONF_DIR或YARN_CONF_DIR指向包含HADOOP集群（客户端）配置文件的目录。这些配置用于写入HDFS并连接到YARN资源管理器。该目录中包含的配置将被分发到YARN集群，以便应用程序使用的所有容器都使用相同的配置。如果配置引用了不由YARN管理的Java系统属性或环境变量，那么它们也应该在Spark应用程序的配置中设置（在客户端模式下运行时，driver、executor和AM）。



有两种部署模式可用于在YARN上启动Spark应用程序。在集群模式下，Spark driver在集群上由YARN管理的ApplicationMaster中运行，客户端可以在启动应用程序后离开。在客户端模式下，driver在客户端进程中运行，ApplicationMaster仅用于向YARN请求资源。



与Spark支持的其他集群管理器不同，在Spark中，master的地址是在--master参数中指定的，在YARN模式下，ResourceManager的地址是从Hadoop配置中获取的。因此，--master参数是yarn。

启动命令：

```
$ ./bin/spark-submit --class path.to.your.Class --master yarn --deploy-mode cluster [options] <app jar> [app options]
```

For example:

```shell
$ ./bin/spark-submit --class org.apache.spark.examples.SparkPi \
    --master yarn \
    --deploy-mode cluster \
    --driver-memory 4g \
    --executor-memory 2g \
    --executor-cores 1 \
    --queue thequeue \
    examples/jars/spark-examples*.jar \
    10
```

SparkPi将运行在cluster模式。

当然spark-shell也可以运行在client模式：

```
$ ./bin/spark-shell --master yarn --deploy-mode client
```