[官网](https://spark.apache.org/docs/latest/ml-features.html#vectorassembler)

作用： 是一个transformer，把多个列转为一个向量，它对于将原始特征和不同特征转换器生成的特征组合成单个特征向量很有用。

为了训练 ML 模型，如逻辑回归和决策树。 VectorAssembler 接受以下输入列类型：所有数字类型、布尔类型和向量类型。在每一行中，输入列的值将按指定顺序连接成一个向量。

```scala
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.linalg.Vectors

val dataset = spark.createDataFrame(
  Seq((0, 18, 1.0, Vectors.dense(0.0, 10.0, 0.5), 1.0))
).toDF("id", "hour", "mobile", "userFeatures", "clicked")

val assembler = new VectorAssembler()
  .setInputCols(Array("hour", "mobile", "userFeatures"))
  .setOutputCol("features")

val output = assembler.transform(dataset)
println("Assembled columns 'hour', 'mobile', 'userFeatures' to vector column 'features'")
output.select("features", "clicked").show(false)
```

