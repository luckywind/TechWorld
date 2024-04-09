# 忽略文件

把不想让git管理的文件或者目录的名字写进一个.gitignore的文件里

可以使用正则表达式

```
```shell
/target/
**/*.rs
*.exe
​```
```

![gitlog](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/gitlog.jpg)

# 查看之前版本

我们使用commit的名字切换不同的commit或者快照。

查看指定commit提交的日志

git show  commit名字

# 查看当前目录的改动

git diff

Git diff TODO.txt

注意，它不检查暂存区文件，要检查暂存区的问题，需要加参数--staged

git diff --staged
