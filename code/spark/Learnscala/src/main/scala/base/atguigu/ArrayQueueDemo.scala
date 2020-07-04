package base.atguigu

import scala.io.StdIn

object ArrayQueueDemo {
  def main(args: Array[String]): Unit = {
    val queue = new ArrayQueue(3)
    var key=""
    while (true){


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
        case "get" =>{
          val res=queue.getQueue()
          if (res.isInstanceOf[Exception]){
            println(res.asInstanceOf[Exception].getMessage)
          }else{
            println(s"取出数据是 $res")
          }
        }
        case "head" =>{
          val res=queue.headQueue()
          if (res.isInstanceOf[Exception]){
            println(res.asInstanceOf[Exception].getMessage)
          }else{
            println(s"队列头数据是 $res")
          }
        }
        case "exit" =>System.exit(0)
      }
    }

  }
}
//使用数组模拟队列
class ArrayQueue(arrMaxSize:Int){
  val maxSize =arrMaxSize
  val arr = new Array[Int](maxSize)
  var front = -1
  var rear = -1

  //判断空
  def isEmpty(): Boolean ={
    front == rear
  }
  def isFull(): Boolean ={
    rear == maxSize-1
  }

  def addQueue(n:Int): Unit ={
    //判断满
    if(isFull()){
      println("队列满")
      return
    }
    rear += 1
    arr(rear) = n
  }

  def getQueue(): Any ={
    if(isEmpty()) {
      return new Exception("队列空")
    }
    front+=1
    return arr(front)
  }

  //显示所有数据
  def showQueue(): Unit ={
    if (isEmpty()){
      println("队列是空的，没有数据")
      return
    }
    for(i <- front+1 to rear){
      printf("arr[%d]=%d\n",i,arr(i))
    }
  }

  //查看头元素
  def headQueue(): Any ={
    if (isEmpty()) {
      return new Exception("队列空")
    }else{
      //注意，不要改变front的值
      return arr(front+1)
    }
  }

}