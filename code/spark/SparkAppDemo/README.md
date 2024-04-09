## Code lab for Spark App Demo
### 打包
- mvn package
- 在target下面会有spark-scala-sparkappdemo-1.0-SNAPSHOT-jar-with-dependencies.jar
### 提交
你需要把**~/infra-client/bin/spark-submit**改为你自己的infra-client的路径
- local模式
```
~/infra-client/bin/spark-submit \
--master local \
--driver-memory 2g \
--executor-memory 2g  \
--class SparkAppDemo \
spark-scala-sparkappdemo-1.0-SNAPSHOT-jar-with-dependencies.jar
```
- cluster模式
```
~/infra-client/bin/spark-submit \
--cluster zjyprc-hadoop-spark2.3 \
--conf spark.yarn.job.owners=zhoukang \
--master yarn \
--deploy-mode cluster \
--driver-memory 2g \
--executor-memory 2g \
--queue root.production.cloud_group.hadoop.sparksql \
--class SparkAppDemo \
spark-scala-sparkappdemo-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### 注意
如果想在IDE里直接运行例子的话，需要去掉**pom.xml**中Spark依赖的provided限定域