# 集群端

1. 在Rapids config 中加入 --conf spark.driver.extraJavaOptions="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
2. 在服务器上运行spark-shell进行单步调试命令,所有spark端的conf都使用默认配置即可。

```shell
$SPARK_HOME/bin/spark-shell \
 --master local \
 --num-executors 1 \
 --conf spark.executor.cores=1 \
 --conf spark.rapids.sql.concurrentGpuTasks=1 \
 --driver-memory 10g \
 --conf spark.rapids.memory.pinnedPool.size=2G \
 --conf spark.locality.wait=0s \
 --conf spark.sql.files.maxPartitionBytes=512m \
 --conf spark.rapids.sql.csv.read.integer.enabled=true \
--conf spark.rapids.sql.csv.read.long.enabled=true \
 --conf spark.plugins=com.nvidia.spark.SQLPlugin \
 --conf spark.driver.extraJavaOptions="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5007" \
 --jars ${SPARK_CUDF_JAR},${SPARK_RAPIDS_PLUGIN_JAR},${SPARK_RAPIDS_PLUGIN_INTEGRATION_TEST_JAR}
 
$SPARK_HOME/bin/spark-shell \
    --master local \
    --num-executors 1 \
    --conf spark.executor.cores=1 \
    --driver-memory 10g \
    --conf spark.locality.wait=0s \
    --conf spark.sql.files.maxPartitionBytes=512m \
    --conf spark.plugins=com.yusur.spark.SQLPlugin \
    --conf spark.driver.extraJavaOptions="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5007" \
    --conf spark.race.sql.enabled=true\
    --jars ${SPARK_RACE_PLUGIN_JAR}
```

1. 在shell中可以使用如下命令进行测试

   ```scala
   import org.apache.spark.sql.{DataFrame,SparkSession}
   import org.apache.spark.sql.types.{IntegerType,LongType,StringType,StructType,StructField}
   import org.apache.spark.sql.{Row, SparkSession}
   
   val spark: SparkSession = SparkSession.builder().appName("Test").getOrCreate()
     
   val simpleSchema = StructType(Array(
       StructField("id",LongType,true),
       StructField("cid",LongType,true),
       StructField("did",LongType,true)
     ))
     
   
   
   val dfr = spark.read.option("header",value = true).schema(simpleSchema) 
   
   val df = dfr.csv("file:/var/workspace/table1.csv") 
   
   val tempV = df.createTempView("table1")
   
   val df = spark.sql("select cid, id from table1 where id = 1")
   
   val res = df.collect
   
   
   val simpleSchema = StructType(Array(
       StructField("id",IntegerType,true),
       StructField("cid",IntegerType,true),
       StructField("did",IntegerType,true),
       StructField("value", StringType, true),
       StructField("nation", StringType, true)
     ))
     
   val df = spark.sql("select cid, id from table1 where id = 1")
   
   // print the res in the df
   res.foreach( row => {
     val salary = row.getLong(0)
     println(salary)} )
   
   
   ```

   

# idea Remote Debug配置

1. 下载rapids 代码，选择的版本应该和服务器使用的rapids版本保持一致。IDE里面build代码。
2. 打开teminal 输入如下指令 完成ssh 隧道连接，将远端服务器端口连接到本地端口 ssh -C -f -N -g -L 5005:localhost:5005 root@192.168.2.4
3. 在intelliJ IDE里面 点击Run→ Profile→Edit Configurations 点击+号然后选择Remote JVM Debug 添加参数如图

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/worddav66bcf44192058893553494ab50dd0dc4.png)

![image-20250306141604042](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250306141604042.png)