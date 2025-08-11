[参考](https://www.modb.pro/db/89612)

# 连续登陆N天的用户数

这里面有两个技巧，row_number()进行分组排序，然后date_sub(date,rn)会使得连续日期产生同一个日期，最后再筛选过滤一下即可。后面的过滤条件是大于等于N天

其实计算最大连续登陆日期是一样的，只不过不是过滤，是取最大

```sql
CREATE TABLE if not exists tmp.conn(
user_id int,
day int
);

insert overwrite table tmp.conn
values 
(1, 20191001),
(1, 20191002),
(1, 20191003),
(1, 20191004),
(2, 20191001);


select user_id
from(
select user_id,b.dt,count(1) as co
from(
select user_id,date_sub(cast(day as string),rn) as dt  --计算差分
    from(  
    select user_id,day,row_number() over(partition by user_id order by day) as rn
    from tmp.conn
    )a
)b
group by user_id,b.dt
having co>=3  
)c
order by user_id;




with user_log as (
select stack(5,
1, 20191001,
1, 20191002,
1, 20191003,
1, 20191004,
2, 20191001
             ) 
             as (uid,dt)
),
--连续登陆N天
a as (
SELECT 
uid,date_sub(cast(dt as string),rk) as diff
from (
SELECT uid, dt , row_number() over(partition by uid order by dt) as rk  
 from user_log )
)
SELECT uid,count(1) from a group by uid, diff having count(1)>=3
```

# 连续时间区间合并

> 这个语句有问题，暂时不关注

[参考](https://stackoverflow.com/questions/15783315/combine-consecutive-date-ranges)

```sql
with  Tbl as (
select stack(6,
 	5, '2007-12-03', '2011-08-26',
  5, '2013-05-02', null,
  30, '2006-10-02', '2011-01-16',
  30, '2011-01-17', '2012-08-12',
  30, '2012-08-13', null,
  66, '2007-09-24', null            
            )
  as(employmentid,startdate,enddate)
),
cte as (
  -- 没有前序可连接的先保留
   select a.employmentid, a.startdate, a.enddate
     from Tbl a
left join Tbl b on a.employmentid=b.employmentid and a.startdate-1=b.enddate
    where b.employmentid is null
    union all
  -- 把所有可连接的区间直接连接
   select a.employmentid, a.startdate, b.enddate
     from Tbl a
     join Tbl b on a.employmentid=b.employmentid and b.startdate-1=a.enddate
)
-- 相同起始的区间，enddate取最大即可
   select employmentid,
          startdate,
          nullif(max(nvl(enddate,'32121231')),'32121231') enddate
     from cte
 group by employmentid, startdate
 order by employmentid
```



| employmentid | startdate  | enddate    |      |
| ------------ | ---------- | ---------- | ---- |
| 5            | 2007-12-03 | 2011-08-26 |      |
| 5            | 2013-05-02 |            |      |
| 30           | 2006-10-02 | 2011-01-16 |      |
| 30           | 2011-01-17 | 2012-08-12 |      |
| 30           | 2012-08-13 |            |      |
| 66           | 2007-09-24 |            |      |

# 履历合并

```sql
create table tmp.asume
id,name,company_name,job_name, entry_time,leave_time
员工id,姓名,公司,职位,入职时间,离职时间。 null代表至今

中间无跳槽的时间区间需要合并，得出员工公司级别的履历
id,name,company_name,entry_time,leave_time



with t as (
select stack(6,
1, '张三', '华为','数据研发',20181011,20191029,
1, '张三', '华为','后台研发',20191031,20201101,
1, '张三', '阿里','数据研发',20201105,20211003,
1, '张三', '华为','后台研发',20211004,null,
2, '李四', '华为','数据研发',20181011,20191029,
2, '李四', 'oppo','数据研发',20191101,null
)
as(id,name,company_name,job_name,entry_time,leave_time)
)
select * from t

结果： 前两段经历合并
(1, '张三', '华为',20181011,20201101),
(1, '张三', '阿里',20201105,20211003),
(1, '张三', '华为',20211004,null)
```

解决思路：

先给员工按entry_time递增拿到一个按时间有序的履历行号， 需求是连续的履历如果公司相同就合并，

即连续是指(id,公司)组合按时间排序后连续出现。这其实是sql中的连续出现问题。要找出连续出现并合并他们，sql中就要把他们放到一个组里，那么我们就要找连续出现的记录他们的共性是什么？不难发现分组内的行号与分组外的行号之间的diff是相同的，这就是共性。

本题其实是高一个维度的连续问题，分组(id,公司)内的行号与分组(id)内的行号之间的diff是相同的。 group by diff 后取值就行了。



```sql
with t as (
select stack(6,
1, '张三', '华为','数据研发',20181011,20191029,
1, '张三', '华为','后台研发',20191031,20201101,
1, '张三', '阿里','数据研发',20201105,20211003,
1, '张三', '华为','后台研发',20211004,null,
2, '李四', '华为','数据研发',20181011,20191029,
2, '李四', 'oppo','数据研发',20191101,null
)
as(id,name,company_name,job_name,entry_time,leave_time)
),
a as (
    select *,
row_number() OVER (PARTITION by id,name,company_name order by entry_time) as `当前公司第几段履历`,
row_number() OVER (PARTITION by id order by entry_time) as `第几段履历`,
-- 在同一个公司的连续n段履历需要合并，即求min(entry_time),max(leave_time)。 他们的共性是什么？
-- 履历序号和公司内履历序号同步增长，意味着其diff是一样的
row_number() OVER (PARTITION by id order by entry_time)
-
row_number() OVER (PARTITION by id,name,company_name order by entry_time)  as `公司内连续履历id`
 from t order by id,  entry_time
)

SELECT id, name ,company_name, min(entry_time) `入职时间`,max(leave_time) `离职时间`
from a 
group by id,name, company_name,`公司内连续履历id`
order by  `入职时间`
```



| id   | name | company_name | 入职时间 | 离职时间 |      |
| ---- | ---- | ------------ | -------- | -------- | ---- |
| 1    | 张三 | 华为         | 20181011 | 20201101 |      |
| 2    | 李四 | 华为         | 20181011 | 20191029 |      |
| 2    | 李四 | oppo         | 20191101 |          |      |
| 1    | 张三 | 阿里         | 20201105 | 20211003 |      |
| 1    | 张三 | 华为         | 20211004 |          |      |

```sql
with t as (
select stack(6,
1, '张三', '华为','数据研发',20181011,20191029,
1, '张三', '华为','后台研发',20191031,20201101,
1, '张三', '阿里','数据研发',20201105,20211003,
1, '张三', '华为','后台研发',20211004,null,
2, '李四', '华为','数据研发',20181011,20191029,
2, '李四', 'oppo','数据研发',20191101,null
)
as(id,name,company_name,job_name,entry_time,leave_time)
),
a as (
select  id, name ,company_name,job_name,entry_time,leave_time,
row_number()over(partition by id order by entry_time) as  user_rk,
row_number()over(partition by id,company_name order by entry_time) as  user_comp_rk
from t ),
b as (SELECT *, user_comp_rk-user_rk as diff  from a)
select id, name, min(entry_time) as entry_time, max(leave_time) as leave_time from b  group by id,name,diff order by id;
```

