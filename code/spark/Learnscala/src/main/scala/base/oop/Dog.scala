package base.oop

/**
 *伴生对象： 与类名相同，且用object修饰的对象
 * 类和其伴生对象可以相互访问私有方法和属性
 */
class Dog {
   private var name ="二哈"
   def printName:Unit={
     //在Dog类中访问其伴生对象的私有属性
     println(Dog.CONSTANT + name)
   }
}

/**
 * 伴生对象，
 */
object Dog{
  private val CONSTANT = "汪汪汪："

  def main(args: Array[String]): Unit = {
    val p = new Dog
    //访问类中的私有字段name
    println(p.name)
    p.name="大黄"
    p.printName
  }
}
