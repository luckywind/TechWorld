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

# 文本处理

```shell
 cat test.txt 
8 hello world
3 hello scala
7 java scala
7 java scala

#按每行开头的数字倒序排序
sort -nr test.txt 
8 hello world
7 java scala
7 java scala
3 hello scala
#去重并记录出现次数
sort -nr test.txt |uniq -c
      1 8 hello world
      2 7 java scala
      1 3 hello scala
#去掉开头的统计值，即截取第9个字符之后的
sort -n test.txt |uniq -c|cut -c 9-
3 hello scala
7 java scala
8 hello world
```



## 临近行去重uniq

```shell
uniq [-cdu][-f<栏位>][-s<字符位置>][-w<字符位置>][--help][--version][输入文件][输出文件]
```

-c或--count 在每列旁边显示该行重复出现的次数。
-d或--repeated 仅显示重复出现的行列。
-f<栏位>或--skip-fields=<栏位> 忽略比较指定的栏位。
-s<字符位置>或--skip-chars=<字符位置> 忽略比较指定的字符。
-u或--unique 仅显示出一次的行列。
-w<字符位置>或--check-chars=<字符位置> 指定要比较的字符。
[输入文件] 指定已排序好的文本文件。如果不指定此项，则从标准读取数据；
[输出文件] 指定输出的文件。如果不指定此选项，则将内容显示到标准输出设备（显示终端）。

## 排序sort

```shell
sort [-bcdfimMnr][-o<输出文件>][-t<分隔字符>][+<起始栏位>-<结束栏位>][--help][--verison][文件]
```

参数说明：

-b 忽略每行前面开始出的空格字符。
-c 检查文件是否已经按照顺序排序。
-d 排序时，处理英文字母、数字及空格字符外，忽略其他的字符。
-f 排序时，将小写字母视为大写字母。
-i 排序时，除了040至176之间的ASCII字符外，忽略其他的字符。
-m 将几个排序好的文件进行合并。
-M 将前面3个字母依照月份的缩写进行排序。
-n **可以识别每行开头的数字，并按其大小对文本行进行排序。**
-o<输出文件> 将排序后的结果存入指定的文件。
-r 以相反的顺序来排序。
-t<分隔字符> 指定排序时所用的栏位分隔字符。
+<起始栏位>-<结束栏位> 以指定的栏位来排序，范围由起始栏位到结束栏位的前一栏位。

示例：
数字排序：  sort  -n

## 按列操作cut

```shell
cut -b list [-n] [file ...]
cut -c list [file ...]
cut -f list -d delim[file ...]
```

上面的-b、-c、-f分别表示字节、字符、字段（即byte、character、field）；
list表示-b、-c、-f操作范围，-n常常表示具体数字；
file表示的自然是要操作的文本文件的名称；
delim（英文全写：delimiter）表示分隔符，默认情况下为TAB；
-s表示不包括那些不含分隔符的行（这样有利于去掉注释和标题）
三种方式中，表示从指定的范围中提取字节（-b）、或字符（-c）、或字段（-f）。

范围的表示方法：
      n   只有第n项 
      n-  从第n项一直到行尾
      n-m 从第n项到第m项(包括m)
      -m  从一行的开始到第m项(包括m)

​      -从一行的开始到结束的所有项



## 比较diff

```shell
more 1.txt 
1
2
22
t

t
more 2.txt 
1
2
77
t

xx

# 逐行比较
diff 1.txt 2.txt 
3c3   第三行比较结果
< 22  左边值
---
> 77  右边值
6c6   
< t
---
> xx

# 打印上下文  打印后者比前者的区别：+代表多的，-代表少的，！代表不同 ⬆️
diff -c 1.txt 2.txt 
*** 1.txt       2023-10-19 11:02:55.620257844 +0800
--- 2.txt       2023-10-19 11:03:08.040257763 +0800
***************
*** 1,6 ****  左边1，6行
  1
  2
! 22
  t
  
! t
--- 1,6 ----  右边1，6行
  1
  2
! 77
  t
  
! xx

# 并排展示差异
diff 1.txt 2.txt -y -W 20
1       1
2       2
22    | 77
t       t

t     | xx
```



```shell
diff [-abBcdefHilnNpPqrstTuvwy][文件1或目录1][文件2或目录2]
diff [-abBcdefHilnNpPqrstTuvwy][-<行数>][-C <行数>][-D <巨集名称>][-I <字符或字符串>][-S <文件>][-W <宽度>][-x <文件或目录>][-X <文件>][--help][--left-column][--suppress-common-line][文件1或目录1][文件2或目录2]
```

