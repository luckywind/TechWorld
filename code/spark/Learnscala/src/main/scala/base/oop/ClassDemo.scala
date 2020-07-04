package base.oop

object ClassDemo {
  def main(args: Array[String]): Unit = {
    val human = new Human
    println(human.name)
    println(human.climb)
    println(human.fight)

  }
}

/**
 * 特质
 * 类似接口，只是属性可以有默认值，方法可以有实现
 * 类可以直接继承trait，但是如果已经继承了一个抽象类类，则只能用with来实现trait
 */
trait Flyable{
  //未副职的字段
  val distance:Int
  //未实现的方法
  def fight:String
  //实现的方法
  def fly:Unit ={
    println("I can fly")
  }
}

/**
 抽象类
 */
abstract class Animal{
  //没有赋值的字段
  val name:String
  //没有实现的方法
  def run:String
  //已实现的方法
  def climb:String ={
    "I can climb "
  }
}

class Human extends Animal with Flyable{
  override val name: String = "张三"

  override def run: String = "I can run "

  override val distance: Int = 1000

  override def fight: String = "with 棒子"

  override def climb: String = "override climb "
}
