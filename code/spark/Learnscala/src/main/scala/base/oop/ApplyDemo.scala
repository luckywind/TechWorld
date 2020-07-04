package base.oop

/**
 * apply方法： 称为注入方法，在伴生对象里做一些初始化操作
 *            参数列表不需要和构造器参数列表统一
 * unapply方法： 称为提取方法，使用unapply方法来提取固定对象
 *            会返回一个序列(Option),内部生成来一个Some对象，来存放一些值
 * apply和unapply方法会被隐式调用
 * @param name
 * @param age
 * @param faceValue
 */
class ApplyDemo(val name:String,var age:Int,var faceValue:Int) {
  
}

object ApplyDemo{
  /**
   * 注入，初始化一个类
   * @param name
   * @param age
   * @param faceValue
   * @return
   */
  def apply(name: String, age: Int, faceValue: Int): ApplyDemo = new ApplyDemo(name, age, faceValue)

  /**
   * 提取方法，提取的值放到Some里
   * @param applyDemo
   * @return
   */
  def unapply(applyDemo: ApplyDemo): Option[(String, Int, Int)] = {
    if(applyDemo==null) {
      None
    }else{
      Some(applyDemo.name,applyDemo.age,applyDemo.faceValue)
    }
  }
}

object Test2{
  def main(args: Array[String]): Unit = {
    val applyDemo = ApplyDemo("jingjing",24,90) //调用apply方法
    applyDemo match {
        //隐士调用unapply
      case ApplyDemo("jingjing",age,faceValue)=>println(s"age: $age")
      case _ =>println("No match nothing")
    }
  }
}