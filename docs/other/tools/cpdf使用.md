```

```

# cpdf 使用帮助文档 - 文件合并与拆分

来源：`cpdf手册.pdf`，重点整理第 1 章“Basic Usage”和第 2 章“Merging and Splitting”中与 PDF 拆分、合并相关的命令。

## 1. 基本命令格式

```bash
cpdf [操作] 输入文件 [页码范围] -o 输出文件
```

最简单的复制：

```bash
cpdf in.pdf -o out.pdf
```

如果文件名不含点号，cpdf 可能无法判断它是输入文件，需要加 `-i`：

```bash
cpdf -i in -o out.pdf
```

## 2. 页码范围写法

拆分、抽取、合并时常用页码范围来选择页面。

```bash
cpdf in.pdf 2-5 -o out.pdf
```

表示抽取第 2 到第 5 页。

常用范围写法：

| 写法 | 含义 |
| --- | --- |
| `1` | 第 1 页 |
| `1-5` | 第 1 到第 5 页 |
| `6-3` | 第 6 到第 3 页，倒序取页 |
| `1,2,7-end` | 第 1、2 页，以及第 7 页到最后 |
| `end` | 最后一页 |
| `~1` | 倒数第 1 页，即最后一页 |
| `~3-~1` | 最后三页，按正常顺序输出 |
| `odd` | 奇数页 |
| `even` | 偶数页 |
| `1-16odd` | 第 1 到 16 页中的奇数页 |
| `reverse` | 全部页面倒序，等同于 `end-1` |
| `all` | 全部页面，等同于 `1-end` |
| `NOT<范围>` | 反选页面范围 |
| `<n>DUP<范围>` | 将范围内每页重复 n 次 |
| `empty` | 空范围 |

示例：

```bash
cpdf in.pdf 1,2,7-end -o out.pdf
```

输出除第 3、4、5、6 页以外的页面。

```bash
cpdf in.pdf 2DUP1-10 -o out.pdf
```

将第 1 到第 10 页都重复一次，输出顺序为 `1,1,2,2,...,10,10`。

## 3. 合并 PDF

### 3.1 普通合并

```bash
cpdf -merge in1.pdf in2.pdf -o out.pdf
```

把 `in1.pdf` 和 `in2.pdf` 按命令行顺序合并为 `out.pdf`。

`-merge` 是默认操作，所以也可以省略：

```bash
cpdf in1.pdf in2.pdf -o out.pdf
```

### 3.2 合并指定页

```bash
cpdf -merge a.pdf 1 b.pdf 2-end -o out.pdf
```

取 `a.pdf` 的第 1 页，再接上 `b.pdf` 的第 2 页到最后一页。

### 3.3 合并目录中的 PDF

```bash
cpdf -merge -idir files -o out.pdf
```

合并 `files` 目录中的 PDF。文件按字母顺序处理。

如果文件名是数字，建议补齐前导零，否则顺序可能不符合预期。例如：

```text
001.pdf
002.pdf
010.pdf
```

只合并目录中扩展名为 `.pdf` 的文件：

```bash
cpdf -merge -idir-only-pdfs -idir files -o out.pdf
```

### 3.4 交错合并页面

```bash
cpdf -merge a.pdf b.pdf -collate -o out.pdf
```

`-collate` 会交错取页：先取 `a.pdf` 第 1 页，再取 `b.pdf` 第 1 页；然后取 `a.pdf` 第 2 页，再取 `b.pdf` 第 2 页，以此类推。

按多页块交错：

```bash
cpdf -merge a.pdf b.pdf -collate-n 2 -o out.pdf
```

`-collate-n 2` 表示每次从每个文件取 2 页。

### 3.5 合并时保留或处理附加信息

合并时 cpdf 会尽量维护书签、命名目标、注释、Tagged PDF 信息等。无法合并的 PDF 特性，通常保留第一个包含该特性的文档中的版本。

常用选项：

