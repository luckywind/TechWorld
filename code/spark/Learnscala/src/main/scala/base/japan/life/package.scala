package base.japan

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved. 
 *
 * @author chengxingfu <chengxingfu@xxx.com>
 * @Date 2021-04-11  
 * @Desc
 */
package object life {
class  Person(val firstName:String,val lastName:String,var spouse:Person){
  def this(fn:String,ln:String) = this(fn,ln,null)
  def introduction="我的名字是"+firstName+" "+lastName+
    (if(spouse !=null)"对方名字是"+spouse.firstName+" "+spouse.lastName+"."
      else ".")

  def getMarriedTo(p:Person): Unit ={
    this.spouse=p;p.spouse=this
  }

  override def toString: String = super.toString+"姓:"+lastName+" 名："+firstName+"配偶:"+
    (if(spouse !=null) spouse.lastName+","+spouse.firstName else "没有")

}
  object Person{
    def marry(p1:Person,p2:Person):Unit={
      p1.spouse=p2; p2.spouse=p1
    }

  }

}
