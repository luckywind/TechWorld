package base.atguigu

object Josephu {
  def main(args: Array[String]): Unit = {
    val game = new BoyGame
    game.addBoy(7)
    game.showBoy()
    game.countBoy(4,3)
    val boyClass: Class[Boy] = classOf[Boy]
    println(boyClass)

  }
}


class Boy(bNo:Int){
  val no:Int = bNo
  var next:Boy = null
}

class BoyGame {
  //定义一个初始的头节点
  var first: Boy = null

  //添加小孩
  def addBoy(nums: Int): Unit = {
    if (nums < 1) {
      println("nums的值不正确")
      return
    }
    //
    var temp: Boy = null
    for (no <- 1 to nums) {
      //根据编号创建小孩对象
      val boy = new Boy(no)
      if (no == 1) {
        first = boy
        first.next = first //自己形成环形链表

        temp = first
      } else {
        temp.next = boy
        boy.next = first
        temp = boy
      }
    }

  }

  //遍历链表
  def showBoy(): Unit = {
    if (first == null) {
      println("没有小孩")
      return
    }
    var temp: Boy = first
    while (temp.next != first) {
      printf("当前小孩：%d\n", temp.no)
      temp = temp.next
    }
    printf("当前小孩:%d\n", temp.no)
  }

  //从startNo开始数，每次数到countNo，就把它删除，再从新开始数
  def countBoy(startNo: Int, countNo: Int) = {
    var temp = first //初始，temp和first指向同一个节点
    for (i <- 1 until startNo - 1) { //注意until 是不包含最后一个的，to是包含最后一个的
      temp = temp.next
    }
    first = temp.next //first比temp多移动一个位置，之后保持temp在first前面，辅助删除节点

    while (first!=temp) {
      //temp和first同时向后数countNo
      for (i <- 1 until countNo) {
        temp = temp.next
        first = first.next
      }
    //删除first指向的节点
      printf("出圈节点:%d\n",first.no)
    first = first.next //first指向删除节点的下一个
    temp.next = first //temp的下一个节点保持是first
  }
    printf("最后一个：%d",first.no)
}

}
