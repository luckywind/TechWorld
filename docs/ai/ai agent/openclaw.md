



# 命令帮助

[中文帮助](https://zhuanlan.zhihu.com/p/2011017776756701009)

Commands:
  Hint: commands suffixed with * have subcommands. Run <command> --help for details.
  acp *                Agent Control Protocol tools
  agent                Run one agent turn via the Gateway
  agents *             Manage isolated agents (workspaces, auth, routing)
  approvals *          Manage exec approvals (gateway or node host)
  backup *             Create and verify local backup archives for OpenClaw state
  browser *            Manage OpenClaw's dedicated browser (Chrome/Chromium)
  channels *           Manage connected chat channels (Telegram, Discord, etc.)
  clawbot *            Legacy clawbot command aliases
  completion           Generate shell completion script
  config *             Non-interactive config helpers (get/set/unset/file/validate). Default: starts setup wizard.
  configure            Interactive setup wizard for credentials, channels, gateway, and agent defaults
  cron *               Manage cron jobs via the Gateway scheduler
  daemon *             Gateway service (legacy alias)
  dashboard            Open the Control UI with your current token
  devices *            Device pairing + token management
  directory *          Lookup contact and group IDs (self, peers, groups) for supported chat channels
  dns *                DNS helpers for wide-area discovery (Tailscale + CoreDNS)
  docs                 Search the live OpenClaw docs
  doctor               Health checks + quick fixes for the gateway and channels
  gateway *            Run, inspect, and query the WebSocket Gateway
  health               Fetch health from the running gateway
  help                 Display help for command
  hooks *              Manage internal agent hooks
  logs                 Tail gateway file logs via RPC
  memory *             Search and reindex memory files
  message *            Send, read, and manage messages
  models *             Discover, scan, and configure models
  node *               Run and manage the headless node host service
  nodes *              Manage gateway-owned node pairing and node commands
  onboard              Interactive onboarding wizard for gateway, workspace, and skills
  pairing *            Secure DM pairing (approve inbound requests)
  plugins *            Manage OpenClaw plugins and extensions
  qr                   Generate iOS pairing QR/setup code
  reset                Reset local config/state (keeps the CLI installed)
  sandbox *            Manage sandbox containers for agent isolation
  secrets *            Secrets runtime reload controls
  security *           Security tools and local config audits
  sessions *           List stored conversation sessions
  setup                Initialize local config and agent workspace
  skills *             List and inspect available skills
  status               Show channel health and recent session recipients
  system *             System events, heartbeat, and presence
  tui                  Open a terminal UI connected to the Gateway
  uninstall            Uninstall the gateway service + local data (CLI remains)
  update *             Update OpenClaw and inspect update channel status
  webhooks *           Webhook helpers and integrations

# 切换模型

## 修改配置文件

### 手动修改

https://dayulab.com/tools/efficiency/openclaw 这个页面可以帮助修改配置文件

设置 models.mode 为 merge

```routeros
openclaw config set models.mode merge
```

设置默认模型（以deepseek-chat为例）

```bash
openclaw models set deepseek/deepseek-chat
```

### 自动修改

openclaw config 会以会话的形式进行配置



# FAQ

## ERR_CONNECTION_REFUSED

openclaw gateway restart

Gateway restart timed out after 60s waiting for health checks.

openclaw gateway status

> Last gateway error: 2026-03-10T12:52:43.311+08:00 Gateway start blocked: set gateway.mode=local (current: unset) or pass --allow-unconfigured.
> Logs: ~/.openclaw/logs/gateway.log
> Errors: ~/.openclaw/logs/gateway.err.log

~/.openclaw/openclaw.json  设置 gateway.mode=local 重启 gateway 即可

## No API key found

⚠️ Agent failed before reply: No API key found for provider "anthropic". Auth store: ~/.openclaw/agents/main/agent/auth-profiles.json (agentDir: ~/.openclaw/agents/main/agent). Configure auth for this agent (openclaw agents add <id>) or copy auth-profiles.json from the main agentDir.
Logs: openclaw logs --follow

try fix: 

openclaw models status

Missing auth
- anthropic Run `claude setup-token`, then `openclaw models auth setup-token` or `openclaw configure`.