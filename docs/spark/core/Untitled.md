```scala
package com.xiaomi.bigdata.people.id.rule

import java.util

import com.xiaomi.data.common.utils.ArgumentParser
import com.xiaomi.data.common.utils.path.PathUtils
import com.xiaomi.data.common.utils.time.DateUtils
import com.xiaomi.data.commons.spark.SparkMain
import com.xiaomi.data.commons.spark.HdfsIO.SparkContextThriftFileWrapper
import com.xiaomi.data.spec.platform.bigdata.{DwsBigdataMonthLivingUser, DwsMonthLivingUserMid, DwsMonthLivingUserPn}
import com.xiaomi.data.spec.platform.bigdata.{DwmOneidEdgeDf, DwmOneidVertexDf}
import org.apache.spark.SparkContext
import com.xiaomi.bigdata.people.id.base.util.CryptoUtils._
import com.xiaomi.bigdata.people.id.base.spark.HdfsIOWrapper._
import com.xiaomi.bigdata.people.id.rule.PeopleDetectMapping.logger
import org.apache.spark.storage.StorageLevel

import scala.collection.mutable.Map
import scala.collection.JavaConverters._
import com.xiaomi.data.bigdata.pid.{DwmPeopleIdEdgeDf, DwsPeopleIdMappingDf, PeopleIdMappingAssistance}

import scala.collection.mutable.ArrayBuffer

object test extends SparkMain {
  override def process(sc: SparkContext, args: Array[String]): Unit = {
    val df = new  DwsPeopleIdMappingDf()
    df.people_id="00019d9791add82851a694b8c143eaeb"
    df.id_value="816a5d43945df17df9c6daa046bdb84c"
    val map = new util.HashMap[java.lang.String,java.lang.String]()
    map.put("kv","816a5d43945df17df9c6daa046bdb84c")
    df.extend_map=map


    val df2 = new  DwsPeopleIdMappingDf()
    df2.people_id="e98ca487fd7de6dcbd1ec5e5434e0a5e"
    df2.id_value="816a5d43945df17df9c6daa046bdb84c"
    val map2 = new util.HashMap[java.lang.String,java.lang.String]()
    map2.put("kv","e18fcf8fc9f49aebb095b0d7446575d4")
    df2.extend_map=map2
    val imsiPeopleIdMappingData=sc.parallelize(Seq(
      ("816a5d43945df17df9c6daa046bdb84c",("00019d9791add82851a694b8c143eaeb",0,df)),
      ("816a5d43945df17df9c6daa046bdb84c",("e98ca487fd7de6dcbd1ec5e5434e0a5e",0,df2))
    ))
    /**imsi  找主人*/
    val imsiToOwner = imsiPeopleIdMappingData
      .groupByKey()
      .map {line =>
        val imsi = line._1
        val targets = line._2.filter(_._3.extend_map.get("kv")==imsi)
        println(s"||targets:$targets")
        val (peopleId,bindFactor,peopleIdMappingService)=
          if (!targets.isEmpty) {
            println("||targets.last:{}",targets.last)
            targets.last
          }else {
            val tuple = line._2.maxBy(x => (x._2, x._1))
           println(s"tuple:$tuple")
            tuple
          }
        logger.info(peopleIdMappingService.toString)
        (peopleId,(imsi,bindFactor,peopleIdMappingService))
      }.persist(StorageLevel.MEMORY_AND_DISK_SER)
  }
}


```

