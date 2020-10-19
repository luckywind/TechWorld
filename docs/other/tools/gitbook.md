使用gitbook+typora制作pdf电子书

```shell
npm install -g gitbook-cli
gitbook -V
CLI version: 2.3.2
GitBook version: 3.2.3
用于生成pdf
npm install gitbook-pdf -g 这里一直提示网络代理问题
下载https://download.calibre-ebook.com/5.3.0/calibre-5.3.0.dmg
ln -s /Applications/calibre.app/Contents/MacOS/ebook-convert /usr/local/bin
```

# 基本使用

## 初始化

```shell
mkdir NoteBook
cd NoteBook
gitbook init
```

README.md 是首页

SUMMARY.md 是目录，你可以在这里创建目录

```markdown
# Summary

* [Introduction](README.md)
* [文章列表](blog/README.md)
    * [第一章](blog/第一章.md)
    * [第二章](blog/第二章.md)
```

这里编辑好目录之后，重新执行 ***gitbook init\*** 命令，会根据目录中的路径和文件名，自动创建对应的文件和文件夹。

执行 ***gitbook serve\*** 命令后，会启动一个 4000 端口，然后就可以通过 http://localhost:4000 地址在浏览器中查看了。

## 生成网页

```shell
gitbook build 把文章生成html网页，会生成一个_book目录，可以直接部署到服务器
gitbook build ./{book_name} --output=./{outputFolde}

```

## 输出pdf

```bash
$ gitbook pdf ./  {book_name}
gitbook pdf ./ ./mybook.pdf
```

如果，你已经在编写的gitbook当前目录，也可以使用相对路径。

```bash
$ gitbook pdf .
```

然后，你就会发现，你的目录中多了一个名为`book.pdf`的文件。

[参考](https://www.codenong.com/cs106959327/)

## 电子书籍

基本结构

```shell
.
├── book.json
├── README.md
├── SUMMARY.md
├── chapter-1/
|   ├── README.md
|   └── something.md
└── chapter-2/
    ├── README.md
    └── something.md
```

GitBook 特殊文件的功能：

| 文件          | 描述                              |
| ------------- | --------------------------------- |
| `book.json`   | 配置数据 (**optional**)           |
| `README.md`   | 电子书的前言或简介 (**required**) |
| `SUMMARY.md`  | 电子书目录 (**optional**)         |
| `GLOSSARY.md` | 词汇/注释术语列表 (**optional**)  |

### 项目与子目录集成

对于软件项目，您可以使用子目录（如 `docs/` ）来存储项目文档的图书。您可以配置根选项来指示 GitBook 可以找到该图书文件的文件夹：

```shell
.
├── book.json
└── docs/
    ├── README.md
    └── SUMMARY.md
```

在 `book.json` 中配置以下内容：

```json
{
    "root": "./docs"
}
```

### summary

向父章节添加嵌套列表将创建子章节。

```markdown
# Summary

* [Part I](part1/README.md)
    * [Writing is nice](part1/writing.md)
    * [GitBook is nice](part1/gitbook.md)
* [Part II](part2/README.md)
    * [We love feedback](part2/feedback_please.md)
    * [Better tools for authors](part2/better_tools.md)
```

每章都有一个专用页面（`part#/README.md`），并分为子章节

### **锚点**

目录中的章节可以使用锚点指向文件的特定部分。

```markdown
# Summary

### Part I

* [Part I](part1/README.md)
    * [Writing is nice](part1/README.md#writing)
    * [GitBook is nice](part1/README.md#gitbook)
* [Part II](part2/README.md)
    * [We love feedback](part2/README.md#feedback)
    * [Better tools for authors](part2/README.md#tools)
```



[详细](https://www.cnblogs.com/jingmoxukong/p/7453155.html)

# 发布

[参考](https://tonydeng.github.io/gitbook-zh/gitbook-howtouse/publish/gitpages.html)

## 发布到GitHub pages

```shell
#!/bin/sh

Usage(){
    echo "welcome use front-end release script
    -----------------------------------------
    use it require input your deploy target
           gook luck!
    author:
        wolf.deng@gmail.com
    -----------------------------------------
    Usage:

    # 发布github.io
    ./deploy.sh github.io
    "
}

die( ){
    echo
    echo "$*"
    Usage
    echo
    exit 1
}

git pull

# get real path
cd `echo ${0%/*}`
abspath=`pwd`

#清除之前生成的文件
rm -rf $abspath/build

TARGET=$1
PROJECT='gitbook-howtouse'

# 把./build/$PROJECT目录转换成pages到GITHUB_PROJECT目录，并同步到github-pages
sync( ){
    case $* in
        "github.io" )
            echo "sync $PROJECT gitbook website to $TARGET"
            GITHUB_PROJECT="~/workspace/github/tonydeng.github.io/source/gitbook-zh"
            Sync="rsync -avu --delete --exclude '*.sh' --exclude '.git*' --exclude '.DS_Store' $abspath/build/$PROJECT $GITHUB_PROJECT"
            echo $Sync
            eval $Sync

            cd $GITHUB_PROJECT
            rake generate
            rake deploy
        ;;

    esac
}
# 构建到./build/$PROJECT下
build(){
    echo "build $PROJECT document"

    OUTPUT="./build/$PROJECT"

    gitbook init

    rm  -rf $OUTPUT
    gitbook build . --output=$OUTPUT
}

blog(){
    echo "build & sync $PROJECT to github.io"
    build
    sync $TARGET
}

# 判断执行参数，调用指定方法
case $TARGET in
    github.io )
       blog
        ;;
    * )
        die "parameters is no reght!"
        ;;
esac
```

