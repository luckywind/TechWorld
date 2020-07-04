package base.atguigu

object InputDemo {
  def main(args: Array[String]): Unit = {
    val name: String = "tom"
    val age: Int = 10
    val sal: Double = 7899.144
    printf("name=%s age=%d sal=%.2f\n", name, age, sal)
    println(s"name=$name age=${age+1} sal=$sal sum2=${sum2(29, 30)}")
  }
  def sum2(n1: Int, n2: Int):Int={
     n1+n2;
  }

}
