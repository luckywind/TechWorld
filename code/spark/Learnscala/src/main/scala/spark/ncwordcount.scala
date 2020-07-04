package spark

import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.StreamingContext._
import spark.listener.MyTestStreamingListener // not necessary since Spark 1.3

// Create a local StreamingContext with two working thread and batch interval of 1 second.
// The master requires 2 cores to prevent from a starvation scenario.
object ncwordcount {
  def main(args: Array[String]): Unit = {


    // 设置提交任务的用户
    System.setProperty("HADOOP_USER_NAME", "root")
    val conf = new SparkConf().setAppName("NetworkWordCount")
      // 设置yarn-client模式提交
      .setMaster("local[4]")

    val ssc = new StreamingContext(conf, Seconds(30))
    ssc.addStreamingListener(new MyTestStreamingListener(ssc))
    // Create a DStream that will connect to hostname:port, like localhost:9999
    val lines = ssc.socketTextStream("localhost", 9999)
    // Split each line into words
    val words = lines.flatMap(_.split(" "))
    // Count each word in each batch
    val pairs = words.map(word => (word, 1))
    val wordCounts = pairs.reduceByKey(_ + _)

    // Print the first ten elements of each RDD generated in this DStream to the console
    wordCounts.print()
    ssc.start()             // Start the computation
    ssc.awaitTermination()  // Wait for the computation to terminate
  }
}
