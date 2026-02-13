# 输出到文件

1. **实时输出**
   `stdbuf -o0 iperf -s -p 16003 -i 2 >> "$port0_rx" &`
   stdbuf -o0的作用就是**将标准输出改为“无缓冲”模式**。这样一来，`iperf`产生的每一段数据都会绕过缓冲区 立即写入文件

2. **同时输出终端和文件**
   `命令 |tee -a xx.log`

3. **shell 丢掉字符串后几位**
   `${var::-3}` 丢掉 var 的后三位
   ifconfig 配置ip 地址带上掩码可以保证掩码的正确，但是带掩码的 ip 地址不能直接用于应用程序，需要在使用时丢掉

# 时间

只获取时间，不要日期 `echo "[$(date '+%T')] "`

# 参数处理

```shell
"$*"像一个“参数收集器”，它把所有参数打包成一个字符串。
例如，执行脚本 ./script.sh A B "C D"，"$*"相当于 "A B C D"
"$@"像一个“参数数组”，它把每个参数分开维护。
同样执行 ./script.sh A B "C D"，"$@"相当于三个独立的字符串："A""B""C D"
```



# 循环

```shell
find "$LOG_DIR" -type f -name "*.log" | sort | while read -r file; do
    echo -e "\n🔍 检查文件: $file"

    if ! grep -qE "err|fail" "$file"; then
        echo "⚠️ 文件无 err|fail 数据，跳过"
        continue
    fi
	grep -Ei "\berr\b|\berrs\b|\berrors\b|\berror\b|\bfail?\w" "$file"| grep -Ei "$2|$3" | while read -r line; do

		   failure_msg=" $file $line -> 存在异常"
		   echo "$failure_msg" | tee -a "$1/../$OUTPUT_FILE"
	done
done
```

## 遍历管道输出

语句1️⃣： `| while read -r file` 
逐行读取前序命令的输出，赋值给变量供循环体使用

- while read 变量名：  每读一行就把该行内容赋值给 `变量名`
- `-r` 选项：`read` 的关键参数，含义是「禁用反斜杠 `\` 的转义功能」。保证文件路径中如果包含反斜杠（比如路径中有 `a\b.log`），能被完整、正确地读取，不会把 `\` 当成转义符处理（避免路径解析错误）。

## grep

-q：  **不输出任何匹配到的内容**（不管是匹配行还是错误信息），只返回「退出状态码」来表示是否找到匹配。

-E： 扩展正则模式

`grep` 的退出状态码（脚本中用 `$?` 查看）决定了 `if` 判断的结果，规则如下：

- 退出码 `0` → 找到至少 1 个匹配内容；
- 退出码 `1` → 未找到任何匹配内容；
- 退出码 `2` → 发生错误（比如文件不存在、权限不足）。

# 日志记录

## log_command

```shell
log_command() {
    local timestamp
    timestamp=$(date "+%Y-%m-%d %H:%M:%S")
    local full_command
    full_command="$*"
    local exit_code

    echo "[$timestamp] 执行命令>>>: $full_command" | tee -a "$LOG_FILE"

    # Use bash -c to preserve more complex command strings
    if PATH="$PATH" /bin/bash -c "$full_command" 2>&1 | tee -a "$LOG_FILE"; then
        exit_code=${PIPESTATUS[0]}
    else
        exit_code=${PIPESTATUS[0]}
    fi

    echo "[$timestamp] 退出码: $exit_code" | tee -a "$LOG_FILE"

    return $exit_code
}
```

支持管道命令，例如： `log_command  ethtool $port |grep "Auto-negotiation"`

控制台确实只打印了管道的输出，但是日志里其实记录了完整内容。

# 调试

从set -eux处开始会进入调试模式，它不需要在开头。