参数

-<行数>：指定要显示多少行的文本。此参数必须与-c或-u参数一并使用。
-a或--text：diff预设只会逐行比较文本文件。
-b或--ignore-space-change：不检查空格字符的不同。
-B或--ignore-blank-lines：不检查空白行。
**-c：显示全部内文，并标出不同之处。**

     ```shell
     “＋” 比较的文件的后者比前者多一行；
     “－” 比较的文件的后者比前者少一行；
     “！” 比较的文件两者有差别的行。
     ```

-C<行数>或--context<行数>：与执行"-c-<行数>"指令相同。
-d或--minimal：使用不同的演算法，以较小的单位来做比较。
-D<巨集名称>或ifdef<巨集名称>：此参数的输出格式可用于前置处理器巨集。
-e或--ed：此参数的输出格式可用于ed的script文件。
-f或-forward-ed：输出的格式类似ed的script文件，但按照原来文件的顺序来显示不同处。
-H或--speed-large-files：比较大文件时，可加快速度。
-l<字符或字符串>或--ignore-matching-lines<字符或字符串>：若两个文件在某几行有所不同，而这几行同时都包含了选项中指定的字符或字符串，则不显示这两个文件的差异。
-i或--ignore-case：不检查大小写的不同。
-l或--paginate：将结果交由pr程序来分页。
-n或--rcs：将比较结果以RCS的格式来显示。
-N或--new-file：在比较目录时，若文件A仅出现在某个目录中，预设会显示：Only in目录：文件A若使用-N参数，则diff会将文件A与一个空白的文件比较。
-p：若比较的文件为C语言的程序码文件时，显示差异所在的函数名称。
-P或--unidirectional-new-file：与-N类似，但只有当第二个目录包含了一个第一个目录所没有的文件时，才会将这个文件与空白的文件做比较。
-q或--brief：仅显示有无差异，不显示详细的信息。
-r或--recursive：比较子目录中的文件。
-s或--report-identical-files：若没有发现任何差异，仍然显示信息。
-S<文件>或--starting-file<文件>：在比较目录时，从指定的文件开始比较。
-t或--expand-tabs：在输出时，将tab字符展开。
-T或--initial-tab：在每行前面加上tab字符以便对齐。
-u,-U<列数>或--unified=<列数>：以合并的方式来显示文件内容的不同。
-v或--version：显示版本信息。
-w或--ignore-all-space：忽略全部的空格字符。
**-W<宽度>或--width<宽度> ：在使用-y参数时，指定栏宽。**
-x<文件名或目录>或--exclude<文件名或目录>：不比较选项中所指定的文件或目录。
-X<文件>或--exclude-from<文件>：您可以将文件或目录类型存成文本文件，然后在=<文件>中指定此文本文件。
**-y或--side-by-side：以并列的方式显示文件的异同之处。**
--help：显示帮助。
--left-column：在使用-y参数时，若两个文件某一行内容相同，则仅在左侧的栏位显示该行内容。

--suppress-common-lines：在使用-y参数时，仅显示不同之处。



### 已排序文件的交并差comm

```shell
cat a_sort.txt 
111 
222
aaa 
bbb 
ccc 
ddd 
eee 
cat b_sort.txt 
aaa 
bbb 
ccc 
hhh 
jjj
ttt 
# 比较它们
comm a_sort.txt b_sort.txt     
111 
222
                aaa 
                bbb 
                ccc 
ddd 
eee 
        hhh 
        jjj
        ttt 
# 只显示第一个文件里出现的列， 即不显示第二三列
comm  -23 a_sort.txt b_sort.txt 
111 
222
ddd 
eee 

```



`comm`命令将两个已排序的文件进行比较，并输出三列结果：

- 第一列：仅在`FILE1`中存在的行。
- 第二列：仅在`FILE2`中存在的行。
- 第三列：同时在`FILE1`和`FILE2`中都存在的行

参数

-1 不显示只在第1个文件里出现过的列。

-2 不显示只在第2个文件里出现过的列。

-3 不显示只在第1和第2个文件里出现过的列。

-i：在比较时忽略大小写。
--output-delimiter=STRING：使用指定的字符串作为字段之间的分隔符。
-z：使用空字符作为行分隔符。
--help：显示帮助信息并退出。
--version：显示版本信息并退出。
