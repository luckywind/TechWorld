# 什么是Gemini CLI?

Gemini CLI 是由 Google Gemini 团队开源的一款命令行 AI 工具，专为开发者设计。它能够理解代码、执行复杂查询、自动化任务，并利用 Gemini 的多模态能力（如图像识别）生成创意内容。GitHub 地址：`https://github.com/google-gemini/gemini-cli`


**主要功能亮点：**

- 处理大型代码库 ：支持超过 100 万个 token 上下文长度，轻松分析大型项目。
- 多模态应用生成 ：通过 PDF 或草图快速生成应用程序原型。
- 自动化运维任务 ：如 Git 操作、PR 查询、代码迁移计划制定等。
- 集成外部工具 ：通过 MCP 服务器连接 Imagen、Veo、Lyria 等媒体生成模型。
- 联网搜索支持 ：内置 Google Search，确保回答基于最新信息。

# 开始使用

## 安装

1. 安装依赖
   [Install Node and NPM natively on Apple Silicon Mac](https://justinwride.medium.com/install-node-and-npm-natively-on-apple-silicon-mac-m1-7432c826389b)

```shell
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.37.2/install.sh | bash
nvm install v15 #install NodeJS 15
source ~/.nvm/nvm.sh


(env-ai) ➜  xhs_ai_publisher git:(main) ✗ npm -v
10.8.2
(env-ai) ➜  xhs_ai_publisher git:(main) ✗ node -v
v20.18.1 # node.js(>18)
```

2. 安装Gemini CLI
   `npx https://github.com/google-gemini/gemini-cli`
   或者

   ```shell
   npm install -g @google/gemini-cli
   // 或（适用于 Mac）
   sudo npm install -g @google/gemini-cli
   npm install -g @google/gemini-cli --no-fund --loglevel verbose
   
   npm list -g --depth=0#检查全局包列表确认
   可能需要export PATH="$PATH:$(npm config get prefix)/bin"才能找到命令
   ```
   
   
   第二次安装
   
   ```shell
   npm config list
   10071  npm install -g @google/gemini-cli
   10072  npm cache clean --force
   10073  npm cache verify
   10074  npm install -g @google/gemini-cli --no-fund
   10075  npm config set registry https://registry.npmmirror.com
   10076  npm install -g @google/gemini-cli --no-fund
   10077  npm config get registry
   npm config set strict-ssl false
   brew install gemini-cli
   ```
   
   问题：
   Failed to login. Message: Failed to exchange authorization code for tokens: request to https://oauth2.googleapis.com/token failed,      │
   │ reason: unable to get local issuer certificate
   
   `file $(which gemini)`  如果输出... node script text executable 则说明安装的是 **Node.js 版本的 gemini-cli**。通常通过 `npm install -g @google/generative-ai` 或 `npm install -g gemini-cli` 安装
   
   - 证书方式解决
   
   [这里提到了cert问题原因](https://github.com/google-gemini/gemini-cli/issues/2300)，[导出证书](https://blog.csdn.net/weixin_43504224/article/details/130500855)
   
   解决：export NODE_EXTRA_CA_CERTS=/etc/ssl/cert.pem
   
   curl --proxy http://127.0.0.1:7890 https://google.com -v 可以看到系统的 CAfile: /etc/ssl/cert.pem，以及确定是http 代理还是sock5代理。遇到the URL must start with `http:` or `https:`. 可以检查proxy 环境变量中是http 还是sock5
   
   
   
   - 解决： export NODE_TLS_REJECT_UNAUTHORIZED=0
   
   输入gemini即可进入交互式CLI
   
3. 认证
   个人选择谷歌邮箱认证
   
   优势：
   
   - **Free tier**: 60 requests/min and 1,000 requests/day
   - **Gemini 2.5 Pro** with 1M token context window,   单次会话token 上限
   - **No API key management** - just sign in with your Google account
   - **Automatic updates** to latest models

![image-20250918143016580](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250918143016580.png)

一旦完成认证，就会出现一个交互对话文本框：

Tips for getting started:
1. Ask questions, edit files, or run commands.
2. Be specific for the best results.
3. Create GEMINI.md files to customize your interactions with Gemini.
4. /help for more information.

![image-20250918143424280](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250918143424280.png)

## 认证

[认证设置](https://github.com/google-gemini/gemini-cli/blob/main/docs/cli/authentication.md)

1. 用谷歌账号
   - 一旦认证，你的凭证就会缓存到本地，后续运行将跳过web 登录
2. Gemini API Key



## 登录失败解决

/token failed, reason: connect ETIMEDOUT 64.233.189.443

[参考](https://www.youtube.com/watch?v=X1Hhpaio6bY) 原因是Gemini cli默认不会使用系统代理，要么开启代理的TUN模式，要么导出环境变量`export https_proxy=http://127.0.0.1:7890` ，我已加入环境变量中

setx http_proxy "http://127.0.0.1:7890" && setx https_proxy "http://127.0.0.1:7890"  永久设置

NO_BROWSER=true手动验证



请你生成一篇小红书图文，核心内容是小学奥数数阵图的典型例题以及解决技巧，按照如下步骤完成发表：

1. 先生成markdown文本，然后复制到剪贴板
2. 调用clipboard2pics.py并传入--d /Users/chengxingfu/code/my/ai_tools/pics参数把图片放到该目录下
3. 调用gen_title_pic.py并传入'小学奥数数阵图解题技巧'  --d /Users/chengxingfu/code/my/ai_tools/pics参数把标题图片放到该目录下
4. 把所有图片发布到小红书

## 使用

1. **理解项目代码**： 在项目根目录下对话

gemini 终端可以直接对话，如果需要引入本地文件，可通过输入 `@` 来选择文件

gemini 从当前目录开始

**gemini --include-directories ../lib,../docs  包含多个目录**

gemini -m gemini-2.5-flash   指定模型

2. 可以让AI 直接修改代码
3. `/` [可以查看相关使用技巧](https://blog.frognew.com/2025/06/gemini-cli-commands.html)

 about  show version info
 auth  change the auth method
 bug  submit a bug report
 **chat**  保存和恢复对话历史，用于状态分支或会话恢复

- `/chat save  <tag>` 保存对话历史
- `/chat save  <tag>` 恢复对话历史
- `/chat list` 列出历史

 clear 清屏，快捷键Cmd+L
 compress  Compresses the context by replacing it with a summary.
 **copy**  自动复制回复
 docs  open full Gemini CLI documentation in your browser
 directory  Manage workspace directories
 editor  set external editor preference
 extensions  list active extensions
 help  for help on gemini-cli
 ide  manage IDE integration
 init  Analyzes the project and creates a tailored GEMINI.md file.
 mcp  list configured MCP servers and tools, or authenticate with OAuth-enabled servers
 quit  exit the cli

@文件路径：  注入文件到提示中

!<shell 命令>： 执行shell，  `!ls -la`列出当前目录

!： 切换进入/退出shell 模式

/tools  查看工具

### 命令行参数

-y 自动接受所有动作请求

-m 模型

-p 追加提示词





### vscode 集成

只要在vscode 的终端窗口内执行gemini，会自动提示连接VsCode，Yes 回车即可。

`/ide enable/disable/status`可以手动控制是否连接到IDE

### 示例

例如，修复一个开源项目的bug 的提示词

1. Explore the current directory and describe the architecture of the project

2. Here's a GitHub issue: [@search https://github.com/AashiDutt/Google-Agent-Development-Kit-Demo/issues/1]. Analyze the codebase and suggest a 3-step fix plan. Which files/functions should I modify?
3. Gemini 会让你确认是否接受代码修改？
4. Write a pytest unit test for this change in test_shared.py.  写单元测试
5. Write a markdown summary of the bug, fix, and test coverage. Format it like a changelog entry under "v0.2.0".  写bugfix 



生成流程图

1. Generate a flowchart that shows how agents communicate via A2A and how the main.py orchestrates the system. Highlight where the issue occurred and how it was fixed.

## Gemini CLI 工具

- ReadFile, WriteFile, Edit
- FindFiles, ReadFolder, ReadManyFiles
- Shell, SaveMemory
- GoogleSearch or Search, WebFetch

official [announcement article](https://blog.google/technology/developers/introducing-gemini-cli-open-source-ai-agent/) and the [GitHub page](https://github.com/google-gemini/gemini-cli?tab=readme-ov-file#gemini-cli).

如需详细了解 Gemini Code Assist 的功能，请参阅 [Gemini Code Assist 文档](https://developers.google.com/gemini-code-assist/docs/overview?hl=zh-cn)。

如需详细了解 Gemini CLI，请参阅 [Gemini CLI 文档](https://github.com/google-gemini/gemini-cli)。



# 使用MCP

## 配置mcp server

使用settings.json 里的mcpServers 配置来定位和链接mcp 服务。

```json
{ ...file contains other config objects
  "mcpServers": {
    "serverName": {
      "command": "path/to/server",
      "args": ["--arg1", "value1"],
      "env": {
        "API_KEY": "$MY_API_TOKEN"
      },
      "cwd": "./server-directory",
      "timeout": 30000,
      "trust": false
    }
  }
}

```



## mcp 交互

/mcp 展示mcp server 信息

`gemini mcp list` 列出mcp

## 小红书mcp

在 `~/.gemini/settings.json` 或项目目录 `.gemini/settings.json` 中配置：

```
{
  "mcpServers": {
    "xiaohongshu": {
      "httpUrl": "http://localhost:18060/mcp",
      "timeout": 30000
    }
  }
}
```

更多信息请参考 [Gemini CLI MCP 文档](https://google-gemini.github.io/gemini-cli/docs/tools/mcp-server.html)

![image-20250924091733122](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250924091733122.png)

发布失败，请打开浏览器调试看看哪里出错`go run . -headless=false`

提示词
请检查小红书是否已登录，如果已登录请你发表一篇小红书，核心内容是高思导引刷题技巧，具体内容需要你自己写，你可以用gen_title_pic.py自己生成一个  图片，注意它把图片放哪里了，你需要拷贝到当前目录，然后完成发布

# 生成图片

[参考](https://github.com/gemini-cli-extensions/nanobanana)

`gemini extensions install https://github.com/gemini-cli-extensions/nanobanana`

The image generation failed because a valid API key is not configured. Please set one of the following
  environment variables: NANOBANANA_GEMINI_API_KEY, NANOBANANA_GOOGLE_API_KEY, GEMINI_API_KEY, or GOOGLE_API_KEY.

  For more details on authentication, you can visit:
  https://github.com/google-gemini/gemini-cli/blob/main/docs/cli/authentication.md



## 命令

/generate - 单次或批量图片生成（支持风格/变体选项）

> ```
> /generate "sunset over mountains" --count=3 --preview
> ```

/edit - 图片编辑

> ```
> /edit my_photo.png "add sunglasses to the person"
> ```

/restore - 图片修复

/icon - 生成多尺寸应用图标、网站图标和界面元素

/pattern - 生成用于背景的无缝图案与纹理

/story - 生成讲述视觉故事或流程的序列图像

/diagram - 生成技术图表、流程图和建筑草图

/nanobanana - 自然语言交互界面

> ```
> /nanobanana create a logo for my tech startup
> ```

✅命令选项

--count=N - 生成数量（1-8，默认值：1）

--styles="风格1,风格2" - 指定艺术风格（逗号分隔）

--variations="变体1,变体2" - 特定变体类型

--format=grid|separate - 输出格式（默认：separate/独立文件）

--seed=123 - 随机数种子（用于生成可复现的变体）

--preview - 自动在默认查看器中打开生成图片





# 参考

[免费白嫖 Claude Code，国内也能免费使用（保姆级教程）](https://cloud.tencent.com/developer/article/2539379)

[Gemini CLI: A Guide With Practical Examples](https://www.datacamp.com/tutorial/gemini-cli)