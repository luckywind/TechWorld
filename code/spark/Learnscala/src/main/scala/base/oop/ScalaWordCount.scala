package base.oop

object ScalaWordCount {
  def main(args: Array[String]): Unit = {
    val lines= List("hello java hello python","hello scala")
    //切分压平
    val words = lines.flatMap(_.split(" "))

    //把每个单词变成一个元组
    val tuples = words.map((_,1))
    //以key进行分组
    val grouped = tuples.groupBy(_._1)
    //统计value的长度
    val wc = grouped.mapValues(_.size)
    //排序
    val sortedwc = wc.toList.sortBy(_._2)
    //反转排序
    val res = sortedwc.reverse
    println(words)
    println(tuples)
    println(grouped)
    println(wc)
    println(sortedwc)
    println(res)
  }
}
