# exists与not exists

原理解释：

exists（sql返回结果集为真）

not exists（sql不返回结果集为真或返回结果集为假）

注意： 返回什么不重要，重要的是是否返回结果集

```sql
select * from A where not exists(select 1 from B where A.id = B.id);
select * from A where exists(select 1 from B where A.id = B.id);
```


先执行外查询，再执行子查询，详细步骤（使用exists）：

1，首先执行外查询select * from A，然后从外查询的数据取出一条数据传给内查询。

2，内查询执行select * from B，外查询传入的数据和内查询获得数据根据where后面的条件做匹对，如果存在数据满足A.id=B.id则返回true，如果一条都不满足则返回false。

3，内查询返回true，则外查询的这行数据保留，反之内查询返回false则外查询的这行数据不显示。外查询的所有数据逐行查询匹对。

not exists和exists的用法相反，就不继续啰嗦了。
原文链接：https://blog.csdn.net/wankao/article/details/109366320