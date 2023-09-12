[时间自动同步](https://www.cnblogs.com/chenmh/p/5485829.html)

# 操作特殊字符文件名

[参考](https://cn.linux-console.net/?p=2296#gsc.tab=0)

处理破折号，用--或者./

```
touch -- -abc.txt
或者
touch ./-abc.txt
```

处理#、(

```
$ touch ./#abc.txt
或者
$ touch '#abc.txt'
```

处理分号

```
$ touch ./';abc.txt'
或者
$ touch ';abc.txt'
```

