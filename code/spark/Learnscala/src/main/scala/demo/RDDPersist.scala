package demo

import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkConf, SparkContext}

object RDDPersist {
  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf().setMaster("local[*]").setAppName("RDDPersist")
    val sc = new SparkContext(sparkConf)
    sc.setCheckpointDir("checkpoint")
    var someList = List[String]("hello","world")
    val defaultPartition = 5;
    val wordsRDD = sc.parallelize(someList, defaultPartition)
    val someThingLess = wordsRDD.map(x => x+",").map(x=>((Math.floor(Math.random()*100)) ,x)).filter(x => x._1 <50)
    someThingLess.count()
    someThingLess.groupByKey().count()
    wordsRDD.repartition(defaultPartition*2);

    wordsRDD.coalesce(defaultPartition ,false)

    wordsRDD.cache()

    wordsRDD.persist(StorageLevel.MEMORY_ONLY);

    /*

StorageLevel.DISK_ONLY

StorageLevel.DISK_ONLY_2

StorageLevel.MEMORY_ONLY

StorageLevel.MEMORY_ONLY_2

StorageLevel.MEMORY_ONLY_SER

StorageLevel.MEMORY_ONLY_SER_2

StorageLevel.MEMORY_AND_DISK

StorageLevel.MEMORY_AND_DISK_2

StorageLevel.MEMORY_AND_DISK_SER

StorageLevel.MEMORY_AND_DISK_SER_2
cache()方法很好理解，就是全都放在内存里面，也就是MEMORY_ONLY。persist方法的默认参数也是仅内存。
MEMORY_AND_DISK的意思就是，内存能放得下就放内存，放不下就放硬盘。
如何理解这几个参数的搭配呢？从三个要素来搭配就行了，存储位置、存储份数，是否序列化。三个的所有搭配都在上面的。

*/

    println(wordsRDD.count())

//    wordsRDD.unpersist();//无论是什么方式，都可以使用unpersist这个API进行删除所有的持久化。

    wordsRDD.checkpoint();

    Thread.sleep(1000*1000)

  }

}
