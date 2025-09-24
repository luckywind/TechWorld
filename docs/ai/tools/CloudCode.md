# 参考

[参考文档](https://cloud.tencent.com/developer/article/2539379)

# 公告

安装 Node.js（已安装可跳过）

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



2️⃣ 安装 Claude Code



```bash
npm install -g @anthropic-ai/claude-code
claude --version
```



3️⃣ 开始使用



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