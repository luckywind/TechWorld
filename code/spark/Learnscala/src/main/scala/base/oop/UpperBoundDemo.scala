package base.oop

/**
 * 上界UpperBound
 * <:
 */
class UpperBoundDemo[T <:Comparable[T]] {
  def select(first:T,second:T):T ={
    if (first.compareTo(second) >0) first else second
  }

}
object UpperBoundDemo{
  def main(args: Array[String]): Unit = {
    val u = new UpperBoundDemo[Goddess]
    val m1= new Goddess("老大",120)
    val m2= new Goddess("老er",190)
    val goddess = u.select(m1,m2)
    println(goddess.name)
  }
}

/**
 * 定义比较规则
 * @param name
 * @param faceValue
 */

class Goddess(val name:String,val faceValue:Int) extends Comparable[Goddess]{
  override def compareTo(o: Goddess): Int = {
    this.faceValue - o.faceValue
  }
}
