# map

## 基本操作

1. 创建map

```sql
map('年龄',23,'性别',1)
```

2. 查询map的key/value集合(Array)

```sql
create table temp_db.map_test(
   id int comment "源数据主键id"
  ,smap map<string,string> comment "string型map"
  ,imap map<string,int> comment "int型map"
  );
  
select map_keys(smap) from temp_db.map_test;
select map_values(smap) from temp_db.map_test;
```

3. map大小

```sql
size(imap)
```

## map数据加工

将map列拆分为key、value列

```sql
-- smap中只存在单个key-value的情况，所以lateral之后，数据有单列变成双列。但是行数没有变化
select id,skey,svalue
from temp_db.map_test
lateral view explode(smap) tb as skey,svalue;

-- imap中 存在多个键值对。这顿操作之后，行数会增加
select id,ikey,ivalue
from temp_db.map_test
lateral view explode(imap) tb as ikey,ivalue;
```

# Array

## 基本操作

1. 创建

```sql
create table temp_db.array_test
(
 id int comment '源数据主键id'
,year_arr array<string> comment '数组记录，年份'
,score_arr array<string> comment '数组记录，分数'
);
```

2. 插入

```sql
insert into  temp_db.array_test (id,year_arr,score_arr)
select 12,array('1991','1990','1989'),array('56','20','23')
;
```

## 加工

1. 包含

```sql
array_contains(year_arr,'1990')
```

2. 拆成多行

```sql
select col1
from temp_db.array_test
lateral view explode(year_arr) tb as col1
```

