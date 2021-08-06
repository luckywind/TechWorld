package base.atguigu.linklist
import scala.util.control.Breaks._
/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved. 
 *
 * @author chengxingfu <chengxingfu@xxx.com>
 * @Date 2020-12-18  
 * @Desc
 * 单向链表技巧：
 *  1. 有一个头节点，不要动它，用一个临时指针代替它移动
 *  2. 有序插入，只要遍历并找到第一个大于目标节点的
 */
object SingleLinkListDemo {
  def main(args: Array[String]): Unit = {
  val h1 = new HeroNode(1,"宋江","及时雨")
  val h2 = new HeroNode(3,"宋江3","及时雨3")
  val h3 = new HeroNode(4,"宋江4","及时雨4")
  val h4 = new HeroNode(2,"宋江2","及时雨2")
    val list = new HeroLinkList
//    list.add(h1)
//    list.add(h2)
//    list.add(h3)
//    list.add(h4)
    list.add2(h1)
    list.add2(h2)
    list.add2(h3)
    list.add2(h4)
    list.list()
    list.delete(h2)
    list.list()
  }

}

class HeroLinkList(){
  val head=new HeroNode(0,"","")

  //删除节点
  def delete(heroNode: HeroNode): Unit ={
    //1.先要找到这个节点
    var temp = head
    var heroExist = false
    breakable{
      while(temp.next!=null){
        if (temp.next.hno==heroNode.hno){
          println("找到英雄")
          heroExist=true
          break()//此时temp.next就是要删除的
        }
        temp=temp.next
      }
      //注意，一旦上面break,整个breakable都结束
    }
    println("-----")
    if(heroExist){ //如果找到英雄
      printf("删除英雄:%d\n",temp.next.hno)
      temp.next=temp.next.next
    }else{
      println("没有找到英雄")
    }


  }

  def add(heroNode: HeroNode): Unit ={
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
          break()
        }else{
          temp = temp.next
        }
      }
    }
  }

  /**
   * 有序插入
   * @param heroNode
   */
  def add2(heroNode: HeroNode): Unit ={
    var temp=head
    var hexist=false //编号是否已存在，默认不存在
    breakable{
      while(true){
        if(temp.hno == heroNode.hno){
          printf("编号%d一存在\n",heroNode.hno)
          hexist=true
          break()
        }
        if (temp.next == null) { //temp已经是最后一个了，直接追加，结束
          temp.next = heroNode
          break()
        }else{ //要加到temp后面
          if (temp.next.hno>heroNode.hno){
            heroNode.next=temp.next
            temp.next=heroNode
            break()
          }
        }

        temp=temp.next
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

class HeroNode(no:Int, name:String, nickName:String){
  val hno=no
  val hname=name
  val hnickName=nickName
  var next: HeroNode = null
}
