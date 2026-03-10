# 防止断开

编辑/etc/ssh/sshd_config文件设置心跳，保持连接

```shell
ClientAliveInterval 60     
ClientAliveCountMax 10
```

ClientAliveInterval 60 表示每60秒发送一次请求，从而保持连接。

ClientAliveCountMax 10 表示服务器发出请求后客户端没有响应的次数达到10次，就自动断开连接。

无响应的SSH客户端将在60x10=600秒后断开连接。

systemctl restart sshd

