package base.data


/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved. 
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
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
    new Horse
  }
}
