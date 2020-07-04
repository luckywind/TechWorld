package base.atguigu

import scala.io.StdIn

object CircleArrayQueueDemo {
  def main(args: Array[String]): Unit = {
    val queue = new CircleArrayQueue(3)
    var key = ""
    while (true) {


      println("show:表示显示队列")
      println("add:添加元素")
      println("exit:表示退出程序")
      println("get:取出数据")
      println("head:查看队列头")

      key = StdIn.readLine()
      key match {
        case "show" => queue.showQueue()
        case "add" => {
          println("请输入一个数")
          val value = StdIn.readInt()
          queue.addQueue(value)
        }
        case "get" => {
          val res = queue.getQueue()
          if (res.isInstanceOf[Exception]) {
            println(res.asInstanceOf[Exception].getMessage)
          } else {
            println(s"取出数据是 $res")
          }
        }
        case "head" => {
          val res = queue.headQueue()
          if (res.isInstanceOf[Exception]) {
            println(res.asInstanceOf[Exception].getMessage)
          } else {
            println(s"队列头数据是 $res")
          }
        }
        case "exit" => System.exit(0)
      }
    }
  }
}
class CircleArrayQueue(arrMaxSize:Int){
  val maxSize =arrMaxSize
  val arr = new Array[Int](maxSize)
  var front = 0
  var rear = 0

  /**
   * 尾节点的下一个节点 就是头节点
   *  尾索引的下一个为头索引时表示队列满，即将队列容量空出一个作为约定，这个在做判断队列满的时候需要注意
   * @return
   */
  def isFull(): Boolean ={
    (rear +1) % maxSize ==front
  }

  def isEmpty(): Boolean ={
    rear == front
  }
  //查看头元素
  def headQueue(): Any ={
    if (isEmpty()) {
      return new Exception("队列空")
    }else{

      return arr(front)
    }
  }

  //
  def addQueue(n:Int): Unit ={
    //判断满
    if(isFull()){
      println("队列满")
      return
    }
    //加入队列
    arr(rear) =n
    //将rear后移,必须考虑取模
    rear = (rear +1) %maxSize
  }
  def getQueue(): Any ={
    if(isEmpty()) {
      return new Exception("队列空")
    }
    //这里需要分析，front已经指向了队列的头元素
    //1,先把front对应的数据保存到一个变量，
    // 2，front后移
    // 3，返回前面保存的值
    val value = arr(front)
    front =(front +1) % maxSize
    return value
  }

  def size():Int = {
    (rear+maxSize-front) % maxSize
  }

  //显示队列
  //显示所有数据
  def showQueue(): Unit ={
    if (isEmpty()){
      println("队列是空的，没有数据")
      return
    }
    //思路：从front取，取出几个元素
    for (i <- front until front + size()) {
      printf("arr[%d]= %d \n",i%maxSize, arr(i%maxSize))
    }
  }
}