# 日期函数

Date_format: date_format(date/timestamp/string ts, string fmt)

> - `date_format`函数如果第一个参数是字符串,连接符只能是`-`,别的识别不了

```sql
select date_format(regexp_replace('2015/04/08', '/', '-'), 'y');
--2015
select date_format(regexp_replace('2015/04/08', '/', '-'), 'y-MM');
--2015-04
```

# other

a join b using (公共字段)

