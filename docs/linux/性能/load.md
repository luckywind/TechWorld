[参考](https://www.computerworld.com/article/2833435/how-to-interpret-cpu-load-on-linux.html)

在 Linux 系统下，可以使用许多工具来监控 CPU、内存和磁盘资源的使用情况。下面是三个常用的监控工具：

1. top：用于实时查看系统的进程状态、CPU 使用率和内存使用情况等。在终端输入 top 命令即可启动该工具。top 命令的选项和功能非常丰富，可以通过 man top 命令查看详细文档。
2. vmstat：用于显示系统虚拟内存的使用情况和统计进程、IO 等系统状态。在终端输入 vmstat 命令即可启动该工具。vmstat 命令的选项和用法在上一个问题中已经介绍过。
3. iostat：用于监控磁盘 I/O 操作的情况。在终端输入 iostat 命令即可启动该工具。iostat 命令可以显示磁盘读写速率、I/O 请求等信息。iostat 命令的选项和用法可以通过 man iostat 命令查看详细文档。

除了上述工具，还有一些其他的资源监控工具，例如 sar、nmon、htop 等。

# cpu

通常服务器的 CPU 占用率在 75%以内是正常的，如果长期在 90%以上，就需要将其看作性能瓶颈进行排查。CPU 占用率高，原因通常如下。

- 代码问题。例如递归调用（当退出机制设计不合理时）、死循环、并发运行了大量线程。
- <font color=red>物理内存不足。操作系统会使用虚拟内存，造成过多的页交换而引发 CPU 使用率高。</font>
- 大量磁盘 I/O 操作。它会让系统频繁中断和切换，引发 CPU 占用率高。
- 执行计算密集型任务。
- 硬件损坏或出现病毒。

## top-进程cpu

https://baijiahao.baidu.com/s?id=1745901459006156810&wfr=spider&for=pc

![img](https://pic.rmb.bdstatic.com/bjh/down/1d522cd61ecaeebf7620bd3b6b573722.png?x-bce-process=image/watermark,bucket_baidu-rmb-video-cover-1,image_YmpoL25ld3MvNjUzZjZkMjRlMDJiNjdjZWU1NzEzODg0MDNhYTQ0YzQucG5n,type_RlpMYW5UaW5nSGVpU01HQg==,w_24,text_QOeoi-W6j-mCo-eCueS6iw==,size_24,x_18,y_18,interval_2,color_FFFFFF,effect_softoutline,shc_000000,blr_2,align_1)

1. `top`：是 top 命令的进程 ID。

2. `Tasks`：表示当前系统运行的进程数，包括正在运行、等待、停止和僵尸进程。

3. `%Cpu(s)`：包含 CPU 使用率的相关信息：
   - `us`：用户占用 CPU 百分比。
   - `sy`：内核占用 CPU 百分比。
   - `ni`：用户进程空间内改变过优先级的进程占用 CPU 的百分比。
   - `id`：系统空闲 CPU 百分比。
   - `wa`：等待 I/O 的 CPU 占用百分比。
   - `hi`：硬件中断（Hardware IRQ）占用的CPU百分比。
   - `si`：软件中断（Software IRQ）占用的CPU百分比。
   
4. `Mem`：包含内存使用情况的相关信息：
   - `total`：总内存大小。
   - `used`：已使用的内存大小。
   - `free`：可用的内存大小。
   - `buf/cache`：用于缓存（Buffer 和 Cache）的内存大小。
   
5. `Swap`：包含交换空间（Swap）使用情况的相关信息：
   - `total`：总交换空间大小。
   - `used`：已使用的交换空间大小。
   - `free`：可用的交换空间大小。
   

表头：

1. `PID`：表示进程的 ID。

7. `USER`：表示进程的所属用户。

8. PR，优先级值。

9. NI，nice值，通过程序给进程设置的。

10. VIRT，进程使用的虚拟内存的大小，单位是KiB。

11. RES，常驻内存的内存大小，单位是KiB。

12. SHR，共享内存的大小，单位是KiB。

13. S，表示进程的状态，有一下几个状态。

    ```shell
    D，不能够中断的睡眠状态。
    R，表示程序正在CPU上执行。
    S，表示进程正在睡眠。
    T，进程被信号停止执行。
    t，表示进程正在被调试器追踪，调试器将这个进程停了下来。
    Z，zombie表示是一个僵尸进程。
    ```
    
    
    
14. `%CPU`：表示进程使用的 CPU 百分比。

15. `%MEM`：表示进程使用的内存百分比。

16. `TIME+`：表示进程运行的时间，包括了用户态和内核态的时间。

11. `COMMAND`：表示进程的命令行。

top - 17:42:30 up  6:16,  4 users,  load average: 0.10, 0.05, 0.18
Tasks: 979 total,   2 running, 977 sleeping,   0 stopped,   0 zombie
%Cpu(s):  0.0 us,  0.0 sy,  0.0 ni, 99.9 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st
KiB Mem : 26341355+total, 23542512+free, 13165988 used, 14822444 buff/cache
KiB Swap: 32767996 total, 32767996 free,        0 used. 24934009+avail Mem 

   PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND                                                                                                        
 18343 hadoop    20   0   33.3g   1.4g  24468 S   1.7  0.6   4:08.27 java                                                                                                           
 18136 hadoop    20   0   33.2g   1.0g  24284 S   0.7  0.4   2:33.80 java                                                                                                           
 22114 root      20   0   66.3g 582940  15292 S   0.7  0.2   0:58.09 java                                                                                                           
 38599 root      20   0 6696292 533136   7044 S   0.7  0.2   1:17.10 cpptools                                                                                                       
 39293 root      20   0  114080   2272   1232 S   0.7  0.0   0:18.11 bash                                                                                                           
 72641 root      20   0  163028   3232   1584 R   0.7  0.0   0:01.27 top                                                                                                            
     9 root      20   0       0      0      0 R   0.3  0.0   0:17.67 rcu_sched  



![image-20231227154448187](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231227154448187.png)





## vmstat-系统cpu

用于展示系统虚拟内存的使用情况和统计进程、IO 等系统状态。以下是 vmstat 的基本用法：

语法

```
vmstat [options] [delay [count]]
```

参数

- `delay`：数据采样的时间间隔。
- `count`：数据采样的次数。

选项

- `-a`：显示所有状态信息。
- `-n`：禁止显示标题。
- `-s`：显示虚拟内存的总量和使用情况。
- `-d`：显示磁盘统计信息。

### 字段含义

```shell
procs -----------memory---------- ---swap-- -----io---- --system-- -----cpu-----
 r  b   swpd   free   buff  cache   si   so    bi    bo    in   cs  us  sy  id  wa  st
 0  0      0 1038756  99464 978408    0    0     0     1    45  306   5   1  94   0   0
 0  0      0 1035496  99464 978412    0    0     0    16  1104 2518   5   1  94   0   0
r：排队等待 CPU 调度的进程数。
b：处于不可中断状态（blocked）的进程数。
swpd：已使用的虚拟内存交换空间大小，即虚拟内存使用总量（单位为 KB）。
free：空闲的物理内存大小（单位为 KB）。
buff：用作缓存的内存大小（单位为 KB）。
cache：用作高速缓存的内存大小（单位为 KB）。

si：从磁盘读入交换页的速度（单位为 KB/s）。
so：把交换页写到磁盘的速度（单位为 KB/s）。

bi：从块设备（磁盘）读入的块数（单位为 1KB/s）。
bo：输出（写）到块设备（磁盘）的块数（单位为 1KB/s）。

in：每秒中断的数量。
cs：每秒上下文切换的数量。

us：用户 CPU 时间占用百分比。
sy：内核 CPU 时间占用百分比。
id：空闲 CPU 时间百分比。
wa：等待 I/O 操作完成的 CPU 时间百分比。
st：由于 hypervisor 需要导出虚拟 CPU 给虚拟机，导致虚拟机等待 CPU 时间的百分比。
```

- r：<font color=red>运行中的队列数，如果该数值长期大于 CPU 数，则出现 CPU 硬件的瓶颈。</font>
- us：用户进程执行时间百分比，简单来说，该数值高通常是由写的程序引起的。
- sy：内核系统进程执行时间百分比。
- wa：<font color=red>磁盘 I/O 等待时间百分比，数值较高时表明 I/O 等待较为严重。</font>
- id：空闲时间百分比。

### 实例

1. 显示每秒的 CPU 上下文切换次数和 CPU 使用率：

```
vmstat 1 5
```

以上命令表示每隔一秒采样一次数据，共采样 5 次，输出结果中包含 CPU 上下文切换次数和 CPU 使用率等信息。

1. 显示内存使用情况：

```
vmstat -s
```

以上命令用来显示内存总量、空闲内存、已使用内存和缓冲区、缓存的内存等信息。

1. 显示磁盘统计信息：

```
vmstat -d
```

以上命令用来显示磁盘读写速率、磁盘阻塞次数、I/O 请求等信息。

```shell
#vmstat -d
disk- ------------reads------------ ------------writes----------- -----IO------
       total merged sectors      ms  total merged sectors      ms    cur    sec
nvme0n1 1908647  13825 98693242  270188 14930197 275495 11542964320 2559325216      0   6221
sda   20752870 679770 5400761313 273230592 4959935   8362 5052433608 297034814      0  68519
dm-0  1908378      0 98525158  269509 15116567      0 11562054640 2561719427      0   5308
dm-1   13096      0  108472    2623  89070      0  712560 2702737      0     11

reads，writes：在这两行中输出读写操作的统计信息。
total：读写操作的总次数。
merged：操作合并的总次数，即多个操作在短时间内执行的次数。
sectors：数据传输的总字节数。
ms：数据传输的总时间。
------IO------：在这行中输出磁盘 I/O 统计信息。
cur：表示当下的活跃 I/O 请求
sec：I/O 请求总时间（单位是微秒）。
```



# 内存

# 磁盘

## iostat

当磁盘成为性能瓶颈时，一般会出现磁盘 I/O 繁忙，导致执行程序在 I/O 处等待。在 Linux 中，使用 top 命令查看 wa 数据，判断 CPU 是否长时间等待 I/O。

用 iostat -x 命令查看磁盘工作时间，返回数据中的 %util 是磁盘读写操作的百分比时间，如果超过 70%就说明磁盘比较繁忙了，返回的 await 数据是平均每次磁盘读写的等待时间。

## fio

```shell
fio -direct=1 -iodepth=32 -rw=randwrite -ioengine=libaio -bs=4k -size=1G -numjobs=1 -runtime=60 -group_reporting -filename=/dev/testblock -name=Rand_Write_Testing
```



- `-direct=1`: 表示使用直接I/O模式，绕过操作系统的缓存，直接对磁盘进行读写操作。
- `-iodepth=32`: 设置I/O深度为32，即同时进行的I/O操作数量为32个。
- `-rw=randwrite`: 指定测试类型为随机写入（random write）。
- `-ioengine=libaio`: 使用Linux AIO（异步I/O）引擎进行I/O操作。
- `-bs=4k`: 设置块大小（block size）为4KB，即每次读写的数据块大小为4KB。
- `-size=1G`: 设置测试文件的大小为1GB。
- `-numjobs=1`: 指定启动一个fio工作线程。
- `-runtime=60`: 设置测试时间为60秒。
- `-group_reporting`: 在测试结果中以组的形式报告各个I/O操作的性能数据。
- `-filename=/dev/testblock`: 指定测试文件的路径为/dev/testblock，即对名为testblock的磁盘设备进行测试。
- `-name=Rand_Write_Testing`: 为这个测试命名，便于识别和区分不同的测试场景。

通过这个命令，可以对指定的磁盘设备进行随机写入性能测试，并收集相关的性能数据。这有助于评估磁盘的性能表现，以及在实际应用中的表现情况。



输出解读：

```shell
Run status group 0 (all jobs):
  WRITE: bw=577MiB/s (605MB/s), 577MiB/s-577MiB/s (605MB/s-605MB/s), io=1024MiB (1074MB), run=1775-1775msec

Disk stats (read/write):
    dm-0: ios=0/243535, merge=0/0, ticks=0/6663, in_queue=6667, util=88.85%, aggrios=0/131237, aggrmerge=0/0, aggrticks=0/3525, aggrin_queue=2779, aggrutil=86.21%
  nvme0n1: ios=0/202, merge=0/0, ticks=0/526, in_queue=256, util=2.64%
  nvme1n1: ios=0/262273, merge=0/0, ticks=0/6525, in_queue=5303, util=86.21%
```

bw表示带宽，

io表示总写入的数据量

run表示测试运行时间

Disk stats表示磁盘读写信息统计







# 网络

## bps带宽是网络链路的最大传输速率

带宽通常用来衡量一个网络连接的数据传输能力，它反映了这个连接理论上可以达到的最高数据传输速度。带宽的单位是bps，即每秒比特数。高带宽意味着在单位时间内可以传输更多的数据。

一般在局域网做压测，网络带宽很少出现瓶颈。当传输大数据量，带宽同时被其他应用占用以及有网络限速等情况时，则带宽可能成为性能瓶颈。理论上，1000Mbit/s 网卡的传输速度是 125MB/s，100Mbit/s 网卡是 12.5MB/s，实际的传输速度会受如交换机、网卡等配套设备影响。在 Linux 服务器上查看网络流量的工具很多，有iperf3, vnStat、NetHogs、iftop 等。



## pps每秒传输的数据包数量

pps和bps两者的不同点在于，带宽侧重于整体的数据传输速率，而PPS关注的是具体到每个数据包的处理速率。在实际使用中，即使带宽很大，如果网络设备的PPS处理能力不足，也可能导致网络拥堵和性能瓶颈。因此，它们都是衡量网络性能的重要指标，且相辅相成。

```shell
sar -n DEV 1 -t 100
```

-n DEV 选择监控网络设备

1 时间间隔1秒

-t 100进行100次采样

输出结果解读：

```shell
Average:        IFACE   rxpck/s   txpck/s    rxkB/s    txkB/s   rxcmp/s   txcmp/s  rxmcst/s
Average:    cali6f46772d931      0.00      0.00      0.00      0.00      0.00      0.00      0.00
Average:    califd1a9391c79      0.00      0.00      0.00      0.00      0.00      0.00      0.00
Average:       dummy0      0.00      0.00      0.00      0.00      0.00      0.00      0.00
Average:    cali69c5e5c1631      0.00      0.00      0.00      0.00      0.00      0.00      0.00
Average:    cali75be0a14365      0.00      0.00      0.00      0.00      0.00      0.00      0.00
Average:    cali865ef3d6814      0.00      0.00      0.00      0.00      0.00      0.00      0.00
Average:           lo    179.67    179.67     33.66     33.66      0.00      0.00      0.00
Average:    virbr0-nic      0.00      0.00      0.00      0.00      0.00      0.00      0.00
Average:       virbr0      0.00      0.00      0.00      0.00      0.00      0.00      0.00
Average:       enp5s0      2.33      3.33      0.16      1.49      0.00      0.00      0.00
Average:    cali087369ad67a      3.33      3.33      0.31      0.29      0.00      0.00      0.00
Average:    kube-ipvs0      0.00      0.00      0.00      0.00      0.00      0.00      0.00
Average:    calibac85a5c04a      0.00      0.00      0.00      0.00      0.00      0.00      0.00
Average:        tunl0      0.00      0.00      0.00      0.00      0.00      0.00      0.00
Average:    cali80d3bb471f2      0.00      0.00      0.00      0.00      0.00      0.00      0.00
Average:    nodelocaldns      0.00      0.00      0.00      0.00      0.00      0.00      0.00
Average:      docker0      0.00      0.00      0.00      0.00      0.00      0.00      0.00
```

- **IFACE**: 表示网络接口的名称。
- **rxpck/s**: 表示每秒接收的数据包数量。
- **txpck/s**: 表示每秒发送的数据包数量。
- **rxkB/s**: 表示每秒接收的千字节数（KB）。
- **txkB/s**: 表示每秒发送的千字节数（KB）。
- **rxcmp/s**: 表示每秒接收的压缩数据包数量。
- **txcmp/s**: 表示每秒发送的压缩数据包数量。
- **rxmcst/s**: 表示每秒接收的多播数据包数量。



# uptime

![2_15.png](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/2_15-100522188-orig.png)

小数代表过去1分钟、5分钟、15分钟cpu平均负载；可以分析高负载是暂时的，还是长期的。但什么是高负载呢？数字代表请求cpu资源的任务量，1.0代表一个100%的cpu核的资源；超过1.0的部分代表等待cpu处理的进程量。

需要说明的是，这些数字和cpu核数相关，如果有4个cpu，4.0才意味着所有核完全被占用。通常70%是比较健康的状态，即每个cpu核负载0.7。



# 性能优化

[Linux性能优化全景指南](https://mp.weixin.qq.com/s/6_utyj1kCyC5ZWpveDZQIQ)

