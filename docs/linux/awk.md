# 基本结构

```shell
awk '模式 {action}' 文件
```

文件可选，如果不提供那么awk会等待你输入，也可以通过管道获取输入

命令可选

程序也可以写入一个文件中，通过-f 执行：

awk -f 程序文件 文件列表

例如：过滤第3列为0，第6列为LISTEN的行， 以及表头

```shell
netstat | awk '&3==0 && 6=="LISTEN" || NR==1 ' 
```

只提供模式，NR==1表示匹配表头，NR是一个内建变量

## 模式

默认的模式是每行都匹配，两个比较特殊的模式是BEGIN和END，BEGIN在读取前执行，END在最后一行结束后执行

```shell
netstat | awk '\
BEGIN { print "START" } \
END   { print "STOP"  }'
```





```shell
ls -l | awk '\
BEGIN { print "START" } \
      { print         } \
END   { print "STOP"  }'
```



```shell
ls -l | awk '\
BEGIN { print "START" } \
 { print  $1,$2,$3       } \
END   { print "STOP"  }'
```

模式组合

```shell
netstat | awk '\
BEGIN { print "START" }
$3==0 && $6=="ESTABLISHED" || NR==1\
 { print        } \
END   { print "STOP"  }'
```

START
Active Internet connections (w/o servers)
tcp        0      0 bogon:ssh               bogon:62454             ESTABLISHED
tcp        0      0 bogon:ssh               bogon:62459             ESTABLISHED
STOP



## action

awk不会按照shell解析引号内的变量，可以`{ print $8, "\t", $3}`打印多个列，但不能`{print "$8\t$3" }`这么打印。

## 脚本

第一种写法：用shell写

注意：单引号引起来的内容可以跨行

```shell
#!/bin/sh
# Linux users have to change $8 to $9
awk '
BEGIN { print "File\tOwner" }
{ print $8, "\t", $3}
END { print " - DONE -" }
'
```

第二种写法：纯awk(推荐)

awk自身也是一个解释器，也可以使用纯awk写, 注意：

1. 第一行是#!/bin/awk -f，  另外，可以使用#来添加注释
2. 只能在右括号后，或者完整命令后进行换行，否则要加反斜线\来换行



```shell
#!/bin/awk -f
BEGIN { print "File\tOwner" }
{ print $8, "\t", $3}
END { print " - DONE -" }
```

这种写法，执行时要用awk -f filename



### 引号的使用

假设有一个脚本cat Column1.sh，打印指定的列：

```shell
#!/bin/sh
column="$1"
awk '{print $'"$column"'}'
```

使用方法是

```shell
ls -l |./Column1.sh 3
```

注意的地方就是脚本里引号的使用：

1. awk会解释单引号，第一个`$`后带单引号所以会交给awk执行。

2. shell会解释双引号，所以"$column"会被shell解析，这里的`$`后面没有单引号，也会被shell解析

### shell变量默认值

语法是${*变量*:-*变量默认值*}， 注意默认值前面有一个短线

```shell
#!/bin/sh
column="${2:-1}"  #:-后面是默认值1
awk '{print $'"$column"'}'
```



## 内建变量

| 0        | 修改参数 | 当前记录（这个变量中存放着整个行的内容）                     |
| -------- | -------- | ------------------------------------------------------------ |
| `$1~$n`  |          | 当前记录的第n个字段，字段间由FS分隔                          |
| FS       | -F分隔符 | 输入字段分隔符 默认是空格或Tab     filter split              |
| NF       |          | 当前记录中的字段个数，就是有多少列, num of fields            |
| NR       |          | 已经读出的记录数，就是行号，从1开始，如果有多个文件话，   这个值也是不断累加中。    number of rows |
| FNR      |          | 当前记录数，与NR不同的是，这个值会是各个文件自己的行号       |
| RS       |          | 输入的记录分隔符， 默认为换行符, row spliter                 |
| OFS      | OFS="\t" | 输出字段分隔符， 默认也是空格   output field spliter         |
| ORS      |          | 输出的记录分隔符，默认为换行符  output row spliter           |
| FILENAME |          | 当前输入文件的名字                                           |



```shell
指定行号、输入、输出分隔符，注意各选项中间有空格
awk  -F: '{print NR,$1,$3,$6}' OFS="\t" /etc/passwd

```

## 统计

下面的命令计算所有的C文件，CPP文件和H文件的文件大小总和。

$ ls -l *.cpp *.c *.h | awk '{sum+=$5} END {print sum}'

2511401