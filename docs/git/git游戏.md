[答案](https://blog.csdn.net/qq_34519487/article/details/107882290)

![image-20201121215649179](https://gitee.com/luckywind/PigGo/raw/master/image/image-20201121215649179.png)

![image-20201121215718393](https://gitee.com/luckywind/PigGo/raw/master/image/image-20201121215718393.png)

们首先看一下 “HEAD”。 HEAD 是一个对当前检出记录的符号引用 —— 也就是指向你正在其基础上进行工作的提交记录。

HEAD 总是指向当前分支上最近一次提交记录。大多数修改提交树的 Git 命令都是从改变 HEAD 的指向开始的。

HEAD 通常情况下是指向分支名的（如 bugFix）。在你提交时，改变了 bugFix 的状态，这一变化通过 HEAD 变得可见。

分离HEAD:  git checkout 某个提交，就把HEAD从指向分支变成指向提交

^ 和 ~+数字 可以基于分支名、HEAD或者提交按相对位置改变HEAD指向

![image-20201121220901968](https://gitee.com/luckywind/PigGo/raw/master/image/image-20201121220901968.png)

![image-20201121221452781](https://gitee.com/luckywind/PigGo/raw/master/image/image-20201121221452781.png)

在reset后， `C2` 所做的变更还在，但是处于未加入暂存区状态。

![image-20201121221712591](https://gitee.com/luckywind/PigGo/raw/master/image/image-20201121221712591.png)

![image-20201121222057942](https://gitee.com/luckywind/PigGo/raw/master/image/image-20201121222057942.png)

两个父节点：

~后面的数字是用来指定向上返回几代，但是合并提交是有两个父节点的，～1到底要到哪个父节点呢？ 这就需要^了，它后面可以接1或者2，1代表提交时间最近的父节点(^的默认操作)，2代表另一个。

远程分支反映了远程仓库在你**最后一次与它通信时**的状态，`git fetch` 就是你与远程仓库通信的方式了！

`git fetch` 并不会改变你本地仓库的状态。它不会更新你的 `master` 分支，也不会修改你磁盘上的文件。

 `git pull` 就是 git fetch 和 git merge 的缩写！

