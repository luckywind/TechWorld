构造方法:

```scala
  /**
   * Creates a sparse vector providing its index array and value array.
   *
   * @param size vector size.
   * @param indices index array, must be strictly increasing.
   * @param values value array, must have the same length as indices.
   */
  @Since("2.0.0")
  def sparse(size: Int, indices: Array[Int], values: Array[Double]): Vector =
    new SparseVector(size, indices, values)
```

1. 索引数组必须递增
2. 值数组长度和索引数组一样