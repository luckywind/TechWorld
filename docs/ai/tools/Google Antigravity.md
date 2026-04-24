# 问题

## 登录后不让下一步

开启TUN 模式

[Antigravity-Manager](https://github.com/lbjlaq/Antigravity-Manager)  解决登录问题**Antigravity Tools** 是一个专为开发者和 AI 爱好者设计的全功能桌面应用。它将多账号管理、协议转换和智能请求调度完美结合，为您提供一个稳定、极速且成本低廉的 **本地 AI 中转站**。

通过本应用，您可以将常见的 Web 端 Session (Google/Anthropic) 转化为标准化的 API 接口，消除不同厂商间的协议鸿沟。

点击切换此账号将自动在 Antigravity 里登录

![image-20260423144513753](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20260423144513753.png)

白屏解决办法： 使用老版本[Antigravity Tools v3.2.8](https://github.com/lbjlaq/Antigravity-Manager/releases/tag/v3.2.8)或者系统升级 13.7.8

[这是](https://x.com/idoubicc/status/2004848130693759213)一个把 Antigravity 里面的模型转成标准 API，给 Claude Code 等 Coding Agent 接入的智能代理项目。提供多账号管理、协议转换和智能请求调度等功能，让你能稳定、低成本地在 Claude Code、Codex 中使用 gemini / claude 系列模型。

使用方法： 在终端配置环境变量，让 Claude Code 使用自定义的 API 端点

export ANTHROPIC_API_KEY="sk-xxx" 

export ANTHROPIC_BASE_URL="http://127.0.0.1:8045"

打开 Claude Code 发送指令，开始使用 CC

在 Antigravity Tools  里可以添加多个 Google 账号，每个账号都有一定的 Antigravity 模型额度，如果额度不够了，可以点击切换账号，智能切换到额度足够的账号。既能在 Antigravity 编辑器使用，也能在 Claude Code、Codex 使用，相当于一次充值，同时分配给多个编程智能体用。一定程度降低了对 Claude、ChatGPT 的账号依赖，仅需一个 Google 账号，即可使用 Antigravity、Claude Code、Codex、Gemini Cli 等编程智能体。