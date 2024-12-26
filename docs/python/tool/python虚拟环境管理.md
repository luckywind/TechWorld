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
- [`pyenv global `](https://github.com/pyenv/pyenv/blob/master/COMMANDS.md#pyenv-shell) -- 全局有效

优先级为：pyenv shell > pyenv local > pyenv global > system。即优先使用 pyenv shell 设置的版本，三种级别都没设置时才使用系统安装的版本。[安装与卸载](https://github.com/pyenv/pyenv-installer),但国内网络不行， [参考这里安装](https://blog.51cto.com/u_14320361/2488888)

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



### pyenv-virtualenv切换、配置虚拟环境

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





