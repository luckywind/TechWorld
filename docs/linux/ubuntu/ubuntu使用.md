# 包管理

常用的包管理包含三类工具：dpkg、apt和aptitude。人们总是对前面的两个工具用得比较多，而对 aptitude 用得比较少，事实上 aptitude 是很强大的。

在这里，对这三个工具做一点总结。

dpkg 主要是对本地的软件包进行管理，本地软件包包括已经在本地安装的软件包和已经下载但还没有安装的 deb 文件，不解决依赖关系。

apt 包含了很多工具，apt-get 主要负责软件包的在线安装与升级，低层对 deb 包的处理还是用的 dpkg，解决依赖关系；apt-cache 主要用来查询软件包的状态和依赖关系；apt-file 主要负责查询软件包名称和软件包包含的文件（值得注意的是它要自己同步）；apt-cross 主要负责为交叉编译的软件包的安装与编译等。apt 还包含很多工具，如 apt-offline 可以离线安装软件包，apt-build 可以简化源码编译等等，有兴趣可以学习一下 apt 开头软件包。用下面的命令可以得到所有以 apt 开头的软件包。

```
aptitude search ~n^apt
```

注：~n 意思是搜索软件包名，^ 是匹配最前面

aptitude 是更强大的安装工具，有两种基本的使用方法，一种是文本界面，另一种是命令行，这里只讨论命令行操作。

下面从安装软件包的顺序来描述这些工具的使用。

## **查找软件包**

