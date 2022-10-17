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
  row_number() over() as id
from  
  (select split(space(99), ' ') as x) t
lateral view
explode(x) ex;
```

