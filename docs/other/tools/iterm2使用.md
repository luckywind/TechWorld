# 快捷键

## 标签

新建标签：command + t
关闭标签：command + w
✅切换标签：command + 数字    或者  command + 左右方向键
✅切换全屏：command + enter
查找：command + f

向前^Tab

向后^+shift+Tab

上一个cmd <-   

✅移动command + shift + 方向键

垂直分屏：command + d

水平分屏：command + shift + d

✅聚焦窗口：command + **option** + 方向键  / command + [ 或 command + ]

> 这里上下切换会与分屏软件pane 的快捷键冲突，我取消了pane 的快捷键

✅查看剪贴板历史：command + shift + h
	command+; 查看输入过的命令，ctrl+r 搜索历史命令

✅广播输入： command+alt+i

😀 隐藏窗口，alt+空格 ， 可在`iTerm2`→`Preferences`→`Keys`-> `Hotkey` 中设置快捷键，方便快速打开或隐藏iTerm2窗口。



# Profile设置

## 免密登录到指定目录

`/usr/local/bin/sshpass -p hados ssh -t xxx@xxx "cd /opt/hados/workspace; bash -l"`

## 定制title

title分两部分：Name和括号里的Foreground Job

![image-20250711153002083](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250711153002083.png)

## 记录会话日志

Session里对Logging打勾，填上记录的目录和文件名

文件名:`\(creationTimeString).\(profileName).\(termid).\(iterm2.pid).\(autoLogId).log`

` 20250724_104050.bptest10.2.20.25hadosbptest20.25.w0t1p0.F621B0F3-780B-4BAA-A0E2-D5741EC278A5.489.3017292438.log) `





控制字符如何处理？

- [参考](https://www.reddit.com/r/macsysadmin/comments/pg20ue/weird_character_when_session_logging_in_terminal/?tl=zh-hans)，[这个项目](https://github.com/RadixSeven/typescript2txt)无效
- `cat output.log | sed -r 's/\x1B\[[0-9;]*[a-zA-Z]//g'`   但是导出到文件还是有部分字符在



## [使用iTerm2管理SSH服务器](https://www.javatang.com/archives/2021/11/29/13063392.html)

[iterm2安装配置指南](https://blog.myxuechao.com/post/19#01%E5%AE%89%E8%A3%85%E6%8C%87%E5%8D%97)

[sshpass记住登录密码](https://blog.csdn.net/CaptainJava/article/details/84316773)

>/usr/local/bin/sshpass -p 密码  ssh 用户@IP
>简单，唯一的麻烦是第一次需要先手动登录一下,命令如下
>/usr/local/bin/sshpass -o StrictHostKeyChecking=no -p 密码  ssh 用户@IP

添加-v参数可以获取更多信息，加参数-o StrictHostKeyChecking=no 可以跳过主机密钥检查

也可以修改配置`vim /etc/ssh/ssh_config`, 把GSSAPIAuthentication改为yes





# 上传下载

[参考](https://github.com/islishude/blog/issues/249)

但不是很优雅，总有报错
