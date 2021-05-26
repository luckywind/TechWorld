package base.atguigu.linklist

import scala.util.control.Breaks.{break, breakable}

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved. 
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2020-12-19  
 * @Desc
 */
object DoubleLinkListDemo {
  def main(args: Array[String]): Unit = {
    val h1 = new HeroNode2(1,"宋江","及时雨")
    val h2 = new HeroNode2(3,"宋江3","及时雨3")
    val h3 = new HeroNode2(4,"宋江4","及时雨4")
    val h4 = new HeroNode2(2,"宋江2","及时雨2")
    val list = new DoubleLinkList
        list.add(h1)
        list.add(h2)
        list.add(h3)
        list.add(h4)
    list.list()
    list.delete(h4.hno)
    list.list()
  }
}
class DoubleLinkList(){
  val head:HeroNode2=new HeroNode2(0,"","") //指向头节点

  //增加
  def add(heroNode:HeroNode2): Unit ={
    //先找到链表尾部
    var temp=head
    var hexist=false //编号是否已存在，默认不存在
    breakable{
      while(true){
        if(temp.hno == heroNode.hno){
          printf("编号%d 已存在\n",heroNode.hno)
          break()
        }
        if (temp.next == null) { //temp已经是最后一个了
          printf("添加:%d\n",heroNode.hno)
          temp.next = heroNode
          heroNode.pre=temp
          break()
        }else{
          temp = temp.next
        }
      }
    }
  }


  //删除
  def delete(no:Int): Unit ={
    var temp = head.next
    if(temp == null){
      println("链表空")
      return
    }

    var nodeExist=false //节点不存在

    breakable{
      while (true){
          if (temp == null){
            break()
          }
          if(temp.hno == no){
            nodeExist=true
            break()
          }
        temp=temp.next
      }

    }
    if (nodeExist){
      //双向链表可以自我删除，不用记住前驱，直接把temp删除即可
      println("删除")
      temp.pre.next = temp.next
      //这里要注意，如果删除的是最后一个节点，就无需这个链
      if (temp.next !=null){
      temp.next.pre=temp.pre
      }
    }



  }


  def list(): Unit ={
    var temp=head.next
    if (temp == null){
      printf("链表空\n")
      return
    }
    breakable{
      while(true){
        //先打印
        printf("当前编号：%d\n",temp.hno)
        if(temp.next!=null){
          temp=temp.next
        }else{
          break
        }
      }

    }
  }
}
class HeroNode2(no:Int,name:String,nickName:String){
  val hno=no
  val hname=name
  val hnickName=nickName
  var pre:HeroNode2=null
  var next:HeroNode2=null
}
