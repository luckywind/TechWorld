# np

```python
np.unique(normalized_kmeans.labels_, return_counts=True)  #分组统计
```

# pandas

## sql

[参考](https://towardsdatascience.com/query-pandas-dataframe-with-sql-2bb7a509793d)

**语法**： sqldf(sql,  globals()/locals() )

> 其中sql可以直接拿dataframe作为表使用
>
> globals()/locals() 是python内置函数和当前session变量，例如，当前创建的dataframe

```python
也支持占位符

COL_NAME = '"sepal length (cm)"'
ALIAS = 'sepal_length'
AGG = 'MAX'query = f"SELECT {AGG}({COL_NAME}) AS {ALIAS} FROM df"
pysqldf(query)
```

改造：

```python
新建一个函数， 用它代替sqldf可以避免每次传global/locals
pysqldf = lambda q: sqldf(q, globals())
使用
query = 'SELECT * FROM df_feature LIMIT 3'
pysqldf(query)
```





```sql
!pip install -U pandasql
from pandasql import sqldf
import pandas as pd
from sklearn import datasets
df_feature = datasets.load_iris(as_frame = True)['data']
df_target = datasets.load_iris(as_frame = True)['target']

q = "SELECT * FROM df_target LIMIT 3"
sqldf(q, globals())
```

