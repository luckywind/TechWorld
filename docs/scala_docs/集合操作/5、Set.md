# Set

1. Set和Java的Set类似，是包含不重复的元素集合，不重复是使用==判断的。
2. 添加重复元素会被忽略
3. 有可变和不可变版本，以及有序版本Set

## 添加元素

### 可变set

```scala
scala> var set = scala.collection.mutable.Set[Int]()
set: scala.collection.mutable.Set[Int] = HashSet()

scala> set += 1
res56: scala.collection.mutable.Set[Int] = HashSet(1)

scala> set ++= Vector(4, 5)
res57: scala.collection.mutable.Set[Int] = HashSet(1, 4, 5)

scala> set.add(6)
res58: Boolean = true

scala> set.contains(5)
res59: Boolean = true

scala> var set = scala.collection.mutable.Set(1, 2, 3)
set: scala.collection.mutable.Set[Int] = HashSet(1, 2, 3)
```

### 不可变Set

```scala
scala> val s1 = Set(1, 2)
s1: scala.collection.immutable.Set[Int] = Set(1, 2)

scala> val s2 = s1 + 3
s2: scala.collection.immutable.Set[Int] = Set(1, 2, 3)


scala> val s3 = s2 ++ List(6,7)
s3: scala.collection.immutable.Set[Int] = HashSet(1, 6, 2, 7, 3)
```

## 删除元素

### 可变Set

```scala
scala> var set = scala.collection.mutable.Set(1, 2, 3, 4, 5)
set: scala.collection.mutable.Set[Int] = HashSet(1, 2, 3, 4, 5)

scala> set -= (2, 3)
res60: scala.collection.mutable.Set[Int] = HashSet(1, 4, 5)

scala> set --= Array(4,5)
res61: scala.collection.mutable.Set[Int] = HashSet(1)

scala> set ++=Array(1,2,3,4)
res62: scala.collection.mutable.Set[Int] = HashSet(1, 2, 3, 4)

scala> set.filterInPlace(_>2) //过滤
res65: scala.collection.mutable.Set[Int] = HashSet(3, 4)

scala> set.remove(3)
res66: Boolean = true

scala> set.clear

scala> set
res68: scala.collection.mutable.Set[Int] = HashSet()
```

不可变set操作类似

## 可排序集合

```scala
//SortedSet保留大小顺序
scala> val s = scala.collection.SortedSet(10, 4, 8, 2)
s: scala.collection.SortedSet[Int] = TreeSet(2, 4, 8, 10)

scala> val s = scala.collection.SortedSet("cherry", "kiwi", "apple")
s: scala.collection.SortedSet[String] = TreeSet(apple, cherry, kiwi)
//LinkedHashSet保留插入顺序
scala>  var s = scala.collection.mutable.LinkedHashSet(10, 4, 8, 2)
s: scala.collection.mutable.LinkedHashSet[Int] = LinkedHashSet(10, 4, 8, 2)
```

![公众号二维码](5、Set.assets/公众号二维码.jpg)

