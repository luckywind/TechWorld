package base

object
Exercise {

  def main(args: Array[String]): Unit = {
    val arr= Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    //并行计算
//    val res= arr.par.sum
    //和线程有关，每个线程计算一部分:(1+2+3+4)+(5+6+7+8)+(9+10)
    //按照特定的顺序进行聚合
//    val res = arr.reduce(_+_)
//    val res = arr.reduceLeft(_+_)
    //折叠，有初始值,不要并行，否则每个线程都会加一次初始值
  //    val res = arr.fold(10)(_+_)
      //    有特定顺序
  //    val res = arr.par.foldLeft(10)(_+_)
    //聚合
    val  list7= List(List(1,2,3),List(4,5,6),List(2),List(0))
//    val res = list7.flatten.reduce(_+_)
//    val res = list7.aggregate(0)(_+_.sum,_+_)

    val l1=List(4,5,6,7,8)
    val l2=List(1,2,3,4)
    // 并集
//    val res = l1 union l2
    // 交集
//    val res = l1 intersect  l2
    // 差集
    val res = l1 diff l2

    println(res)
  }

}
