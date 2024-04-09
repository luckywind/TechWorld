# [UnsafeRow介绍](https://zhuanlan.zhihu.com/p/298203303)

## UnsafeRow介绍

Row的不安全实现，由原始内存而不是Java对象支持。

每个元组有三个部分:*[null bit set] [values] [variable length portion]*

- null bit set用于null跟踪，并对齐到8字节。每个字段存储一个比特。

- 在“values”区域，我们为每个字段存储一个8字节的单词。对于持有定长基本类型(如long、double或int)的字段，直接将值存储在字中。对于具有非原语值或变长值的字段，我们存储指向变长字段开头的相对偏移量(即行基址)和长度(它们组合成一个long)。' 

  

UnsafeRow 的实例充当指向以这种格式存储的行数据的指针。

## UnsafeRow内存布局

对于一段连续内存，Spark UnsafeRow是有一个分配规则的，默认实现下所有字段都是按照64bit（8byte）对齐的。以下面的一行为例子，一共有4列，首先前64bit都是用来表示null的，每一个bit表示每一列的值是否为null，然后每一列的value都用64bit来表示，对于long、double本身就占64位所以没问题，而int、float实际只需要32bit但这里为了对齐都用了64bit存储，因为使用了小端（little endian)存储因此前面32bit补0就可以了，然后字符串列的64bit分别用来存两个int值分别表示offset和size，真正的字符串内容在所有列后面根据长度顺延。

![image-20230504154203913](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230504154203913.png)

那么我们可以总结下Spark UnsafeRow内存布局的几个特点：

- 64bit（8byte）对齐，内存空间不紧凑但有利于提高访存性能
- 小端存储，这样低位类型存到高位内存（如存int到64位）不需要额外编码
- 所有列不管什么类型都按64bit存储，变长内容顺延存储

实际存储细节还有很多，这些也逐一介绍下，首先是nullbitset，每一个列都需要有一个bit来表示value是否为null，那么64bit只可以表示64列，如果超过64列就要考虑拓展nullbitset，实际算法也比较简单，如果64bit不够就加64bit，因为还要考虑64bit对齐，实现代码如下。

```java
public static int calculateBitSetWidthInBytes(int numFields) {
  return ((numFields + 63)/ 64) * 8;
}
```

​       对于int类型的列也是占用64bit，那么前32bit会先Unsafe.putLong把64位都值为0，然后再把int value写入，因为是小端存储所以会写到后32bit。

​      对于string类型的列，存的offset是指从nullbitset开始的偏移量，例如默认值是nullbitset加上列数乘以64bit，也就是上图的“initialized cursor”位置，每添加一个string列cursor就会往后移到队尾，通过这个offset和size（字符串长度）可以很容易读到完整的字符串内容。默认UnsafeRow不会申请无限长的内容，会创建一个有默认长度的byte array，添加变长的字符串内容有可能会超过整个byte array的长度，这时候需要提前检查并考虑扩充byte array创建一个新的byte array，这是就避免不了内存拷贝了，这个grow的过程也不复杂但也要考虑先按64bit（8bytes）对齐后在扩大。

## 属性

```scala
  private Object baseObject;
  private long baseOffset;

  /** The number of fields in this row, used for calculating the bitset width (and in assertions) */
  private int numFields;

  /** The size of this row's backing data, in bytes) */
  private int sizeInBytes;

  /** The width of the null tracking bit set, in bytes */
  private int bitSetWidthInBytes;
```

- 首先使用Java Unsafe API时访问的基础对象是一个Object，在UnsafeRow对象中就是baseObject，实际类型是byte[]，基于这个Object和offset就可以读到任意的列的值了。

- 然后是表示基础偏移的baseOffset，因为Object实际类型是byte[]因此这个值通过查JVM源码也可以找到实现方法“*UNSAFE*.arrayBaseOffset(byte[].class)“，在大部分JVM实现上应该都是16，这是Unsafe API的约定后面的offset都需要加上这个baseOffset。
- 然后numFields就表示这一行有多少列，有这个信息以后就可以算nullbitset的长度已经为每一列预留64bit基础空间了。
- 然后sizeInBytes表示这一行的总长度，这是包括字符串顺延的变长部分，因为分配空间都已经是64bit对齐了所以这里单位用byte也可以，记录这个信息方便后面grow的时候来保证按完整数据长度来拷贝。
- 最后的bitSetWidthInBytes就是用前面的简单算法来计算的nullbitset大小，单位是byte，默认是8byte并每次按需增加多个8byte。