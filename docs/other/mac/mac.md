# shell

```shell
查看所有shell
cat /etc/shells
/bin/bash
/bin/csh
/bin/ksh
/bin/sh
/bin/tcsh
/bin/zsh

查看当前shell
echo $SHELL

切换shell
chsh -s /bin/bash
```

# 百度网盘不限速

[不限速插件](https://github.com/CodeTips/BaiduNetdiskPlugin-macOS)

```shell
安装
cd ~/Downloads && git clone https://github.com/CodeTips/BaiduNetdiskPlugin-macOS.git && ./BaiduNetdiskPlugin-macOS/Other/Install.sh
卸载
cd ~/Downloads && ./BaiduNetdiskPlugin-macOS/Other/Uninstall.sh
```

# 通知条持续时间

[修改通知条](https://howchoo.com/mac/how-to-change-the-duration-of-notifications-on-macos)

# 安装多个版本Java

[mac配置多版本java](https://segmentfault.com/a/1190000013131276)

# git中文乱码

[解决中文乱码](https://www.cnblogs.com/ayseeing/p/4268655.html)

# 网络

## wifi连接无法上网

[参考](https://blog.csdn.net/lyxleft/article/details/79971963)

```shell
1、打开系统偏好设置—>网络—>WiFi—>高级—>WiFi—>删除首选网络框内的所有网络—>点击好—>点击应用； 

2、还是在网络页面先，在边框有WiFi、蓝牙PAN、网桥等，选中WiFi，点击下面的减号删除WiFi，点击应用； 

3、再次在系统偏好设置中打开网络页面，在左边框的下方点击加号，接口选择WiFi，服务名称随便写，点击创建，然后点击打开WiFi，链接你的WiFi。应该可以上网了。亲测可行。
```

