package base.atguigu

import scala.io.StdIn

object StackDemo {
  def main(args: Array[String]): Unit = {
    // 测试栈的基本使用
    val arrayStack = new ArrayStack(3)

    var key = ""
    while (true) {
      println("list：表示显示栈的数据")
      println("exit：表示退出程序")
      println("push：表示将数据压栈")
      println("pop：表示将数据弹栈")

      key = StdIn.readLine()
      key match {
        case "list" => arrayStack.list()
        case "exit" => System.exit(0)
        case "push" => {
          print("请输入一个数据(Int类型)：")
          val n = StdIn.readInt()
          arrayStack.push(n)
        }
        case "pop" => arrayStack.pop()
      }
    }
  }
}
class ArrayStack(size:Int){
  val maxSize=size
  var top = -1
  var stack = new Array[Int](maxSize)

  def isFull(): Boolean ={
    top == maxSize-1
  }
  def isEmpty(): Boolean ={
    top == -1
  }
  def push(data:Int): Any ={
    if (isFull()){
      new Exception("栈满")
    }
    top+=1
    stack(top)=data
  }
  def pop(): Any ={
    if(isEmpty()){
      new Exception("栈空")
    }
    top-=1
    stack(top+1)
  }
  def list(): Unit ={
    for(i <- 0 to top reverse){
    printf("stack[%d]=%d \n",i,stack(i))
    }
  }



}
