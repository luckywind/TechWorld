package spark

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object SparkRDDTest {

  def main(args: Array[String]): Unit = {
    val conf: SparkConf = new SparkConf().setAppName("SparkRDDTest").setMaster("local")
    val sc = new SparkContext(conf)
    val rdd1: RDD[Int] = sc.parallelize(List(5,6,7,8,9,1,10,11))
    //double并排序
//    val res: RDD[Int] = rdd1.map(_*2).sortBy(x=>x ,true)
//    val res: RDD[Int] = rdd1.filter(_>=10)

    val rdd2 = sc.parallelize(Array("a","b","d d f d s"))
//    val res: RDD[String] = rdd2.flatMap(_.split(' '))
    val rdd3: RDD[List[String]] = sc.parallelize(List(List("a b c","d e f"),List("a b","d e")))
//    val res: RDD[String] = rdd3.flatMap(_.flatMap(_.split(" ")))

    val rdd4: RDD[Int] = sc.parallelize(List(3,5,76))
//    val res: RDD[Int] = rdd1 union rdd4
//    val res: RDD[Int] = rdd1.intersection(rdd4)
//    val res: RDD[Int] = rdd1.distinct()
    val rdd6: RDD[(String, Int)] = sc.parallelize(List(("tom",1),("jerry",2)))
    val rdd7: RDD[(String, Int)] = sc.parallelize(List(("tom",0),("jerry",4),("kitty",1)))
//    val res: RDD[(String, (Int, Int))] = rdd6 join  rdd7
//    val res: RDD[(String, (Int, Option[Int]))] = rdd6.leftOuterJoin(rdd7)
    val res: RDD[(String, Iterable[Int])] = rdd6.union(rdd7).groupByKey()
    println(res.collect().toBuffer)
    val wc: RDD[(String, Int)] = (rdd6.union(rdd7)).groupByKey().mapValues(_.sum)
    println(wc.collect().toBuffer)
    //先局部聚合再全局聚合
    println((rdd6.union(rdd7)).reduceByKey(_ + _).collect().toBuffer)
    //注意cogroup和groupbykey的区别
    println(rdd6.cogroup(rdd7).collect().toBuffer)
    println(rdd1.reduce(_+_))
  }

}
