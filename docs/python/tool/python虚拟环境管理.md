# python虚拟环境工具

[python多环境管理(venv与virtualenv)](https://www.cnblogs.com/doublexi/p/15783355.html)

[细数 Python 虚拟环境的管理方案](https://sspai.com/post/75978)

[一文了解virtualenv、pyvenv、pyenv、pyenv virtualenv](https://cloud.tencent.com/developer/article/1593451)

主要四个：

1. pyenv
   pyenv 不是用来管理同一个库的多个版本，而是用来管理一台机器上的多个 Python 版本

2. pyvenv/venv

   pyvenv 与 virtualenv 功能和用法类似。不同点在于：

   1. pyvenv 只支持 Python 3.3 及更高版本，而 virtualenv 同时支持 Python 2.x 和 Python 3.x；
   2. pyvenv 是 Python 3.x 自带的工具，不需要安装，而 virtualenv 是第三方工具，需要安装。

   pyvenv 实际上是 Python 3.x 的一个模块 venv，等价于 python -m venv。

3. virtualenv
   virtualenv 所要解决的是同一个库不同版本共存的兼容问题。

   例如项目 A 需要用到 requests 的 1.0 版本，项目 B 需要用到 requests 的 2.0 版本。如果不使用工具的话，一台机器只能安装其中一个版本，无法满足两个项目的需求。

   virtualenv 的解决方案是为每个项目创建一个独立的虚拟环境，在每个虚拟环境中安装的库，对其他虚拟环境完全无影响。所以就可以在一台机器的不同虚拟环境中分别安装同一个库的不同版本。

4. pyenv virtualenv

   前面提到 pyenv 要解决的是多个 Python 的版本管理问题，virtualenv 要解决的是同一个库的版本管理问题。但如果两个问题都需要解决呢？分别使用不同工具就很麻烦了，而且容易有冲突。为此，pyenv 引入了了 virtualenv 插件，可以在 pyenv 中解决同一个库的版本管理问题。

   通过 pyenv virtualenv 命令，可以与 virtualenv 类似的创建、使用虚拟环境。但由于 pyenv 的垫片功能，使用虚拟环境跟使用 Python 版本的体验一样，不需要手动执行 activate 和 deactivate，只要进入目录即生效，离开目录即失效。

5. conda

6. poetry

6. uv 据说更好用，还没试过



# pyenv 

[pyenv神器原理分析](https://cloud.tencent.com/developer/article/1593478?from_column=20421&from=20421)

## 使用

Python 多版本及虚拟环境管理器，支持：

- 使用与系统不同的 Python 版本或虚拟环境
- 每个项目使用不同的 Python 版本或虚拟环境
- 通过环境变量切换不同的 Python 版本或虚拟环境
- 同时使用多个 Python 版本或虚拟环境的命令

### 使用pyenv切换、配置Python版本

pyenv 使用了垫片的原理，使用某个 Python 版本或虚拟环境完全是自动的，无需手动指定。

```shell
# 安装 pyenv（推荐方法，此脚本会自动安装若干插件，包括下文即将提到的 pyenv virtualenv）
curl https://pyenv.run | bash
# 查看所有支持安装的 Python 版本
pyenv install -l
# 安装 Python 2.7.17 和 3.8.2
pyenv install 2.7.17
pyenv install 3.8.2

pyenv global 2.7.17 # 指定全局使用 Python 2.7.17 需要退出当前终端，再次进入执行python
pyenv local 3.8.2 # 指定当前目录使用 Python 3.8.2
pyenv shell 3.8.2 # 在当前 shell 中临时使用 Python 3.8.2
```

- [`pyenv shell `](https://github.com/pyenv/pyenv/blob/master/COMMANDS.md#pyenv-shell) -- 当前会话有效的版本
- [`pyenv local `](https://github.com/pyenv/pyenv/blob/master/COMMANDS.md#pyenv-local) -- 当前目录及子目录下生效的版本
  pyenv local --unset 撤销
- [`pyenv global `](https://github.com/pyenv/pyenv/blob/master/COMMANDS.md#pyenv-shell) -- 全局有效

优先级为：pyenv shell > pyenv local > pyenv global > system。即优先使用 pyenv shell 设置的版本，三种级别都没设置时才使用系统安装的版本。[安装与卸载](https://github.com/pyenv/pyenv-installer),但国内网络不行， [参考这里安装](https://blog.51cto.com/u_14320361/2488888)



```shell
# Load pyenv automatically by appending
# the following to
# ~/.bash_profile if it exists, otherwise ~/.profile (for login shells)
# and ~/.bashrc (for interactive shells) :

export PYENV_ROOT="$HOME/.pyenv"
[[ -d $PYENV_ROOT/bin ]] && export PATH="$PYENV_ROOT/bin:$PATH"
eval "$(pyenv init - bash)"

# Restart your shell for the changes to take effect.

# Load pyenv-virtualenv automatically by adding
# the following to ~/.bashrc:

eval "$(pyenv virtualenv-init -)"
```

但是测试发现pyenv local可以完成虚拟环境的切换，但是python总是指向`/Users/chengxingfu/.pyenv/shims/python`,  改用下面的配置就可以了：

```shell
export PYENV_ROOT="$HOME/.pyenv"
export PATH="$PYENV_ROOT/bin:$PATH"
if command -v pyenv 1>/dev/null 2>&1; then
 eval "$(pyenv init -)"
fi
eval "$(pyenv virtualenv-init -)"
```





#### 可能出现的问题

1. 安装python时报错`python-build: line 1805: patch: command not found`

解决办法:`yum install patch`

```shell
yum install zlib zlib-devel openssl  bzip2-devel ncurses-devel
```

1. `ModuleNotFoundError: No module named '_ssl'`

```shell
CPPFLAGS="$(pkg-config --cflags openssl11)" LDFLAGS="$(pkg-config --libs openssl11)" pyenv install  3.9.7
```

[centos默认的openssl版本太低，python3+的版本要求openssl1.1.1](https://blog.csdn.net/soupersoul/article/details/139182250)

3. ` setlocale: LC_CTYPE: cannot change locale (zh_CN.UTF-8): No such file or directory`

`sudo yum -y install glibc-locale-source glibc-langpack-zh`

### pyenv-virtualenv切换、配置虚拟环境

#### 安装pyenv-virtualenv

```shell
git clone https://github.com/pyenv/pyenv-virtualenv.git $(pyenv root)/plugins/pyenv-virtualenv

echo 'eval "$(pyenv virtualenv-init -)"' >> ~/.bashrc
```



#### 使用

```shell
pyenv virtualenvs   #查看
pyenv virtualenv 3.9.7 py3.9.7  #新建指定版本的虚拟环境
pyenv activate py3.9.7  #进入
pyenv deactivate py3.9.7 #退出
pyenv uninstall py3.9.7 #删除

和设置python版本一样，也可以给全局、目录、shell配置虚拟环境
pyenv local venv2	# 本目录及子目录使用基于虚拟环境 venv2
pyenv shell venv3	# 当前 shell 临时使用基于虚拟环境 venv3
```

虚拟环境存储位置`~/.pyenv/versions/py3.9.7`, pycharm就可以选择这个作为项目解释器

![image-20241128180557260](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20241128180557260.png)

#### shell中切换虚拟环境

可以在shell中写激活命令`pyenv activate hados-env`,  需要在当前shell中激活虚拟环境时，使用source 运行该脚本，这样该脚本中的修改对当前shell有效。如果直接执行这个脚本，这个激活命令会在一个新的shell进程中有效，但对当前进程无效。

建议使用pyenv local xxx设置路径下的默认虚拟环境

### 虚拟环境同步

```shell
将当前虚拟环境中，pip的安装包和版本保存
(v368) [python@master pydir]$ pip freeze > requirement
将虚拟环境所有的包，移植到新的环境中。如此保证两个环境的包是一致的
(v368) [python@master pydir]$ pip install -r requirement
```

## 示例环境

```shell
➜  hados pyenv versions #打印所有Python环境，包括虚拟环境，以两种格式打印虚拟环境
  system
* 3.9.7 (set by /Users/chengxingfu/.pyenv/version)
  3.9.7/envs/hados-env  
  3.9.7/envs/py3.9.7
  						❤️#虚拟环境映射到实际的路径，IDE里可以直接填这个实际的路径来启用虚拟环境！
  hados-env --> /Users/chengxingfu/.pyenv/versions/3.9.7/envs/hados-env
  py3.9.7 --> /Users/chengxingfu/.pyenv/versions/3.9.7/envs/py3.9.7
  
  
➜  hados pyenv virtualenvs #仅打印虚拟环境，以两种格式打印虚拟环境<Python版本号>/envs/<虚拟环境名称>
  3.9.7/envs/hados-env (created from /Users/chengxingfu/.pyenv/versions/3.9.7)
  3.9.7/envs/py3.9.7 (created from /Users/chengxingfu/.pyenv/versions/3.9.7)
  hados-env (created from /Users/chengxingfu/.pyenv/versions/3.9.7)
  py3.9.7 (created from /Users/chengxingfu/.pyenv/versions/3.9.7)
  
  
  
目录结构
/Users/chengxingfu/.pyenv/versions/3.9.7
/Users/chengxingfu/.pyenv/versions/3.9.7/envs/hados-env
/Users/chengxingfu/.pyenv/versions/3.9.7/envs/py3.9.7
/Users/chengxingfu/.pyenv/versions/hados-env -> /Users/chengxingfu/.pyenv/versions/3.9.7/envs/hados-env
/Users/chengxingfu/.pyenv/versions/py3.9.7 -> /Users/chengxingfu/.pyenv/versions/3.9.7/envs/py3.9.7
```

py3.9.7的依赖包随意安装

hados-env只安装项目需要的包



## 命令帮助

```shell
➜  ~ pyenv --help
Usage: pyenv <command> [<args>]

Some useful pyenv commands are:
   --version   Display the version of pyenv
   activate    Activate virtual environment
   commands    List all available pyenv commands
   deactivate   Deactivate virtual environment
   exec        Run an executable with the selected Python version
   global      #设置全局Python版本
   help        Display help for a command
   hooks       List hook scripts for a given pyenv command
   init        Configure the shell environment for pyenv
   install     Install a Python version using python-build
   latest      Print the latest installed or known version with the given prefix
   local       #设置当前目录python版本 Set or show the local application-specific Python version(s)
   prefix      Display prefixes for Python versions
   rehash      Rehash pyenv shims (run this after installing executables)
   root        Display the root directory where versions and shims are kept
   shell       #设置当前shell使用的python版本
   shims       List existing pyenv shims
   uninstall   #卸载python版本
   version     # Show the current Python version(s) and its origin 包括虚拟环境
   version-file   Detect the file that sets the current pyenv version
   version-name   Show the current Python version
   version-origin   Explain how the current Python version is set
   versions    #列出可用的python版本
   virtualenv   [python version] <virtualenv-name> # Create a Python virtualenv using the pyenv-virtualenv plugin
   virtualenv-delete   #删除虚拟环境
   virtualenv-init   Configure the shell environment for pyenv-virtualenv
   virtualenv-prefix   Display real_prefix for a Python virtualenv version
   virtualenvs   List all Python virtualenvs found in `$PYENV_ROOT/versions/*'.
   whence      List all Python versions that contain the given executable
   which       Display the full path to an executable
```



## 原理

Linux 执行命令时，是依次遍历 PATH 环境变量的每个路径，查找所执行的命令。当在某个目录下找到第一个匹配时即停止遍历，所以 PATH 环境变量中，前面的路径比后面的路径具有更高的优先级。

pyenv 在 ~/.pyenv/shims 目录下创建了各种 python 相关命令的垫片（~/.bashrc 中加入的命令调用 pyenv-rehash 生成的，pyenv install 命令也会调用 pyenv-rehash 进行更新）

当我们执行某个命令 program "param1" "param2" ……时，实际执行的是 pyenv exec "program" "param1" "param2" ……。

```python
#!/usr/bin/env bash
#
# Summary: Run an executable with the selected Python version
#
# Usage: pyenv exec <command> [arg1 arg2...]
#
# Runs an executable by first preparing PATH so that the selected Python
# version's `bin' directory is at the front.
#
# For example, if the currently selected Python version is 2.7.6:
#   pyenv exec pip install -r requirements.txt
#
# is equivalent to:
#   PATH="$PYENV_ROOT/versions/2.7.6/bin:$PATH" pip install -r requirements.txt

set -e
[ -n "$PYENV_DEBUG" ] && set -x

# Provide pyenv completions
if [ "$1" = "--complete" ]; then
  exec pyenv-shims --short
fi

PYENV_VERSION="$(pyenv-version-name)"   # 确定版本号
PYENV_COMMAND="$1"

if [ -z "$PYENV_COMMAND" ]; then
  pyenv-help --usage exec >&2
  exit 1
fi

export PYENV_VERSION
PYENV_COMMAND_PATH="$(pyenv-which "$PYENV_COMMAND")"  # 找到与版本号对应的可执行文件
PYENV_BIN_PATH="${PYENV_COMMAND_PATH%/*}"

OLDIFS="$IFS"
IFS=$'\n' scripts=(`pyenv-hooks exec`)
IFS="$OLDIFS"
for script in "${scripts[@]}"; do
  source "$script"
done

shift 1
if [ "${PYENV_BIN_PATH#${PYENV_ROOT}}" != "${PYENV_BIN_PATH}" ]; then
  # Only add to $PATH for non-system version.
  export PATH="${PYENV_BIN_PATH}:${PATH}"   #把bin目录放到$PATH前面
fi
exec "$PYENV_COMMAND_PATH" "$@"   # 执行命令
```



## 多用户共享

[pyenv-multiuser](https://github.com/macdub/pyenv-multiuser)插件可以完成，但未尝试。

# pycharm切换python环境

## 切换

[参考官方文档](https://www.jetbrains.com/help/pycharm/configuring-python-interpreter.html#widget)

方法一：从状态拦切换

![image-20250415175354504](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250415175354504.png)

方法二：IDE设置
![image-20250415175522930](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250415175522930.png)

## 新建

### 从已有环境新建解释器

![image-20250415180655115](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250415180655115.png)

`venv/bin/python`

## 执行器的修改和删除

![image-20250415175927824](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250415175927824.png)



# 软件包管理

## pip命令

| 命令      | 命令解释                                         | 功能描述                                                   |
| --------- | ------------------------------------------------ | ---------------------------------------------------------- |
| install   | Install packages                                 | 在线或离线安装依赖包                                       |
| download  | Download packages                                | 下载离线依赖包                                             |
| uninstall | Uninstall packages                               | 卸载依赖包                                                 |
| freeze    | Output installed packages in requirements format | 将已经安装的依赖包输出到文件                               |
| list      | List installed packages                          | 显示当前环境已经安装的依赖包                               |
| show      | Show information about installed packages        | 显示已经安装的依赖包的详细信息，如版本、依赖库、被依赖库等 |
| wheel     | Build wheels from your requirements              | 构建适配当前环境的离线依赖包                               |

```shell
# install 离线安装包
$python -m pip install --no-index --find-links=".\packages" requests
$python -m pip install --no-index --find-links=".\packages" -r requirements.txt
# show 显示已经安装的包的相关信息及其依赖包
$python -m pip show Django  
Name: Django
Version: 3.2.25
Summary: A high-level Python Web framework that encourages rapid development and clean, pragmatic design.
Home-page: https://www.djangoproject.com/
Author: Django Software Foundation
Author-email: foundation@djangoproject.com
License: BSD-3-Clause
Location: /Users/chengxingfu/.pyenv/versions/3.9.7/envs/env-hardci/lib/python3.9/site-packages
Requires: sqlparse, pytz, asgiref
Required-by: djangorestframework
# 导出whl文件
$python -m pip wheel requests
# 下载指定whl包 
$pip download --only-binary=:all: --platform=win_amd64 --python-version=2.7 -d pk requests -i http://mirrors.aliyun.com/pypi/simple/ --trusted-host mirrors.aliyun.com

```















默认虚拟环境使用全局安装的系统包。 但也可以通过`--no-site-packages`选项安装独立的包。

> 1.7 使用--no-site-packages参数使用系统包

## pip切换源

```shell
# 增加参数
-i https://pypi.tuna.tsinghua.edu.cn/simple
```

也可以永久配置

```shell
vim ~/.pip/pip.conf
  
[global]
index-url = https://pypi.tuna.tsinghua.edu.cn/simple
```





## 离线环境迁移

### 离线python和pip

#### 下载

[参考](https://developer.aliyun.com/article/1248300)

1. 下载python源码包

```shell
wget  https://www.python.org/downloads/release/python-397/
wget https://www.python.org/ftp/python/3.9.7/Python-3.9.7.tgz
```

2. 下载pip及其依赖

   ```shell
   wget https://bootstrap.pypa.io/get-pip.py
   mkdir pip-offline
   cd pip-offline
   python -m pip download pip setuptools wheel
   ```

[pip的官网](https://pip.pypa.io/en/stable/installation/)说pip是跟随python自动安装的：
![image-20250520141342486](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250520141342486.png)

只需要建立链接就行
```shell
ln -s /usr/local/python3.9/bin/pip3.9 /usr/bin/pip3
```



##### Ubuntu下载依赖

方法一：

```shell
# 在有网络的 Ubuntu 机器上执行
mkdir debs
cd debs
# 安装 apt-rdepends 工具
sudo apt install apt-rdepends
# 生成 Python 编译依赖列表
apt-get download $(apt-rdepends python3.9 build-essential libssl-dev zlib1g-dev libncurses5-dev libsqlite3-dev libreadline-dev libtk8.6 libgdm-dev libdb4o-cil-dev libpcap-dev | grep -v "^ ")

```

方法二：[需要先安装好依赖库](https://blog.csdn.net/ChaimMacTavish/article/details/140389651), 安装一个库build-essential 及其依赖库

```shell
# 安装“apt-rdepends”工具
sudo apt-get install apt-rdepends

# 下载build-essential的依赖包
apt-rdepends build-essential | grep "^\w" > build-essential-deps.txt

# 建立并进入安装包的缓存目录
mkdir build-essential-deps
cd build-essential-deps

# 下载所有依赖包
for pkg in $(cat ../build-essential-deps.txt); do apt-get download $pkg; done
```





##### Centos下载依赖

[Centos 7离线安装Python3](https://blog.csdn.net/weixin_43807520/article/details/128662769) 这里提供了一个百度网盘

[Linux离线安装Python](https://www.cnblogs.com/LittleMaster/p/16469534.html)

cat /etc/redhat-release  查看系统版本

```shell
> download.sh 文件内容

# 基础编译工具
dependencies=(
zlib zlib-devel bzip2-devel epel-release ncurses-devel mpfr libmpc kernel-headers glibc glibc-common glibc-headers glibc-devel cpp gcc libffi-devel libgcc libgomp libstdc++ libstdc++-devel gcc-c++
)
# 创建下载目录
mkdir -p ~/yum-packages
# 下载依赖包（含所有子依赖）
yum reinstall -y --downloadonly --downloaddir=/home/hados/yum-packages ${dependencies[@]}
```





安装依赖

```shell
方法1
rpm -Uvh --force --nodeps *rpm
方法2
cd /path/to/yum-packages
yum localinstall -y *.rpm
```



#### 安装

##### Ubuntu离线安装python

事先下载好： zlib1g-dev libbz2-dev libssl-dev libncurses5-dev  libsqlite3-dev libreadline-dev tk-dev libgdbm-dev libdb-dev libpcap-dev xz-utils libexpat1-dev   liblzma-dev libffi-dev  libc6-dev

```shell
sudo tar -zxvf Python-3.9.7.tgz -C ~
cd Python-3.9.7
# 在线命令sudo apt-get install zlib1g-dev libbz2-dev libssl-dev libncurses5-dev  libsqlite3-dev libreadline-dev tk-dev libgdbm-dev libdb-dev libpcap-dev xz-utils libexpat1-dev   liblzma-dev libffi-dev  libc6-dev
# 离线安装，需要先下载好
sudo dpkg -i *.deb
sudo ./configure --prefix=/usr/local/python3
sudo make
sudo make test
sudo make install
PATH=$PATH:$HOME/bin:/usr/local/python3/bin
ln -s /usr/local/python3/bin/python3.9 /usr/bin/python3
ln -s /usr/local/python3/bin/pip3.9 /usr/bin/pip3
# 校验是否安装成功
python3 --version

```



##### Centos离线安装Python





### pip迁移离线包

示例

```shell
# 下载requirements.txt所有包到packages目录
$ python -m pip download --only-binary=:all: --platform=manylinux2014_x86_64 --python-version=3.9.7 -d ./packages -r requirements.txt -i http://mirrors.aliyun.com/pypi/simple/ --trusted-host mirrors.aliyun.com


# 从packages目录安装
$ python -m pip install --no-index --find-links=".\packages" -r requirements.txt

```

`--only-binary=:all:`: 强制下载 `.whl` 二进制文件，而不下载源代码

`--python-version 38`: 表示目标平台上使用 Python 3.8





常见平台标识对照表：


| 平台        | 架构                  | `--platform` 值                              |
| ----------- | --------------------- | -------------------------------------------- |
| **Linux**   | x86 (32-bit)          | `manylinux1_i686`, `manylinux2014_i686`      |
| **Linux**   | x86_64 (64-bit)       | `manylinux1_x86_64`, `manylinux2014_x86_64`  |
| **Linux**   | ARM (32-bit)          | `manylinux2014_armv7l`                       |
| **Linux**   | ARM (64-bit)          | `manylinux2014_aarch64`                      |
| **Linux**   | PowerPC (64-bit)      | `manylinux2014_ppc64le`                      |
| **Linux**   | IBM Z (s390x)         | `manylinux2014_s390x`                        |
| **Windows** | x86 (32-bit)          | `win32`                                      |
| **Windows** | x86_64 (64-bit)       | `win_amd64`                                  |
| **macOS**   | x86_64 (64-bit)       | `macosx_10_9_x86_64`, `macosx_11_0_x86_64`   |
| **macOS**   | ARM64 (Apple Silicon) | `macosx_11_0_arm64`, `macosx_12_0_arm64`     |
| **Linux**   | RISC-V (64-bit)       | `manylinux2014_riscv64`                      |
| **FreeBSD** | x86_64 (64-bit)       | `freebsd_11_0_x86_64`, `freebsd_12_0_x86_64` |
| **Solaris** | SPARC (64-bit)        | `solaris_2_11_sparc`                         |
| **Solaris** | x86_64 (64-bit)       | `solaris_2_11_x86_64`                        |
| **AIX**     | PowerPC (64-bit)      | `aix_7_2_ppc64`                              |







# uv

[uv使用](https://blog.frognew.com/2025/03/uv-as-python-package-manager.html)

[uv github](https://github.com/astral-sh/uv)

```shell
# On macOS and Linux.
法一
curl -LsSf https://astral.sh/uv/install.sh | sh
法二
brew install uv

export PATH="$HOME/.local/bin:$PATH"
```

## 基本使用

### python版本

```shell
#查看可用的python版本:
uv python list
#安装python:
uv python install 3.9.7  
```

### 管理项目

uv支持管理Python项目，这些项目在`pyproject.toml`文件中定义了它们的依赖项。

### 全部命令

```shell
Commands:
  run      Run a command or script
  init     初始化项目
  add      添加依赖
  remove   删除依赖
  sync     Update the project's environment
  				 安装项目依赖
  lock     Update the project's lockfile
  		--upgrade-package requests  单独升级依赖requests
  export   Export the project's lockfile to an alternate format
  tree     Display the project's dependency tree
  tool     Run and install commands provided by Python packages， 代替pipx
			install [toolname]  安装工具
			list 查看已安装的工具
  python   Manage Python versions and installations
  pip      Manage Python packages with a pip-compatible interface
  venv     创建虚拟环境
				source .venv/bin/activate  激活环境
				deactivate 退出虚拟环境
				--seed  强制安装基础包（如pip, setuptools, wheel）
  build    Build Python packages into source distributions and wheels
  publish  Upload distributions to an index
  cache    管理uv缓存
  				dir 
  				clean
  				purge
  self     Manage the uv executable
  version  Read or update the project's version
  help     Display documentation for a command
```

虚拟环境中安装软件包

```shell
uv pip install flask                # Install Flask.
uv pip install -r requirements.txt  # Install from a requirements.txt file.
uv pip install -e .                 # Install current project in editable mode.
uv pip install "package @ ."        # Install current project from disk
uv pip install "flask[dotenv]"      # Install Flask with "dotenv" extra.
```



