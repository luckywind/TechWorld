# git 帮助







# 常用操作

## 查看提交历史

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

git reset --soft HEAD~1 撤销提交，删除最近一个commit







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

## 分支与tag的区别

1.**Branch（分支）**：
分支是指项目中的一个独立的、可移动的指针，它指向一个特定的提交（commit）对象。在Git中，每个分支都可以代表项目的一个独立开发路径，允许开发者并行开发多个功能、修复bug或者进行其他工作；
分支可以用来创建新的特性、修复bug、实现实验性的功能等。可以在不影响主分支（通常是master或main）的情况下，在自己的分支上工作，并在完成后将变更合并到主分支中；
分支的创建、切换、合并和删除等操作都可以通过Git命令进行管理。
2.**Tag（标签）**：
标签是指项目中某个特定版本的一个标记，用于标识项目的重要节点，例如发布版本或者里程碑。标签可以附加到任意的提交对象上，通常用于标记项目的稳定版本。
标签一般用于固定项目的某个特定版本，以便后续可以方便地回溯到该版本。<font color=red>与分支不同，标签是不可移动的，一旦创建，它就与特定的提交对象相关联，并且不能被修改。</font>一个提交对象，可以打多个tag。
标签可以用来发布软件版本、记录项目的重要事件或者用于其他类似的目的。它们通常用于公共发布或发布到生产环境中。

总而言之：

**1.tag是一系列commit的中的一个点，只能查看，不能移动。branch是一系列串联的commit的线。**
**2.tag是静态的，branch是动态的，要向前走。**

## tag操作

tag就是某个时间点上的版本

```shell
列出所有tag
git tag

搜索tag
git tag -l 'v1.4.2.*'

查看tag(包括它对应的commit信息)
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

```SHELL
#新增分支
git branch cat
#如果把cat分支改成tiger分支，使用的是-m参数：
git branch -m cat tiger
#查看当前分支
git branch
#删除分支 可以使用-d参数来删除
git branch -d dog
#tiger的内容还没有被合并，使用-d参数无法将其删除。这时只需改用-D参数即可将其强制删除
git branch -D tiger
#要切换分支，就是git checkout：
git checkout tiger
#使用git merge命令合并分支
git merge cat
#恢复已被删除的还没合并过的分支
git branch -D cat 
Deleted branch cat （was b174a5a）.
git branch new_cat b174a5a
还没有把刚刚删除的那个cat分支的SHA-1值记下来怎么办？查得到吗
可以用git reflog命令去查找，Reflog默认会保留30天，所以30天内还找得到。
```



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

### 查看父分支

```shell
git reflog show 子分支
```



## 账号相关

测试连接

```shell
ssh -T git@192.168.2.114
```

### ssh登录

生成公钥和私钥后，把公钥配置到gitLab，私钥配置到.ssh/config文件里：

```shell
# GitLab.com server
Host gitlab.yusur.tech
IdentityFile ~/.ssh/id_rsa.gitlab-pre
```









### 查看登录信息

```shell
 git config –-list
 user.name=chengxf
 user.email=chengxf@yusur.tech
```



### 登录账号

```shell
git config --global user.name hadosdev
git config --global user.email hadosdev@yusur.tech
```

hadosyusur

### 问题

#### Fingerprint has already been taken

[参考](https://www.5bug.wang/post/97.html)



# git worktree

[参考](https://zhuanlan.zhihu.com/p/1938293629455152167)

git worktree add [-f] [--detach] [--checkout] [--lock [--reason <string>]]
                   [-b <new-branch>] <path> [<commit-ish>]
git worktree list [-v | --porcelain [-z]]
git worktree lock [--reason <string>] <worktree>
git worktree move <worktree> <new-path>
git worktree prune [-n] [-v] [--expire <expire>]
git worktree remove [-f] <worktree>
git worktree repair [<path>...]
git worktree unlock <worktree>

**1. `add` - 添加工作树**
**核心用法**：在指定路径创建一个新的工作树，并检出指定的提交或分支。如果不指定分支，Git 会自动以路径的最后一级目录名作为新分支名并创建它。使用 `-b` 可显式指定新分支名；使用 `--detach` (或 `-d`) 可创建游离状态（detached HEAD）的临时工作树。
**示例**：
```bash
# 自动创建名为 "hotfix" 的分支，并检出到 ../hotfix 目录
git worktree add ../hotfix 

# 检出已存在的 "feature-A" 分支到 ../feature-A 目录
git worktree add ../feature-A feature-A

# 创建一个不关联任何分支的临时工作树（用于实验或测试）
git worktree add --detach ../temp-test
```

**2. `list` - 列出工作树**
**核心用法**：列出当前仓库关联的所有工作树（主工作树 + 链接工作树），显示路径、当前提交、分支状态及是否被锁定/可修剪。
**示例**：
```bash
# 查看标准列表
git worktree list

# 查看详细/机器可读格式
git worktree list -v
```

**3. `lock` - 锁定工作树**
**核心用法**：锁定某个工作树，防止其管理文件被 `prune` 自动清理。通常用于存放在移动硬盘或网络共享等非始终挂载的目录中的工作树。
**示例**：
```bash
# 锁定工作树并附带原因
git worktree lock ../hotfix --reason "存放在外部移动硬盘上"
```

**4. `move` - 移动工作树**
**核心用法**：将一个链接工作树移动到新的物理路径。注意：不能移动主工作树，也不能移动包含子模块的工作树。
**示例**：
```bash
# 将工作树从 ../hotfix 移动到 /backup/hotfix
git worktree move ../hotfix /backup/hotfix
```

**5. `prune` - 修剪失效记录**
**核心用法**：清理仓库中已失效的工作树管理信息（例如：工作树的物理目录被手动删除了，但 Git 内部还有记录）。
**示例**：
```bash
# 清理失效的工作树记录
git worktree prune

# 模拟清理（仅显示会被清理的内容，不实际执行）
git worktree prune -n
```

**6. `remove` - 删除工作树**
**核心用法**：安全删除一个链接工作树及其关联文件。前提：工作树必须是“干净”的（无未提交修改、无未跟踪文件）。如果有修改，需加 `-f` 强制删除。不能删除主工作树。
**示例**：
```bash
# 删除干净的工作树
git worktree remove ../hotfix

# 强制删除有未提交修改的工作树
git worktree remove -f ../hotfix
```

**7. `repair` - 修复工作树连接**
**核心用法**：修复因手动移动物理目录（而非使用 `git worktree move`）导致的主仓库与链接工作树之间的连接断裂问题。
**示例**：
```bash
# 场景A：手动移动了链接工作树，在该链接工作树内执行修复
cd /new/path/to/worktree
git worktree repair

# 场景B：手动移动了多个链接工作树，在主仓库内指定新路径进行批量修复
git worktree repair /new/path/to/worktree1 /new/path/to/worktree2
```

**8. `unlock` - 解锁工作树**

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

