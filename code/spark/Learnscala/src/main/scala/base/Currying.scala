package base

object Context{
  implicit val a="java"
}
object Currying {
  def putong(x:Int, y:Int):Int={
    x*y
  }


  /**
   * 克里化函数，例如：foldLeft
   * @param x
   * @param y
   * @return
   */
  def mcurry(x:Int)(implicit y:Int=5):Int={
    x*y
  }

  def m1(str:String)(implicit name:String="scala")={
    str+name
  }




  def main(args: Array[String]): Unit = {
//    println(mcurry(3))
//    println(mcurry(3)(4))
//    implicit val x=100 //改变隐士值，按类型找的，且不能出现多个隐士值
//    println(mcurry((4)))

//    val func=m1("Hi~")
//    println(func)

    import Context.a
    println(m1("Hi~"))
  }
}
