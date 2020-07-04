package base.atguigu

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}

import scala.collection.mutable.ArrayBuffer

/**
 * https://cloud.tencent.com/developer/article/1415747
 */
object SparseArrayDemo01 {
  def main(args: Array[String]): Unit = {
    val rowSize = 11;
    val colSize = 11;
    val chessMap: Array[Array[Int]] = Array.ofDim[Int](rowSize, colSize)
    chessMap(1)(2) = 1 //1黑子
    chessMap(2)(3) = 2 //2白子
    println("---------原始的棋盘地图")
    for (item1 <- chessMap){
      for(item2 <-item1){
        printf("%d\t",item2)
      }
      println()
    }

    /**
     * 将chessMap转成细数数组
     * 思路：效果是达到对数据的压缩
     */

    val sparseArray = new ArrayBuffer[Node]()
    val node = new Node(rowSize,colSize,0)
    sparseArray.append(node)  //数组的第一个位置存储棋盘坐标最大的点，这样可以知道维度
    for (i <- 0 until chessMap.length){
      for (j <- 0 until chessMap(i).length){
        //值不为0就存储
        if (chessMap(i)(j) !=0){
          val node = new Node(i,j,chessMap(i)(j))
          sparseArray.append(node)
        }
      }
    }

    println("-----------输出稀疏数组")
    for ( node <- sparseArray){
      printf("%d\t%d\t%d\n",node.row,node.col,node.value)
    }
    /**
     * 从稀疏数组恢复棋盘
     */
      val newnode: Node = sparseArray(0)
    val newrow: Int = newnode.row
    val newcol: Int = newnode.col
    val chessMap2: Array[Array[Int]] = Array.ofDim[Int](newrow,newcol)
    for (i<- 1 until sparseArray.length){
      val node: Node = sparseArray(i)
      chessMap2(node.row)(node.col) = node.value
    }
    println("------------恢复后的棋盘-------")
    for (item1 <- chessMap2){
      for(item2 <-item1){
        printf("%d\t",item2)
      }
      println()
    }

    //序列化到文件
//    serialize_file(sparseArray,"/Users/chengxingfu/code/my/studying/Learnscala/src/main/scala/base/atguigu/sparseArray.data")

    val deserialized_nodes: ArrayBuffer[Node] = deserialize_file[ArrayBuffer[Node]]("/Users/chengxingfu/code/my/studying/Learnscala/src/main/scala/base/atguigu/sparseArray.data")
    println("------------从文件恢复ArrayBuffer-------")
    for (node <- deserialized_nodes){
      printf("%d\t%d\t%d\n",node.row,node.col,node.value)
    }





  }


  //序列化（将对象传入，变成字节流）
  def serialize[T](o:T):Array[Byte]={
    val bos = new ByteArrayOutputStream()//内存输出流，和磁盘输出流从操作上讲是一样的
    val oos = new ObjectOutputStream(bos)
    oos.writeObject(o)
    oos.close()
    bos.toByteArray
  }
  //反序列化
  def deserialize[T](bytes:Array[Byte]):T={
    val bis=new ByteArrayInputStream(bytes)
    val ois=new ObjectInputStream(bis)
    ois.readObject.asInstanceOf[T]//进行类型转换，因为你要返回这个类型
  }

  //文件输出流
  def serialize_file[T](o:T,path:String)={
    val bos = new FileOutputStream(path)
    val oos = new ObjectOutputStream(bos)
    oos.writeObject(o)
    oos.close()
  }
  //文件输入流
  def deserialize_file[T](file:String):T={
    val bis=new FileInputStream(file)
    val ois=new ObjectInputStream(bis)
    ois.readObject.asInstanceOf[T]//进行类型转换，因为你要返回这个类型
  }
}
//序列化版本ID
@SerialVersionUID(1)
class Node(val row:Int,val col:Int,val value:Int) extends Serializable
