[best-practices](https://developers.openai.com/codex/learn/best-practices)

# 好的提示词

- **Goal:** What are you trying to change or build?
- **Context:** Which files, folders, docs, examples, or errors matter for this task? You can @ mention certain files as context.
- **Constraints:** What standards, architecture, safety requirements, or conventions should Codex follow?
- **Done when:** What should be true before the task is complete, such as tests passing, behavior changing, or a bug no longer reproducing?

# 推理级别选择

- Low for faster, well-scoped tasks
- Medium or High for more complex changes or debugging
- Extra High for long, agentic, reasoning-heavy tasks



# Plan 模式

1. Shift+Tab 开启 Plan 模式
2. 让 Codex 提出疑问
3. 使用 PLANS.md 模板,  [Using PLANS.md for multi-hour problem solving](https://developers.openai.com/cookbook/articles/codex_exec_plans)

## AGENTS ：可复用指令

AGENTS.md 是 Codex 的自定义指令文件，是 Codex 在执行任何工作之前读取的指令文件，允许你为 Codex 设置全局指导和工作流规范。



AGENTS.md 覆盖：

- repo layout and important directories
- How to run the project
- Build, test, and lint commands
- Engineering conventions and PR expectations
- Constraints and do-not rules
- What done means and how to verify work



放在哪里？ 

- **全局范围**：读取 `~/.codex/AGENTS.override.md` 或 `~/.codex/AGENTS.md`
- **项目范围**：从项目根目录到当前目录，逐层检查 `AGENTS.override.md` 和 `AGENTS.md`
- **就近优先**：更接近当前目录的文件会覆盖之前的指导

# 一致性配置

- 默认`~/.codex/config.toml` (Settings → Configuration → Open config.toml）

  

# test 和 revie