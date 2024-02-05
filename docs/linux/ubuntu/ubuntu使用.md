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

