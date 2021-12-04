# 常用操作

```shell
git  add   添加到暂存区  /  git rm --cached从暂存区删除

git commit  #把暂存区提交到当前分支

git diff 比较的是和上一次commit之间的差异

修改文件后，还是需要先add再commit

git log  --pretty=oneline 查看提交记录

#回退
git使用HEAD表示当前版本，上一个版本就是`HEAD^`，上上一个版本就是`HEAD^^`，上100个版本HEAD~100
git reset --mixed  默认参数，不删除工作空间改动代码，撤销commit，并且撤销git add . 操作
git reset --soft  不删除工作空间改动代码，撤销commit，不撤销git add . 
git reset --hard 删除工作空间改动代码，撤销commit，撤销git add . 
git reset --hard HEAD^  #回退到上一个版本
git reset --hard 1094a #切换到指定版本
git reflog   #查看命令记录，里面包含了版本列表，从这里查看版本号，随意切换
#文件修改撤销
git checkout -- file #撤销工作区的修改，有两种情况，1，修改了还没有add，这会回到和版本库一样。2，add了，再修改，这时已经add的内容不会被撤销
			#注意，这个--很重要，没有的话就是切分支的命令了
git checkout -- file #从版本库更新本地工作区，对于误删文件恢复时很有用
git rm file ; git commit #从版本库中删除文件

#远程库操作
git remote add 【远程库名】(默认是origin) 仓库地址  #添加远程库
git push  #把当前分支推送到远程
		-u 远程主机  分支（例如-u origin master ） #指定要推送的远程主机和分支，-u参数意思是记下这次的选择，以后push时就默认这个选择

# 分支操作
git checkout -b dev /git switch -c dev#创建并切换分支，等价于下面两个命令
		 git branch dev  #创建分支
     git checkout dev /git switch master#切换分支
git branch #查看当前分支
git merge xxx #把指定分支合并到当前分支，注意，合并到哪个分支，要先切换过去再合并
		git merge --no-ff -m "merge with no-ff" xxx #这样是禁用Fast forward模式，git在merge时生成一个commit，即使删除分支，分支信息也不会丢掉。
git branch -d dev #删除dev分支，合并完后，就可以把分支删除了


# 冲突解决
当我们merge分支时，如果提示冲突，查看冲突的文件，会发现Git用<<<<<<<，=======，>>>>>>>标记出不同分支的内容
1. 在当前分支解决冲突后add并commit
git log --graph --pretty=oneline --abbrev-commit #查看日志情况
git log --graph #查看分支合并图

解决冲突可以选择图形化工具kaleidoscope。如果merge冲突，使用命令git mergetool会打开界面，解决完成后再add/commit。







 
```

**分支策略**

在实际开发中，我们应该按照几个基本原则进行分支管理：

首先，`master`分支应该是非常稳定的，也就是仅用来发布新版本，平时不能在上面干活；

那在哪干活呢？干活都在`dev`分支上，也就是说，`dev`分支是不稳定的，到某个时候，比如1.0版本发布时，再把`dev`分支合并到`master`上，在`master`分支发布1.0版本；

你和你的小伙伴们每个人都在`dev`分支上干活，每个人都有自己的分支，时不时地往`dev`分支上合并就可以了。

所以，团队合作的分支看起来就像这样：

![git-br-policy](廖雪峰git学习.assets/0.png)

# bug分支

每个bug都可以通过一个新的临时分支来修复，修复后，合并分支，然后将临时分支删除。

假设我们当前在dev分支上，已经有修改了的文件了，但是还不能提交，因为还没开发完。 此时发现了之前的一个bug，要先修复bug。可以用stash命令把当前工作现场"储藏"起来，等以后再继续工作

git stash 

首先确定要在哪个分支上修复bug，假定需要在`master`分支上修复，就从`master`创建临时分支, 再临时分支上修复bug，并merge到master后，再回到dev分支

但是当我们切换回dev分支后，怎么找回原来的工作现场呢？ 
```shell
git stash list
stash@{0}: WIP on dev: 3b73622 add merge #可以发现这里有一个stash内容
git stash pop #恢复工作，并删除stash记录
```

但此时有一个问题，我们是在dev分支创建之后再去master修改的bug, 当前dev分支是没有修复这个bug的，还好git提供了一个复制特定提交到当前分支的命令

git cherry-pick commit号(master分支用于fix bug的commit) 把提交"重放"到当前分支

此时，dev分支也修复了这个bug。



# 多人协作

## 推送分支

```shell
git push 远程仓库名 本地分支名
```

## 抓取分支

当我们推送时出现冲突了，就需要先抓取分支，再推送

```shell
git branch --set-upstream-to=远程仓库名/dev dev #设置本地分支和远程分支的链接
git pull 抓取远程分支
```

# 权威资料

[分支操作指南](https://git-scm.com/book/zh/v2/Git-%E5%88%86%E6%94%AF-%E5%88%86%E6%94%AF%E7%9A%84%E6%96%B0%E5%BB%BA%E4%B8%8E%E5%90%88%E5%B9%B6)

## 合并的原理：

### fast-forward的情况

要把hotfix合并回master，由于你想要合并的分支 `hotfix` 所指向的提交 `C4` 是你所在的提交 `C2` 的直接后继， 因此 Git 会直接将指针向前移动。换句话说，当你试图合并两个分支时， 如果顺着一个分支走下去能够到达另一个分支，那么 Git 在合并两者的时候， 只会简单的将指针向前推进（指针右移），因为这种情况下的合并操作没有需要解决的分歧——这就叫做 “快进（fast-forward）”。

![基于 `master` 分支的紧急问题分支（hotfix branch）。](https://git-scm.com/book/en/v2/images/basic-branching-4.png)

### 变基的情况

例如这种情况，要合并iss53分支和master分支，`master` 分支所在提交并不是 `iss53` 分支所在提交的直接祖先。此时

![一次典型合并中所用到的三个快照。](https://git-scm.com/book/en/v2/images/basic-merging-1.png)

Git 会使用两个分支的末端所指的快照（`C4` 和 `C5`）以及这两个分支的共同祖先（`C2`），做一个简单的三方合并。

和之前将分支指针向前推进所不同的是，**Git 将此次三方合并的结果做了一个新的快照并且自动创建一个新的提交指向它。 这个被称作一次合并提交，它的特别之处在于他有不止一个父提交。**合并成功的结果就如下图

![一个合并提交。](https://git-scm.com/book/en/v2/images/basic-merging-2.png)



### 冲突的解决

但如果冲突了呢？

此时 Git 做了合并，但是没有自动地创建一个新的合并提交。 Git 会暂停下来，等待你去解决合并产生的冲突。 你可以在合并冲突后的任意时刻使用 `git status` 命令来查看那些因包含合并冲突而处于未合并（unmerged）状态的文件。

在你解决了所有文件里的冲突之后，对每个文件使用 `git add` 命令来将其标记为冲突已解决。 一旦暂存这些原本有冲突的文件，Git 就会将它们标记为冲突已解决。

合并出现冲突时的解决办法：

可以运行 `git mergetool`使用图形化的方式解决冲突，等你退出合并工具之后，Git 会询问刚才的合并是否成功。 如果你回答是，Git 会暂存那些文件以表明冲突已解决，如果你对结果感到满意，并且确定之前有冲突的的文件都已经暂存了，这时你可以输入 `git commit` 来完成合并提交。

