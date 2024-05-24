package base.oop

import scala.collection.mutable.ArrayBuffer

/**
 * 在scala中没有静态方法和静态字段，但是可以使用object关键字加类名的语法结构实现该功能
 * 1、工具类，存放常量和工具方法
 * 2、实现单例模式
 * 伴生对象一定是单例对象
 */
object SingletonDemo {
  def main(args: Array[String]): Unit = {
    val factory=SessionFactory
    println(factory.getSessions)
    println(factory.getSessions.size)
    println(factory.getSessions(0))
    println(factory.removeSession)
  }
}


object SessionFactory{
  /**
   * 相当于java中的静态块
   */
  println("SessionFactory被执行")
  var i = 5
  private val session = new ArrayBuffer[Session]()
  while (i>0) {
    session+=new Session
    i -=1
  }

  def getSessions = session
  def removeSession:Unit={
    val s= session(0)
    session.remove(0)
    println("session 被移除"+s)
  }
}

class Session{}