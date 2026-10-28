1. pdf试卷文件
2. latex 文件
3. 原子化拆分为多个题，每个题一个 markdown 文件，自动编号，自动打 tags 、difficulty、question_type、knowledge等，以便后续 obsidian 使用 sql 语法查询
4. markdown 讲义可以引用这些题目文档组成讲义源文件
把讲义源文件嵌入到讲义 latex 模板中获得完整讲义 latex源文件，例如 main.tex 可以\input{xxx.tex}这种方式



人工调整 latex： 

vscode 打开 latex，使用 latex workshop 插件可以 tex 和 pdf双向定位。自动编译 latex



# 依赖安装

brew install --cask mactex-no-gui

> 不带 GUI 应用的mactex 

# LaTeX Workshop 

