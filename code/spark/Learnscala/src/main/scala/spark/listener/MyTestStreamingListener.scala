package spark.listener

import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.scheduler.{StreamingListener, StreamingListenerBatchCompleted}

class MyTestStreamingListener(ssc : StreamingContext) extends StreamingListener {



  override def onBatchCompleted(batchCompleted: StreamingListenerBatchCompleted): Unit = {
    val batchInfo = batchCompleted.batchInfo
    val execTime = batchInfo.processingDelay.getOrElse(0L)
    val schedulingTime = batchInfo.schedulingDelay.getOrElse(0L)
    println(s"执行时间: $execTime 调度延时 : $schedulingTime")
  }

}