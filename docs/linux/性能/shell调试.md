# 调试方法

- -n 读一遍脚本中的命令但不执行，用于检查脚本中的语法错误
- -v，一边执行脚本，一边将执行过的脚本命令打印到标准输出
- **-x，提供跟踪执行信息，将执行的每一条命令和结果依次打印出来。**

使用方法

1. 命令中提供

   ```shell
   sh -x buggy.sh 
   ```

2. 脚本开头提供

   ```shell
   #!/bin/sh -xv
   ```

3. 脚本代码中启用/关闭

   ```shell
   set -x
   echo $i
   set +x
   ```

   

# 其他

## ./和sh的区别

./执行，系统会查看脚本的第一行（也称为"shebang"）来确定要使用哪个解释器来运行脚本

sh执行，将始终使用系统默认的 Shell 解释器来运行脚本，而不考虑脚本的第一行

echo $SHELL  可以查看系统默认的解释器， sh链接到默认shell解释器

## source

在当前上下文中执行脚本，不会生成新的进程。脚本执行完毕，回到当前shell。source方式也叫点命令。

.test.sh与source test.sh等效。

## exec

使用exec command方式，会用command进程替换当前shell进程，并且保持PID不变。执行完毕，直接退出，不回到之前的shell环境。

# 问题

## source找不到当前目录下的文件

解决方法如下

1. 使用绝对路径/相对路径， 不要只提供文件名
2. 脚本添加到PATH

## 如何不继承父脚本的参数

父脚本在调用子脚本前使用 set --  命令可以清空参数

