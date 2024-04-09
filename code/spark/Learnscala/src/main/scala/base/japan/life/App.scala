package base.japan.life

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved. 
 *
 * @author chengxingfu <chengxingfu@xxx.com>
 * @Date 2021-04-12  
 * @Desc
 */
object App {
  def main(args: Array[String]): Unit = {
    val cxf = new Person("bu","lv")
    val xishi = new Person("xi","shi")
//    cxf.getMarriedTo(xishi)
    println(cxf)

    Person.marry(cxf,xishi)
    println(cxf)
  }

}
