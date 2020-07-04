package base.implict

/**
 * 隐式转换和隐式参数
 * 作用： 能够丰富现有类的功能，对类的方法进行增强
 * 隐式转换函数： 以implicit关键字声明并带有单个参数的函数
 */

/**
 * 范型
 *    [B<:A] UpperBound上界：B类型的父类是A类型
 *    [B>:A] LowerBouond下界：B类型的子类是A类型
 *    [B<%A] ViewBound B类型要转换成A类型，需要一个隐式转换函数
 *    [B:A] ContextBound 需要一个隐式转换的值
 *    [-A] 逆变，作为参数类型，如果A是T的子类，那么C[T]是C[A]的子类
 *    [+B] 协变，作为返回类型，如果A是T的子类，那么C[A]是C[T]的子类
 */


//定义了一个Girl类排序规则
object ImplicitContext{
  implicit object OrderingGirl extends Ordering[Girl]{
    override def compare(x: Girl, y: Girl): Int = if(x.faceValue>y.faceValue) 1 else  -1
  }
}


class Girl(val name:String,var faceValue:Int){
  override def toString: String = s"name: $name, facevalue:$faceValue"
}

//提供一个克里化的choose方法，你可以不传排序规则，直接使用默认的隐式规则
class Goddess[T:Ordering](val v1:T,val v2:T){
  //隐式转换函数
  def choose()(implicit ord:Ordering[T]) = if (ord.gt(v1,v2)) v1 else v2
}

object Goddess{
  def main(args: Array[String]): Unit = {
    import ImplicitContext.OrderingGirl
    val g1= new Girl("幂幂",90)
    val g2= new Girl("爽爽",80)
    val goddess = new Goddess(g1,g2)
    println(goddess.choose())
  }
}