# Argmin/argmax

hive按照字段顺序比较struct，例如：{a:1,b:2} < {a:2,b:1}。 

因此，取y列最小的x的方法是： min(named_struct('y', y, 'x', x)).x 

```sql
with test as (
select 1 a, 3 b
union all 
select 1 a, 2 b  
union all 
select 2 a ,1 b
)
-- select min(named_struct('b', b, 'a', a)).a from test; --2
select min(named_struct( 'a', a, 'b', b)).b from test; --1
```



