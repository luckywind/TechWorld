# 执行原生SQL

## raw

它会将查询语句中的字段映射至模型中的字段，但如果有多的字段，也会加到对象上去:

```python
import os
import django
os.environ['DJANGO_SETTINGS_MODULE'] = 'hados_vmp.settings'
django.setup()
from pipeline.pipelineModels import BuildHistory, PipelineRelease

if __name__ == '__main__':

    for b in PipelineRelease.objects\
       .raw("select pipeline_id,status as newfield  from build_history limit %s",[10]):
        print(b.newfield)
        print(b.__dict__)
```

1. 可以查询指定字段，但必须包含**查询对象(未必是表)第一个主键**，示例中是PipelineRelease的pipeline_id，而不是build_history表

2. 可以返回新字段，例如newfield并不是查询对象PipelineRelease的属性

3. 可以传递变量，注意用中括号包起来

4. 在导入模型前一定要设置`os.environ['DJANGO_SETTINGS_MODULE'] = 'hados_vmp.settings.dev'`

5. 查询结果直接转list(效果类似cusor.fetchall())

   ```python
   # Using a RawQuerySet that returns model instances
   raw_qs = MyModel.objects.raw("SELECT id, field1, field2 FROM my_app_mymodel")
   tuple_list = [(obj.id, obj.field1, obj.field2) for obj in raw_qs]
   ```

   



## connection.cursor()

完全绕过模型层的需求，对象 `django.db.connection` 代表默认数据库连接。要使用这个数据库连接，调用 `connection.cursor()` 来获取一个指针对象。然后，调用 `cursor.execute(sql, [params])` 来执行该 SQL 和 `cursor.fetchone()`，或 `cursor.fetchall()` 获取结果数据。

```sql
from django.db import connection


def my_custom_sql(self):
    with connection.cursor() as cursor:
        cursor.execute("UPDATE bar SET foo = 1 WHERE baz = %s", [self.baz])
        cursor.execute("SELECT foo FROM bar WHERE baz = %s", [self.baz])
        row = cursor.fetchone()

    return row
```

如果是在调试时使用脚本使用该连接，应该设置一个环境变量：

```python
    os.environ['DJANGO_SETTINGS_MODULE'] = 'hados_vmp.settings'
```

### 游标cursor的使用

```python
with connection.cursor() as cursor:
    cursor.execute("SELECT * FROM my_table")
    results = cursor.fetchall()

cursor.fetchone()  取一条
cursor.fetchall()  取所有
cursor.fetchmany(size=5) 取多条    
```



获取数据量

```python
#  方式一：
count_query = 'SELECT COUNT(*) FROM your_app_yourmodel'
with connection.cursor() as cursor:
    cursor.execute(count_query)
    # Fetch the result of the query
    result = cursor.fetchone()
    # The result of COUNT(*) is a single value in the first element of the tuple
    count = result[0]
    print(f"The number of records in the table is: {count}")
# 方式二    
select_query = 'SELECT * FROM your_app_yourmodel WHERE some_column = %s'
condition_value = 'some_value'
with connection.cursor() as cursor:
    cursor.execute(select_query, [condition_value])
    # Fetch all the results
    results = cursor.fetchall()
    count = len(results)    
```





## 多数据库

```python
from django.db import connections
with connections['my_db_alias'].cursor() as cursor:
    pass
```





# 错误

```python
import os
import django
os.environ['DJANGO_SETTINGS_MODULE'] = 'hados_vmp.settings'
django.setup()
from pipeline.pipelineModels import BuildHistory

if __name__ == '__main__':

    for b in BuildHistory.objects.raw("select * from pipeline_release limit 10"):
        print(b)
        print(b.__dict__)
```

## DJANGO_SETTINGS_MODULE

`os.environ['DJANGO_SETTINGS_MODULE'] = 'myproject.settings'` 需要放到import前面。即使是子目录中也可以使用

也可以参考](https://blog.csdn.net/ZT7524/article/details/89916879)在IDE运行参数中添加环境变量

## Apps aren't loaded yet

报错语句前面加`django.setup()`
