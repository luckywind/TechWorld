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

![image-20210902154912934](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210902154912934.png)

录频选择“聚集设备”

麦克风选择"内置麦克风"    因为我目前只有这个麦克风

扬声器选择 "多输出设备" 才能听到对方声音

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20210902163739119.png" alt="image-20210902163739119" style="zoom:25%;" />



## Soundflower使用



# 破解软件大全

https://www.macwk.com/soft/all/p1

上面的网站永久关闭了，可用下面这个

https://macpedia.xyz/

[folx下载器](https://www.zhiniw.com/folx_pro-mac.html)

## 打不开无法检查

[打开任意](https://www.yaxi.net/2020-05-09/1990.html)

```shell
打开显示“任何来源”
sudo spctl --master-disable
关闭显示“任何来源”
sudo spctl --master-enable
```

# word图片乱码

可用预览打开

# 查看操作系统安装时间

```shell
ls -l /var/db/.AppleSetupDone
```

# mdworker问题

http://www.rawinfopages.com/mac/content/mdworker-and-mds-osx-problems-solved-speed-your-mac

这是用于sporlight快速搜索的一个服务，停止索引某些文件夹可加速

![image-20220721094005367](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220721094005367.png)
>>>>>>> d17e9164be9ecc119a5256bf914cb0e40ac7eabb

# vpn

[ios vpn对比](https://archive.blog.hly0928.com/post/talk-about-some-proxy-apps-on-ios/)

## clashX设置

[clashX](https://clashx.org/clashx-node/)

#### 忽略某些网站的代理

多个域名之间需要用逗号分割，保存后会自动转为、。

```shell
*adip.devhub.yusur.tech*、*yusur*、192.168.0.0/16、10.0.0.0/8、172.16.0.0/12、127.0.0.1、localhost、*.local、timestamp.apple.com、sequoia.apple.com、seed-sequoia.siri.apple.com
```





![image-20241028161953293](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20241028161953293.png)

问题： 每次关闭代理，重新打开，新增的域名就没了，发现是被clash给覆盖了。

解决：clashx -> 更多设置 里填写要忽略的域名，有效！

>  [ClashX配置指定域名不经过代理](https://www.shiqidu.com/d/1023), 新建文件，自测无效



#### 终端代理

[参考](https://limbopro.com/archives/MacOS_terminal_proxy_setting.html)

开启：proxy

关闭：unproxy

## anyConnect

129.146.138.229:1443

sthoffer/2022  缺点是连接后无法访问内网









## [greenHub](chrome-extension://knmhokeiipedacnhpjklbjmfgedfohco/options.html)

每天60分钟



# 效率

## 多屏切换

[切换窗口](https://blog.csdn.net/guolindonggld/article/details/122262113)

[NTFS软件](https://www.macyy.cn/archives/78#J_DLIPPCont)

激活码：3VV2VF-HQZ01U-MZ5ZU9-ZVEH45-H6CFE4

## 开机自启动

[参考](https://www.cnblogs.com/x-kq/p/17952462)

# 休眠命令

## 合盖不休眠

```shell
# 禁止合盖休眠
alias dislid='sudo pmset -b sleep 0; sudo pmset -b displaysleep 0; sudo pmset -b disablesleep 1'
# 启动合盖休眠
alias enlid='sudo pmset -b sleep 15; sudo pmset -b displaysleep 10; sudo pmset -b disablesleep 0'
```

最近遇到一个问题：

mac在不接电源时，如果合盖则会自动进入休眠模式，外接屏同时黑屏。再次敲击键盘恢复时可能要几十秒，太耽误事儿。

解决：就是执行上面那个禁止合盖休眠的命令，然后合盖状态即使断点外接屏也不会黑屏。这个命令可以放到开机自动执行任务里，完美。



## 命令说明

1. 命令如下：
    `sudo shutdown [-h | -r | -s] [time]`
    **此命令需要管理员权限**
2. 参数说明：

- `-h` ：关机（halt）

- `-r` ：重启（reboot）

- `-s` ：休眠（sleep）

- ```
  time
  ```

   ：执行操作的时间

  - yymmddhhmm ：指定年月日时分，如 17022318 表示2017年2月23日18时
  - now ：现在
  - +n ：n分钟后
  - hh:mm ：今天某时某分

1. 其他关机和重启命令
    立刻关机：`sudo halt`
    立刻重启：`sudo reboot`

# 软件问题

## 已损坏

https://sysin.org/blog/macos-if-crashes-when-opening/

```shell
 sudo spctl --master-disable                                                                                     1 ↵
 sudo xattr -dr com.apple.quarantine /Applications/ChatGPT.app
```

## 双面打印

[参考](https://h30471.www3.hp.com/t5/da-yin-ji-shi-yong-xiang-guan-wen-ti/Mac-xi-tong-word-bu-neng-shuang-mian-da-yin/td-p/1133834)

在打印的页中，打印方向下方默认是布局选项。
将布局改为：纸张处理。
第一次打印时，打印页数选择：仅奇数页
第二次打印机，将纸张平移到纸中后，打印页数选择：仅偶数页，页面顺序改为：倒序。

# 切换地区

## iphone修改地区

- **切换AppleID地区**

1. 打开“设置”App。
2. 轻点你的姓名，然后轻点“媒体与购买项目”。
3. 轻点“显示账户”。系统可能会要求你登录。
4. 轻点“国家/地区”。
5. 选择新国家或地区。
6. 轻点“更改国家或地区”。
7. 轻点新国家或地区，然后查看“条款与条件”。
8. 轻点右上角的“同意”，然后再次轻点“同意”以确认。

電話: (559) 592-2538 商家名稱: The Book Garden
美國地址
街道: 189 E Pine St. 地圖 行車路線
城市: Exeter, 州: CA（加利福尼亚州）, 郵遞區號: 93221

姓：cheng  名:xing

[数据来源](https://blog9go.com/learning/article/374)

- **切换系统地区**

查看地区： 设置->通用->语言与地区

这里切换地区需要重启手机

> 切换后，可以后台播放youtube



apple Music添加支付方式，需要两者都切换会中国大陆

- 取消支付宝免密支付项目

在支付宝-我的-设置-支付设置-免密支付/自动扣款-找到不想续费的关掉就可以啦

apple Music取消： 点击头像->管理订阅。





# 多版本java



# appStore

1364983233@qq.com

Aa001132

# mac词典

[少数派](https://sspai.com/post/43155)

[](https://www.bilibili.com/opus/421455537215631785)

## 字体

[免费下载](https://chinesefonts.org/)
