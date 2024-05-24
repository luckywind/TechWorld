package spark.etl.core

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import  spark.sql

/**

 */
object JoinImpl {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("JoinImpl")
//      .config("spark.sql.warehouse.dir", warehouseLocation)
//      .enableHiveSupport()
      .master("local[6]")
      .getOrCreate()

    val sc: SparkContext = spark.sparkContext
     val rdd1: RDD[Int] = sc.parallelize(1 to 10)
    val kv_left: RDD[(String, Int)] = rdd1.map(n=>("a",n))
     val rdd2: RDD[Int] = sc.parallelize(1 to 10)
    val kv_right: RDD[(String, Int)] = rdd2.map(n=>("a",n*2))
    val common=kv_left
      .leftOuterJoin(kv_right)
      .map{line=>
        val key: String = line._1
        line._2._1
      }
    common.foreach(println(_))
    println("共同"+common.count())

  }
}
