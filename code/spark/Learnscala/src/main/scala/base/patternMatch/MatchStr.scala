package base.patternMatch

import scala.util.Random

/**
 * 匹配字符串
 */

object MatchStr{
  def main(args: Array[String]): Unit = {
    val arr =Array("zhoudongyu","zhengshuang","guanxiaotong","yangzi")

    val name = arr(Random.nextInt(arr.length))
    println(name)
    name match {
      case "zhoudongyu" => println("周冬雨")
      case "zhengshuang" => println("郑爽")
      case "guanxiaotong" => println("关晓彤")
      case "yangzi" => println("杨紫")
      case _ => println("Nothing")
    }
  }
}
