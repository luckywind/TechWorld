package spark.sql

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

/**
  * 统计不同渠道进件数量
  * Created by Michael on 2016/11/29.
  */
object Custmer_Statistics_CaseClass {

  /**
    * 使用模板类描述表元数据信息
    * @param chnl_code
    * @param id_num
    */
  case class blb_intpc_info(chnl_code:String,id_num:String)

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("Custmer_Statistics_CaseClass").setMaster("local[2]")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    //RDD隐式转换成DataFrame
    import sqlContext.implicits._
    //读取本地文件
    val blb_intpc_infoDF = sc.textFile("/Users/chengxingfu/code/open/spark/SparkSQL/data/channel/blb_intpc_info_10000_2.txt")
      .map(_.split("\\t"))
      .map(d => blb_intpc_info(d(0), d(1))).toDF()

    //注册表
    blb_intpc_infoDF.registerTempTable("blb_intpc_info")

    /**
      * 分渠道进件数量统计并按进件数量降序排列
      */
    blb_intpc_infoDF.registerTempTable("blb_intpc_info")
    sqlContext.sql("" +
      "select chnl_code,count(*) as intpc_sum " +
      "from blb_intpc_info " +
      "group by chnl_code").toDF().sort($"intpc_sum".desc).show()
  }

}
