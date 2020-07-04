package cookbook

class Person(var firstName: String, var lastName: String)
{
  println("the constructor begins")
  // some class fields
  private val HOME = System.getProperty("user.home")
  var age = 0
  // some methods
  override def toString = s"$firstName $lastName is $age years old"
  def printHome { println(s"HOME = $HOME") }
  def printFullName { println(this) } // uses toString
  printHome
  printFullName
  println("still in the constructor")
}
object Person{
  def main(args: Array[String]): Unit = {
    val p=new Person("aa","bb")
  }
}