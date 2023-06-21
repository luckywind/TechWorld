# tap(三方仓库)

[brew tap](https://docs.brew.sh/Taps)

[中文介绍](https://sspai.com/post/56009)

brew tap用于添加更多仓库到列表，以供brew从其跟踪、更新、安装

默认tap假设仓库来自GitHub，但这个命令不限于任何一个地址。

## 命令

1. brew tap 列出当前tapped仓库
2. brew tap <user/repo> 

clone 仓库https://github.com/user/homebrew-repo 

3. brew untap  user/repo 删除指定tap



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
