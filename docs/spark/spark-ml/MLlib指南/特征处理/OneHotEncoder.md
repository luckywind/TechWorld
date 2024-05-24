[官网](https://spark.apache.org/docs/latest/api/scala/org/apache/spark/ml/feature/OneHotEncoder.html)

作用：把类别索引映射为二进制向量列， 这个向量最多只有一个1来标识索引。 例如5个类别，索引2将映射为一个向量[0.0, 0.0, 1.0, 0.0].

最后一个索引默认丢掉(configurable via `dropLast`)，因为它保证了向量和为1，因此是线性独立，这样，如果输入是4，则输出是[0.0, 0.0, 0.0, 0.0]







CountVectorizerModel, [参考](https://stackoverflow.com/questions/40302188/create-one-hot-encoded-vector-from-category-list-in-spark)