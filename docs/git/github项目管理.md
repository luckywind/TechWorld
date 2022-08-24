# 将本地仓库链接到远程仓库

假设我们在github上有一个仓库https://github.com/luckywind/myapp-1.git 

现在我们创建一个本地仓库，名字可以和远程仓库不同，不过建议一样

mdir  myapp-1

cd my app-1

git init 

此时使用git remote将看不到任何关联的远程仓库，如果有，可以使用git remote rm [remote名字]删除

**链接命令： git remote add [名字]  [链接]**

一个本地工程可以有多个远程仓库，默认名字是origin, 也建议用这个

# push到远程仓库

Git add 

git  commit 这些命令把本地更改提交到数据库。 之后就可以使用git push把更改推送到远程仓库，

**git. push  远程仓库名  分支名**

例如，git push origin master 

```shell
git push origin #将当前分支推送到origin主机的对应分支
git push #如果当前分支只有一个追踪分支，可以省略主机名
#如果当前分支与多个主机存在追踪关系，-u选项指定一个默认主机，这样后面可以直接git push

git push -u origin master #将本地master分支推送到origin主机，同时指定origin为默认主机，后面可以直接git push
```





git操作概览图

[git操作一览](https://www.processon.com/diagraming/5e8e919e5653bb6e6ebfea14)

![image-20211128103845770](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20211128103845770.png)







