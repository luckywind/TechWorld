# tap(三方仓库)

| 术语     | 意译   | 说明                                                         |
| :------- | :----- | :----------------------------------------------------------- |
| formula  | 配方   | 表示安装包的描述文件。复数为 formulae。                      |
| cask     | 木桶   | 装酒的器具，表示具有 GUI 界面的原生应用。                    |
| keg      | 小桶   | 表示某个包某个版本的安装目录，比如 /usr/local/Cellar/foo/0.1。 |
| Cellar   | 地窖   | 存放酒的地方，表示包的安装目录，比如 /usr/local/Cellar。     |
| Caskroom | 木桶间 | 表示类型为 Cask 的包的安装目录，比如：/usr/local/Caskroom。  |
| tap      | 水龙头 | 表示包的来源，也就是镜像源。                                 |
| bottle   | 瓶子   | 表示预先编译好的包，下载好直接使用。                         |

[brew tap](https://docs.brew.sh/Taps)

[中文介绍](https://sspai.com/post/56009)

brew tap用于添加更多仓库到列表，以供brew从其跟踪、更新、安装

默认tap假设仓库来自GitHub，但这个命令不限于任何一个地址。

## 管理应用

**🧰查找**

brew search xxx

https://formulae.brew.sh/  网页查找

**📌安装**

brew install xxx

指定版本

**✅查看**

brew list

brew info xxx

brew deps package_name  # 查看依赖关系

brew versions package_name 查看已安装的版本历史

brew ls --full package_name  查看安装路径

```bash
# 显示 Homebrew 本地的 Git 仓库
$ brew --repo
# 显示 Homebrew 安装路径
$ brew --prefix
# 显示 Homebrew Cellar 路径
$ brew --cellar
# 显示 Homebrew Caskroom 路径
$ brew --caskroom
# 缓存路径
$ brew --cache
```

**🛠️卸载**

brew uninstall xxx

brew cleanup xxx  清理旧版本

**🚀更新**

brew outdated

brew upgrade xxx

brew pin [FORMULA ...]      # 锁定某个包
brew unpin [FORMULA ...]    # 取消锁定

✅**清理**

brew cleanup # 清理所有包的旧版本
brew cleanup [FORMULA ...] # 清理指定包的旧版本
brew cleanup -n # 查看可清理的旧版本包，不执行实际操作

💻**核心目录**

/opt/homebrew   主安装目录软链接，链接到Cellar的bin目录

/opt/homebrew/bin   可执行文件软链接，链接到Cellar的具体版本目录

/opt/homebrew/Cellar  多版本存储

export PATH="/opt/homebrew/bin:$PATH"   环境变量注入





例如apache-flink的安装

1. Cellar目录维护多个版本

```shell
/opt/homebrew/Cellar/apache-flink/
├── 1.19.1/               # 具体版本目录
│   ├── bin/              # 可执行文件
│   ├── libexec/           # 核心库文件
│   └── LICENSE            # 许可文件
└── 1.18.0/               # 另一版本目录
```

2. 软链接机制：提供**当前激活版本**的统一访问点

/opt/homebrew/opt/apache-flink   指向 /opt/homebrew/Cellar/apache-flink/1.19.1

/opt/homebrew/bin/flink  指向  /opt/homebrew/Cellar/apache-flink/1.19.1/bin/flink

3. 版本切换

brew unlink apache-flink
brew link apache-flink@1.18

也可以用switch命令： brew switch apache-flink 1.18.0







### 指定版本

[参考](https://makeoptim.com/tool/brew-install-specific-version/)到仓库`git clone https://github.com/Homebrew/homebrew-cask.git`,或者`git clone https://github.com/Homebrew/homebrew-core.git`  (执行`brew info xxx` 会列出github地址),  找指定软件的指定commit， 切换到指定commit后，再安装指定.rb文件即可:

```shell
# cask仓下
brew install --cask ./Casks/<your-package-name>.rb
# core仓下, --formula也可以不加
brew install --formula ./Formula/<your-package-name>.rb
```







1. 官方多版本 formula

brew install  xxx@版本号

2. Formula Git 历史版本

[参考](https://cmichel.io/how-to-install-an-old-package-version-with-brew/)

```shell
➜  Downloads brew tap-new $USER/local-apache-flink
Initialized empty Git repository in /opt/homebrew/Library/Taps/chengxingfu/homebrew-local-apache-flink/.git/
[main (root-commit) 0172077] Create chengxingfu/local-apache-flink tap
 3 files changed, 107 insertions(+)
 create mode 100644 .github/workflows/publish.yml
 create mode 100644 .github/workflows/tests.yml
 create mode 100644 README.md
==> Created chengxingfu/local-apache-flink
/opt/homebrew/Library/Taps/chengxingfu/homebrew-local-apache-flink

When a pull request making changes to a formula (or formulae) becomes green
(all checks passed), then you can publish the built bottles.
To do so, label your PR as `pr-pull` and the workflow will be triggered.
➜  Downloads brew extract --version=1.19.0 apache-flink $USER/local-apache-flink
Error: No available formula with the name "homebrew/core/apache-flink".
Please tap it and then try again: brew tap homebrew/core
➜  Downloads brew tap
adoptopenjdk/openjdk
chengxingfu/local-apache-flink
```







## 管理后台服务

- `brew services list`： 查看所有服务
- `brew services run [服务名]`: 单次运行某个服务
- `brew services start [服务名]`: 运行某个服务，并设置开机自动运行。
- `brew services stop [服务名]`：停止某个服务
- `brew services restart`：重启某个服务。



## 常用tap

1. brew tap 列出当前tapped仓库
2. brew tap <user/repo>     添加新的tap

clone 仓库https://github.com/user/homebrew-repo 

3. brew untap  user/repo 删除指定tap



## 命令帮助

   install formula

       Many Homebrew commands accept one or more formula arguments. These arguments can take several different forms:
    
       •   The name of a formula: e.g. git, node, wget.
       •   The fully-qualified name of a tapped formula: Sometimes a formula from a tapped repository may conflict with one in homebrew/core. You
           can still access these formulae by using a special syntax, e.g. homebrew/dupes/vim or homebrew/versions/node4.
    
       •   An arbitrary file: Homebrew can install formulae from a local path. It can point to either a formula file or a bottle. Prefix relative
           paths with ./ to prevent them from being interpreted as a formula or tap name.









## 重装使用国内源

重装brew了，使用国内源，我选择了中科大

```shell
/bin/zsh -c "$(curl -fsSL https://gitee.com/cunkai/HomebrewCN/raw/master/Homebrew.sh)"
```

[知乎专栏](https://zhuanlan.zhihu.com/p/111014448)

### 443

Failed to connect to raw.githubusercontent.com port 443: Connection refused

1. 修改hosts文件185.199.108.133 raw.githubusercontent.com
2. 修改dns为114.114.114.114或者8.8.8.8

[解决办法](https://www.debugpoint.com/failed-connect-raw-githubusercontent-com-port-443/#:~:text=There%20are%20many%20ways%20to%20fix%20this.%20Try,Update%20the%20proxy%20settings%20with%20your%20network%20details)
