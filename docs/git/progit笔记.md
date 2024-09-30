# 查看提交历史

```shell
git log
git log -p -2   -p显示差异，-2显示最近两次更新
git log --pretty=oneline  一行显示
git log --graph 图形哈
```

### 提交

修改最后一次提交

```shell
git commit -m 'initial commit'
git add forgotten_file
git commit --amend
```

#### 合并多个提交

有时commit了多次来干一件事，怎么合并为一个commit?

```shell
git log    # 列出commit，确定想从哪个commit开始合并(不包括它)，假如是28d145(不包括)
git rebase -i 28d145    
 #第一步：此时除了第一行外，其余行开头的pick都改为s，  可用:%s/pick/s/g 全局替换
 #第二步：修改评论
完成
```





### 撤销

```shell
git reset HEAD xx  撤销add操作
git checkout -- xx 撤销还未add的修改
git revert commit_id 用一次新的commit来撤销之前的某个commit
如果想撤销的不是一次commit，而是连着的几次，那么
git revert --no-commit commit1..commit2
```

## 远程仓库

查看使用帮助： git remote --help

```shell
git remote -v 列出远程仓库，  只有ssh链接才能推送数据上去

git remote add 远程仓库别名 url  ,  例如：
	git remote add pd  git@xxxx.com
	现在就可以拉取这个仓库
	git fetch pd  这样就把pd的master分支拉过来了，对应的名字是pd/master
	如果是克隆的仓库，则自动归于origin名下。git fetch origin会拉取origin仓库的更新到本地仓库，注意，并不会自动合并到当前工作分支, 如果想自动合并到当前分支，则使用git pull。 这是因为git clone本质上自动创建了本地master分支用于跟踪远程仓库的master分支。

跟踪远程分支，作用是git push 等可省略远程仓库名
git push --set-upstream origin master


推送到远程分支
git push 远程仓库名  分支名
   例如，git push origin master  (这是因为clone自动把远程仓库取名为origin)

查看远程仓库信息
git remote show 远程仓库名
	例如，git remote show origin 
	它会告诉你git push默认推送到的分支是哪个
	
远程仓库删除和重命名
git remote rename 原名 新名
git remote rm 仓库名


```

## 打tag

tag就是某个时间点上的版本

```shell
列出所有tag
git tag

搜索tag
git tag -l 'v1.4.2.*'

查看tag
git show v1.4

新建tag
git tag -a v1.4 -m 'my version 1.4'   含备注的标签
git tag v1.4  轻量级标签


分享标签
默认git push不会把tag传送到远程服务器上
git push origin 标签名
git push origin --tags 也可以一次推送所有tag


检出tag
git checkout 2.0.0
```

## 分支操作

### merge

merge命令，是把两个分支最新的快照以及最新的共同祖先进行**三方合并**，并产生一个新的提交对象

![image-20220825095102419](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/imageimage-20220825095102419.png)

### rebase

还有另一个选择，可以把子分支里产生的变化在主分支的基础上重新打一遍，这叫做衍合(rebase)

```shell
git checkout experiment
git rebase master
```

原理： 从共同祖先开始，子分支的所有提交逐个应用到主分支上，并产生一个新的提交；主分支再快进到这个提交。

![image-20220825100100492](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/imageimage-20220825100100492.png)



### 撤销merge

```shell
如果是冲突了，想撤销本次merge
git merge --abort

如果没冲突，但想回到merge之前的版本
git log 找到版本号
git reset --hard xxxx
```



#### 分支操作

```shell
列出所有分支
git branch
删除分支
	git branch -d 分支名，  注意，如果该分支还没合并进来，则需要-D强制删除
```



### 远程分支

```shell
推送到远程
git push origin 分支名
	也可以推送时修改远程分支名
	git push origin 本地分支名:远程分支名
跟踪远程分支
git checkout  --track  远程仓库/远程分支

```

### git工具

#### stashing

```shell
git stash 储藏，这会创建一个stash，即使没有改动代码
	 git stash save 可加注释
git stash list 查看现有储藏
git stash pop 应用并删除stash
git stash apply 使用最新的储藏, 但不删除stash
git stash apply stash@{第几个储藏}
git stash drop 移除储藏,从最老的开始drop
git stash branch 新分支   用stash中的修改创建一个新的分支，创建成功后会删除此stash
```

# 问题解决记录

## 强制丢弃本地修改,[参考](https://www.cnblogs.com/feifeicui/p/11351433.html)

```shell
git clean -d -fx
```

## 443问题

[Git报错： Failed to connect to github.com port 443 解决方案](https://blog.csdn.net/zpf1813763637/article/details/128340109)

```shell
git config --global http.proxy 127.0.0.1:7890
git config --global https.proxy 127.0.0.1:7890
```







# 参考

[图解GIT](https://marklodato.github.io/visual-git-guide/index-zh-cn.html#detached)

[GIT User Guide](https://mirrors.edge.kernel.org/pub/software/scm/git/docs/user-manual.html#using-git-rebase)

# rebase命令

[翻译自](https://mirrors.edge.kernel.org/pub/software/scm/git/docs/git-rebase.html)

用途：在另一个分支上回放提交

语法

```shell
git rebase [-i | --interactive] [<options>] [--exec <cmd>]
        [--onto <newbase> | --keep-base] [<upstream> [<branch>]]
git rebase [-i | --interactive] [<options>] [--exec <cmd>] [--onto <newbase>]
        --root [<branch>]
git rebase (--continue | --skip | --abort | --quit | --edit-todo | --show-current-patch)
```

## 描述

