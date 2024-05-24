package base.oop

import base.implict.Girl

/**
 * ViewBound
 * [B <% A]
 * 需要一个隐式转换
 * @tparam T
 */

class ViewBoundDemo[T <% Ordered[T]] {
      def select(first:T,second:T):T={
        if (first > second)first else second
      }
    }
    object ViewBoundDemo{
      def main(args: Array[String]): Unit = {

        import base.implict.MyPredef.selectGirl
        val u = new ViewBoundDemo[Girl]
        val g1 = new Girl("biaozi",120)
        val g2 = new Girl("erbiao",130)
        println(u.select(g1,g2))
  }
}

