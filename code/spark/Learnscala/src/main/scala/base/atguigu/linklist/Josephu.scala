package base.atguigu.linklist
import scala.util.control.Breaks._
/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved. 
 *
 * @author chengxingfu <chengxingfu@xxx.com>
 * @Date 2020-12-19  
 * @Desc
 */
object Josephu {
  def main(args: Array[String]): Unit = {
      val b1 = new Boy(1)
      val b2 = new Boy(1)
      val b3 = new Boy(1)
    val link = new BoyLink
    link.add(b1)
    link.add(b2)
    link.add(b3)
    link.list()
  }

}

class BoyLink(){
  var head = new Boy(0)
  var temp= head
  //尾部增加小孩,编号不能重复
   def add(boy:Boy): Unit ={
     var exists:Boolean=false //是否已经存在
     breakable{
       while (temp.next!=null){
         temp=temp.next
         if(temp.no == boy.no){
           exists =true
           break()
         }
       }

     }
     //最后head.next为空，head肯定就是最后一个
     if(!exists){
       temp.next=boy
       boy.next=head
     }
   }

  //list 链表
  def list(): Unit ={
     var temp  =head.next
    if (temp == null){
      printf("链表空")
      return
    }
    breakable{
      while (true){
        printf("Boy no:%d\n",temp.no)
        if (temp.next.no==0){
          break()
        }
        temp=temp.next
      }
    }
  }
}

class Boy(val no:Int){
  var next:Boy = null
}
