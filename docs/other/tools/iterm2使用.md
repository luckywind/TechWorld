# 快捷键

## 标签

```
新建标签：command + t
关闭标签：command + w
切换标签：command + 数字    或者  command + 左右方向键
切换全屏：command + enter
查找：command + f
```

## 分屏

```
垂直分屏：command + d

水平分屏：command + shift + d

切换屏幕：command + option + 方向键 command + [ 或 command + ]

查看历史命令：command + ;

查看剪贴板历史：command + shift + h
```

## 其他

```
粘贴：中键
⌘ + shift + h 会列出剪切板历史
```

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







# 上传下载

[参考](https://github.com/islishude/blog/issues/249)

但不是很优雅，总有报错
