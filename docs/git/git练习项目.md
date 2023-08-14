本文参考http://www.qtcn.org/bbs/simple/?t53628.html

# 代码修改

fork了一个仓库https://github.com/luckywind/tianchi/tree/master

```shell
git clone https://github.com/luckywind/tianchi.git
开启一个新分支进行代码修改
git checkout -b mybranch
修改完推送到github,此时github上会有我的修改
git push origin mybranch
```

此时可以看一下log

```shell
mybranch > git log
commit 8ceb74626ccf72296acbb628940bc6740a5852d9 (HEAD -> mybranch, origin/mybranch)
Author: chengxingfu <1318247907@qq.com>
Date:   Tue Aug 8 14:32:03 2023 +0800

    finish my work

commit bc752da936c42c3fed01b84815f9855863df6d82 (origin/master, origin/HEAD, master)
Merge: b1f2797 977a2b8
Author: XChinux <XChinux@163.com>
Date:   Sat Dec 18 20:17:50 2021 +0800

    Merge pull request #114 from XChinux/master

    add Qt6-windows support
```

# 添加远程仓库

```shell
git remote add upstream https://github.com/qtcn/tianchi.git
git remote -v
origin	https://github.com/luckywind/tianchi.git (fetch)
origin	https://github.com/luckywind/tianchi.git (push)
upstream	https://github.com/qtcn/tianchi.git (fetch)
upstream	https://github.com/qtcn/tianchi.git (push)
```

注意，不要使用git@githubxx这个地址，否则不能拉取更新

# 导入上游的仓库的更新

就是让当前开发的分支mybranch的开发基础(base)推进到一个新的起点，而不像merge那样引入一个Merge xxx commits。

导入之前，我们需要拉取一下上游仓库的更新到本地仓库

```shell
git pull --all
拉取后，可以看到remote仓库upstream的master分支已经到本地仓库，且用upstream/master表示
git branch -a
  master   
* mybranch
  remotes/origin/HEAD -> origin/master
  remotes/origin/master
  remotes/origin/mybranch
  remotes/upstream/master
```

然后就可以进行rebase了，

```shell
（假设上游branch为upstream/master）
$ git rebase upstream/master mybranch 
或者直接（如果当前branch已经是mybranch）：
$ git rebase upstream/master
```



# 创建pull request

GitHub的界面：左边选择base branch，右边选择head branch，这里就是mybranch。

**base branch****：相当于target branch，你希望Pull Request被merge到上游项目的哪个branch里。为什么要叫**base** branch：base可以理解为你在进行git rebase操作时的那个“base”，也就是你的主题branch所基于的开发base（基础）。

**head branch**：相当于source branch，你希望自己开发库里的哪个branch被用来进行Pull Request（当然也就是mybranch）。