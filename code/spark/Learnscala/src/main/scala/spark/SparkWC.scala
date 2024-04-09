package spark

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object SparkWC {
  def main(args: Array[String]): Unit = {
    /**
     * 本地调试
     */
//    val conf: SparkConf = new SparkConf().setAppName("SparkWC").setMaster("local[*]")
    /**
     * 集群运行
     */
    val conf: SparkConf = new SparkConf().setAppName("SparkWC")

    val sc = new SparkContext(conf)

    val lines: RDD[String] = sc.textFile(args(0))
    val words: RDD[String] = lines.flatMap(_.split(" "))
    val paired: RDD[(String, Int)] = words.map((_,1))
    val reduced: RDD[(String, Int)] = paired.reduceByKey(_+_)
    val res: RDD[(String, Int)] = reduced.sortBy(_._2,false)
    //保存

    res.saveAsTextFile(args(1))
//    println(res.collect().toBuffer)
    //结束任务
    sc.stop()
  }
}
