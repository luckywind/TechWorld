package base.oop

/**
 * 构造器：
 *   主构造器： 类名后的参数列表,可以有默认值的参数(默认val)
 *   辅助构造器：
 * @param name
 * @param age
 * @param faceValue
 */
class StructDemo(val name:String,var age:Int,faceValue:Int =90) {
  var gender:String =_
    def getFaceValue():Int ={
      faceValue
    }
  //辅助构造器
  def this(name:String,age:Int,faceValue:Int , gender:String){
    this(name,age,faceValue)  // 辅助构造器第一行必须先调用主构造器
    this.gender=gender
  }
}

object StructDemo{
  def main(args: Array[String]): Unit = {
//    val s = new StructDemo("ningning",26,98)
//    val s = new StructDemo("ningning",26)   //使用默认值
    //使用辅助构造器
    val s = new StructDemo("ningning",26,98,"女")
//    s.name="tingting" //val无法改变
    s.age=27
    println(s.name)
    println(s.age)
//    println(s.faceValue) //无法访问
    println(s.getFaceValue())
    println(s.gender)
  }
}

