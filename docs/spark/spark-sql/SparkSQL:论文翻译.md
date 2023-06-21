[paper翻译](https://changbo.tech/blog/8973d72a.html)

Spark SQL通过两方面努力，架起了两种模型之间的桥梁。首先，Spark SQL提供了一个*DataFrame API*，可以在任何外部数据源和Spark内置分布式集合上执行关系型操作。这个API类似R[32]中被广泛使用的data frame的概念，但是会延迟计算操作，以便它可以执行关系优化。其次，**为了支持大量数据源和大数据算法，Spark SQL引入了一个新式的可扩展优化器，叫做Catalyst**。Catalyst可以使得Spark SQL更容易添加数据源，优化规则以及领域内数据类型，如机器学习。

