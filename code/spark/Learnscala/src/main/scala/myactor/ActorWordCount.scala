package myactor

import scala.actors.{Actor, Future}
import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
 * 用Actor并发编程实现wordcount
 *
 */
object ActorWordCount {
  def main(args: Array[String]): Unit = {
    // 存放接收到的每个文件结果数据
    val replys: ListBuffer[Future[Any]] = new ListBuffer[Future[Any]]

    //存放有值的Future里的数据
    val res = new ListBuffer[Map[String,Int]]
    val files = Array("/Users/chengxingfu/code/my/studying/Learnscala/logs/error.log", "/Users/chengxingfu/code/my/studying/Learnscala/logs/info.log", "/Users/chengxingfu/code/my/studying/Learnscala/logs/log.log")
    for (file <- files) {
//      val lines: List[String] = Source.fromFile(file).getLines().toList
//      val words: List[String] = lines.flatMap(_.split(" "))
//      val res: Map[String, Int] = words.map((_,1)).groupBy(_._1).mapValues(_.size)
    val task = new Task
      task.start()
      //异步发送消息，有返回值
      val reply: Future[Any] = task  !! SmTask(file)
      //把每个文件的结果放到ListBuffer里
      replys += reply
    }
    while (replys.size>0) {

      //过滤每个future对象，如果有值
      val dones: ListBuffer[Future[Any]] = replys.filter(_.isSet)
      for (done <-dones){
          res += done.apply().asInstanceOf[Map[String,Int]]
          replys -= done
      }

    }
    //全局聚合
    val result: Map[String, Int] = res.flatten.groupBy(_._1).mapValues(_.foldLeft(0)(_ + _._2))
    println(result)

  }
}

class Task  extends Actor{
  override def act(): Unit = {
    while (true) {
      receive({
        case SmTask(file)=>{
                val lines: List[String] = Source.fromFile(file).getLines().toList
                val words: List[String] = lines.flatMap(_.split(" "))
                val res: Map[String, Int] = words.map((_,1)).groupBy(_._1).mapValues(_.size)
                //异步发送出去，不需要返回值
                sender ! res
        }
      })
    }
  }
}

case class SmTask(file:String)