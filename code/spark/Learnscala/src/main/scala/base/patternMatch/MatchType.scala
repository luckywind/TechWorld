package base.patternMatch

import scala.util.Random


object MatchType {
  def main(args: Array[String]): Unit = {
    val arr = Array("abcde",100,3.14,true,MatchType)
    val element = arr(Random.nextInt(arr.length))
    println(element)
    element match {
      case str:String => println(s"match string :$str")
      case int:Int => println(s"match int :$int")
      case bool:Boolean => println(s"match Boolean :$bool")
      case matchTest:MatchTest => println(s"match string :$matchTest")
      case _ => println("not matched")
    }
  }
}

class MatchTest{}