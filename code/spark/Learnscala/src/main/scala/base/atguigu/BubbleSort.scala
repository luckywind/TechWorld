package base.atguigu

object BubbleSort {
  def main(args: Array[String]): Unit = {
    // 数组
     val arr = Array(3, 9, -1, 10, 20)
    // 创建一个80000个随机数据的数组，冒泡排序用时10秒
//    val random = new util.Random()
//    val arr = new Array[Int](80000)
//    for (i <- 0 until 80000) {
//      arr(i) = random.nextInt(8000000)
//    }

    println("冒泡排序前")
     println(arr.mkString(" "))


    println("冒泡排序后")
    BubbleSort(arr)
     println(arr.mkString(" "))

  }

  def BubbleSort(arr:Array[Int]): Unit ={
      for( i<- 0 until arr.length- 1) {
        for(j <-0 until arr.length-1-i) {
          if (arr(j) > arr(j+1)){
            val temp = arr(j+1)
            arr(j+1)=arr(j)
            arr(j)=temp
          }
        }
      }
  }

}
