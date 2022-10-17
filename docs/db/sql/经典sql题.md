[参考](https://jiamaoxiang.top/2019/10/15/%E7%BB%8F%E5%85%B8Hive-SQL%E9%9D%A2%E8%AF%95%E9%A2%98/)

# 访问次数累计

```sql
要求使用SQL统计出每个用户的累积访问次数，如下表所示：
	用户id    月份  小计  累积
	u01 2017-01 11  11
	u01 2017-02 12  23
	u02 2017-01 12  12
	u03 2017-01 8   8
	u04 2017-01 3   3
```

```sql
with t as (
select stack(
9,
	'u01', '2017/1/21',5,
	'u02', '2017/1/23',6,
	'u03', '2017/1/22',8,
	'u04', '2017/1/20',3,
	'u01', '2017/1/23',6,
	'u01', '2017/2/21',8,
	'u02', '2017/1/23',6,
	'u01', '2017/2/22',4
)as (userid, visitDate, visitCount)
),
a as (
select userid,date_format(regexp_replace(visitdate,'/','-'),'yyyy-MM') as month,sum(visitCount) as cnt  from t 
group by userid, date_format(regexp_replace(visitdate,'/','-'),'yyyy-MM')
)

select 
userid,
month,
cnt  `小记`,
sum(cnt) over(partition by userid order by month) `累计`
from a
order by userid,month 
```

# 

```
有50W个京东店铺，每个顾客访客访问任何一个店铺的任何一个商品时都会产生一条访问日志，
访问日志存储的表名为Visit，访客的用户id为user_id，被访问的店铺名称为shop，数据如下：
				u1	a
				u2	b
				u1	b
请统计：
(1)每个店铺的UV（访客数）
(2)每个店铺访问次数top3的访客信息。输出店铺名称、访客id、访问次数
```

```sql

with t as (
select stack(
19,
'u1', 'a',
'u2', 'b',
'u1', 'b',
'u1', 'a',
'u3', 'c',
'u4', 'b',
'u1', 'a',
'u2', 'c',
'u5', 'b',
'u4', 'b',
'u6', 'c',
'u2', 'c',
'u1', 'b',
'u2', 'a',
'u2', 'a',
'u3', 'a',
'u5', 'a',
'u5', 'a',
'u5', 'a'
)as (user_id, shop)
),
a as ( -- 先计算客户访问次数
select shop,user_id,count(1) cnt from t 
group by shop,user_id
)
select 
shop,user_id,cnt
from (
	select 
	shop,
	user_id,
	row_number(cnt) over(partition by shop order by cnt desc) as rn, -- 每个店内访问次数排名
	cnt
	from a 
)
where rn<=3
```

# 

```
有一个5000万的用户文件(user_id，name，age)，
一个2亿记录的用户看电影的记录文件(user_id，url)， 计算不同年龄段观看电影的次数进行排序？
```

```sql

with user as (
select stack(
9,
'001','u1',10,
'002','u2',15,
'003','u3',15,
'004','u4',20,
'005','u5',25,
'006','u6',35,
'007','u7',40,
'008','u8',45,
'009','u9',50
)as (user_id, name, age)
),
log as (
select stack(
9,
'001','url1',
'002','url1',
'003','url2',
'004','url3',
'005','url3',
'006','url1',
'007','url5',
'008','url7',
'009','url5'
 )as (user_id,url)
),
a as (
select user_id, count(1) cnt from log group by user_id
)



select 
CASE WHEN age <= 10 AND age > 0 THEN '0-10' 
  WHEN age <= 20 AND age > 10 THEN '10-20'
  WHEN age >20 AND age <=30 THEN '20-30'
  WHEN age >30 AND age <=40 THEN '30-40'
  WHEN age >40 AND age <=50 THEN '40-50'
  WHEN age >50 AND age <=60 THEN '50-60'
  WHEN age >60 AND age <=70 THEN '60-70'
  ELSE '70以上' END as age_phase, 

sum(cnt) as `总观看次数`  

from 
user join a on user.user_id=a.user_id

group by 
CASE WHEN age <= 10 AND age > 0 THEN '0-10' 
  WHEN age <= 20 AND age > 10 THEN '10-20'
  WHEN age >20 AND age <=30 THEN '20-30'
  WHEN age >30 AND age <=40 THEN '30-40'
  WHEN age >40 AND age <=50 THEN '40-50'
  WHEN age >50 AND age <=60 THEN '50-60'
  WHEN age >60 AND age <=70 THEN '60-70'
  ELSE '70以上' END 
order by   `总观看次数`  
```

# 

```sql

with user as (
select stack(
11,
'2019-02-11','test_1','23',
'2019-02-11','test_2','19',
'2019-02-11','test_3','39',
'2019-02-11','test_1','23',
'2019-02-11','test_3','39',
'2019-02-11','test_1','23',
'2019-02-12','test_2','19',
'2019-02-13','test_1','23',
'2019-02-15','test_2','19',
'2019-02-16','test_2','19'
)as (date, name, age)
)
```