| 选项 | 作用 |
| --- | --- |
| `-retain-numbering` | 保留每个输入 PDF 原有的页码标签，而不是从 1 重新编号 |
| `-remove-duplicate-fonts` | 多个输入文件使用相同字体时，尽量只在输出中保留一份 |
| `-merge-add-bookmarks` | 为每个输入文件添加一个顶层书签，书签名默认使用文件名 |
| `-merge-add-bookmarks-use-titles` | 与 `-merge-add-bookmarks` 一起用，改用 PDF 元数据中的标题作为书签名 |
| `-process-struct-trees` | 合并结构树，用于 Tagged PDF / PDF/UA 等需要保留逻辑结构的文件 |
| `-subformat PDF/UA-2` | 合并多个 PDF/UA 文件时，可用于让输出符合 PDF/UA-2 结构要求 |

示例：

```bash
cpdf -merge a.pdf b.pdf -merge-add-bookmarks -o out.pdf
```

输出文件中会增加以输入文件名命名的顶层书签。

```bash
cpdf -merge a.pdf b.pdf -process-struct-trees -subformat PDF/UA-2 -o out.pdf
```

用于更好地处理 PDF/UA 或带结构树的 PDF。

## 4. 拆分 PDF

### 4.1 按页拆分

```bash
cpdf -split a.pdf -o out%%%.pdf
```

把 `a.pdf` 拆成单页文件：

```text
out001.pdf
out002.pdf
out003.pdf
...
```

### 4.2 按固定页数拆分

```bash
cpdf -split in.pdf -chunk 10 -o Chunk%%%.pdf
```

每 10 页拆成一个文件：

```text
Chunk001.pdf
Chunk002.pdf
...
```

也可以先筛选页面，再拆分：

```bash
cpdf a.pdf even AND -split -chunk 10 -o dir/out%%%.pdf
```

含义：先取 `a.pdf` 的偶数页，再每 10 页拆成一个文件，输出到 `dir` 目录。注意：输出目录必须已经存在。

### 4.3 输出文件名格式

`-split`、`-split-bookmarks`、`-split-max` 的 `-o <format>` 可以使用占位符。

| 占位符 | 含义 |
| --- | --- |
| `%`、`%%`、`%%%` | 序号，按百分号数量补零。例如 `%%%` 生成 `001` |
| `@F` | 原文件名，不含扩展名 |
| `@N` | 序号，不补零 |
| `@S` | 当前分块的起始页 |
| `@E` | 当前分块的结束页 |
| `@B` | 当前页的书签名，如果有 |
| `@b<w>@` | 当前页书签名的前 w 个字符 |

示例：

```bash
cpdf -split in.pdf -chunk 5 -o @F_@S-@E.pdf
```

可能生成：

```text
in_1-5.pdf
in_6-10.pdf
```

如果需要给 `@S` 或 `@E` 补零，可以在后面加多个 `@`：

```bash
cpdf -split in.pdf -chunk 5 -o @S@@@-@E@@@.pdf
```

### 4.4 按书签拆分

```bash
cpdf -split-bookmarks 0 a.pdf -o out%%%.pdf
```

按顶层书签拆分。`0` 表示顶层书签，`1` 表示顶层和下一级书签都作为拆分边界，以此类推。

使用书签名作为文件名：

```bash
cpdf -split-bookmarks 0 a.pdf -o @B.pdf
```

书签名用于文件名时，cpdf 会移除一些不适合文件名的字符，例如：

```text
/ ? < > \ : * | " ^ + =
```

截断书签名：

```bash
cpdf -split-bookmarks 0 a.pdf -o @b10@.pdf
```

表示使用书签名前 10 个字符作为输出文件名。

如果书签名包含非 ASCII 字符，通常应加 `-utf8`：

```bash
cpdf -split-bookmarks 0 a.pdf -utf8 -o @B.pdf
```

### 4.5 按最大文件大小拆分

```bash
cpdf -split-max 100kB in.pdf -o out%%%.pdf
```

从文件开头开始拆分，每个输出文件尽量不超过指定大小。

支持的大小单位包括：

```text
kB, KiB, MB, MiB, GB, GiB
```

如果无法按指定大小完成拆分，cpdf 会报错，并且不会生成部分输出文件。

### 4.6 Spray：轮流分发页面到多个输出文件

