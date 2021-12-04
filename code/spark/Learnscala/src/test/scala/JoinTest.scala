/**

 */

import joins.RDDJoinExamples
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.scalatest.FunSuite


object JoinTest {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("SparkSessionZipsExample")
      .enableHiveSupport()
      .master("local[6]")
      .getOrCreate()
    val sc: SparkContext = spark.sparkContext
    val keySet = "a, b, c, d, e, f, g".split(",")
    val smallRDD = sc.parallelize(keySet.map(letter => (letter, letter.hashCode)))
    val largeRDD: RDD[(String, Double)] =
      sc.parallelize(keySet.flatMap{ letter =>
        Range(1, 50).map(i => (letter, letter.hashCode() / i.toDouble))})
    val result: RDD[(String, (Double, Int))] =
      RDDJoinExamples.manualBroadCastHashJoin(
        largeRDD, smallRDD)
    val nativeJoin: RDD[(String, (Double, Int))] = largeRDD.join(smallRDD)
  println(s"result.count=${result.count()}")
  println(s"nativeJoin.count=${nativeJoin.count()}")
    Thread.sleep(1000*1000)
    assert(result.subtract(nativeJoin).count == 0)
  }

}
