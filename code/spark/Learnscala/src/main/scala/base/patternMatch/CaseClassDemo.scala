package base.patternMatch

import scala.util.Random

object CaseClassDemo {
  def main(args: Array[String]): Unit = {
    val arr =Array(CheckTimeOutTask,SubmitTask("1000","task_0001"),HeartBeat(3000))
    arr(Random.nextInt(arr.length)) match {
      case CheckTimeOutTask=>println("CheckTimeOutTask")
      case SubmitTask(port,task)=>println("SubmitTask")
      case HeartBeat(time)=>println("HeartBeat")
    }
  }
}

case class HeartBeat(time:Long)
case class SubmitTask(id:String,taskName:String)
case object CheckTimeOutTask