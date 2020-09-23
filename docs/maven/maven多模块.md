# 多模块

## 反应堆

maven处理多模块项目的机制被称为反应堆，maven核心逻辑：

1. 收集需要构建的模块
2. 按照构建顺序排序
3. 按顺序构建

## 排序

必须保证每个项目再需要之前被构建

当项目排序的时候根据这些关系被使用：

- 构建中一个项目依赖另外一模块
- 一个插件声明在另外一个模块当这个模块在构建
- 一个插件构建时依赖另外一个模块
- 构建中另一个模块的扩展声明
- 按照在**<modules>**的声明顺序（如果没有其他规则的话）

笔记：在“实例化”被引用后 —— **dependencyManagement**和**pluginManagement**节点将不能构建反应器的排序了。

## 命令行选项

- `--resume-from` - resumes a reactor from the specified project (e.g. when it fails in the middle)
- `--also-make` - build the specified projects, and any of their dependencies in the reactor
- `--also-make-dependents` - build the specified projects, and any that depend on them
- `--fail-fast` - the default behavior - whenever a module build fails, stop the overall build immediately
- `--fail-at-end` - if a particular module build fails, continue the rest of the reactor and report all failed modules at the end instead
- `--non-recursive` - do not use a reactor build, even if the current project declares modules and just build the project in the current directory

























