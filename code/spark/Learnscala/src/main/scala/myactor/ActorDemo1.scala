package myactor
import java.util.concurrent.TimeUnit

import scala.actors.Actor
object ActorDemo1 extends Actor{
  override def act(): Unit = {
    for (i <- 1 to 20) {
      println(s"actor1  $i")
      Thread.sleep(1000)
    }
  }
}


object ActorDemo2 extends Actor{
  override def act(): Unit = {
    for (i <- 1 to 20) {
      println(s"actor2  $i")
      Thread.sleep(1000)
    }
  }
}

/**
 * 创建两个线程
 */
object ActorTest{
  def main(args: Array[String]): Unit = {
    ActorDemo1.start()
    ActorDemo2.start()
  }
}