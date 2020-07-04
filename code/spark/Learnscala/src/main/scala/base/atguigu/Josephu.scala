package base.atguigu

object Josephu {
  def main(args: Array[String]): Unit = {

  }
}


class Boy(bNo:Int){
  val no:Int = bNo
  var next:Boy = null
}

class BoyGame{
  //定义一个初始的头节点
  var first:Boy=new Boy(-1)
  //添加小孩
  def addBoy(nums:Int): Unit ={
      if (nums <1) {
        println("nums的值不正确")
        return
      }
    //
    for(no<- 1 to nums){

      //根据编号创建小孩对象
      val boy = new Boy(no)
      if (no ==1){
        first = boy
        first.next = first //自己形成环形链表

      }
    }

  }
}
