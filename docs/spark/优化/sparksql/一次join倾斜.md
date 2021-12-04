spark sql join操作

```sql
set spark.sql.shuffle.partitions=800;
set  spark.executor.memory=8g;
with d as (  --43392488
select 
month, mid
from dwm.dwm_oneid_iot_mid_imei_mi
where  type='D'
group by month,mid
)
-- select month,count(1) from d group by month order by month;
,
e as ( 
select 
mid,
imei,
did,
month
 from  dwm.dwm_oneid_iot_mid_imei_mi
  where month between 202001 and 202106
  and type='E'
)
select 
d.month, e.month,
count(distinct d.mid) midcnt,
count(distinct e.imei) imeicnt,
count(distinct e.did) didcnt
from 
d join e --一定是绑定了did的，所以不用left join
on d.mid=e.mid
where d.month<e.month
group by d.month, e.month
order by d.month, e.month;
```

![image-20210916102914423](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210916102914423.png)

从DAG图看stage7457涉及到两个RDD的Join操作，发生了数据shuffle

第1149行代码是jdk的线程池调度task，点进去可以看到该stage已经完成的499个task的汇总信息，重点关注shuffle相关指标：
![image-20210916103356392](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210916103356392.png)

然后，去看第500个正在运行的task，其shuffle情况，直接找到task列表，按shuffle  Read大小排序：
![image-20210916103619813](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210916103619813.png)

可以发现这个task已经读取了16.2G的数据块，远超前面499个task的最大读取量。至此，我们发现了数据倾斜。

我们只要对join操作的条件中的key进行分组统计，看是否有一些key数量很大即可，本例中是发现了一个mid对应的数据量远超其他key。调研发现是演示用，就直接在join前过滤掉。







Spark-sql UI技巧

1. 通过dag读取的表和join条件可以定位到出现倾斜的语句
2. 