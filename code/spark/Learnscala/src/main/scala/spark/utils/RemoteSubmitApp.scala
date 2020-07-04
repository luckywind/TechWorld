//package spark.utils
//
//import org.apache.kafka.common.serialization.StringDeserializer
//import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
//import org.apache.spark.streaming.{Seconds, StreamingContext}
//import org.apache.spark.{SparkConf}
//import spark.wordcount.kafkaStreams
//
//object RemoteSubmitApp {
//  def main(args: Array[String]) {
//    // 设置提交任务的用户
//    System.setProperty("HADOOP_USER_NAME", "root")
//    val conf = new SparkConf()
//      .setAppName("WordCount")
//      // 设置yarn-client模式提交
//      .setMaster("yarn")
//      // 设置resourcemanager的ip
//      .set("yarn.resourcemanager.hostname","master")
//      // 设置executor的个数
//      .set("spark.executor.instance","2")
//      // 设置executor的内存大小
//      .set("spark.executor.memory", "1024M")
//      // 设置提交任务的yarn队列
//      .set("spark.yarn.queue","spark")
//      // 设置driver的ip地址
//      .set("spark.driver.host","192.168.17.1")
//      // 设置jar包的路径,如果有其他的依赖包,可以在这里添加,逗号隔开
//      .setJars(List("D:\\develop_soft\\idea_workspace_2018\\sparkdemo\\target\\sparkdemo-1.0-SNAPSHOT.jar"
//      ))
//    conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
//.set("spark.driver.host", "127.0.0.1").set("spark.driver.port", "7077")
//    val scc = new StreamingContext(conf, Seconds(1))
//    scc.sparkContext.setLogLevel("WARN")
//    //scc.checkpoint("/spark/checkpoint")
//    val topic = "jason_flink"
//    val topicSet = Set(topic)
//    val kafkaParams = Map[String, Object](
//      "auto.offset.reset" -> "latest",
//      "value.deserializer" -> classOf[StringDeserializer]
//      , "key.deserializer" -> classOf[StringDeserializer]
//      , "bootstrap.servers" -> "master:9092,storm1:9092,storm2:9092"
//      , "group.id" -> "jason_"
//      , "enable.auto.commit" -> (true: java.lang.Boolean)
//    )
//    kafkaStreams = KafkaUtils.createDirectStream[String, String](
//      scc,
//      LocationStrategies.PreferConsistent,
//      ConsumerStrategies.Subscribe[String, String](topicSet, kafkaParams))
//    kafkaStreams.foreachRDD(rdd=> {
//      if (!rdd.isEmpty()) {
//        rdd.foreachPartition(fp=> {
//          fp.foreach(f=> {
//            println(f.value().toString)
//          })
//        })
//      }
//    })
//    scc.start()
//    scc.awaitTermination()
//  }
//}