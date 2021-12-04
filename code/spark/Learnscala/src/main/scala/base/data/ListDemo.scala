package base.data

import scala.collection.immutable

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved. 
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2021-11-11  
 * @Desc
 */
object ListDemo {
  def main(args: Array[String]): Unit = {
    var list = List("a","b")
    val a: List[Any] = List.concat(list,"c")
    a.foreach(println(_))
    val y: immutable.IndexedSeq[Any] = list +: "c"
    y.foreach(println(_))
  }
}
