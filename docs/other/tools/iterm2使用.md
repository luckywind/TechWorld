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

✅窗口组合： 点击切换到需要组合的 tab，拖动到想要停留的位置，会和前一个 tab 组合

# Profile设置

## 免密登录到指定目录

`/usr/local/bin/sshpass -p hados ssh -t xxx@xxx "cd /opt/hados/workspace; bash -l"`

## 定制title

显示IP 地址：Name 选择Profile & Session Name

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

/opt/local/bin/iterm2-zmodem

```shell
# This is a re-implementation of the shell scripts "iterm2-recv-zmodem.sh" and
# "iterm2-send-zmodem.sh" found at https://github.com/mmastrac/iterm2-zmodem
#

# usage
if [[ $1 != "sz" && $1 != "rz" ]]; then
    echo "usage: $0 sz|rz"
    exit
fi

# send Z-Modem cancel sequence
function cancel {
	echo -e \\x18\\x18\\x18\\x18\\x18
}

# send notification using growlnotify
function notify {
    local msg=$1
    
    if command -v growlnotify >/dev/null 2>&1; then
        growlnotify -a /Applications/iTerm.app -n "iTerm" -m "$msg" -t "File transfer"
    else
        echo "# $msg" | tr '\n' ' '
    fi
}

#setup
[[ $LRZSZ_PATH != "" ]] && LRZSZ_PATH=":$LRZSZ_PATH" || LRZSZ_PATH=""

PATH=$(command -p getconf PATH):/opt/homebrew/bin:/usr/local/bin$LRZSZ_PATH
ZCMD=$(
    if command -v $1 >/dev/null 2>&1; then
        echo "$1"
    elif command -v l$1 >/dev/null 2>&1; then
        echo "l$1"
    fi
)

# main
if [[ $ZCMD = "" ]]; then
    cancel
    echo

    notify "Unable to find Z-Modem tools"
    exit
elif [[ $1 = "rz" ]]; then
    # receive a file
    DST=$(
        osascript \
            -e "tell application \"iTerm\" to activate" \
            -e "tell application \"iTerm\" to set thefile to choose folder with prompt \"Choose a folder to place received files in\"" \
            -e "do shell script (\"echo \"&(quoted form of POSIX path of thefile as Unicode text)&\"\")"
    )
    
    if [[ $DST = "" ]]; then
        cancel
        echo 
    fi

	cd "$DST"
	
    notify "Z-Modem started receiving file"

    $ZCMD -e -y
    echo 

    notify "Z-Modem finished receiving file"
else
    # send a file
    SRC=$(
        osascript \
            -e "tell application \"iTerm\" to activate" \
            -e "tell application \"iTerm\" to set thefile to choose file with prompt \"Choose a file to send\"" \
            -e "do shell script (\"echo \"&(quoted form of POSIX path of thefile as Unicode text)&\"\")"
    )

    if [[ $SRC = "" ]]; then
        cancel
        echo 
    fi

    notify "Z-Modem started sending
$SRC"

    $ZCMD -e "$SRC"
    echo 

    notify "Z-Modem finished sending
$SRC"
fi
```

然后 配置两个Trigger：

```shell
Regular expression: \*\*B0100
Action:             Run Coprocess
Parameters:         /usr/local/bin/iterm2-zmodem sz
instant: true

Regular expression: \*\*B00000000000000
Action:             Run Coprocess
Parameters:         /usr/local/bin/iterm2-zmodem rz
instant: true
```

