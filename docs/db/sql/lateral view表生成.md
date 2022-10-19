# 参考

[Hive之常用内置函数二 ](https://www.cnblogs.com/tashanzhishi/p/10904144.html)

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

## collect_list

非聚合列值，组装list。通常配合concat_ws函数使用

例如：concat_ws(',', collect_list(job_name))

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

# explode

explode在select句中和在from子句中给虚拟字段命名的格式稍微有些差别，select句中需要加括号，from子句中不需要括号。



# lateral View 表生成函数



这应该是hive独有的一行变多行的语法,[参考](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+LateralView)

语法：

```sql
lateralView: LATERAL VIEW  [ OUTER ]  udtf(expression) tableAlias AS columnAlias (',' columnAlias)*
fromClause: FROM baseTable (lateralView)*
两个使用地方：
1. udtf前面
2. from baseTable后面用
```





lateral view是Hive中提供给UDTF的结合，它可以解决UDTF不能添加额外的select列的问题。

lateral view其实就是用来和想类似explode这种UDTF函数联用的，lateral view会将UDTF生成的结果放到一个虚拟表中，然后这个虚拟表会和输入行进行join来达到连接UDTF外的select字段的目的。

**格式一:  用在udtf函数之前**

```sql
lateral view udtf(expression) tableAlias as columnAlias (,columnAlias)*
```

> as后面是虚拟表的列名，(,columnAlias)* 说明可以有多个列

- lateral view在UDTF前使用，表示连接UDTF所分裂的字段。
- UDTF(expression)：使用的UDTF函数，例如explode()。
- tableAlias：表示UDTF函数转换的虚拟表的名称。
- columnAlias：表示虚拟表的虚拟字段名称，如果分裂之后有一个列，则写一个即可；如果分裂之后有多个列，按照列的顺序在括号中声明所有虚拟列名，以逗号隔开。

**格式二： 用在from 语句里**

```sql
from basetable (lateral view)*
```

- 在from子句中使用，一般和格式一搭配使用，这个格式只是说明了lateral view的使用位置。
- <font color=red>from子句后面也可以跟多个lateral view语句，使用空格间隔就可以了。会依次执行</font>

**格式三**

```sql
from basetable (lateral view outer)*
```

它比格式二只是多了一个outer，这个outer的作用是在UDTF转换列的时候将其中的空也给展示出来，UDTF默认是忽略输出空的，加上outer之后，会将空也输出，显示为NULL。这个功能是在Hive0.12是开始支持的。









## 追加一列

```sql
with tmp2 as (
    select stack(3,
             0,'height',11,
             1,'weight',33,
             2,'age',18   ) 
             as (id,label,value)
)
select id,rd,label,value from tmp2
            lateral view explode(split('0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9',',')) myTable as  rd
```

侧视图(Lateral view) 主要与表生成函数联合使用，例如explode（生成的是一张表）。UDTF通常把一行变多行，侧视图首先把udtf应用到基础表的每一行，然后把结果和输入行进行join形成一个虚拟表(这还可以达到连接udtf之外的select 字段的目的，实际上新版本udtf已经支持select其他字段了，主要作用还是把一行炸开)，可提供别名。

```sql
insert overwrite table dwm.lateral_explode_tmp
values
("front_page",split("1,2,3",",")),
("contact_page",split("3,4,5",","));

新版本支持select其他字段了
SELECT pageid,explode(adid_list)
FROM dwm.lateral_explode_tmp;
|    pageid     | col  |
+---------------+------+--+
| contact_page  | 3    |
| contact_page  | 4    |
| contact_page  | 5    |
| front_page    | 1    |
| front_page    | 2    |
| front_page    | 3    |


变成多行,explode把一列展开，其他字段复制,⚠️，被展开的字段不再可用，且空值记录会丢失(可用nvl把空值替换为一个特殊值或者使用下面的outer关键字保留)
SELECT pageid, adid
FROM dwm.lateral_explode_tmp a LATERAL VIEW explode(adid_list)  AS adid;

SELECT pageid, explode(adid_list)
FROM dwm.lateral_explode_tmp;
|    pageid     | col  |
+---------------+------+--+
| contact_page  | 3    |
| contact_page  | 4    |
| contact_page  | 5    |
| front_page    | 1    |
| front_page    | 2    |
| front_page    | 3    |
SELECT pageid,b.*, adid
FROM dwm.lateral_explode_tmp a LATERAL VIEW explode(adid_list) b AS adid;
-- ⚠️这里explode后的表名b可省略
+---------------+-------+-------+--+
|    pageid     | adid  | adid  |
+---------------+-------+-------+--+
| contact_page  | 3     | 3     |
| contact_page  | 4     | 4     |
| contact_page  | 5     | 5     |
| front_page    | 1     | 1     |
| front_page    | 2     | 2     |
| front_page    | 3     | 3     |
+---------------+-------+-------+--+
理解： lateral view 相当于两个表在join
左表：是原表
右表：是explode(某个集合字段)之后产生的表
而且：这个join只在同一行的数据间进行
```

## 多个 Lateral Views

一个from语句可以有多个侧视图，子侧视图可以使用左侧所有表中的字段

```sql
select pageid, myCol1,myCol2
FROM dwm.lateral_explode_tmp a
LATERAL VIEW explode(split(pageid,",")) a AS myCol1
LATERAL VIEW explode(adid_list) a AS myCol2;
|    pageid     |    myCol1     | myCol2  |
+---------------+---------------+---------+--+
| contact_page  | contact_page  | 3       |
| contact_page  | contact_page  | 4       |
| contact_page  | contact_page  | 5       |
| front_page    | front_page    | 1       |
| front_page    | front_page    | 2       |
| front_page    | front_page    | 3       |
+---------------+---------------+---------+--+
```

## Outer Lateral Views

保留展开的空值，类似left outer join

```sql
SELECT pageid,a
FROM dwm.lateral_explode_tmp  LATERAL VIEW OUTER explode(array()) C AS a ;
+---------------+-------+--+
|    pageid     |   a   |
+---------------+-------+--+
| contact_page  | NULL  |
| front_page    | NULL  |
```

## 内置udtf-一行变多行的函数

https://cwiki.apache.org/confluence/display/Hive/LanguageManual+UDF#LanguageManualUDF-Built-inTable-GeneratingFunctions(UDTF)

| 列类型               | 签名                                                   | 描述                                                         |
| -------------------- | ------------------------------------------------------ | ------------------------------------------------------------ |
| int,T                | posexplode(ARRAY<T> a)                                 | <font color=red>炸开，且第一列是索引</font>                  |
| string1,...,stringn  | json_tuple(string jsonStr,string k1,...,string kn)     | 从一个json串中解析出一个数组出来                             |
| string 1,...,stringn | parse_url_tuple(string urlStr,string p1,...,string pn) | Takes URL string and a set  of n URL parts, and  returns a tuple of n values. This  is similar to the parse_url() UDF but can extract multiple parts at once out of a URL.  Valid part names are: HOST, PATH, QUERY, REF, PROTOCOL, AUTHORITY, FILE,  USERINFO, QUERY:<KEY>. |
| T                    | explode(ARRAY<T> a)                                    | <font color=red>炸开，并与原表join</font>                    |
| T1,...,Tn            | inline(ARRAY<STRUCT<f1:T1,...,fn:Tn>> a)               | [Explodes an array of   structs to multiple rows. Returns a row-set with N columns (N =   number of top level elements in the struct), one row per struct from the   array. (As of Hive 0.10.)](https://issues.apache.org/jira/browse/HIVE-3238) |
| T1,...,Tn/r          | stack(int r,T1 V1,...,Tn/r Vn)                         | <font color=red>把n个值变成r行，每行n/r列</font>             |
| Tkey,Tvalue          | explode(MAP<Tkey,Tvalue> m)                            | 把map展开为两列(key,value)，每个kv一行                       |

### poseexplode

```sql
SELECT posexplode(array(10, 20)) AS (r, elem)
```

| r    | elem |      |
| ---- | ---- | ---- |
| 0    | 10   |      |
| 1    | 20   |      |

# 函数

## Least/greatest

求提供数据的最大/小值

## 集合函数

```sql
array_contains(Array<T>, value)  返回boolean值
sort_array(Array<T>) 返回排序后的数组
size()
map_keys(Map<T,T>)
map_values(Map<T,T>)
```

## 日期函数

\1. UNIX时间戳转日期函数: from_unixtime
\2. 获取当前UNIX时间戳函数: unix_timestamp
\3. 日期转UNIX时间戳函数: unix_timestamp
\4. 指定格式日期转UNIX时间戳函数: unix_timestamp
\5. 日期时间转日期函数: to_date
\6. 日期转年函数: year
\7. 日期转月函数: month
\8. 日期转天函数: day
\9. 日期转小时函数: hour
\10. 日期转分钟函数: minute
\11. 日期转秒函数: second
\12. 日期转周函数: weekofyear
\13. 日期比较函数: datediff
\14. 日期增加函数: date_add
\15. 日期减少函数: date_sub

## **条件函数**  

\1. If函数: if
\2. 非空查找函数: COALESCE
\3. 条件判断函数：CASE

## **字符串函数** 

\1.   字符ascii码函数：ascii
\2.   base64字符串
\3. 字符串连接函数：concat
\4.   带分隔符字符串连接函数：concat_ws
\5. 数组转换成字符串的函数：concat_ws
\6. 小数位格式化成字符串函数：format_number
\7. 字符串截取函数：substr,substring
\8. 字符串截取函数：substr,substring
\9. 字符串查找函数：instr
\10. 字符串长度函数：length
\11. 字符串查找函数：locate
\12. 字符串格式化函数：printf
\13. 字符串转换成map函数：str_to_map
\14. base64解码函数：unbase64(string str)
\15. 字符串转大写函数：upper,ucase
\16. 字符串转小写函数：lower,lcase
\17. 去空格函数：trim
\18. 左边去空格函数：ltrim
\19. 右边去空格函数：rtrim
\20. 正则表达式替换函数：regexp_replace
\21. 正则表达式解析函数：regexp_extract
\22. URL解析函数：parse_url
\23. json解析函数：get_json_object
\24. 空格字符串函数：space
\25. 重复字符串函数：repeat
\26. 左补足函数：lpad
\27. 右补足函数：rpad
\28. 分割字符串函数: split
\29. 集合查找函数: find_in_set
\30.   分词函数：sentences
\31. 分词后统计一起出现频次最高的TOP-K
\32. 分词后统计与指定单词一起出现频次最高的TOP-K

## **混合函数** 

\1. 调用Java函数：java_method
\2. 调用Java函数：reflect
\3. 字符串的hash值：hash

## **XPath解析XML函数**  

\1. xpath
\2. xpath_string
\3. xpath_boolean
\4. xpath_short, xpath_int, xpath_long
\5. xpath_float, xpath_double, xpath_number

## 汇总统计函数（UDAF）

\1. 个数统计函数: count
\2. 总和统计函数: sum
\3. 平均值统计函数: avg
\4. 最小值统计函数: min
\5. 最大值统计函数: max
\6. 非空集合总体变量函数: var_pop
\7. 非空集合样本变量函数: var_samp
\8. 总体标准偏离函数: stddev_pop
\9. 样本标准偏离函数: stddev_samp
10．中位数函数: percentile
\11. 中位数函数: percentile
\12. 近似中位数函数: percentile_approx
\13. 近似中位数函数: percentile_approx
\14. 直方图: histogram_numeric
\15. 集合去重数：collect_set
\16. 集合不去重函数：collect_list

## **表格生成函数Table-Generating Functions (UDTF)** 

\1. 数组拆分成多行：explode
\2. Map拆分成多行：explode

# 窗口分析函数

## 窗口函数+over

**在SQL处理中，窗口函数(也就是聚合函数+over)都是最后一步执行，而且仅位于Order by字句之前。**

### partition by也可以称为查询分区子句

非常类似于Group By，都是将数据按照边界值分组，而Over之前的函数在每一个分组之内进行，如果超出了分组，则函数会重新计算.

### order by 

order by子句会让输入的数据强制排序（文章前面提到过，窗口函数是SQL语句最后执行的函数，因此可以把SQL结果集想象成输入数据）。Order By子句对于诸如Row_Number()，Lead()，LAG()等函数是必须的，因为如果数据无序，这些函数的结果就没有任何意义。因此如果有了Order By子句，则Count()，Min()等计算出来的结果就没有任何意义。

### window子句

比partition by更细粒度的划分， 几个注意的地方：

1. 如果只使用partition by子句,未指定order by的话,我们的聚合是分组内的聚合. 
2. 使用了order by子句,未使用window子句的情况下,默认从起点到当前行.
3. **当同一个select查询中存在多个窗口函数时,他们相互之间是没有影响的.每个窗口函数应用自己的规则.**

```markdown
window子句： 
- PRECEDING：往前 
- FOLLOWING：往后 
- CURRENT ROW：当前行 
- UNBOUNDED：起点，UNBOUNDED PRECEDING 表示从前面的起点， UNBOUNDED FOLLOWING：表示到后面的终点
```

```sql
我们按照name进行分区,按照购物时间进行排序,做cost的累加. 如下我们结合使用window子句进行查询

select name,orderdate,cost,
sum(cost) over() as sample1,--所有行相加
sum(cost) over(partition by name) as sample2,--按name分组，组内数据相加
sum(cost) over(partition by name order by orderdate) as sample3,--按name分组，组内数据累加
sum(cost) over(partition by name order by orderdate rows between UNBOUNDED PRECEDING and current row )  as sample4 ,--和sample3一样,由起点到当前行的聚合
sum(cost) over(partition by name order by orderdate rows between 1 PRECEDING   and current row) as sample5, --当前行和前面一行做聚合
sum(cost) over(partition by name order by orderdate rows between 1 PRECEDING   AND 1 FOLLOWING  ) as sample6,--当前行和前边一行及后面一行
sum(cost) over(partition by name order by orderdate rows between current row and UNBOUNDED FOLLOWING ) as sample7 --当前行及后面所有行
from t_order;
```

## 分析函数

### ntile

功能：用于将分组数据按顺序切分成n片，返回当前切片值

　　注意：　　ntile不支持 rows between

# 分组topN

```sql
select * from (select *,row_number() over(partition by course order by score desc) as rn from sc ) t where t.rn< 4;
```



