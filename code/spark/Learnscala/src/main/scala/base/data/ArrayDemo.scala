package base.data

import scala.collection.mutable.ArrayBuffer


/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved. 
 *
 * @author chengxingfu <chengxingfu@xxx.com>
 * @Date 2020-09-27  
 * @Desc
 */
object ArrayDemo {
  def main(args: Array[String]): Unit = {
    val x = Array.range(1, 10)
    val init: Array[String] = Array.fill(3)("foo")
    println(init.mkString(","))
    val tab: Array[Int] = Array.tabulate(5)(n=>n*n)
    println(tab.mkString(","))

    val a = Array(1,2,3)
    val b = new Array[String](3)
    for (e<-a) {
      println(e)
    }
    for(i<-0 until a.length) {
      b(i)=a(i).toString
    }
    for(be <- b) {
      println(be)
    }
    val buff: ArrayBuffer[Int] = ArrayBuffer[Int]()
    for(e<-a) {
      buff.append(e)
    }
    buff.remove(2)
    buff.foreach(println(_))
    for (i<-a.iterator) {
      println(i)
    }

  }
}