```bash
cpdf -spray in.pdf -o a.pdf -o b.pdf
```

把输入文件页面轮流写入多个输出文件。两个输出文件时，效果是：

```text
a.pdf：第 1、3、5、... 页
b.pdf：第 2、4、6、... 页
```

这是 cpdf 中少数允许多次使用 `-o` 的操作。

也可以分发到更多文件：

```bash
cpdf -spray in.pdf -o a.pdf -o b.pdf -o c.pdf
```

效果是：

```text
a.pdf：第 1、4、7、... 页
b.pdf：第 2、5、8、... 页
c.pdf：第 3、6、9、... 页
```

## 5. 拆分时保留结构和加密

### 5.1 结构树

对于 Tagged PDF 或 PDF/UA 文件，拆分时建议使用：

```bash
cpdf -split in.pdf -process-struct-trees -o out%%%.pdf
```

`-process-struct-trees` 会为每个输出文件裁剪结构树，只保留对应页面需要的结构信息。否则结构树可能会被完整保留到每个拆分文件中，既增大体积，也可能影响 PDF/UA 等标准符合性。

### 5.2 加密

拆分操作可以配合第 4 章中的加密参数，为每个拆分后的 PDF 加密。

如果输入 PDF 已加密，并且希望输出文件沿用原来的加密设置，可以使用：

```bash
cpdf -split in.pdf -recrypt -o out%%%.pdf
```

## 6. 串联操作：AND

`AND` 可以把多个 cpdf 操作串在同一条命令中，避免反复读写中间文件。

示例：先合并，再加文字，再追加另一个文件：

```bash
cpdf -merge in.pdf in2.pdf AND -add-text "Label" AND -merge in3.pdf -o out.pdf
```

示例：先取偶数页，再按每 10 页拆分：

```bash
cpdf a.pdf even AND -split -chunk 10 -o dir/out%%%.pdf
```

## 7. 常见任务速查

### 抽取指定页面

```bash
cpdf in.pdf 1-3,6 -o out.pdf
```

### 删除指定页面

```bash
cpdf in.pdf NOT3-6 -o out.pdf
```

### 合并多个 PDF

```bash
cpdf -merge a.pdf b.pdf c.pdf -o out.pdf
```

### 合并目录中的全部 PDF

```bash
cpdf -merge -idir-only-pdfs -idir files -o out.pdf
```

### 合并并自动添加顶层书签

```bash
cpdf -merge a.pdf b.pdf c.pdf -merge-add-bookmarks -o out.pdf
```

### 每页拆成一个 PDF

```bash
cpdf -split in.pdf -o page%%%.pdf
```

### 每 10 页拆成一个 PDF

```bash
cpdf -split in.pdf -chunk 10 -o part%%%.pdf
```

### 按书签拆分，并用书签名命名

```bash
cpdf -split-bookmarks 0 in.pdf -utf8 -o @B.pdf
```

### 按最大 1MB 拆分

```bash
cpdf -split-max 1MB in.pdf -o part%%%.pdf
```

### 奇偶页分离

```bash
cpdf -spray in.pdf -o odd.pdf -o even.pdf
```

## 8. 注意事项

- `-merge` 按命令行中的输入顺序合并页面。
- `-idir` 合并目录文件时按字母顺序排序，数字文件名建议使用 `001.pdf`、`002.pdf` 这种格式。
- `-split` 的输出目录必须已经存在，cpdf 不会自动创建目录。
- `-split` 输出格式中的 `%` 数量要足够容纳输出文件数量；如果位数不够，结果未定义。
- 处理中文书签文件名时，按书签拆分建议加 `-utf8`。
- 处理 PDF/UA 或 Tagged PDF 时，拆分或合并建议加 `-process-struct-trees`。
- `-spray` 是轮流分发页面，不是连续分块；连续分块应使用 `-split -chunk`。
- 多个操作连续处理时，优先考虑 `AND`，可减少中间文件。


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
cpdf -split-bookmarks 1 input.pdf -o output

# 按两级书签拆分
cpdf -split-bookmarks 2 input.pdf -o output
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