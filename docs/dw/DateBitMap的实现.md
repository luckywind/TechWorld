![image-20210903143343329](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210903143343329.png)

# 移位运算

用到了移位运算：

| 符号 | 描述 | 运算规则                                                     |
| :--- | :--- | :----------------------------------------------------------- |
| &    | 与   | 两个位都为1时，结果才为1                                     |
| \|   | 或   | 两个位都为0时，结果才为0                                     |
| ^    | 异或 | 两个位相同为0，相异为1                                       |
| ~    | 取反 | 0变1，1变0                                                   |
| <<   | 左移 | 各二进位全部左移若干位，高位丢弃，低位补0 <br>若左移时舍弃的高位不包含1，则每左移一位，相当于该数乘以2。 |
| >>   | 右移 | 各二进位全部右移若干位，对无符号数，高位补0，有符号数，各编译器处理方法不一样，有的补符号位（算术右移），有的补0（逻辑右移）<br>**左补0 或者 左补1得看被移数是正还是负。<br>操作数每右移一位，相当于该数除以2** |

int与long的在移位操作时移位个数计算的不同：

> 对于int类型的整数移位a>>b，系统先用b对32求余，得到的结果才是真正移位的位数
>
> 对于long类型的整数移位，同上，不过是对64求余
>
> 对于1L<<32，实际移动位数32%64=32
>
> 对于1<<32，实际移动位数32%32=0

```java

    1L：0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0001
1L<<32：0000 0000 0000 0000 0000 0000 0000 0001 0000 0000 0000 0000 0000 0000 0000 0000 = 2^32 = 4294967296
 
1： 0000 0000 0000 0000 0000 0000 0000 0001
 1<<32：0000 0000 0000 0000 0000 0000 0000 0001 = 1
```



# 代码

```scala
class DateBitMap {

  var dates: java.util.SortedSet[Long] = new java.util.TreeSet[Long]()

  def getLastTimestamp: Long = dates.last

  def getFistTimestamp: Long = dates.head

  def getLogNum: Int = dates.size

  def getDict: java.util.Map[java.lang.String, java.lang.Long] = {
    val res: java.util.Map[java.lang.String, java.lang.Long] = new util.HashMap[java.lang.String, java.lang.Long]()
    val t = toJson
    t.keys.foreach {
      x =>
        res.put(x.toString, t.getLong(x.toString))
    }
    res
  }

  def add(x: DateTime): DateBitMap = {
    val y = DateUtils.toDayZeroClock(x)
    dates.add(y.getMillis)
    this
  }

  def add(x: Long): DateBitMap = {
    val y = DateUtils.toDayZeroClock(x)
    dates.add(y)
    this
  }

  def merge(other: DateBitMap): DateBitMap = {
    other.dates.foreach {
      x =>
        dates.add(x)
    }
    this
  }

  def shrink(days: Int = 300): DateBitMap = {
    if (days >= 0) {
      val s = dates.size - math.min(dates.size, days)
      val res = new java.util.TreeSet[Long]()
      var index = 0
      dates.foreach {
        x =>
          if (index >= s) {
            res.add(x)
          }
          index += 1
      }
      dates = res
    }
    this
  }

  def toJson: JSONObject = {
    val obj = new JSONObject()
    var curMonth: String = ""
    var curBitMap: Long = 0L
    dates.foreach {
      x =>
        val t = new DateTime(x)
        val month = t.toString(DateBitMap.DATE_FORMAT)
        if (month != curMonth) {
          if (curMonth != "") {
            obj.put(curMonth, curBitMap)
          }
          curMonth = month
          curBitMap = 0L
        }
        curBitMap |= (1L << (t.getDayOfMonth - 1))
    }
    if (curMonth != "") {
      obj.put(curMonth, curBitMap)
    }
    obj
  }

  override def toString: String = {
    toJson.toString
  }
}

object DateBitMap {

  final val DATE_FORMAT: String = "yyyyMM"

  def apply(json: String): DateBitMap = {
    val obj = new JSONObject(json)
    val res = new DateBitMap()
    obj.keys.foreach {
      x =>
        val month = DateTimeFormat.forPattern(DATE_FORMAT).parseDateTime(x.toString)
        val bits: Long = obj.getLong(x.toString)
        for (i <- 0 until 31) {
          val t: Long = 1L << i
          if ((t & bits) == t) {
            res.add(month.plusDays(i))
          }
        }
    }
    res
  }

  def apply(dict: java.util.Map[java.lang.String, java.lang.Long]): DateBitMap = {
    val res = new DateBitMap()
    dict.foreach {
      x =>
        val key = x._1
        val value = x._2
        val month = DateTimeFormat.forPattern(DATE_FORMAT).parseDateTime(key.substring(0,6))//key 正常是201907这种形式，但是旧的tv_mid边数据time_dist的key有错误
        val bits: Long = value
        for (i <- 0 until 31) {
          val t: Long = 1L << i
          if ((t & bits) == t) {
            res.add(month.plusDays(i))
          }
        }
    }
    res
  }

  /**含头尾*/
  def allDaysBetween(startDay:DateTime, endDay:DateTime):DateBitMap={
    val bitMap = new DateBitMap()
    var curDay=startDay
    while(curDay.isBefore(endDay.plusDays(1))){
      bitMap.add(curDay)
      curDay=curDay.plusDays(1)
    }
    bitMap
  }
}
```

## 代码解释

整个BitMap类只维护一个有序Set数据结构即可，即dates。

1. dates里的时间戳都是当天归零后的，一天最多有一个

2. 因为从右边数二进制的1的位置是当月活跃日，所以我们想办法把二进制的1向左移动(DayofMonth-1)位。

   那为什么要用Long型的1呢？因为活跃日可能是第31天，这个1要向左移30位，则二进制数就有31位了，这个数是2^31 已经超过int类的最大值2^31-1了，所以必须用Long。

   > 用Long型，计算机系统在计算实际的移位数时，是先对64取模的，而int是先对32取模

3. Shrink 实际是保留最近N天的活跃情况

4. map<month, Long>如何逆向构造BitMap呢？依次拿1向左移0-30位得到的二进制数与map中的Long值进行与操作，如果与的结果仍然是这个二进制数，说明二进制数表示的是一个活跃日，把这个日期加到数据结构dates里即可。这样就可以知道1号到31号哪天活跃哪天没活跃了。
