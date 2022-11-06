# 参考

https://waltyou.github.io/Spark-Source-Code-Build-And-Run-In-Idea-Intellij/

```shell
git clone https://github.com/apache/spark.git
```

1. 下载 IntelliJ IDEA， *Preferences > Plugins*，搜索 [Scala Plugin](https://www.jetbrains.com/help/idea/discover-intellij-idea-for-scala.html) 并安装.
2. *File -> Import Project*, 到达代码位置，并选择 “Maven Project”。
3.  Import 过程中，选中 “Import Maven projects automatically”，其他选项不变。
4. 接下来要参考另外一个官方文档：[Building Spark](https://spark.apache.org/docs/latest/building-spark.html#apache-maven) 。
5. 在项目根目录打开终端
6. Run `export MAVEN_OPTS="-Xmx2g -XX:ReservedCodeCacheSize=512m"`
7. Run `./build/mvn -DskipTests clean package`

第7步会花费一些时间。等到第七步完成，整个项目就build 成功了。

## 运行 example

举个例子，比如运行 SparkPi。只需修改 Run Configuration 两处地方就好。 

VM options:  -Dspark.master=local

Use classpath of module:spark-example勾选include dependencies with "Provided" scope

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/spark-debug-in-idea-3.png)