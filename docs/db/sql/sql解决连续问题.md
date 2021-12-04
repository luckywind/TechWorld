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
```

# 连续时间区间合并

[参考](https://stackoverflow.com/questions/15783315/combine-consecutive-date-ranges)

```sql
with cte as (
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

>  这个问题的连续定义是，一个区间的end_date+1=start_date。 可以直接判断是否可连接
>
> 和下面这个问题还有点不一样, 但给我们提供了一个思路：
> 能连则连，不能连则保留，最后按start_day合并即可；

```sql
create table tmp.asume
id,name,company_name,job_name, entry_time,leave_time
员工id,姓名,公司,职位,入职时间,离职时间。 null代表至今

中间无跳槽的时间区间需要合并，得出员工公司级别的履历
id,name,company_name,job_name,entry_time,leave_time



insert overwrite table tmp.asume
values 
(1, '张三', '华为','数据研发',20181011,20191029) ,
(1, '张三', '华为','后台研发',20191031,20201101) ,
(1, '张三', '阿里','数据研发',20201105,20211003) ,
(1, '张三', '华为','后台研发',20211004,null),
(2, '李四', '华为','数据研发',20181011,20191029) ,
(2, '李四', 'oppo','数据研发',20191101,null) 
;

结果： 前两段经历合并
(1, '张三', '华为',20181011,20201101),
(1, '张三', '阿里',20201105,20211003),
(1, '张三', '华为',20211004,null)
```

解决思路：

先给员工按entry_time递增拿到一个按时间有序的履历行号， 需求是连续的履历如果公司相同就合并，

即连续是指(id,公司)组合按时间排序后连续出现。这其实是sql中的连续出现问题。要找出连续出现并合并他们，sql中就要把他们放到一个组里，那么我们就要找连续出现的记录他们的共性是什么？不难发现分组内的行号与分组外的行号之间的diff是相同的，这就是共性。

本题其实是高一个维度的连续问题，分组(id,公司)内的行号与分组(id)内的行号之间的diff是相同的。



```sql
with a as (
select id,name,company_name,entry_time,leave_time,
    -- 同一个id的履历行号，一定要按id分组
  row_number()over(partition by id order by entry_time) as rn
from tmp.asume
)
,
b as (
select id,name,company_name,
entry_time,leave_time,
rn,
-- 计算(ID，company_name）组合内的行号  
row_number()over(partition by id,company_name order by entry_time) prn,
-- 计算diff,这就是共性，下面就可以利用这个共性进行聚合了  
rn-row_number()over(partition by id,company_name order by entry_time) as diff
from a )

select id,first_value(name),company_name,min(entry_time) as e_time,max(leave_time) as l_time
from b
group by id,company_name,diff
order by e_time;
```

