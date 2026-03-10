# 工具介绍

HP 开发的网络性能测量工具，主要测试 TCP 及 UDP 吞吐量性能。测试结果主要反映系统向其他系统发送数据的速度，以及其他系统接收数据的速度。

## 命令参数

netserver -h

Usage: netserver [options]

Options:
    -h                Display this text
    -D                Do not daemonize
    -d                Increase debugging output
    -f                Do not spawn chilren for each test, run serially
    -L name,family    Use name to pick listen address and family for family
    -N                No debugging output, even if netperf asks
    -p portnum        Listen for connect requests on portnum.
    -4                Do IPv4
    -6                Do IPv6
    -v verbosity      Specify the verbosity level
    -V                Display version information and exit
    -Z passphrase     Expect passphrase as the first thing received







netperf -h

Usage: netperf [global options] -- [test options]

Global options:
    -a send,recv      Set the local send,recv buffer alignment
    -A send,recv      Set the remote send,recv buffer alignment
    -B brandstr       Specify a string to be emitted with brief output
    -c [cpu_rate]     Report local CPU usage
    -C [cpu_rate]     Report remote CPU usage
    -d                Increase debugging output
   ✅ -D 以指定间隔显示中间结果，默认单位秒。
    ✅-f G|M|K|g|m|k  设置单位，默认 m, 我们设置为 g。  小写是比特，大写是字节
    -F lfill[,rfill]* Pre-fill buffers with data from specified file
    -h                Display this text
   ✅ -H **必需**。指定运行 `netserver`的服务端IP地址。
    -i max,min        Specify the max and min number of iterations (15,1)
    -I lvl[,intvl]    Specify confidence level (95 or 99) (99)
                      and confidence interval in percentage (10)
    -j                Keep additional timing statistics
   ✅ -l testlen      指定测试持续时间（秒），默认10秒。
    -L name|ip,fam *  Specify the local ip|name and address family
    -o send,recv      Set the local send,recv buffer offsets
    -O send,recv      Set the remote send,recv buffer offset

> -m 发送缓冲区大小，-M 接收缓冲区大小
>
> | **`-m`** | 发送消息大小      | `-m <size>`        | 仅设置**发送**缓冲区大小         |
> | -------- | ----------------- | ------------------ | -------------------------------- |
> | **`-M`** | 发送+接收消息大小 | `-M <send>,<recv>` | 分别设置**发送和接收**缓冲区大小 |
>
> -R 核心作用是**启用「结果置信度 / 统计信息」输出**，帮你判断测试结果的可靠性，而非单纯给出一个 “平均数值”。

​    -n numcpu         Set the number of processors for CPU util
​    -N                Establish no control connection, do 'send' side only
​    -p port,lport*    Specify netserver port number and/or local port
​    -P 0|1            Don't/Do display test headers
​    -r                Allow confidence to be hit on result only
​    -s seconds        Wait seconds between test setup and test start
​    -S                Set SO_KEEPALIVE on the data connection
 ✅   -t  **必需**。指定测试类型。这个要放在--**参数的前面**
​    -T lcpu,rcpu      Request netperf/netserver be bound to local/remote cpu
​    -v verbosity      Specify the verbosity level
​    -W send,recv      Set the number of send,recv buffers
​    -v level          Set the verbosity level (default 1, min 0)
​    -V                Display the netperf version and exit
​    -y local,remote   Set the socket priority
​    -Y local,remote   Set the IP_TOS. Use hexadecimal.
​    -Z passphrase     Set and pass to netserver a passphrase

## 使用

[参考](https://cloud.tencent.com/document/product/213/56299)

pkill netserver && pkill netperf

服务端：`netserver`

客户端： 

| 测试场景 | 客户端运行命令                                               | SAR 监控指标 |
| -------- | ------------------------------------------------------------ | ------------ |
| UDP 64   | netperf -t UDP_STREAM -H <server ip> -l 10000 -- -m 64 -R 1 & | PPS          |
| TCP 1500 | netperf -t TCP_STREAM -H <server ip> -l 10000 -- -m 1500 -R 1 & | 带宽         |
| TCP RR   | netperf -t TCP_RR -H <server ip> -l 10000 -- -r 32,128 -R 1 & | PPS          |

发包测试： 客户端观察性能变化，取最大值

`sar -n DEV 1`

收包测试： 服务端执行`sar -n DEV 1`



显示结果中各字段含义如下表所示。

| 字段           | 含义               |
| :------------- | :----------------- |
| Socket Size    | 缓冲区大小         |
| Message Size   | 数据包大小（Byte） |
| Elapsed Time   | 测试时间（s）      |
| Message Okay   | 发送成功的报文数   |
| Message Errors | 发送失败的报文数   |
| Throughput     | 网络吞吐量（Mbps） |

netperf 的吞吐量单位是 Mbps， 一般我们使用 Gbps，除以 1000 即可。

## 多进程脚本

```shell
#!/bin/bash
count=$1
for ((i=1;i<=count;i++))
do
    echo "Instance:$i-------"
    # 下方命令可以替换为测试场景表格中的命令
    # -H 后填写服务器 IP 地址;
    # -l 后为测试时间，为了防止 netperf 提前结束，因此时间设为 10000;
    netperf -t UDP_STREAM -H <server ip> -l 10000 -- -m 64 -R 1 &
done
```

