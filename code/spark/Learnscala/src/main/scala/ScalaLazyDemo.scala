/**
 * lazy 延迟加载，只能是不可变变量
 * 调用时才会实例化
 */

class ScalaLazyDemo {

}
//静态类，也称为伴生对象
object ScalaLazyDemo1{
  def init():Unit = {
    println("call init")
  }
  def main(args: Array[String]): Unit = {
    val property = init()//没有lazy
    println("after init")
    println(property)
  }
}
object ScalaLazyDemo2{
  def init():Unit = {
    println("call init")
  }
  def main(args: Array[String]): Unit = {
    lazy val property = init()//有lazy
    println("after init")
    println(property)
  }
}
