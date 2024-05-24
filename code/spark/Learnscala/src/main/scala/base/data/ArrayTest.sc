val a = Array.ofDim[String](3, 2)
val b = Array(Array("a", "b", "c"), Array("d", "e"))
for (elem<-b) {
 for (e <-elem) {
   println(e)
 }
}
import scala.collection.mutable.ArrayBuffer
val mutableArr =ArrayBuffer[Int]()
new ArrayBuffer[Int]()
