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

### 撤销

```shell
git reset HEAD xx  撤销add操作
git checkout -- xx 撤销还未add的修改
```

## 远程仓库

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

```

## 分支操作

1. 最基本的合并操作

   merge命令，是把两个分支最新的快照以及最新的共同祖先进行三方合并，并产生一个新的提交对象

   ![image-20220825095102419](/home/mi/.config/Typora/typora-user-images/image-20220825095102419.png)

还有另一个选择，可以把子分支里产生的变化在主分支的基础上重新打一遍，这叫做衍合(rebase)

```shell
git checkout experiment
git rebase master
```

原理： 从共同祖先开始，子分支的所有提交逐个应用到主分支上，并产生一个新的提交；主分支再快进到这个提交。

![image-20220825100100492](/home/mi/.config/Typora/typora-user-images/image-20220825100100492.png)

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

