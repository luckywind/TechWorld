package spark.listener

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}


object test{
  def main(args : Array[String]) : Unit = {
    val sparkConf = new SparkConf()
    val ssc = new StreamingContext(sparkConf,Seconds(20))
    ssc.addStreamingListener(new MyTestStreamingListener(ssc))

  }
}