package base.implict

object MyPredef {
  implicit def fileToRichFile(file:String) = new RichFile(file)
  implicit val selectGirl = (g:Girl) => new Ordered[Girl]{
    override def compare(that: Girl): Int = {
        g.faceValue-that.faceValue
    }
  }

  /**
   * 比较规则
   */
  implicit object OrderingGirl extends Ordering[Girl]{
    override def compare(x: Girl, y: Girl): Int = {
      x.faceValue-y.faceValue
    }
  }
}
