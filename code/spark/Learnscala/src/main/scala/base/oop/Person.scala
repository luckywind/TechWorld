package base.oop

/**
 * 声明类时，默认是public
 * 一个类文件可以声明多个类
 */
class Person {
  // val属性自动有get方法，没有set方法
  val id: String ="100"
  // var属性自动有get/set
  var name:String = _
  // private属性，私有，只有本类和其伴生对象可访问
  private var age:Int = _
  // private[this]属性，属于对象私有，只有本类能访问，伴生对象也不行
  private[this] val gender = "男"
}

/**
 * Person类的伴生对象
 */
object Person{
  def main(args: Array[String]): Unit = {
    val p = new Person
    println(p.id)
    println(p.name)  //null
    println(p.age)   //0
//    println(p.gender)   //伴生对象无法访问private[this]
    p.name = "ningning"
    p.age = 26
    println(p.age)
  }
}

/**
 * 静态类
 */
object Test1{
  def main(args: Array[String]): Unit = {
    val p = new Person
//    println(p.age) //无法访问私有属性
    println(p.name)
    p.name="tingting"
    println(p.name)
  }
}
