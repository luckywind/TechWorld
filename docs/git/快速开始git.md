# 仓库

仓库就是存储变更的目录，初始化仓库很简单

```shell
$ mkdir mynewproject
$ cd mynewproject/
$ git init
```

git将创建一个.git目录存储变更集和快照。

```shell
git status 
#修改readme.md
git add readme.md
git commit readme.md
```

提交之间有父子关系，上一次提交是本次提交的parent。如果一个提交有两个parent,说名这个提交是merge两个分支的结果。

特定commit的引用称为head，当前工作的head又称为“HEAD”.





git rm 从暂存区撤回文件

git checkout 放弃更改，和svn的revert一样

