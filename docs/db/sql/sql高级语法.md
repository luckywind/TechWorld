# ON DUPLICATE KEY UPDATE

[参考](http://blog.sae.sina.com.cn/archives/3491)

如果您指定了ON DUPLICATE KEY UPDATE，并且插入行后会导致在一个UNIQUE索引或PRIMARY KEY中出现重复值，则执行旧行UPDATE。也就是这个语句是基于唯一索引或者主键使用的

例如，如果列a被定义为UNIQUE，并且包含值1，则以下两个语句具有相同的效果：

```sql
INSERT INTO `table` (`a`, `b`, `c`) VALUES (1, 2, 3) 
ON DUPLICATE KEY UPDATE `c`=`c`+1; 

UPDATE `table` SET `c`=`c`+1 WHERE `a`=1;
```

如果行作为新记录被插入，则受影响行的值为1；如果原有的记录被更新，则受影响行的值为2。

如果列b也是唯一列，则INSERT与此UPDATE语句相当：

```sql
UPDATE `table` SET `c`=`c`+1 WHERE `a`=1 OR `b`=2 LIMIT 1;
```

如果a=1 OR b=2与多个行向匹配，则只有一个行被更新。通常，您应该尽量避免对带有多个唯一关键字的表使用ON DUPLICATE KEY子句。

# 技巧

## limit offset

```sql
limit  m,n 从m行开始读n条数据
① select * from table limit 2,1;                 
//含义是跳过2条取出1条数据，limit后面是从第2条开始读，读取1条信息，即读取第3条数据


limit m offset n; 跳过n条读m条数据
② select * from table limit 2 offset 1;      
//含义是从第1条（不包括）数据开始取出2条数据，limit后面跟的是2条数据，offset后面是从第1条开始读取，即读取第2,3条
```

## ifnull

ifnull(a,b)函数解释：

如果value1不是空，结果返回a

如果value1是空，结果返回b

[第二高的薪水](https://leetcode-cn.com/problems/second-highest-salary/)

```sql
select ifnull(
(select distinct Salary  from Employee order by Salary desc limit 1 offset 1), null)
 as SecondHighestSalary;
```

## 连续出现n次

```sql
select distinct Num as ConsecutiveNums from 

(select Num, orderedId - row_number() over(partition by Num) as id_diff from 
  (select row_number() over () as orderedId,Num from Logs)a
)b 

group by Num,id_diff
having count(1)>=3;
```

