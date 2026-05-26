# qperf

qperf 是 IBM 开发的网络性能测量工具，基于 RDMA 框架，支持测试 TCP/UDP 带宽（吞吐量）和延迟。相比 netperf，qperf 无需 `--` 分隔符，参数更简洁，且天然支持 RDMA 协议。

## 安装

```shell
# CentOS/RHEL
yum install -y qperf

# Ubuntu/Debian
apt install -y qperf
```

## 基本原理

qperf 在服务端启动一个监听进程 `qperf`，客户端连接后发起测试。服务端不需要额外启动 daemon，客户端通过 ssh 或直接 TCP 连接即可。

- 本地测试：`qperf` 直接在同一台机器上测试
- 远程测试（SSH 模式）：`qperf <server> <test>`
- 远程测试（监听模式）：服务端先启动 `qperf`，客户端 `qperf <ip> <test>`

## 服务端启动

```shell
# 监听模式，默认端口 19765，不加参数直接启动
qperf

# 指定端口
qperf -lp 19766
```

## 命令语法

```
qperf [options] <server> <test1> [test2 ...]
```

| 参数 | 说明 |
|------|------|
| `-lp <port>` | 指定监听端口（listen port） |
| `-t <seconds>` | 测试持续时间（秒），默认较短，建议设 10-60 |
| `-m <size>` | 消息大小，如 `1K`、`64K`、`1M` |
| `-v` / `-vv` | 详细信息输出 |
| `-cm1` | 连接管理端口 |
| `-un` | 使用 UDP 而非默认的 TCP |
| `-ip` | 仅使用 IPv4 |
| `--id` | 双向测试同时进行（bidirectional） |
| `--cpu-affinity` | 绑定 CPU |

## 常用测试项

qperf 内置多种测试项，通过 `<test>` 参数指定。

### 带宽测试

| 测试项 | 含义 |
|--------|------|
| `tcp_bw` | TCP 流式吞吐量（单向） |
| `tcp_bw_bidir` | TCP 双向吞吐量 |
| `udp_bw` | UDP 吞吐量（单向） |
| `udp_bw_bidir` | UDP 双向吞吐量 |

### 延迟测试

| 测试项 | 含义 |
|--------|------|
| `tcp_lat` | TCP 延迟（Ping-Pong） |
| `udp_lat` | UDP 延迟（Ping-Pong） |
| `tcp_rr` | TCP Request/Response（乒乓）延迟 |

## 使用示例

### 服务端

```shell
# 启动监听
qperf
```

### 客户端

✅qperf 10.10.10.11 -oo msg_size:1:64K:*2 udp_lat

从 1B 到 64K，每次乘 2， 共进行17轮测试

1, 2, 4, 8, 16, 32, 64, 128, 256, 512,1K, 2K, 4K, 8K, 16K, 32K, 64K

```shell
# TCP 带宽测试，默认 1MB 消息大小，持续 10 秒
qperf -t 10 10.0.0.1 tcp_bw

# TCP 带宽测试，指定消息大小 64K
qperf -t 10 -m 64K 10.0.0.1 tcp_bw

# UDP 带宽测试
qperf -t 10 10.0.0.1 udp_bw

# TCP 双向带宽测试
qperf -t 10 10.0.0.1 tcp_bw_bidir

# TCP 延迟测试
qperf -t 10 10.0.0.1 tcp_lat

# UDP 延迟测试
qperf -t 10 10.0.0.1 udp_lat

# 同时测试带宽和延迟
qperf -t 10 10.0.0.1 tcp_bw tcp_lat

# 不同消息大小的带宽测试
qperf -t 10 -m 1K 10.0.0.1 tcp_bw
qperf -t 10 -m 64K 10.0.0.1 tcp_bw
qperf -t 10 -m 1M 10.0.0.1 tcp_bw
```

### 本地测试（无需服务端）

```shell
# 本地 TCP 带宽
qperf localhost tcp_bw

# 本地 TCP 延迟
qperf localhost tcp_lat
```

## 结果解读

```
tcp_bw:
    bw          =  9.41 Gb/sec
    msg_rate    =  1.12 K/sec
    msg_size    =  1 MB
    time        =  10 sec
    ...

tcp_lat:
    latency     =  23.5 us
    msg_rate    =  42.6 K/sec
    msg_size    =  1 KB
    time        =  10 sec
    ...
```

| 字段 | 含义 |
|------|------|
| `bw` | 带宽（bits/sec），注意除以 8 换算为 Bytes/s |
| `msg_rate` | 每秒消息数 |
| `latency` | 延迟（微秒） |
| `msg_size` | 测试使用的消息大小 |
| `time` | 实际测试持续时间 |

## vs netperf

| 维度 | qperf | netperf |
|------|-------|---------|
| 语法复杂度 | 简洁，无 `--` 分隔 | 分全局和测试参数，用 `--` 分隔 |
| RDMA 支持 | 原生支持 | 需额外配置 |
| 延迟测试 | 内置 `tcp_lat` / `udp_lat` | 使用 `TCP_RR` / `UDP_RR` |
| 双向测试 | `tcp_bw_bidir` 一步到位 | 需手动启动多个实例 |
| 社区 | IBM 维护，更新较少 | HP 开发，使用更广泛 |

## 注意事项

- 服务端 `qperf` 监听端口默认 19765，确保防火墙放行
- 带宽测试结果受消息大小影响较大，小包测 PPS 瓶颈，大包测带宽瓶颈，建议分别用 `1K`、`64K`、`1M` 测试
- 测试期间建议同时在两端执行 `sar -n DEV 1` 观察网卡负载
- qperf 的 `bw` 单位是 `Gb/sec`（即 Gbps），注意与 `GB/sec`（Gigabytes/s）区分
