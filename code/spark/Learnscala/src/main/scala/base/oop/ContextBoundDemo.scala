package base.oop

import base.implict.Girl

/**
 * [B:A] ContextBound
 *
 * @param ordering$T$0
 * @tparam T
 */
class ContextBoundDemo[T:Ordering] {
  def select(first:T,second:T):T={
    val ord:Ordering[T] = implicitly[Ordering[T]] //隐式值
    if (ord.gt(first,second)) first else second
  }
}
object ContextBoundDemo{
  def main(args: Array[String]): Unit = {
        import base.implict.MyPredef.OrderingGirl
        val u = new ContextBoundDemo[Girl]
        val g1= new Girl("daya",90)
        val g2= new Girl("二ya",100)
     val girl = u.select(g1,g2)
    println(girl.name)
  }
}

