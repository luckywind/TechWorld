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