package cookbook.oop

class Person(name:String,age:Int) {
  def canEqual(a:Any) = a.isInstanceOf[Person]

  override def equals(obj: Any): Boolean = obj match {
    case obj:Person =>obj.canEqual(this) && this.hashCode==obj.hashCode
    case _=>false
  }
  override def hashCode:Int = {
    val prime = 31
    var result = 1
    result = prime * result + age;
    result = prime * result + (if (name == null) 0 else name.hashCode)
    return result
  }
}

