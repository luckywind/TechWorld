package base.oop

/**
 * 包访问权限：private [包名] 类名
 *private [oop] class PrivateDemo
 *
 * 构造器参数列表前加private是指只有伴生对象才能访问
 * private [oop] class private PrivateDemo(val gender:int,var faceValue:Int) {
 */


private [oop] class PrivateDemo {
  //字段前加private，此时该字段称为私有字段
  private val name= "jingjing"
  //称为对象私有字段，只能在本类访问，伴生对象无法访问
  private [this] var age =24
  //私有方法
  private def sayHello():Unit ={
    println(s"jingjing's age is $age")
  }
}

object PrivateDemo{

}
