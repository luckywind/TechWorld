package base.patternMatch

/**
 * 匹配数组、元组、集合
 */
object MatchList {
  def main(args: Array[String]): Unit = {
    //匹配数组
    val arr =Array(3,2,5,7)
    arr match {
      case Array(3,a,b,c) => println(s"case:$a,$b,$c")
      case Array(_,x,y) => println(s"case: $x,$y")
      case _ => println("not matched ")
    }

    //匹配元组
    val tup= (2,3,4)
    tup match {
      case (2,a,b) =>println(s"case:$a,$b")
      case (_,a,b) =>println(s"case:$a,$b")
      case _ => println("not match ")
    }

    //匹配集合
    val list1 = List(0,1,2,3)
    list1  match {
      case 0::Nil =>println(s"case: 0")
      case a::b::c::d::Nil =>println(s"case $a,$b,$c,$d")
      case 0::a=>println(s"case3:$a")
      case _ =>println("not match ")
    }



  }
}
