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

# quickTime录制屏内声音

[录屏内声音](https://blog.csdn.net/haifangnihao/article/details/105028491)

扬声器选择"多路输出设备"

多输出设备：可以将音频输出同时镜像到多个设备。

聚集设备：可以将多个设备捆绑在一起，以使其成为一个I / O数量比任何单个设备都多的单个设备

[Filmage Screen录制屏幕](https://www.filmagepro.com/zh-cn/help/how-to-record-system-audio)

![image-20210902154912934](https://gitee.com/luckywind/PigGo/raw/master/image/image-20210902154912934.png)

录频选择“聚集设备”

麦克风选择"内置麦克风"    因为我目前只有这个麦克风

扬声器选择 "多输出设备" 才能听到对方声音

<img src="https://gitee.com/luckywind/PigGo/raw/master/image/image-20210902163739119.png" alt="image-20210902163739119" style="zoom:25%;" />

# 破解软件大全

https://www.macwk.com/soft/all/p1

## 打不开无法检查

[打开任意](https://www.yaxi.net/2020-05-09/1990.html)

```shell
打开显示“任何来源”
sudo spctl --master-disable
关闭显示“任何来源”
sudo spctl --master-enable
```

