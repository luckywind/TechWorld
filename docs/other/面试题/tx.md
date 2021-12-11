1. Flink  vs SparkStreaming
2. 反转链表

```java
linkedList reverse(head:LinkedList){
if(head==null || head.next==null) return head
else{
newhead=reverse(head.next)
head.next.next=head
head.next=null
return newhead
}
}

```



非递归版

```scala
```



1. 设计：实时统计top10的IP



