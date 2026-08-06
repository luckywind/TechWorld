# 服务如何启停？

┌──────────┬─────────────────────────────────────┐
│   操作   │                命令                 │
├──────────┼─────────────────────────────────────┤
│ 启动     │ cd ~/deer-flow && make docker-start │
├──────────┼─────────────────────────────────────┤
│ 停止     │ cd ~/deer-flow && make docker-stop  │
├──────────┼─────────────────────────────────────┤
│ 查看日志 │ cd ~/deer-flow && make docker-logs  │
├──────────┼─────────────────────────────────────┤
│ 访问     │ http://localhost:2026               │
└──────────┴─────────────────────────────────────┘

# 输出

在容器内的路径是 /app/backend/.deer-flow/users/<用户ID>/threads/<对话ID>/user-data/outputs/，但这个目录已挂载到你的电脑上了。挂载路径~/deer-flow/backend/.deer-flow/users/<用户ID>/threads/<对话ID>/user-data/outputs/

例如：

/Users/chengxingfu/deer-flow/backend/.deer-flow/users/a3fbb037-ab18-4ec6-8a4d-1e9e79ef5f1f/threads/dcb3e961-0976-49cd-b7c2-71b0b16d362e/user-data/outputs

# 公网服务

方案一：Cloudflare Tunnel（推荐，免费免公网IP）需要 mac Sonoma 以上版本

不需要公网 IP、不用配路由器、免费。在你的 Mac 上跑一个 cloudflared 隧道即可。
1. 安装
brew install cloudflare
2. 登录（绑定你的域名，或用 Cloudflare 提供的免费子域名）
cloudflared tunnel login
3. 创建隧道并指向本地服务
cloudflared tunnel create deer-flow
cloudflared tunnel route dns deer-flow deer-flow.你的域名.com
4. 启动隧道
cloudflared tunnel run --url http://localhost:2026 deer-flow
之后访问 https://deer-flow.你的域名.com 就行了。还可以加 --config 写成系统服务，开机自启。

---
方案二：frp 内网穿透（国内速度快）

需要一台有公网 IP 的轻量云服务器（阿里云/腾讯云最低配即可，几十块/月）。

服务器端跑 frps，Mac 上跑 frpc 把 localhost:2026 映射出去。

---
方案三：Tailscale（最简单但需装客户端）

brew install tailscale
tailscale up

两台设备都装 Tailscale 客户端，自动组成虚拟局域网。通过 Tailscale 分配的 100.x.x.x IP 直接访问。缺点是家里电脑也要装客户端。