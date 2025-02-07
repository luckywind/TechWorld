# 执行原生SQL

## raw

```python
lname = "Doe"
Person.objects.raw("SELECT * FROM myapp_person WHERE last_name = %s", [lname])
```

它会将查询语句中的字段映射至模型中的字段，但如果有多的字段，也会加到对象上去

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

1. 可以查询指定字段，但必须包含查询对象第一个主键，示例中是PipelineRelease，二非build_history表
2. 可以返回新字段
3. 可以传递变量，注意用中括号包起来





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
