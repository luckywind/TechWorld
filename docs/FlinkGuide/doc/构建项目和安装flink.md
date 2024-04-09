# 项目构建

## 创建项目

```shell
mvn archetype:generate                               \
      -DarchetypeGroupId=org.apache.flink              \
      -DarchetypeArtifactId=flink-quickstart-java      \
      -DarchetypeVersion=1.9.0
  #它将以交互式的方式询问你项目的 groupId、artifactId 和 package 名称。
```

idea设置jvm堆大小：

/Users/chengxingfu/Library/Preferences/IntelliJIdea2019.2/idea.vmoptions

调大-Xmx

# 安装flink

```shell
 brew install apache-flink
 echo $(dirname $(dirname $(greadlink -f $(which flink)))) #查看flink安装目录
 cd /usr/local/Cellar/apache-flink/1.9.0/libexec/bin    #启动脚本
 ./bin/start-cluster.sh  # Start Flink
 ./bin/stop-cluster.sh  #stop 
```

[访问](http://localhost:8081)

```shell
echo $(dirname $(dirname $(greadlink -f $(which sbt))))
```

# demo

```shell
./bin/flink run examples/streaming/WordCount.jar 
tail log/flink-*-taskexecutor-*.out
  (to,1)
  (be,1)
  (or,1)
  (not,1)
  (to,2)
  (be,2)
```



导入/Users/chengxingfu/code/flink/flink/flink-examples/flink-examples-streaming到ide，打包



/usr/local/Cellar/apache-flink/1.9.0/libexec/bin/flink run target/flink-examples-streaming_2.11-1.11-SNAPSHOT-SocketWindowWordCount.jar --port 9000

