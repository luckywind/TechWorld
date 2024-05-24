**Spark on Hive是spark做sql解析并转换成RDD执行，hive仅仅是做为外部数据源**

Spark SQL 对 SQL 查询语句先后进行语法解析、语法树构建、逻辑优化、物理优化、数据结构优化、以及执行代码优化，等等。然后Spark SQL 将优化过后的执行计划，交付给 Spark Core执行引。



“Hive on Spark” 指的是 Hive 采用 Spark 作为其后端的分布执行引擎。

**Hive on Spark 是由 Hive 的 Driver 来完成 SQL 语句的解析、规划与优化，还需要把执行计划“翻译”成 RDD 语义下的 DAG，然后再把 DAG 交付给 Spark Core执行。**【Spark on hive是由Spark SQL + Spark Core执行，性能更好】



[参考](https://juejin.cn/post/7200196102968344634)