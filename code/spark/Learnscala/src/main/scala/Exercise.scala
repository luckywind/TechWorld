object Exercise {
  def main(args: Array[String]): Unit = {
    val list0 =List(2,5,9,6,7,2,1,6,7)

    val list1 = list0.map(_ *2)
    println(list1)
    val list2 = list0.filter(_ %2 ==0)
    println(list2)

    val list3 = list0.sorted
    println(list3)
    println(list3.reverse)

    val it = list0.grouped(4)
//    println(it.toBuffer)

    val list5 = it.toList
    println(list5)

    val list6 = list5.flatten
    println(list6)

    val lines = List("hello java hello scala", "hello python")
    val words = lines.map(_.split(" "))
    val flatwords = words.flatten
    println(flatwords)
    val res1 = lines.flatMap(_.split(" "))
    println(res1)

    val arr=Array(1,2,3,4,5,6,7,8,9,10)
//    val res = arr.sum
    val res = arr.par.sum  //并行执行聚合
    println(res)
    val  reduce_res =arr.reduce(_+_)



  }
}
