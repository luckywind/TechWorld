

import org.apache.spark.internal.Logging
import org.apache.spark.{SparkConf, SparkContext}
import org.slf4j.LoggerFactory

/**
  * Created by mi on 18-2-28.
  */
object SparkAppDemo extends Logging{

    def main(args: Array[String]): Unit = {
        val sparkAppName = "SparkAppDemo"
        val conf = new SparkConf().setAppName(sparkAppName)
        // for running directly in IDE
        conf.setIfMissing("spark.master", "local[2]")
        val sc = new SparkContext(conf)
        val rdd1 = sc.parallelize(Array(1, 2, 3, 4, 5))

        // Note: If you are grouping in order to perform an aggregation (such as a sum or average) over
        // each key, using reduceByKey or aggregateByKey will yield much better performance.

        // val res = rdd1.map(x=>(x%2==0,x)).groupByKey().mapValues(v=>v.size).collect()
        val res = rdd1.map(x => (x % 2 == 0, 1)).reduceByKey(_ + _).collect()
        logInfo(res.mkString(","))
        sc.stop()
    }
}
