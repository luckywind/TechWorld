package spark.sql

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, SQLContext}
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.{SparkConf, SparkContext}

object SpecifyingSchema {
  def main(args: Array[String]): Unit = {
    val conf: SparkConf = new SparkConf().setAppName("SQL").setMaster("local[*]")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)

    val lineRDD: RDD[Array[String]] = sc.textFile(args(0)).map(_.split(","))

    //通过StructType指定每个字段的schema
    val schema= StructType(
      List(
        StructField("id",IntegerType,true),
        StructField("name",StringType,true),
        StructField("age",IntegerType,true),
        StructField("faceValue",IntegerType,true)
      )
    )
    //将RDD映射到rowRDD
    val rowRDD: RDD[Row] = lineRDD.map(x => Row(x(0).toInt, x(1), x(2).toInt, x(3).toInt))
    val personDF: DataFrame = sqlContext.createDataFrame(rowRDD,schema)
    personDF.registerTempTable("t_person")
    val df: DataFrame = sqlContext.sql("select * from t_person")
    df.write.json(args(1))
    sc.stop()
  }
}
