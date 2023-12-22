[youtube](https://www.youtube.com/watch?v=rpKjcMoega0&t=1309s)

# [失败重试机制](https://www.codenong.com/cs106931464/)

1. 普通task失败，会计算失败次数，达到maxTaskFailures将终止stage和job
2. Fetch Failure不会计算失败次数，而是计算stage失败次数，达到maxStageFailures则终止job

stage重试： 节点失败

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20211204155728792.png" alt="image-20211204155728792" style="zoom:50%;" />

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20211204155751613.png" alt="image-20211204155751613" style="zoom:50%;" />

# 代码运行在driver还是executor

所有对RDD内部具体数据的操作执行都是在executor上进行的，所有对rdd自身的操作都是在driver上执行的。

[参考](https://cloud.tencent.com/developer/article/1545723)

下面这段代码是对rdd操作，没有对rdd内部的数据操作，所以执行在driver端

```scala
public static void main(String[] args){
 Map<String,int> hashmap = new HashMap();
 dstream.foreachrdd(rdd=>{//操作rdd自身
   String today = getToday();
   if(hashmap.containsKey(today )){
      hashmap.put(today ,hashmap.get(today )+1);
   }else{
      hashmap.put(today,1);
   }
 })
 System.out.println(hashmap.size())
 }
```

下面是对rdd内部的数据操作，所以执行在executor端

```scala
public static void main(String[] args){
 Map<String,int> hashmap = new HashMap();
 dstream.foreachrdd(rdd=>{
   rdd.map(each=>{   //操作rdd内部的数据
     if(hashmap.containsKey(each)){
        hashmap.put(each,hashmap.get(each)+1);
     }else{
        hashmap.put(each,1);
     }
   })
 })
 System.out.println(hashmap.size())
 
```

