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

# Vectors

```scala
def dense(values: Array[Double]): Vector
Creates a dense vector from a double array.

def dense(firstValue: Double, otherValues: Double*): Vector
Creates a dense vector from its values.

def fromJson(json: String): Vector
Parses the JSON representation of a vector into a Vector.

def fromML(v: ml.linalg.Vector): Vector
Convert new linalg type to spark.mllib type.

def norm(vector: Vector, p: Double): Double
// 计算p-范数

def parse(s: String): Vector
// 解析Vector.toString 

def sparse(size: Int, elements: Iterable[(Integer, Double)]): Vector
// 解析(index, value) map

def sparse(size: Int, elements: Seq[(Int, Double)]): Vector
Creates a sparse vector using unordered (index, value) pairs.

def sparse(size: Int, indices: Array[Int], values: Array[Double]): Vector
Creates a sparse vector providing its index array and value array.

def sqdist(v1: Vector, v2: Vector): Double
Returns the squared distance between two Vectors.

def zeros(size: Int): Vector
Creates a vector of all zeros.
```

# Vector

两个实现类：[DenseVector](https://spark.apache.org/docs/3.2.1/api/java/org/apache/spark/mllib/linalg/DenseVector.html), [SparseVector](https://spark.apache.org/docs/3.2.1/api/java/org/apache/spark/mllib/linalg/SparseVector.html)

索引类型为Int, 值类型为Double的向量

```scala
double[]	toArray()
//Converts the instance to a double array.
DenseVector	toDense()
//Converts this vector to a dense vector.
String	toJson()
//Converts the vector to a JSON string.
SparseVector	toSparse()
//Converts this vector to a sparse vector with all explicit zeros removed.
```

