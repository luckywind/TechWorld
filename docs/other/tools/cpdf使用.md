# cpdf 使用帮助文档 - 文件合并与拆分

## 简介
cpdf 是一个强大的命令行工具，用于处理 PDF 文件。它可以在不依赖其他软件的情况下对 PDF 文件进行各种操作，包括合并、拆分、旋转、加密等。

## 安装方法

### macOS
```bash
# 使用 Homebrew 安装
brew install cpdf
```

### Linux
```bash
# 使用包管理器安装（以Ubuntu为例）
sudo apt-get install cpdf

# 或从官方网站下载
wget https://github.com/coherentgraphics/cpdf-binaries/raw/master/Linux/cpdf
chmod +x cpdf
sudo mv cpdf /usr/local/bin/
```

### Windows
从官方网站下载 Windows 版本并安装：
https://www.coherentpdf.com/

## 文件合并操作

### 基本合并
将多个 PDF 文件按顺序合并为一个文件：

```bash
# 合并两个文件
cpdf file1.pdf file2.pdf -o output.pdf

# 合并多个文件
cpdf file1.pdf file2.pdf file3.pdf -o output.pdf

# 使用通配符合并
cpdf *.pdf -o output.pdf
```

### 合并时保留书签
```bash
# 合并文件并保留原始书签
cpdf -merge file1.pdf file2.pdf -o output.pdf
```

### 合并时添加分隔页
```bash
# 合并文件并在每个文件前添加分隔页
cpdf -merge -add-page-labels file1.pdf file2.pdf -o output.pdf
```

### 指定页面范围进行合并
```bash
# 合并特定页面
cpdf file1.pdf 1-5 file2.pdf 2-10 -o output.pdf

# 合并奇数页
cpdf file1.pdf odd file2.pdf odd -o output.pdf

# 合并偶数页
cpdf file1.pdf even file2.pdf even -o output.pdf
```

## 文件拆分操作

### 按页数拆分
将 PDF 文件按指定页数拆分成多个文件：

```bash
# 每10页拆分成一个文件
cpdf -split-in-two 10 input.pdf -o output

# 将文件拆分为单页
cpdf -split-by-page input.pdf -o output
```

### 按章节拆分
根据书签结构拆分文件：

```bash
# 按顶层书签拆分
cpdf -split-by-bookmarks 1 input.pdf -o output

# 按两级书签拆分
cpdf -split-by-bookmarks 2 input.pdf -o output
```

### 按页面范围拆分
```bash
# 提取特定页面范围
cpdf input.pdf 1-10 -o output1.pdf
cpdf input.pdf 11-20 -o output2.pdf

# 提取奇数页
cpdf input.pdf odd -o odd_pages.pdf

# 提取偶数页
cpdf input.pdf even -o even_pages.pdf
```

### 按份数拆分
将 PDF 文件平均拆分为指定份数：

```bash
# 将文件拆分为3份
cpdf -split-by-n 3 input.pdf -o output
```

## 高级合并选项

### 重新排列页面顺序
```bash
# 反向合并页面
cpdf input.pdf end-1 -o reversed.pdf

# 自定义页面顺序
cpdf input.pdf 1,3,2,4 -o reordered.pdf
```

### 添加空白页
```bash
# 在每个文件后添加空白页
cpdf -pad-after file1.pdf file2.pdf -o output.pdf
```

### 合并时应用变换
```bash
# 合并时旋转页面
cpdf -rotate 90 file1.pdf file2.pdf -o output.pdf

# 合并时缩放页面
cpdf -scale 0.8 file1.pdf file2.pdf -o output.pdf
```

## 高级拆分选项

### 根据页面内容拆分
```bash
# 根据页面尺寸拆分
cpdf -split-by-size input.pdf -o output

# 根据页面方向拆分
cpdf -split-by-orientation input.pdf -o output
```

### 提取特定类型内容
```bash
# 只提取包含图像的页面
cpdf -split-images-only input.pdf -o output

# 只提取包含文本的页面
cpdf -split-text-only input.pdf -o output
```

## 批处理操作

### 批量合并多个文件夹中的文件
```bash
# 批量处理多个目录
for dir in */; do
    cpdf "$dir"*.pdf -o "${dir%/}_merged.pdf"
done
```

### 批量拆分文件
```bash
# 批量拆分多个文件
for file in *.pdf; do
    cpdf -split-by-page "$file" -o "${file%.pdf}_pages"
done
```

## 常用参数说明

| 参数                | 说明                     |
| ------------------- | ------------------------ |
| -o                  | 指定输出文件名           |
| -merge              | 合并模式                 |
| -split-in-two       | 按页数拆分               |
| -split-by-page      | 按页面拆分               |
| -split-by-bookmarks | 按书签拆分               |
| -split-by-n         | 按份数拆分               |
| -verbose            | 显示详细处理信息         |
| -quiet              | 静默模式，不显示处理信息 |

## 注意事项

1. **文件路径**: 确保文件路径正确，包含空格的路径需要用引号包围
2. **权限问题**: 确保对输入文件有读取权限，对输出路径有写入权限
3. **内存使用**: 处理大型文件时可能需要较多内存
4. **备份原文件**: 在进行重要操作前建议备份原文件
5. **编码问题**: 处理包含中文等非ASCII字符的文件时注意编码设置

## 示例应用场景

### 场景1: 合并季度报告
```bash
cpdf Q1_report.pdf Q2_report.pdf Q3_report.pdf Q4_report.pdf -o Annual_Report.pdf
```

### 场景2: 拆分合同文档
```bash
cpdf contract.pdf 1-5 -o contract_part1.pdf
cpdf contract.pdf 6-10 -o contract_part2.pdf
```

### 场景3: 提取特定页面
```bash
# 提取前10页
cpdf document.pdf 1-10 -o first_10_pages.pdf

# 提取最后5页
cpdf document.pdf 'end-4-end' -o last_5_pages.pdf
```

### 场景4: 创建打印友好版本
```bash
# 拆分为单页便于双面打印
cpdf document.pdf -split-by-page -o printable_pages
```