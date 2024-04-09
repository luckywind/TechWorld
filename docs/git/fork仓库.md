# fork

一个fork是一个仓库的拷贝，fork一个仓库可以让我们自由的更改代码而不影响原始original仓库

## 修bug

1. fork仓库
2. 修复bug
3. 给项目所有者提交pull request

## 使用别人的工程作为起点

## 保持fork的同步

假设fork了一个仓库，fork后地址为https://github.com/YOUR-USERNAME/Spoon-Knife

克隆到本地

```shell
git clone https://github.com/YOUR-USERNAME/Spoon-Knife
```

我们可以配置git从original或者upstream仓库拉取更新到我们的fork仓库的本地客隆

### 添加上游远程仓库

复制源仓库的地址https://github.com/octocat/Spoon-Knife.git

到本地客隆路径执行git remote -v 可以看到当前配置的远程仓库

```shell
$ git remote -v
> origin  https://github.com/YOUR_USERNAME/YOUR_FORK.git (fetch)
> origin  https://github.com/YOUR_USERNAME/YOUR_FORK.git (push)
```

把复制的原始仓库地址添加为上游仓库

```shell
$ git remote add upstream https://github.com/octocat/Spoon-Knife.git
```

此时查看本地客隆的远程仓库：

```shell
$ git remote -v
> origin    https://github.com/YOUR_USERNAME/YOUR_FORK.git (fetch)
> origin    https://github.com/YOUR_USERNAME/YOUR_FORK.git (push)
> upstream  https://github.com/ORIGINAL_OWNER/ORIGINAL_REPOSITORY.git (fetch)
> upstream  https://github.com/ORIGINAL_OWNER/ORIGINAL_REPOSITORY.git (push)
```

现在就可以保持和源仓库保持同步了： 

### 同步fork

```shell
$git fetch upstream # 提交到master的commits将会存储到本地分支，upstream/master
From https://github.com/Snailclimb/springboot-guide
 * [new branch]      master     -> upstream/master
$git checkout master #检出fork的本地master分支
Already on 'master'
Your branch is up to date with 'origin/master'.
从upstream/master合并更新到本地master分支，这样fork仓库的本地mster分支就和上游远程仓库同步了，且本地更改不会丢失
$git merge upstream/master
```



总结

upstream与origin是并列的关系，他们都是远程仓库的名字，只不过origin是你所克隆的原始仓库的默认名字，upstream是fork的原项目的远程仓库的默认名字。

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20211128103033628.png" alt="image-20211128103033628" style="zoom:50%;" />



git fetch upstream //将远程仓库新的提交抓取到本地仓库

git checkout master

git merge upstream/master
