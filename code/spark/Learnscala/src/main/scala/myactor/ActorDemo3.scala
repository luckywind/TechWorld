package myactor

import scala.actors.{Actor, Future}

/**
 * 用Actor实现同步和异步的消息发送和接收
 */
class ActorDemo3 extends Actor{
  override def act(): Unit = {
    while (true) {
      //偏函数
      receive{
        case "start" => println("starting ... ")
        case AsynMsg(id,msg) =>{
          println(s"id:$id, AsyncMsg: $msg")
          Thread.sleep(2000)
          sender ! ReplyMsg(5,"success")   //异步发送
        }
        case SyncMsg(id,msg) =>{
          println(s"id:$id,SyncMsg:$msg")
          Thread.sleep(2000)
          sender ! ReplyMsg(5,"success")
        }
      }
    }
  }
}

object ActorDemo3{
  def main(args: Array[String]): Unit = {
    val actorDemo3 = new ActorDemo3
    //异步发送消息，没有返回值
    actorDemo3.start()
/*    actorDemo3 ! AsynMsg(1,"Hi~ Honey")
    println("没有返回值的异步消息发送完成")

    //同步发送消息，有返回值
    val content: Any = actorDemo3 !? SyncMsg(2,"Hi~ tingting,请回答")
    println("有返回值的同步消息发送完成")
    println(content)*/

    //异步发送消息，有返回值，类型为Future[Any]

    val futureReply: Future[Any] = actorDemo3 !! AsynMsg(3, "Hi~, ningning")
    Thread.sleep(6000)
    if (futureReply.isSet) {
      val value: Any = futureReply.apply()
      println(value)
    }else{
      println("快回复。。。")
    }
  }
}



case class AsynMsg(id:Int,msg:String)
case class SyncMsg(id:Int,msg:String)
case class ReplyMsg(id:Int,msg:String)

