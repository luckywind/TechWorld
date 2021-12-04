

```
DwmOneTrackAppUsage
```

```scala
package com.xiaomi.onedata.onetrack.offline.dwm

import java.text.SimpleDateFormat
import java.util.Calendar

import com.xiaomi.onedata.onetrack.offline.monitor.monitorJob._
import com.xiaomi.onedata.onetrack.utils.HDFSUtils
import org.apache.log4j.Logger
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Row, SaveMode, SparkSession}

import scala.util.Try

/**
  * usage相关计算：
  * notice：spark 2.3+
  * update by yinmuyang on 20-9-01 10:26.
  */
object DwmOneTrackAppUsage {
  val logger = Logger.getLogger(this.getClass.getName)
  val SESSION_SPAN = 30000

  def main(args: Array[String]): Unit = {

    val spark = SparkSession
      .builder()
      .config("spark.hadoop.fs.permissions.umask-mode","000")
      .config("spark.sql.sources.partitionOverwriteMode","dynamic")
      .getOrCreate()
    import spark.sqlContext.implicits._

    val configPath = args(0)
    val workFlowId = args(1)
    val outputPath = args(2)
    val date = args(3)

    val path = "/user/h_data_platform/platform/dw/dwd_ot_event_di_"

    val getList = getAppListWithGroupId(spark,configPath,workFlowId).select("app_id").collect().map(_.getString(0))
    logger.info("get monitor list size " + getList.size)

    val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val bases = getList.map(app_id => s"${path}$app_id/date=${date.trim}" )
      .map(data_path => (data_path,HDFSUtils.testHdfsSuccessIsReday(data_path + "/_SUCCESS")))

    bases.foreach(kv => logger.info(s"data_path[$kv]"))
    val having_data = bases.filter(_._2).map(_._1)
    if(!having_data.isEmpty){
      val all_data = spark.read.parquet(having_data:_*)
      if(!all_data.rdd.isEmpty()) {
        all_data
          .select(col("distinct_id"), col("platform"), col("event_name"), col("properties"), col("e_ts"), col("app_id"))
          .filter($"distinct_id".isNotNull && $"platform".isNotNull && !$"distinct_id".equalTo("") && !$"platform".equalTo(""))
          .filter($"event_name".isin(List("onetrack_upgrade", "onetrack_pa"): _*))
          .select($"distinct_id",
            $"platform",
            $"event_name",
            $"properties.type".as("pa_type"),
            $"e_ts", $"properties.duration".as("duration"),
            $"properties.app_start".as("start_flag"),
            $"properties.app_end".as("end_flag"),
            $"app_id")
          .groupBy($"distinct_id", $"platform", $"app_id")
          .agg(collect_list(struct($"event_name", $"pa_type", $"e_ts", $"duration", $"start_flag", $"end_flag")).as("events"))
          .withColumn("stat", statEvents($"events"))
          .select($"distinct_id", $"platform", $"stat._1".as("startup_times"), $"stat._2".as("upgrade_times"),
            $"stat._3".as("use_duration"), $"stat._4".as("pv"), lit(sdf.format(Calendar.getInstance().getTimeInMillis)).as("etl_tm")
            , lit(date).as("date"), $"app_id")
          .repartition(5)
          .write.partitionBy("app_id")
          .mode(SaveMode.Overwrite)
          .parquet(outputPath)
      }
    }else{
      logger.warn("data_path:is empty!!!!!!!")
    }
  }

  def statEvents: UserDefinedFunction = udf((events: Seq[Row]) => {
    val startupEvents = events.flatMap(e => {
      val ts = e.getAs[Long]("e_ts")
      val start = Try(e.getAs[String]("start_flag").toBoolean).getOrElse(false)
      val end = Try(e.getAs[String]("end_flag").toBoolean).getOrElse(false)
      if(start || end)
        Some(ts, start)
      else
        None
    }).sortBy(_._1)
    val startupTimes =
      if (startupEvents.isEmpty) 0
      else Range(1, startupEvents.length).count(index =>
      (startupEvents(index)._2 && startupEvents(index)._1 - startupEvents(index-1)._1 > SESSION_SPAN).equals(true)
    ) + 1

    val upgradeTimes = events.count(e => e.getAs[String]("event_name").toLowerCase.trim.equals("onetrack_upgrade"))
    val useDuration = events.map(e => Try(e.getAs[String]("duration").toLong).getOrElse(0l)).sum / 1000
    val pv = events.count(e => Try(e.getAs[String]("pa_type").equals("2")).getOrElse(false))
    (startupTimes.toLong, upgradeTimes.toLong, useDuration, pv.toLong)
  })
}

```

