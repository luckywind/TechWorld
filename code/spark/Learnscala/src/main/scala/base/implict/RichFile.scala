package base.implict

import scala.io.Source

class RichFile(val file:String) {
  def read():String={
    Source.fromFile(file).mkString
  }
}
object RichFile{
  def main(args: Array[String]): Unit = {
    val file="/Users/chengxingfu/code/my/studying/Learnscala/src/main/scala/base/implict/RichFile.scala"
    val str = new RichFile(file).read()
//    println(str)
    import MyPredef.fileToRichFile
    val content = file.read()
    println(content)
  }
}
