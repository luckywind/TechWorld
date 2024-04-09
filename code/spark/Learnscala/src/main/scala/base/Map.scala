package base
/**
 *  map常用操作
 */
object Map {

  def main(args: Array[String]): Unit = {

    //1.不可变map(有序)
    val map1 = scala.collection.immutable.Map("wyc"->30,"tele"->20,"yeye"->100,"haha"->1000);
    println(map1("wyc"));
    println("map1---" + map1);

    //error
    // map1("wyc")=100;



    //2. 可变map
    val map2 = scala.collection.mutable.Map("wyc"->30,"tele"->20,"yeye"->100);
    map2("wyc")=10000;
    println(map2("wyc"));


    val map3 = scala.collection.mutable.Map(("wyc",100),("tele",1000),("yeye",10000));
    println(map3("wyc"));


    //3.创建HashMap(无序)
    val hashMap = new scala.collection.mutable.HashMap[String,Int];


    //为map增加元素
    hashMap += ("wyc"->1);
    println(hashMap("wyc"))


    //移除元素
    hashMap -= "wyc";


    //检查key是否存在
    if(hashMap.contains("wyc")) println(hashMap("wyc")) else println(0);

    //使用getOrElse进行检查
    println(hashMap.getOrElse("wyc","不存在"));


    //根据key进行排序的SortedMap,注意是immutable
    val sortedMap = scala.collection.immutable.SortedMap("wyc"->100,"tele"->1000);
    println("sortedMap----" + sortedMap);


    //有序的LinkedHashMap
    val linkedHashMap = scala.collection.mutable.LinkedHashMap("wyc"->100,"tele"->1000);
    println("linkedHashMap----" + linkedHashMap);




    //对于不可变的map1进行更新,其实是返回新的不可变map
    val mapX = map1 + ("newEntry"->1);
    val mapY = map1 - "wyc";


    //遍历Map
    for((key,value)<- map3) {
      println(key + ":" + value);
    }

    for(key <- map3.keySet) {
      println(key + ":" + map3(key));
    }

    //只遍历values
    /*for(value<-map3.values) {
      println(value);
    }*/


    //反转key与value
    val reverseMap = for((key,value)<-map3) yield (value,key);
    for(key<- reverseMap.keySet) {
      println(key + ":" + reverseMap(key));
    }

  }


}