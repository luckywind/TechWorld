[Linux下双网卡绑定七种模式 多网卡的7种bond模式原理](https://support.huawei.com/enterprise/zh/knowledge/EKB1001172264)

[linux多网卡绑定聚合-Bond详细完整版](https://developer.aliyun.com/article/503848)



1. 添加bond 接口

```shell
ip link add bond0 type bond mode balance-rr miimon 200 
ifconfig bond0 2.2.2.170/24 
```

其中mode 字段可以是：

- balance-rr 轮询模式
- active-backup 主备模式
- balance-xor 平衡异或模式
- broadcast 广播模式
- 802.3ad miimon 200 [ xmit_hash_policy layer3+4 ] 动态链路聚合模式
- balance-tlb miimon 200 自适应传输负载均衡模式。
-  balance-alb miimon 200 自适应负载均衡模式。

2. 将网口添加到bond 接口下

```shell
#将网口添加到bond 接口下
ifconfig enp1s0f0 down 
ip link set enp1s0f0 master bond0 
ifconfig enp1s0f0 up 
ifconfig enp1s0f1 down 
ip link set enp1s0f1 master bond0 
ifconfig enp1s0f1 up 
```