dpkg --get-selections pattern #查找软件包名称包含 pattern 的软件包，可以在后面用 grep install/deinstall 来选择是否已经被 remove 的包(曾经安装过了的)
apt-cache search pattern #查找软件包名称和描述包含 pattern 的软件包 (可以是安装了也可以是没有安装)，可以用参数来限制是否已经安装
aptitude search ~i #查找已经安装的软件包
aptitude search ~c #查找已经被 remove 的软件包，还有配置文件存在
aptitude search ~npattern #查找软件包名称包含 pattern 的软件包 (可以是安装了也可以是没有安装)
aptitude search \!~i~npattern #查找还没有安装的软件包名字包含 pattern 的软件包。(前面的 ! 是取反的意思，反划线是 escape 符号)
注：还有很多用法，可以去看看我在 forum 中写的帖子 aptitude Search Patterns[[1\]](http://forum.ubuntu.org.cn/viewtopic.php?f=52&t=259550)

apt-cache depends package #查找名称是 package 软件包的依赖关系
aptitude search ~R~npackage #查找名称是 package 软件包的依赖关系，可以同时看到是不是已经安装

apt-cache rdepends package #查找哪些软件包依赖于名称是 package 软件包
aptitude search ~D~npackage #查找哪些软件包依赖于名称是 package 软件包

dpkg -I package_name.deb #参数是大写i，查找已经下载但末安装的 package_name.deb 软件包的信息
dpkg -l package #参数是小写L，查找已经安装软件包 package 的信息，精简
apt-cache show pattern ##查找软件包pattern的信息 (可以是安装了也可以是没有安装)
aptitude show ~npattern #显示名称是 pattern 软件包的信息(可以是安装了也可以是没有安装)

apt-cache policy pattern #显示 pattern 软件包的策略(可以是安装了也可以是没有安装)
apt-cache showpkg pattern #显示pattern 软件包的其它信息(可以是安装了也可以是没有安装)

dpkg -S pattern #查找已经安装的文件 pattern 属于哪个软件包
apt-file search pattern #查找文件 pattern 属于哪个软件包(可以是安装了也可以是没有安装)

dpkg -c package_name.deb #查找已经下载但末安装的 package.deb 软件包包含哪些文件
dpkg -L package #查找已经安装 package 软件包包含哪些文件
apt-file show pattern #查找 pattern 软件包(可以是安装了也可以是没有安装)包含哪些文件

## **下载软件包**

apt-get install package -d #下载软件包
aptitude download pattern #同上，不同的是下载的是符合 pattern 的软件包，后面不再指出

## **安装软件包**

dpkg -i package_name.deb #安装本地软件包，不解决依赖关系
apt-get install package #在线安装软件包
aptitude install pattern #同上

apt-get install package --reinstall #重新安装软件包
apitude reinstall package #同上

## **移除软件包**

dpkg -r package #删除软件包
apt-get remove package #同上
aptitude remove package #同上

dpkg -P #删除软件包及配置文件
apt-get remove package --purge #删除软件包及配置文件
apitude purge pattern #同上

**自动移除软件包**
apt-get autoremove #删除不再需要的软件包
注：aptitude 没有，它会自动解决这件事

**清除下载的软件包**
apt-get clean #清除 /var/cache/apt/archives 目录
aptitude clean #同上

apt-get autoclean #清除 /var/cache/apt/archives 目录，不过只清理过时的包
aptitude autoclean #同上

**编译相关** apt-get source package #获取源码
apt-get build-dep package #解决编译源码 package 的依赖关系
aptitude build-dep pattern #解决编译源码 pattern 的依赖关系

**平台相关**
apt-cross --arch ARCH --show package 显示属于 ARCH 构架的 package 软件包信息
apt-cross --arch ARCH --get package #下载属于 ARCH 构架的 package 软件包
apt-cross --arch ARCH --install package #安装属于 ARCH 构架的 package 软件包
apt-cross --arch ARCH --remove package #移除属于 ARCH 构架的 package 软件包
apt-cross --arch ARCH --purge package #移除属于 ARCH 构架的 package 软件包
apt-cross --arch ARCH --update #升级属于 ARCH 构架的 package 软件包
注：慎重考虑要不要用这种方法来安装不同构架的软件包，这样会破坏系统。对于 amd64 的用户可能需要强制安装某些 i386 的包，千万不要把原来 amd64 本身的文件给 replace 了。最好只是安装一些 lib 到 /usr/lib32 目录下。同样地，可以用 apt-file 看某个其它构架的软件包包含哪些文件，或者是文件属于哪个包，不过记得最先要用 apt-file --architecture ARCH update 来升级 apt-file 的数据库，在 search 或 show 时也要指定 ARCH。

**更新源**
apt-get update #更新源
aptitude update #同上

**更新系统**
apt-get upgrade #更新已经安装的软件包
aptitude safe-upgrade #同上
apt-get dist-upgrade #升级系统
aptitude full-upgrade #同上

## deb

```shell
sudo dpkg -i package.deb
```

## 破解typora

[破解typora并创建快捷方式](https://blog.csdn.net/weixin_42905141/article/details/124071137)

# 通知栏

[参考](https://ubuntuqa.com/article/787.html)

仓库没有release文件，

1. 打开终端，进入etc/apt/sources.list.d
`cd /etc/apt/sources.list.d`

2. 删除对应文件

# 终端

https://gitcode.com/asbru-cm/asbru-cm/overview?utm_source=csdn_github_accelerator&isLogin=1

# 死机

## 可尝试的解决方法

进入TTY终端

1. Ctrl+Alt+F1进入TTY1终端字符界面, 输入用户名和密码以登录
2. 输入top命令, 找到可能造成假死的进程, 用kill命令结束掉进程。然后Ctrl+Alt+F7回到桌面

直接注销用户

Ctrl+Alt+F1进入TTY1终端字符界面, 输入用户名和密码以登录。

然后执行以下的任意一个命令注销桌面重新登录。

```mipsasm
sudo pkill Xorg
```

或者

```undefined
sudo restart lightdm
```

### 底层方法

如果上面两种方法不成功, 那有可能是比较底层的软件出现问题。

可以试试 :** reisub 方法**。

说具体一点, 是一种系统请求, 直接交给内核处理。

键盘上一般都有一个键SysRq, 和PrintScreen(截屏)在一个键位上，这就是系统请求的键。

这个方法可以在死机的情况下安全地重启计算机, 数据不会丢失。

下面解释一下这个方法：

> 其实 SysRq是一种叫做系统请求的东西, 按住 Alt-Print 的时候就相当于按住了SysRq键，这个时候输入的一切都会直接由 Linux 内核来处理，它可以进行许多低级操作。

这个时候 reisub 中的每一个字母都是一个独立操作，分别表示：

- r : unRaw 将键盘控制从 X Server 那里抢回来
- e : terminate 给所有进程发送 SIGTERM 信号，让它们自己解决善后
- i : kIll 给所有进程发送 SIGKILL 信号，强制他们马上关闭
- s : Sync 将所有数据同步至磁盘
- u : Unmount 将所有分区挂载为只读模式
- b : reBoot 重启

##### 魔法键组合 reisub 究竟该怎么用？

如果某一天你的 Linux 死机了，键盘不听使唤了，Ctrl+Alt+F1 已经没有任何反应，该怎么办呢？

使用“魔法键”：Alt+SysRq + r,e,i,s,u,b（确实很好背，就是单词 busier (英语"更忙"的意思)的倒写）。

好的，平时电脑那么正常，你自然也不会去按这些按钮。等到真的出事的时候，你把记在小纸条上的这些 tips 拿出来，然后在键盘上按，结果发现啥反应也没有，于是只能欲哭无泪了。

##### 问题在于：究竟该怎么按这些按钮才会有效？

首先，你的系统要支持这个功能，查看和开启的方法大家应该很熟悉了，网上也有很多说明，而且最幸运的是：Ubuntu 默认已经开启了这个功能。

接下来就是操作：马上你就会发现，同时按下<Alt>+<SysRq>压根儿行不通！只会蹦出来一个屏幕截图窗口。所以，真正的做法应该是：

1. 伸出你的左手，同时按住<Ctrl>+<Alt>键，别松开
2. 右手先按一下<SysRq>，左手别松开，等1秒
3. 右手按一下 R，左手别松开，等1秒
4. 右手按一下 E，左手别松开。这时包括桌面在内，所有程序都会终止，你会看到一个黑乎乎的屏幕，稍微等一段时间
5. 右手依次按下 I，S，U，B，左手别松开。每按一次都等那么几秒种，你会发现每按一次，屏幕上信息都会有所变化。最后按下B时，屏幕显示reset，这时你的左手可以松开了，等几秒钟，计算机就会安全重启。

# 磁盘

## fsck

```
Emergency help:
 -p                   Automatic repair (no questions)
 -n                   Make no changes to the filesystem
 -y                   Assume "yes" to all questions
 -c                   Check for bad blocks and add them to the badblock list
 -f                   Force checking even if filesystem is marked clean
 -v                   Be verbose
 -b superblock        Use alternative superblock
 -B blocksize         Force blocksize when looking for superblock
 -j external_journal  Set location of the external journal
 -l bad_blocks_file   Add to badblocks list
 -L bad_blocks_file   Set badblocks list
```

### unable to set superblock flags on

```yaml

unable to set superblock flags on 怎么解决
报错信息 "unable to set superblock flags on" 通常出现在Linux文件系统（如ext4）的日志管理过程中，可能是因为文件系统处于挂起状态或者文件系统的元数据损坏导致无法设置超级块的标志。
原因是文件系统因为故障切换到只读模式了

解决方法：
sudo e2fsck -C0 -p -f -v /dev/sdb1   参考https://ubuntuforums.org/showthread.php?t=2282368
检查硬件问题：
如果文件系统损坏是由硬件问题导致的（如硬盘损坏），可能需要更换硬件。
查看日志文件：
检查dmesg或/var/log/syslog等日志文件，可能会有更详细的错误信息，帮助确定问题的根源。


List backup superblocks:
sudo dumpe2fs /dev/sda5 | grep -i backup
then use backup superblock, 32768 just an example, try several
sudo fsck -b 32768 /dev/sda5
```



[HOWTO: Repair a broken Ext4 Superblock in Ubuntu](https://linuxexpresso.wordpress.com/2010/03/31/repair-a-broken-ext4-superblock-in-ubuntu/)

```shell
sudo mke2fs -n /dev/xxx  列出backup superblock
#使用backup  superblock 修复文件系统 
sudo e2fsck -b 第一个backup-block_number /dev/xxx
```

都不行，强制关机解决



### initramfs无法退出

```shell
echo 1 > /proc/sys/kernel/sysrq
echo b > /proc/sysrq-trigger
这两个命令可以通过将1写入/proc/sys/kernel/sysrq文件来启用SysRq键，然后使用echo b > /proc/sysrq-trigger命令强制关闭计算机。请注意，在使用此命令之前请确保保存所有重要数据。
```

### fsck exited with status code 4

有时文件系统有问题，但是Fsck检查不出，可以用-f参数强制

```shell
fsck -yvf xxx
```

[再参考从u盘修复](https://www.cnblogs.com/miaojx/p/15955592.html)
