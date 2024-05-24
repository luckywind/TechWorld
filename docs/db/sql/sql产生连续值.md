# 利用space和posexplode

```sql
select
id_start+pos as id
from(
    select
    1 as id_start,
    100 as id_end
) m  lateral view posexplode(split(space(id_end-id_start), '')) t as pos, val
```

# 利用lateral view

```sql
select
  row_number() over(order by x) as id
from  
  (select split(space(99), ' ') as x) t
lateral view
explode(x) ex;
```







```sql
with t as (select
  row_number() over(order by x) as id,
   cast(rand() * 3 +2 as int) as r
from  
  (select split(space(99), ' ') as x) t
lateral view
explode(x) ex )
select 
case when r=0 then '0'
when r=1 then '1'
when r=2 then '2'
when r=3 then '3'
else '99' end as prod_catalog_cd,
date_format(date_sub(date('2023-05-09'),r),'yyyyMMdd') p_day_id,
r+10 as p_plan_id
from t;
 

```

