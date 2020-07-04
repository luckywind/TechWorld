package base

object listDemo {
  def main(args: Array[String]): Unit = {
    val list: List[String] = List("a", "b" ,"a")
    //为列表预添加元素
    println("A" +: list)
    //在列表开头添加元素
    println("c" :: list)
    //在列表开头添加指定列表的元素
    println(List("d","e") ::: list)
    //复制添加元素后列表
    println(list :+ "1")
    //将列表的所有元素添加到 StringBuilder
    val sb = new StringBuilder("f")
    println(list.addString(sb))
    //指定分隔符
    println(list.addString(sb,","))
    //通过列表索引获取元素
    println(list.apply(0))
    //检测列表中是否包含指定的元素
    println(list.contains("a"))
    //将列表的元素复制到数组中,在给定的数组xs中填充该列表的最多为长度(len)元素,从start位置开始。
    val a = Array('a', 'b', 'c')
    val b : Array[Char] = new Array(5)
    a.copyToArray(b,0,1)
    b.foreach(println)
    //去除列表的重复元素,并返回新列表
    println(list.distinct)
    //丢弃前n个元素，并返回新列表
    println(list.drop(1))
    //丢弃最后n个元素，并返回新列表
    println(list.dropRight(1))
    //从左向右丢弃元素，直到条件p不成立
    println(list.dropWhile(_.equals("a")))
    //检测列表是否以指定序列结尾
    println(list.endsWith(Seq("a")))
    //判断是否相等
    println(list.head.equals("a"))
    //判断列表中指定条件的元素是否存在,判断l是否存在某个元素
    println(list.exists(x=> x == "a"))
    //输出符号指定条件的所有元素
    println(list.filter(x=> x.equals("a")))
    //检测所有元素
    println(list.forall(x=> x.startsWith("b")))
    //将函数应用到列表的所有元素
    list.foreach(println)
    //获取列表的第一个元素
    println(list.head)
    //从指定位置 from 开始查找元素第一次出现的位置
    println(list.indexOf("b",0))
    //返回所有元素，除了最后一个
    println(list.init)
    //计算多个集合的交集
    println(list.intersect(Seq("a","b")))
    //检测列表是否为空
    println(list.isEmpty)
    //创建一个新的迭代器来迭代元素
    val it = list.iterator
    while (it.hasNext){
      println(it.next())
    }
    //返回最后一个元素
    println(list.last)
    //在指定的位置 end 开始查找元素最后出现的位置
    println(list.lastIndexOf("b",1))
    //返回列表长度
    println(list.length)
    //通过给定的方法将所有元素重新计算
    list.map(x=> x+"jason").foreach(println)
    //查找最大元素
    println(list.max)
    //查找最小元素
    println(list.min)
    //列表所有元素作为字符串显示
    println(list.mkString)
    //使用分隔符将列表所有元素作为字符串显示
    println(list.mkString(","))
    //列表反转
    println(list.reverse)
    //列表排序
    println(list.sorted)
    //检测列表在指定位置是否包含指定序列
    println(list.startsWith(Seq("a"),1))
    //计算集合元素之和,这个地方必须是int类型,如果是string直接报错
    //println(list.sum)
    //返回所有元素，除了第一个
    println(list.tail)
    //提取列表的前n个元素
    println(list.take(2))
    //提取列表的后n个元素
    println(list.takeRight(1))
    //列表转换为数组
    println(list.toArray)
    //返回缓冲区，包含了列表的所有元素
    println(list.toBuffer)
    //List 转换为 Map
    val arr = Array(("jason", 24), ("jim", 25))
    arr.toMap.foreach(println)
    //List 转换为 Seq
    println(list.toSeq)
    //List 转换为 Set
    println(list.toSet)
    //列表转换为字符串
    println(list.toString())
  }
}