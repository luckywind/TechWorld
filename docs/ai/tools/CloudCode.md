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

## 命令

/btw ： 临时提问，回答完按按空格或者回车，直接把这一段消除掉，继续主程序。

/rewind： 回退，可以选择回退代码、对话到历史的某个时间(可以选择)

/branch: 对话分叉

/simplify: Claude Code会同时启动三个平行的Agent，分别从代码复用、代码质量、运行效率三个角度审查你的改动。

/remote-control： 它会生成一个URL，手机上打开这个链接整个会话就出现在手机上

## 使用 minimax 模型

![image-20260403085757968](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20260403085757968.png)

cc-switch 代理可开可不开

一个/init 指令，余额23.52 元->23.22 元，有点贵，可以包月、包年

## 硅基流动

✅按量计费，每个模型都有独立的计费标准



问题： 400 thinking type should be enabled or disabled

解决： 在配置里加上一行：   "alwaysThinkingEnabled": false,

vscode 里关闭 Thinking

![image-20260403113035702](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20260403113035702.png)

## 魔塔社区





https://ccforpms.com/vibe-coding/build-iterate

## OpenRouter

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
