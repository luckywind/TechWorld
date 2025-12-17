1. `npx @modelcontextprotocol/inspector`  验证MCP并获取登录地址
2. 网页上使用二维码登录
3. gemini-cli中自动发布

cat xx| pbcopy  复制到剪贴板



# 使用步骤

## 启动mcp 服务

### 使用预编译工具

1. 首先运行登录工具
./xiaohongshu-login-darwin-arm64

2. 然后启动 MCP 服务
./xiaohongshu-mcp-darwin-arm64
-headless=false 带浏览器

### 使用源码

1. 登录

go run cmd/login/main.go

2. 启动mcp 服务

```
go run .
go run . -headless=false
```



## 验证

npx @modelcontextprotocol/inspector









# n8n

1. 打开浏览器访问：[http://localhost:5678](http://localhost:5678/)









