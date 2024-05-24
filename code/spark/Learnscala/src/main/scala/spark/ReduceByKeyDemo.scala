package spark

import org.apache.spark.rdd.RDD
import org.apache.spark.{Partitioner, SparkConf, SparkContext}

/**
 */
object ReduceByKeyDemo {
  def main(args: Array[String]): Unit = {
    val conf: SparkConf = new SparkConf().setAppName("ReduceByClass")

    conf.setMaster("local[*]")//设置为本地运行

    //设置程序的入口
    val sc: SparkContext = new SparkContext(conf)

    //创建RDD
    val rdd1: RDD[Int] = sc.makeRDD(List(1,2,3,4,5))

    //调用groupBy方法
    val groupedRdd1: RDD[(String, Iterable[Int])] = rdd1.groupBy(t => t.toString)

    val rdd2: RDD[(String, Int)] = sc.makeRDD(List(("rb", 1000), ("baby", 990),
      ("yangmi", 980), ("bingbing", 5000), ("bingbing", 1000), ("baby", 2000)), 3)

    val groupByKeyRDD2: RDD[(String, Iterable[Int])] = rdd2.groupByKey(2)

    val groupByKeyRDD1: RDD[(String, Iterable[Int])] = rdd2.groupByKey()
    //调用groupByKey + mapValues 就相当于 reduceByKey方法
    val result: RDD[(String, Int)] = groupByKeyRDD1.mapValues(_.sum)

    val reduceByKeyRDD: RDD[(String, Int)] = rdd2.reduceByKey(_+_)
    val rdd6: RDD[(String, Int)] = rdd2.reduceByKey(_ + _)

    // 指定生成的rdd的分区的数量
    val rdd7: RDD[(String, Int)] = rdd2.reduceByKey(_ + _, 10)

    val rdd5: RDD[(String, List[Int])] = sc.makeRDD(List(("a", List(1, 3)), ("b", List(2, 4))))
    rdd5.reduceByKey(_ ++ _)

    sc.stop()

  }
}

