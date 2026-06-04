# 性能指标

**官方定义**：

网络带宽是指单位时间内通过网络传输的数据量，通常以 **比特每秒**（bits per second, bps）或其衍生单位如 **Mbps**（Megabits per second）来衡量。



**计算方法**：

在 **iperf** 中，客户端通过发送一定量的数据到服务器，服务器记录接收到的数据量和时间，通过公式计算出带宽：

$$ \text{带宽} = \frac{\text{总数据量（bits）}}{\text{传输时间（seconds）}} $$



​	•	**总数据量**：iperf 会传输一个预设的数据块大小（通常以字节计量，如 -l 参数设置的数据块大小），通过网络发送到目标端口。

​	•	**传输时间**：从开始发送到最后一个数据包的接收完成时间。

在使用多个并行流（如 iperf -P 参数指定）时，iperf 会计算每个流的带宽并将其加总，以反映总带宽。

# iperf

iperf可以利用多核，而iperf3(3.16版之前)默认是单线程(单线程意味着用-P参数指定的多个连接都是用同一个CPU核处理的)。[**iperf3 parallel stream performance is much less than iperf2. Why?**](https://software.es.net/iperf/faq.html)

> Beginning with version 3.16, iperf3 is multi-threaded, which allows it to take advantage of multiple CPU cores during a test (one thread per stream). 

[iperf3也可以通过这种方式实现利用多核](https://blog.csdn.net/meihualing/article/details/129251477)，[官方文档](https://fasterdata.es.net/performance-testing/network-troubleshooting-tools/iperf/multi-stream-iperf3/)

```shell
#server
iperf3 -s -p 5101& iperf3 -s -p 5102& iperf3 -s -p 5103 & iperf3 -s -p 5104 &
# client 
   iperf3 -c 10.10.110.7 -A0,0 -T s1 -p 5101 &
   iperf3 -c 10.10.110.7 -A1,1 -T s2 -p 5102 &
   iperf3 -c 10.10.110.7 -A2,2 -T s3 -p 5103 &
   iperf3 -c 10.10.110.7 -A3,3 -T s4 -p 5104 &
# 批量杀死进程
ps -ef | grep 'iperf3 ' | awk '{print $2}' | xargs kill -9
```

这里-A2,2就是设置收发端CPU亲和性。

## 使用

```shell
服务器端：
iperf -s

客户端：
iperf -c 192.168.1.1 -t 60
在tcp模式下，客户端到服务器192.168.1.1上传带宽测试，测试时间为60秒。

iperf -c 192.168.1.1  -P 30 -t 60
客户端同时向服务器端发起30个连接线程。

iperf -c 192.168.1.1  -d -t 60
进行上下行带宽测试。
```



[参考](https://wangchujiang.com/linux-command/c/iperf.html)

## 参数

| **命令行选项**                    | **描述**                                                     |
| --------------------------------- | ------------------------------------------------------------ |
| **客户端与服务器共用选项**        |                                                              |
| -f, --format [bkmaBKMA]           | 格式化带宽数输出。支持的格式有： 'b' = bits/sec 'B' = Bytes/sec 'k' = Kbits/sec 'K' = KBytes/sec 'm' = Mbits/sec 'M' = MBytes/sec 'g' = Gbits/sec 'G' = GBytes/sec 'a' = adaptive bits/sec 'A' = adaptive Bytes/sec 自适应格式是kilo-和mega-二者之一。除了带宽之外的字段都输出为字节，除非指定输出的格式，默认的参数是a。 注 意：在计算字节byte时，Kilo = 1024， Mega = 1024^2，Giga = 1024^3。通常，在网络中，Kilo = 1000， Mega = 1000^2， and Giga = 1000^3，所以，Iperf也按此来计算比特（位）。如果这些困扰了你，那么请使用-f b参数，然后亲自计算一下。 |
| -i, --interval #                  | 设置每次报告之间的时间间隔，单位为秒。如果设置为非零值，就会按照此时间间隔输出测试报告。默认值为零。 |
| -l, --len #[kmKM]                 | 设置读写缓冲区的长度。TCP方式默认为128KB，UDP方式默认为1470字节。 |
| -m, --print_mss                   | 输出TCP MSS值（通过TCP_MAXSEG支持）。MSS值一般比MTU值小40字节。通常情况 |
| -p, --port #                      | 设置端口，与服务器端的监听端口一致。默认是5001端口，与ttcp的一样。 |
| -u, --udp                         | 使用UDP方式而不是TCP方式。参看-b选项。                       |
| -w, --window #[KM]                | 设置套接字缓冲区为指定大小。对于TCP方式，此设置为TCP窗口大小。对于UDP方式，此设置为接受UDP数据包的缓冲区大小，限制可以接受数据包的最大值。 |
| -B, --bind host                   | **绑定到主机的多个地址中的一个。对于客户端来 说，这个参数设置了出栈接口。对于服务器端来说，这个参数设置入栈接口**。这个参数只用于具有多网络接口的主机。在Iperf的UDP模式下，此参数用于绑 定和加入一个多播组。使用范围在224.0.0.0至239.255.255.255的多播地址。参考-T参数。 |
| -C, --compatibility               | 与低版本的Iperf使用时，可以使用兼容模式。不需要两端同时使用兼容模式，但是强烈推荐两端同时使用兼容模式。某些情况下，使用某些数据流可以引起1.7版本的服务器端崩溃或引起非预期的连接尝试。 |
| -M, --mss #[KM}                   | 通过TCP_MAXSEG选项尝试设置TCP最大信息段的值。MSS值的大小通常是TCP/IP头减去40字节。在以太网中，MSS值 为1460字节（MTU1500字节）。许多操作系统不支持此选项。 |
| -N, --nodelay                     | 设置TCP无延迟选项，禁用Nagle's运算法则。通常情况此选项对于交互程序，例如telnet，是禁用的。 |
| -V (from v1.6 or higher)          | 绑定一个IPv6地址。 服务端：$ iperf -s –V 客户端：$ iperf -c <Server IPv6 Address> -V 注意：在1.6.3或更高版本中，指定IPv6地址不需要使用-B参数绑定，在1.6之前的版本则需要。在大多数操作系统中，将响应IPv4客户端映射的IPv4地址。 |
| **服务器端专用选项**              |                                                              |
| -s, --server                      | Iperf服务器模式                                              |
| -D (v1.2或更高版本)               | Unix平台下Iperf作为后台守护进程运行。在Win32平台下，Iperf将作为服务运行。 |
| -R(v1.2或更高版本，仅用于Windows) | 卸载Iperf服务（如果它在运行）。                              |
| -o(v1.2或更高版本，仅用于Windows) | 重定向输出到指定文件                                         |
| -c, --client host                 | 如果Iperf运行在服务器模式，并且用-c参数指定一个主机，那么Iperf将只接受指定主机的连接。此参数不能工作于UDP模式。 |
| -P, --parallel #                  | <font color=red>服务器关闭之前保持的连接数。默认是0，这意味着永远接受连接。如果想服务端自动退出，就设置它</font> |
| **客户端专用选项**                |                                                              |
| -b, --bandwidth #[KM]             | UDP模式使用的带宽，单位bits/sec。此选项与-u选项相关。默认值是1 Mbit/sec。 |
| -c, --client host                 | 运行Iperf的客户端模式，连接到指定的Iperf服务器端。           |
| -d, --dualtest                    | **运行双向测试模式**。这将使服务器端反向连接到客户端，使用-L 参数中指定的端口（或默认使用客户端连接到服务器端的端口）。这些在操作的同时就立即完成了。如果你想要一个交互的测试，请尝试-r参数。 |
| -n, --num #[KM]                   | 传送的缓冲器数量。通常情况，Iperf按照10秒钟发送数据。-n参数跨越此限制，按照指定次数发送指定长度的数据，而不论该操作耗费多少时间。参考-l与-t选项。 |
| -r, --tradeoff                    | 往复测试模式。当客户端到服务器端的测试结束时，服务器端通过-l选项指定的端口（或默认为客户端连接到服务器端的端口），反向连接至客户端。当客户端连接终止时，反向连接随即开始。如果需要同时进行双向测试，请尝试-d参数。 |
| -t, --time #                      | 设置传输的总时间。Iperf在指定的时间内，重复的发送指定长度的数据包。默认是10秒钟。参考-l与-n选项。 |
| -L, --listenport #                | 指定服务端反向连接到客户端时使用的端口。默认使用客户端连接至服务端的端口。 |
| -P, --parallel #                  | <font color=red>线程数。指定客户端与服务端之间使用的线程数。默认是1线程。需要客户端与服务器端同时使用此参数。</font> |
| -S, --tos #                       | 出栈数据包的服务类型。许多路由器忽略TOS字段。你可以指定这个值，使用以"0x"开始的16进制数，或以"0"开始的8进制数或10进制数。 例如，16进制'0x10' = 8进制'020' = 十进制'16'。TOS值1349就是： IPTOS_LOWDELAY minimize delay 0x10 IPTOS_THROUGHPUT maximize throughput 0x08 IPTOS_RELIABILITY maximize reliability 0x04 IPTOS_LOWCOST minimize cost 0x02 |
| -T, --ttl #                       | 出栈多播数据包的TTL值。这本质上就是数据通过路由器的跳数。默认是1，链接本地。 |
| -F (from v1.2 or higher)          | 使用特定的数据流测量带宽，例如指定的文件。 $ iperf -c <server address> -F <file-name> |
| -I (from v1.2 or higher)          | 与-F一样，由标准输入输出文件输入数据。                       |
| 杂项                              |                                                              |
| -h, --help                        | 显示命令行参考并退出 。                                      |
| -v, --version                     | 显示版本信息和编译信息并退出。                               |







# [iperf3](https://iperf.fr/)

## 简介

iPerf3是用于主动测试IP网络上最大可用带宽的工具。它支持时序、缓冲区、协议（TCP，UDP，SCTP与IPv4和IPv6）有关的各种参数。对于每次测试，它都会详细的带宽报告，延迟抖动和数据包丢失。

iperf3在3.16版本之后是多线程的，可以使用多核，每个流一个线程

## 安装

yum install -y iperf3 或者 官网下载离线安装包



### 编译安装

```shell
wget https://downloads.es.net/pub/iperf/iperf-3.17.tar.gz
tar -zxf iperf-3.17.tar.gz
./configure && make && make install
#编译成功后会在src目录下生成iperf3可执行文件
root@smcr-1:~# iperf3
-bash: /usr/bin/iperf3: No such file or directory 应该退出重新登录
root@smcr-1:~# ln -s /root/iperf3/iperf-3.17/src/iperf3 /usr/bin/iperf3
root@smcr-1:~# iperf3 -v  这一步可能看到的是3.9版本，执行以下ldconfig再次查看就会是3.16
iperf 3.16+ (cJSON 1.7.15)
Linux smcr-1 5.15.0-122-generic #132-Ubuntu SMP Thu Aug 29 13:45:52 UTC 2024 x86_64
Optional features available: CPU affinity setting, IPv6 flow label, TCP congestion algorithm setting, sendfile / zerocopy, socket pacing, bind to device, support IPv4 don't fragment, POSIX threads
whereis iperf3
```

如果失败，可以尝试先执行`./bootstrap.sh`

卸载： `sudo make uninstall`



[如果遇到error while loading shared libraries: libiperf.so.0](https://blog.csdn.net/u012587734/article/details/97630278)

则执行`ldconfig`

### 参数

Usage: iperf [-s|-c host] [options]
       iperf [-h|--help] [-v|--version]

📌通用参数:
  -p, --port      #         server port to listen on/connect to
  -f, --format    [kmgKMG]  format to report: Kbits, Mbits, KBytes, MBytes
  -i, --interval  #       报告输出间隔，默认1秒，如果是0则只在结束时输出一次
  -F, --file name           服务端：读取的文件， 客户端：写入的文件
  -A, --affinity n/n,m     **设置CPU亲和性**

​	线程级别绑定，-P 4 -A 0,1,2,3  意味着4个并行流分别绑定到4个不同的核心上

  -B, --bind      <host>   **绑定特定网卡**
  -V, --verbose             more detailed output
  -J, --json              输出json
  --logfile f               send output to a log file
  --forceflush              force flushing output at every interval
  -d, --debug               emit debugging output
  -v, --version             show version information and quit
  -h, --help                show this message and quit

❤️服务端专有参数:
  -s, --server              run in server mode
  -D, --daemon        **后台运行**
  -I, --pidfile file        write PID file
  -1, --one-off           只接受一个链接然后退出 handle one client connection then exit
💛客户端专有参数:
  -c, --client    <host>    run in client mode, connecting to <host>
 ✅ -u, --udp                 UDP 测试
  -b, --bandwidth  [KMG]目标带宽大小bits/秒 （默认UDP 1M/s， TCP无限）
  --fq-rate #[KMG]          enable fair-queuing based socket pacing in
                            bits/sec (Linux only)
  -t, --time      #        **测试时间**(default 10 secs)
  -n, --bytes     #       传输字节量 (instead of -t)[KMG]
  -k, --blockcount #  传输包量 (instead of -t or -n)[KMG]
  **-l**, --len       #     **读写缓冲区**[KMG]      (default 128 KB for TCP, dynamic or 1 for UDP)
  --cport                绑定指定端口 (TCP and UDP, default: ephemeral port)
  -P, --parallel  #  **并发流数量**
  **-R**, --reverse          **反向模式** (server sends, client receives)
  -w, --window    #设置套接字缓冲区大小，TCP 模式下为窗口大小；
>  要大-w 8M  -w 16M

  -C, --congestion <algo>   set TCP congestion control algorithm (Linux and FreeBSD only)
  -M, --set-mss   #         set TCP/SCTP maximum segment size (MTU - 40 bytes)
  -N, --no-delay            set TCP/SCTP no delay, disabling Nagle's Algorithm
  -4, --version4            only use IPv4
  -6, --version6            only use IPv6
  -S, --tos N               set the IP 'type of service'
  -L, --flowlabel N         set the IPv6 flow label (only supported on Linux)
  -Z, --zerocopy            use a 'zero copy' method of sending data, **推荐试试**
  -O, --omit N             忽略前n秒
  -T, --title str           每条流的前缀
  --get-server-output       get results from server
  --udp-counters-64bit      use 64-bit counters in UDP test packets

[KMG] 表示选项支持 K/M/G suffix for kilo-, mega-, or giga-


## 使用

### 脚本

smc版本

```shell
#!/bin/bash
echo "Usage: bash $0"
set -eux
server="192.168.1.5"
r_values=("512k")
t_values=("512k")
l_values=("512k")
p_values=(128)

# 遍历每个参数的值
for p in "${p_values[@]}"; do
  for r in "${r_values[@]}"; do
   for t in "${t_values[@]}"; do
      for l in "${l_values[@]}"; do
      # 执行命令服务端和客户端参数都要加
      echo "Running command with -r $r, -t $t,-l $l,-P $p"
      ssh root@$server "smc_run -r "$r" -t "$t" iperf3 -D -s"
      smc_run -r "$r" -t "$t" iperf3 -c $server -l $l -P "$p"
      ssh root@$server "pgrep -f 'iperf3 -D -s' |xargs kill -9"
      done
    done
  done
done
```



cpu版本

```shell
#!/bin/bash
echo "Usage: bash $0"
#set -eux
server="192.168.1.5"
r_values=("512k")
t_values=("512k")
l_values=("512k")
p_values=(1 2 4 8 128)

r_values=("64k" "128k" "256k" "512k")
t_values=("64k" "128k" "256k" "512k")
l_values=("64k" "128k" "256k" "512k")
p_values=(1 2 4 8 16 32 64)
# 遍历每个参数的值
r=11
t=11
for p in "${p_values[@]}"; do
      for l in "${l_values[@]}"; do
      # 执行命令服务端和客户端参数都要加
      echo "Running command with -r $r, -t $t,-l $l,-P $p"
      ssh root@$server "iperf3 -D -s"
      iperf3 -c $server -l $l -P "$p"
      ssh root@$server "pgrep -f 'iperf3 -D -s' |xargs kill -9"
      done
done
```



### 测带宽

[iperf3使用教程](https://iii80.com/?action=show&id=1139)

```shell
## 在server端开启iperf
iperf -s

## 在clinet端通过iperf测试
iperf -c <server_ip> -P 16
```



#### 案例

测量最大可用网络带宽，检测网络延迟和抖动，并帮助诊断网络问题

iperf server:

```
iperf3 -s -p 5678
```

iperf client:

```sh
iperf3 -c <iperf-server-vm-ip> -t 0 -i 1 -b 240m -p 5678 -P 1
```

- **`-c <iperf-server-vm-ip>`**: 指定要连接的iperf服务器的IP地址。这里 `<iperf-server-vm-ip>` 需要替换为实际的iperf服务器的IP地址。
- **`-t 0`**: 实际上意味着测试将无限期地进行下去，直到用户手动停止它。
- **`-i 1`**: 设置报告间隔为1秒，即每隔1秒输出一次测试结果的报告。
- **`-b 240m`**: 设置带宽为240兆比特每秒（Mbits/sec），这是测试中要尝试达到的网络带宽。
- **`-p 5678`**: 指定客户端与服务器之间通信使用的端口号为5678。
- **`-P 1`**: 表示并行客户端的数量，但仍然是单线程，因此如果是CPU绑核，这将不会提升吞吐。
    这一点与iperf不一样， [参考](https://blog.csdn.net/xiaosheng124/article/details/121574017) 可以启动多个iperf3进程达到与iperf的-P参数多线程的效果。
- `-R`: 反向测试
- `-d`: 双向测试

测试结果解读

```shell
[  5] local 10.0.151.22 port 40446 connected to 10.0.151.21 port 5201
[ ID] Interval           Transfer     Bitrate         Retr  Cwnd
[  5]   0.00-1.00   sec   114 MBytes   953 Mbits/sec    0    369 KBytes
- - - - - - - - - - - - - - - - - - - - - - - - -
[ ID] Interval           Transfer     Bitrate         Retr
[  5]   0.00-243.04 sec   283 MBytes  9.78 Mbits/sec  2890             sender
[  5]   0.00-243.04 sec  0.00 Bytes  0.00 bits/sec                  receiver
```

- **[ ID]**: 表示测试会话的ID号。
- **Interval**: 表示测试的时间间隔，这里是0.00到243.04秒。
- **Transfer**: 表示在测试期间传输的数据量，这里是283 MBytes（兆字节）。
- **Bitrate**: 表示测试期间的平均比特率，这里是9.78 Mbits/sec（兆比特每秒）。
- **Retr**: 表示在测试期间重传的数据包数量，这里是2890个数据包。
- **Cwnd**：拥塞窗口排队数据量大小

分割线下方的数据为单位测试时间内单项数据的总和。



#### udp案例

```shell
$ iperf3 -c node15 -u  -i 1  -p 5678 
Connecting to host node15, port 5678
[  5] local 10.2.25.54 port 60595 connected to 10.0.113.15 port 5678
[ ID] Interval           Transfer     Bitrate         Total Datagrams
[  5]   0.00-1.00   sec   128 KBytes  1.05 Mbits/sec  91  
[  5]   1.00-2.00   sec   128 KBytes  1.05 Mbits/sec  91  
[  5]   2.00-3.00   sec   128 KBytes  1.05 Mbits/sec  91  
[  5]   3.00-4.00   sec   127 KBytes  1.04 Mbits/sec  90  
[  5]   4.00-5.00   sec   128 KBytes  1.05 Mbits/sec  91  
[  5]   5.00-6.00   sec   128 KBytes  1.05 Mbits/sec  91  
[  5]   6.00-7.00   sec   128 KBytes  1.05 Mbits/sec  91  
^C[  5]   7.00-7.51   sec  64.9 KBytes  1.04 Mbits/sec  46  
- - - - - - - - - - - - - - - - - - - - - - - - -
[ ID] Interval           Transfer     Bitrate         Jitter    Lost/Total Datagrams
[  5]   0.00-7.51   sec   962 KBytes  1.05 Mbits/sec  0.000 ms  0/682 (0%)  sender
[  5]   0.00-7.51   sec  0.00 Bytes  0.00 bits/sec  0.000 ms  0/0 (0%)  receiver
iperf3: interrupt - the client has terminated

$ iperf3 -c node15 -u  -b 1000M  -p 5678 
Connecting to host node15, port 5678
[  5] local 10.2.25.54 port 48944 connected to 10.0.113.15 port 5678
[ ID] Interval           Transfer     Bitrate         Total Datagrams（#数据包数量）
[  5]   0.00-1.00   sec  16.4 MBytes   137 Mbits/sec  11893  
[  5]   1.00-2.00   sec  15.9 MBytes   134 Mbits/sec  11575  
[  5]   2.00-3.00   sec  13.9 MBytes   117 Mbits/sec  10086  
[  5]   3.00-4.00   sec  13.8 MBytes   115 Mbits/sec  9989  
[  5]   4.00-5.00   sec  15.3 MBytes   128 Mbits/sec  11074  
[  5]   5.00-6.00   sec  14.3 MBytes   120 Mbits/sec  10364  
[  5]   6.00-7.00   sec  12.6 MBytes   106 Mbits/sec  9149  
[  5]   7.00-8.00   sec  11.9 MBytes  99.6 Mbits/sec  8618  
[  5]   8.00-9.00   sec  12.6 MBytes   106 Mbits/sec  9180  
[  5]   9.00-10.00  sec  13.1 MBytes   110 Mbits/sec  9516  
- - - - - - - - - - - - - - - - - - - - - - - - -
# 发送端带宽117Mbps，丢包率为0
[ ID] Interval           Transfer     Bitrate         Jitter    Lost/Total Datagrams
[  5]   0.00-10.00  sec   140 MBytes   117 Mbits/sec  0.000 ms  0/101444 (0%)  sender
# 接收端带宽10.1Mbps,丢包率91%
[  5]   0.00-10.00  sec  12.0 MBytes  10.1 Mbits/sec  1.340 ms  92696/101440 (91%)  receiver
```

- Jitter: 网络抖动，数据包在网络中的延迟变化情况

- Lost/Total Datagrams：丢包数/总数据包数（丢包率）

UDP默认是1Mbits/s（TCP不限制），所以，上面才只跑出这么低的带宽。因此，**-u通常需要与-b参数结合使用，才能测出实际链路最大带宽。**

### 64B小包测试

发送64B小包，建议使用UDP，只需要设置-l=18，即可得到64B的最小帧






### 双向测试

iperf3 不支持原生的 `-d` 或 `--dualtest` 参数来实现旧版 iperf 中那种简单的双向同时测试。这是 iperf3 和旧版 iperf（iperf2）在功能设计上的一个显著区别。

**为什么 iperf3 移除了 `-d`？**

iperf3 的设计理念更倾向于结构清晰和结果明确。`-d` 模式在单个连接上进行双向同时测试，流量会相互干扰，这可能导致：

1. **结果不够精确：** 发送和接收的数据流会竞争带宽、CPU 和网络缓冲区，可能无法真实反映网络的最大双向同时吞吐量。
2. **诊断困难：** 结果报告混合了发送和接收流量，分析瓶颈（如发送端瓶颈 vs 接收端瓶颈）比较困难。
3. **实现复杂：** 管理单个连接上的双向高流量增加了复杂性。

iperf3 推荐使用 **两个独立的、单向的测试连接** 来实现双向同时测试：
```shell
# server
iperf3 -s -D -p 5201 & iperf3 -s -D -p 5202
```

客户端

```shell
iperf3 -c <server_ip> -p 5201 -t 20 &  iperf3 -c <server_ip> -p 5202 -R -t 20 
```



正向测试 (客户端 -> 服务器)，后台运行，反向测试 (服务器 -> 客户端)，前台运行

### 参考

[网络性能测试介绍](https://hhb584520.github.io/kvm_blog/2017/01/01/perf-network.html)

