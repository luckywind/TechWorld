1. 将 `.git` 重命名为其他名称，例如 `obsidian-git` (mv `.git obsidian-git`)
2. 等待此更改同步到我关心的其他设备
3. 将 `obsidian-git` 移动到云存储之外的某个地方 (`mv obsidian-git ~/.obsidian-git`)
4. 让 git 知道新的 `git-dir`。为此，在icloud 项目下创建一个新文件.git，指定 `.obsidian-git`的位置：`gitdir: ~/.obsidian-git`

​	要注意⚠️：`gitdir：`后面有个空格！！

5. 然后就可以在icloud 项目下执行git 命令了。

[参考](https://www.reddit.com/r/ObsidianMD/comments/1fia8yi/using_git_for_versioning_alongside_cloud_for/?tl=zh-hans)