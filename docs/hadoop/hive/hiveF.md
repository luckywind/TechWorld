[hiveF 开发思路](https://juejin.cn/post/6844903553161494535)



hiveF

```shell
#!/bin/sh
. /etc/profile

# 返回正常的sql语句
cmd=`java -jar /root/project/lib/HiveF.jar $*`  
echo $cmd
hive -e "$cmd" -i /root/project/bin/init.hql
```

调用

`hiveF aa.sql -date 2015-01-02 -date1 2015-01-03`



`sh ./rpt_act_visit_daily.sh 2015-08-28`