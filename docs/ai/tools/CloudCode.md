# 参考

[参考文档](https://cloud.tencent.com/developer/article/2539379)

# 安装

1. 安装 Node.js（已安装可跳过）

确保 Node.js 版本 ≥ 18.0

```bash
# Ubuntu / Debian 用户
curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo bash -
sudo apt-get install -y nodejs
node --version

# macOS 用户
sudo xcode-select --install
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
brew install node
node --version
```



2. 安装 Claude Code

```bash
npm install -g @anthropic-ai/claude-code
claude --version
这个会偶尔找不到命令，不清楚为什么

更推荐下面这个，它不依赖 node 和 npm
curl -fsSL https://claude.ai/install.sh | bash

echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.zshrc && source ~/.zshrc
```

下载cc switch， [也可以自己手搓](https://zhuanlan.zhihu.com/p/1984939410752570691)

https:/github.com/farion1231/cc-switch/releases

brew tap farion1231/ccswitch
brew install --cask cc-switch

3. 添加模型供应商
  [到魔塔社区](https://www.cnblogs.com/yada/p/19723381)会提供各种开源LLM的服务，并提供每日2000次免费调用额度。modelscope 目前提供 [OpenAI](https://zhida.zhihu.com/search?content_id=262458825&content_type=Article&match_order=1&q=OpenAI&zhida_source=entity) 和 [Anthropic](https://zhida.zhihu.com/search?content_id=262458825&content_type=Article&match_order=1&q=Anthropic&zhida_source=entity) 兼容的 2 套 API，可以使用各种 AI 编程工具，选择 OpenAI

![image-20260402162047944](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20260402162047944.png)
配置模型会记录到~/.claude/settings.json文件中

4. 免登录

~/.claude.json 配置
```json
   {
     "hasCompletedOnboarding": true
   }
```

4. 开始使用







- **获取 Auth Token：** `ANTHROPIC_AUTH_TOKEN` ：注册后在 `API令牌` 页面点击 `添加令牌` 获得（以 `sk-` 开头）
  - 名称随意，额度建议设为无限额度，其他保持默认设置即可

- **API地址：** `ANTHROPIC_BASE_URL`：`https://anyrouter.top` 是本站的 API 服务地址，**与主站地址相同**

在您的项目目录下运行：

```bash
cd your-project-folder
export ANTHROPIC_AUTH_TOKEN=sk-... 
export ANTHROPIC_BASE_URL=https://anyrouter.top
claude
```

运行后

- 选择你喜欢的主题 + Enter
- 确认安全须知 + Enter
- 使用默认 Terminal 配置 + Enter
- 信任工作目录 + Enter

开始在终端里和你的 AI 编程搭档一起写代码吧！🚀





4️⃣ 配置环境变量（推荐）

为避免每次重复输入，可将环境变量写入 bash_profile 和 bashrc：

```bash
echo -e '\n export ANTHROPIC_AUTH_TOKEN=sk-...' >> ~/.bash_profile
echo -e '\n export ANTHROPIC_BASE_URL=https://anyrouter.top' >> ~/.bash_profile
echo -e '\n export ANTHROPIC_AUTH_TOKEN=sk-...' >> ~/.bashrc
echo -e '\n export ANTHROPIC_BASE_URL=https://anyrouter.top' >> ~/.bashrc
echo -e '\n export ANTHROPIC_AUTH_TOKEN=sk-...' >> ~/.zshrc
echo -e '\n export ANTHROPIC_BASE_URL=https://anyrouter.top' >> ~/.zshrc
```

重启终端后，直接使用：

```bash
cd your-project-folder
claude
```

即可使用 Claude Code







# 使用

## /命令

/btw ： 临时提问，回答完按按空格或者回车，直接把这一段消除掉，继续主程序。

/rewind： 回退，可以选择回退代码、对话到历史的某个时间(可以选择)

/branch: 对话分叉

/simplify: Claude Code会同时启动三个平行的Agent，分别从代码复用、代码质量、运行效率三个角度审查你的改动。

/remote-control： 它会生成一个URL，手机上打开这个链接整个会话就出现在手机上

## yolo 模式

YOLO 模式（You Only Live Once）指的是让 Claude Code 自动执行操作而无需逐次确认的权限模式。Claude Code 提供了几种不同安全级别的自动模式：

### 权限模式速览

| 模式 | 自动执行的内容 | 适用场景 |
|---|---|---|
| `default` | 仅读取操作 | 入门使用、敏感项目 |
| `acceptEdits` | 读取、文件编辑、常见文件系统命令 | 迭代编码 |
| `plan` | 仅读取 | 探索代码后再动手 |
| `auto` | AI 分类器审核后自动执行 | 长时间任务，减少确认打断 |
| `dontAsk` | 仅预审批的工具 | 锁定环境 / CI 脚本 |
| `bypassPermissions` | 所有操作（无安全检查） | 仅限隔离容器/VM |

### 1. 命令行启动时指定

```bash
# 方式一：直接进入 bypassPermissions（真正的 yolo）
claude --dangerously-skip-permissions

# 方式二：等价的完整写法
claude --permission-mode bypassPermissions

# 方式三：启动时用安全模式，但允许后续切换到 bypassPermissions
claude --permission-mode plan --allow-dangerously-skip-permissions

# 方式四：AI 监督的自动模式（推荐）
claude --permission-mode auto

# 方式五：自动批准文件编辑（半 yolo）
claude --permission-mode acceptEdits

# 方式六：CI/脚本模式（仅允许预设工具）
claude --permission-mode dontAsk --allowedTools "Bash(npm test),Read"

# 非交互模式（-p）也一样支持
claude -p "修复所有 bug" --dangerously-skip-permissions
claude -p "运行测试" --permission-mode auto
```

### 2. 会话内切换

| 操作 | 说明 |
|---|---|
| `Shift+Tab` | 循环切换权限模式：`default` → `acceptEdits` → `plan` → （可选：`bypassPermissions`）→ （可选：`auto`） |
| `/plan` | 将下一条提示词以 plan 模式执行（仅该条） |

> **注意：** `bypassPermissions` 只有在启动时带了 `--dangerously-skip-permissions` 或 `--allow-dangerously-skip-permissions` 参数时，才会出现在 `Shift+Tab` 的切换循环中。你无法在未带这些参数启动的会话中进入 `bypassPermissions`。

### 3. settings.json 持久化配置

在 `~/.claude/settings.json`（用户级）或 `.claude/settings.json`（项目级）中配置：

```json
{
  "permissions": {
    // 设置默认权限模式
    "defaultMode": "acceptEdits",

    // 预审批特定工具（在 bypassPermissions 之外的所有模式都生效）
    "allow": [
      "Bash(npm run lint)",
      "Bash(npm run test *)",
      "Read(~/.zshrc)"
    ],

    // 始终阻止的工具（包括 bypassPermissions 模式）
    "deny": [
      "Bash(curl *)",
      "Read(./.env)",
      "Read(./secrets/**)"
    ],

    // 始终要求确认的工具（包括 bypassPermissions 模式）
    "ask": [
      "Bash(git push *)"
    ]
  },

  // auto 模式配置（AI 监督的 yolo）
  "autoMode": {
    "environment": ["项目部署到 fly.io，项目名: my-app"],
    "allow": ["预批准 terraform plan 在 staging 环境"],
    "soft_deny": ["永远不要执行 terraform apply"],
    "hard_deny": ["永远不要删除生产数据库"]
  }
}
```

> **注意：**
> - `defaultMode: "auto"` 必须配置在 `~/.claude/settings.json`（用户级），项目级配置会被忽略，防止仓库自行授予 auto 模式
> - `defaultMode: "bypassPermissions"` 和 `"dontAsk"` 在 claude.ai 云端会话中会被静默忽略
> - permit 规则 `allow/deny/ask` 在多个作用域间**合并**

### 4. IDE/桌面端启用

- **VS Code：** 点击提示框底部的模式指示器，在 UI 中选择模式。需要在扩展设置中勾选 "Allow dangerously skip permissions" 才能使用 bypassPermissions
- **桌面应用：** 使用发送按钮旁的模式选择器，需要在桌面设置中先启用 Auto 和 Bypass 模式

### ⚠️ 安全警告

- **bypassPermissions** 对提示注入或意外操作无任何防护，**仅在隔离容器/VM/无网络访问的开发容器中使用**
- Linux/macOS 下以 root 或 sudo 运行时拒绝启动
- 显式 `ask` 规则即使在 bypassPermissions 模式下仍会强制提示
- `rm -rf /` 和 `rm -rf ~` 作为熔断机制仍会提示
- 管理员可通过 managed settings 中 `"disableBypassPermissionsMode": "disable"` 禁止此模式
- 使用第三方 API 代理时，`auto` 模式的 AI 分类器可能无法正常工作，但 `bypassPermissions` 为客户端行为，可正常使用

## 命令帮助

`claude --help` 命令输出的简体中文翻译：

```
用法: claude [选项] [命令] [提示词]

Claude Code - 默认启动交互式会话，使用 -p/--print 可进行非交互式输出

参数:
  prompt                                你的提示词

选项:
 ✅--add-dir <目录...>                   允许工具访问的额外目录
  --agent <agent>                       当前会话使用的 Agent，覆盖 'agent' 配置
  --agents <json>                       用 JSON 定义自定义 Agent（例如
                                        '{"reviewer": {"description": "Reviews
                                        code", "prompt": "You are a code
                                        reviewer"}}'）
✅--allow-dangerously-skip-permissions  允许跳过所有权限检查（作为可选行为，并非默认
                                        启用）。仅建议在无网络访问的沙箱环境中使用
  --allowedTools, --allowed-tools <工具...>
                                        逗号或空格分隔的允许工具列表（如 "Bash(git *)
                                        Edit"）
  --append-system-prompt <提示词>       向默认系统提示词追加内容
  --ax-screen-reader                    渲染适合屏幕阅读器的输出（纯文本，无装饰边框
                                        或动画）
  --bg, --background                    以后台 Agent 方式启动会话并立即返回（通过
                                        `claude agents` 管理）
  --bare                                最小模式：跳过 hooks、LSP、插件同步、归属
                                        信息、自动记忆、后台预加载、钥匙串读取以及
                                        CLAUDE.md 自动发现。设置
                                        CLAUDE_CODE_SIMPLE=1。Anthropic 认证严格
                                        使用 ANTHROPIC_API_KEY 或通过 --settings
                                        指定的 apiKeyHelper（不会读取 OAuth 和钥匙
                                        串）。第三方提供商（Bedrock/Vertex/Foundry）
                                        使用各自的凭证。技能仍可通过 /skill-name 解
                                        析。通过以下方式显式提供上下文：
                                        --system-prompt[-file]、
                                        --append-system-prompt[-file]、--add-dir
                                        （CLAUDE.md 目录）、--mcp-config、
                                        --settings、--agents、--plugin-dir
  --betas <beta头...>                   在 API 请求中包含的 Beta 头（仅限 API key
                                        用户）
  --brief                               启用 SendUserMessage 工具，用于 Agent 与
                                        用户之间的通信
  --chrome                              启用 Claude in Chrome 集成
  ✅-c, --continue                        继续当前目录中最近的对话
  ✅--dangerously-skip-permissions        跳过所有权限检查。仅建议在无网络访问的沙箱
                                        环境中使用
  -d, --debug [过滤器]                  启用调试模式，可选按类别过滤（如
                                        "api,hooks" 或 "!1p,!file"）
  --debug-file <路径>                   将调试日志写入指定文件路径（隐式启用调试模式）
  --disable-slash-commands              禁用所有技能
  --disallowedTools, --disallowed-tools <工具...>
                                        逗号或空格分隔的禁用工具列表（如 "Bash(git
                                        *) Edit"）
  --effort <级别>                       当前会话的推理力度（low, medium, high,
                                        xhigh, max）
  --exclude-dynamic-system-prompt-sections
                                        将与机器相关的部分（cwd、环境信息、内存路
                                        径、git 状态）从系统提示词移到第一条用户消
                                        息中。可改善跨用户的提示缓存复用。仅在默认
                                        系统提示词下生效（使用 --system-prompt 时
                                        忽略）（默认：false）
  --fallback-model <模型>               当默认模型过载或不可用时，自动回退到指定模
                                        型。接受逗号分隔的列表以按顺序尝试。每个用
                                        户回合开始时重新尝试主模型（仅与 --print
                                        配合使用）
  --file <文件规格...>                  启动时下载的文件资源。格式：
                                        file_id:relative_path（如 --file
                                        file_abc:doc.txt file_def:img.png）
  --fork-session                        恢复会话时创建新的会话 ID，而非复用原 ID
                                        （与 --resume 或 --continue 配合使用）
  --from-pr [值]                        通过 PR 编号/URL 恢复关联的会话，或打开带
                                        可选搜索词的交互式选择器
  -h, --help                            显示命令帮助
  --ide                                 启动时自动连接到 IDE（当恰好有一个有效的
                                        IDE 可用时）
  --include-hook-events                 在输出流中包含所有 hook 生命周期事件（仅与
                                        --output-format=stream-json 配合使用）
  --include-partial-messages            在消息块到达时即包含部分消息（仅与 --print
                                        和 --output-format=stream-json 配合使用）
  --input-format <格式>                 输入格式（仅与 --print 配合使用）："text"
                                        （默认）或 "stream-json"（实时流式输入）
                                        （可选值："text", "stream-json"）
  --json-schema <schema>                用于结构化输出验证的 JSON Schema。示例：
                                        {"type":"object","properties":{"name":
                                        {"type":"string"}},"required":["name"]}
  --max-budget-usd <金额>               API 调用的最大美元消费限额（仅与 --print
                                        配合使用）
  --mcp-config <配置...>                从 JSON 文件或字符串加载 MCP 服务器（空格
                                        分隔）
  --model <模型>                        当前会话使用的模型。可使用别名（如 'fable'、
                                        'opus' 或 'sonnet'）或模型全名（如
                                        'claude-fable-5'）
✅-n, --name <名称>                     为当前会话设置显示名称（显示在提示框、
                                        /resume 选择器和终端标题中）
  --no-chrome                           禁用 Claude in Chrome 集成
  --no-session-persistence              禁用会话持久化——会话不会保存到磁盘且无法
                                        恢复（仅与 --print 配合使用）
  --output-format <格式>                输出格式（仅与 --print 配合使用）："text"
                                        （默认）、"json"（单个结果）或
                                        "stream-json"（实时流式输出）（可选值：
                                        "text", "json", "stream-json"）
  --permission-mode <模式>              会话使用的权限模式（可选值："acceptEdits",
                                        "auto", "bypassPermissions", "default",
                                        "dontAsk", "plan"）
  --plugin-dir <路径>                   从目录或 .zip 文件加载插件，仅本次会话有效
                                        （可重复使用：--plugin-dir A --plugin-dir
                                        B.zip）（默认：[]）
  --plugin-url <URL>                    从 URL 获取插件 .zip 文件，仅本次会话有效
                                        （可重复使用：--plugin-url A --plugin-url
                                        B）（默认：[]）
  -p, --print                           打印响应并退出（适用于管道）。注意：当
                                        Claude 以非交互模式运行时（通过 -p 或
                                        stdout 不是 TTY，如管道或重定向输出），会跳
                                        过工作区信任对话框。请仅在信任的目录中使用。
                                        此模式下，校验失败的设置文件会被静默忽略（不
                                        显示错误对话框）
  --prompt-suggestions [值]             启用提示词建议。在 print/SDK 模式下，每轮
                                        结束后发送 prompt_suggestion 消息，包含预
                                        测的下一条用户提示（可选值："true",
                                        "false", "1", "0", "yes", "no", "on",
                                        "off"，预设："true"）
  --remote-control [名称]               启动交互式会话并启用 Remote Control（可选
                                        命名）
  --remote-control-session-name-prefix <前缀>
                                        自动生成的 Remote Control 会话名称前缀（默
                                        认：主机名）
  --replay-user-messages                将来自 stdin 的用户消息重新输出到 stdout 以
                                        进行确认（仅与
                                        --input-format=stream-json 和
                                        --output-format=stream-json 配合使用）
  -r, --resume [值]                     按会话 ID 恢复对话，或打开带可选搜索词的交
                                        互式选择器
  --safe-mode                           禁用所有自定义配置（CLAUDE.md、技能、插件、
                                        hooks、MCP 服务器、自定义命令和 Agent、输出
                                        样式、工作流、自定义主题、按键绑定等）后启
                                        动——用于排查配置问题。管理员管理的（策略）
                                        设置仍然生效。认证、模型选择、内置工具和权
                                        限正常工作。设置 CLAUDE_CODE_SAFE_MODE=1
  --session-id <uuid>                   为对话使用指定的会话 ID（必须是有效的 UUID）
  --setting-sources <来源>              逗号分隔的要加载的设置来源列表（user,
                                        project, local）
  --settings <文件或JSON>               要加载额外设置的 settings JSON 文件路径或
                                        JSON 字符串
  --strict-mcp-config                   仅使用来自 --mcp-config 的 MCP 服务器，忽
                                        略所有其他 MCP 配置
  --system-prompt <提示词>              会话使用的系统提示词
  --tmux                                为 worktree 创建 tmux 会话（需要
                                        --worktree）。当可用时使用 iTerm2 原生窗
                                        格；使用 --tmux=classic 使用传统 tmux
  --tools <工具...>                     从内置工具集中指定可用的工具列表。使用 ""
                                        禁用所有工具，使用 "default" 使用所有工
                                        具，或指定工具名称（如 "Bash,Edit,Read"）
  --verbose                             覆盖配置文件中的 verbose 模式设置
  -v, --version                         输出版本号
  -w, --worktree [名称]                 为当前会话创建新的 git worktree（可选指定
                                        名称）

命令:
  agents [选项]                         管理后台 Agent
  auth                                  管理认证
  auto-mode                             检查自动模式分类器配置
  doctor                                检查 Claude Code 自动更新器的健康状态。
                                        注意：会跳过工作区信任对话框，并为健康检查
                                        启动 .mcp.json 中的 stdio 服务器。请仅在信
                                        任的目录中使用此命令
  install [选项] [目标]                 安装 Claude Code 原生构建。使用 [目标] 指
                                        定版本（stable、latest 或具体版本号）
  mcp                                   配置和管理 MCP 服务器
  plugin|plugins                        管理 Claude Code 插件
  project                               管理 Claude Code 项目状态
  setup-token                           设置长期认证令牌（需要 Claude 订阅）
  ultrareview [选项] [目标]             对当前分支（或 PR 编号/基准分支）运行云端
                                        托管的多个 Agent 代码审查并输出发现结果
  update|upgrade                        检查更新并在有可用更新时安装
```

## 三方模型接入

### 使用 minimax 模型

![image-20260403085757968](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20260403085757968.png)

cc-switch 代理可开可不开

一个/init 指令，余额23.52 元->23.22 元，有点贵，可以包月、包年

### 硅基流动

✅按量计费，每个模型都有独立的计费标准



问题： 400 thinking type should be enabled or disabled

解决： 在配置里加上一行：   "alwaysThinkingEnabled": false,

vscode 里关闭 Thinking

![image-20260403113035702](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20260403113035702.png)

### 魔塔社区





https://ccforpms.com/vibe-coding/build-iterate

### OpenRouter

[免费使用](https://mp.weixin.qq.com/s/yM5S7RSLgASK8bKBDP94KA)

### 问题解决

#### Not logged in · Please run /login  

解决：配置 ANTHROPIC_AUTH_TOKEN，而不是ANTHROPIC_AUTH_KEY，这个教程有错误。

#### Cannot read properties of undefined (reading 'input_tokens')

# 插件

## Codex plugin for Claude Code

1. /codex:review
   --background  后台 review

2. /codex:status    

3. /codex:cancel

4. /codex:result

5. /codex:rescue  直接向 codex 提交任务

   > /codex:rescue investigate why the tests started failing
   > /codex:rescue fix the failing test with the smallest safe patch
   > /codex:rescue --resume apply the top fix from the last run
   > /codex:rescue --model gpt-5.4-mini --effort medium investigate the flaky integration test
   > /codex:rescue --model spark fix the issue quickly
   > /codex:rescue --background investigate the regression
