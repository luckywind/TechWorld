[参考](https://www.bmpi.dev/self/build-write-tool-v1/)

# 安装博客系统

```shell
npm install hexo-cli -g
hexo init blog
cd blog
npm install
hexo server
```

这时候你已经部署好了Hexo博客系统，并且初始化好了博客系统所需的文件，blog就是你的博客根目录。这时候我们需要安装一款名为Next的主题，这个主题也是本博客目前使用的主题，你也可以选择你自己喜欢的主题。Next主题很强大，响应式设计并且支持很多第三方服务的特性，这个大家可以通过官网Hexo来查看。

经过Next主题官方文档的指导后，相信你已经配置好了该主题。那目前我们已经完成了博客系统和主题的配置，接下来需要把博客系统发布到互联网上，这样所有人就可以来访问我们的博客了。

# 主题

[Next主题文档](https://theme-next.iissnan.com/getting-started.html)

https://www.jianshu.com/p/84a8384be1ae

https://github.com/theme-next/hexo-theme-next

> ```
> $ cd hexo
> $ git clone https://github.com/theme-next/hexo-theme-next themes/next
> ```



更新

```shell
$ cd themes/next
$ git pull
```



# 系统部署(GitHub Pages)

部署到gitHub后就可以这么把网站上传到GitHub的网站上了，可以访问 [http://username.github.io](http://username.github.io/) ，这网址就是你的博客地址，需要把里面的username替换为你的GitHub账户名。

```shell
git add --all
~$git commit -m "Initial commit"
~$git push -u origin master
```

## 域名绑定

毕竟 [http://username.github.io](http://username.github.io/) 这种域名无法体现我们对逼格，可以在Godaddy或者万网注册一个域名去，可以以你的名字来注册，一般短的都注册不到了。万网的域名如果托管到阿里云到话需要备案，至于备案流程的话阿里云会有相关的流程帮你做，大概需要半个月之久。备案后我们可以在域名管理里把域名解析到 [http://username.github.io](http://username.github.io/) ，如果觉得万网的DNS不好的话可以把DNS解析放到DNSNode上面去，然后在里面设置CNAME子域名。

然后在blog目录里面的source目录下新建一个CNAME文件，文件内容就是你自己的域名。这样你可以直接访问你的域名就可以访问你的博客了。实际上做了一次跳转。

## 运营统计

接下来我们需要对博客和主题进行一些配置，以达到更好的一些效果，比如评论分享（多说）、文章阅读次数（LeanCloud）、文章分类标签、侧边栏的设置、第三方服务的配置（Google Webmaster & Analytics、腾讯站点分析）。这方面可以看Next主题的配置文档。 大概说下流程，评论可以在Next主题里面启用多说评论系统（你需要注册一个多说账户），文章阅读次数可以参考这篇文章。文章分类标签可以在用Hexo命令来新建分类和标签页面，这个可以参考Hexo官方文档，如果你想对主题CSS等文件进行一些修改后发现并没有效果的话，那可以先清空Hexo数据库和产生的文件。进入blog目录在命令行执行：

```shell
hexo clean #清空Hexo数据库和产生的文件
hexo d -g #重新产生博客文章并发布到GitHub，前提得在Hexo里配置好GitHub账户
```

侧边栏得配置可以在Next主题里面配置。第三方服务等都可以参考Next主题的配置文档。这样通过Google的Webmaster你可以把你的站点设置的对搜索引擎更友好，这样别人就可以方便的通过搜索引擎找到你的文章了，提高流量了。同时设置腾讯分析和Google Analytics可以让你很方便的了解网站的流量和访问情况等。腾讯分析可以每天或每周订阅网站统计分析报告。

# 写文章

## 常用命令

![image-20220619170425550](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20220619170425550.png)

1. hexo init  [folder]  命令用于初始化本地文件夹为网站的根目录,`folder` 可选参数，用以指定初始化目录的路径，若无指定则默认为当前目录
2. hexo new [layout] <title> 命令用于新建文章，一般可以简写为 `hexo n`

- `layout` 可选参数，用以指定文章类型，若无指定则默认由配置文件中的 default_layout 选项决定
- `title` 必填参数，用以指定文章标题，如果参数值中含有空格，则需要使用双引号包围

3. hexo generate 命令用于生成静态文件，一般可以简写为 `hexo g`

   > `-d` 选项，指定生成后部署，与 `hexo d -g` 等价

4. hexo server 启动本地服务器，简写为hexo s

5. hexo deploy 部署网站，简写hexo d

   > -g` 选项，指定生成后部署，与 `hexo g -d` 等价

6. hexo clean 清理缓存文件

```shell
cd blog
hexo new 文章标题
hexo d -g 重新产生博客文章并发布到GitHub

hexo g == hexo generate#生成
hexo s == hexo server #启动服务预览
hexo d == hexo deploy#部署
```



服务器：

`hexo server` #Hexo 会监视文件变动并自动更新，您无须重启服务器。
`hexo server -s` #静态模式
`hexo server -p 5000` #更改端口
`hexo server -i 192.168.1.1` #自定义 IP

`hexo clean` #清除缓存 网页正常情况下可以忽略此条命令
`hexo g` #生成静态网页
`hexo d` #开始部署



删除文章

首先进入到source / _post 文件夹中，找到helloworld.md文件，在本地直接执行删除。然后依次执行

> hexo d -g

## 图片问题

[参考](https://www.cnblogs.com/cocowool/p/hexo-image-link.html)

目标： 

我的目标是使用Typora编写博客，编写过程中可能通过拷贝、粘贴插入图片，也可能从网络上下载图片。希望能够达到以下效果：

- 使用Typora编写的时候能够实时看到图片
- 本地使用`hexo server`浏览效果时，也能够看到图片
- 图片和Markdown文件放一起都上传到GitHub pages。

### typora编写博客

Typora支持将插入的图片文件拷贝到指定路径，通过`Typora->偏好设置->图像`，然后参照下图选择`复制到指定路径`将图片拷贝到与Markdown文件同名目录下。

![image-20220415213448381](../../../../../../Library/Application Support/typora-user-images/image-20220415213448381.png)



### hexo配置

1. 生成文章资源文件夹

首先修改 hexo 全局配置文件 `_config.yml` 中的配置：

```yaml
post_asset_folder: true
```

当该配置被应用后，使用`hexo new`命令创建新文章时，会生成相同名字的文件夹，也就是文章资源文件夹。

2. 安装插件hexo-renderer-marked

npm install hexo-renderer-marked，这是为了使网页只认识{% asset_img image.jpg 这是一张图片 %}这种语法的图片，安装该插件后，网页认识`![](image.jpg)`这种markdown语法的图片

3. 安装插件hexo-image-link

npm install hexo-image-link --save，这是因为typora在我们插入图片的时候用的是包含了一个与Markdown文件同名文件夹的相对路径，而生成的静态文件夹下没有那个同名文件夹所以造成了访问404。

## 发布新建文章

hexo d -g

> 如果发布的时候报错： *ERROR Deployer not found: git*， 执行*npm install hexo-deployer-git –save*

## 删除文章

先本地删除: source / _post 文件夹中删除即可

同步到github前，执行hexo clean命令

重新发布就行

hexo d -g

# 集成百度统计

[参考](https://blog.csdn.net/mqdxiaoxiao/article/details/93137447)

# 评论系统

## waline评论系统

[参考](https://waline.js.org/guide/get-started.html#html-%E5%BC%95%E5%85%A5-%E5%AE%A2%E6%88%B7%E7%AB%AF)

AppID

```
kuJ1MtNFcNXjUQ98U5xheUCX-MdYXbMMI
```

AppID 是该项目的唯一标识符。此 ID 不可变更。

AppKey

```
XJuNQRfFOOnU8YNyLr8igy2a
```

AppKey 是公开的访问密钥，适用于在公开的客户端中使用。使用 AppKey 进行的访问受到 ACL 的限制。

MasterKey

`BlBgbHOhcThAcHNon5j0pENp` 重置

MasterKey 是私密的访问密钥，适用于在服务器等受信任的客户端环境中使用。使用 MasterKey 进行的访问拥有最高权限，不受任何 ACL 限制。

https://vercel.com/luckywind/my-waline/2YZ9Ak99H2zpwvQSfDukNqATzq6F

domain地址：

DOMAINS：[my-waline-lyart.vercel.app](https://my-waline-lyart.vercel.app/)



服务端地址： https://my-waline-9yd7e2x5u-luckywind.vercel.app/

评论管理：https://my-waline-9yd7e2x5u-luckywind.vercel.app/ui







next主题配置

[参考](https://qianfanguojin.top/2022/01/20/Hexo%E5%8D%9A%E5%AE%A2%E8%BF%9B%E9%98%B6%EF%BC%9A%E4%B8%BA-Next-%E4%B8%BB%E9%A2%98%E6%B7%BB%E5%8A%A0-Waline-%E8%AF%84%E8%AE%BA%E7%B3%BB%E7%BB%9F/)

```
npm install @waline/hexo-next
```

配置好next主题配置文件后，遇到问题

```java
Unhandled rejection Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/archive.swig)
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/archive.swig) [Line 30, Column 53]
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/archive.swig) [Line 36, Column 23]
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/archive.swig) [Line 54, Column 17]
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/archive.swig)
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/_partials/head/head-unique.swig) [Line 10, Column 23]
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/archive.swig) [Line 4, Column 22]
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/archive.swig)
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/_partials/header/index.swig) [Line 6, Column 15]
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/archive.swig)
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/_partials/header/sub-menu.swig) [Line 2, Column 29]
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/archive.swig)
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/_partials/header/sub-menu.swig)
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/archive.swig) [Line 6, Column 3]
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/archive.swig) [Line 33, Column 29]
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/archive.swig)
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/_partials/pagination.swig)
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/archive.swig)
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/_partials/comments.swig)
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/archive.swig)
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/_partials/languages.swig)
  Template render error: (/Users/chengxingfu/code/my/myBlog/blog/themes/next/layout/archive.swig)

```

[这里](https://finisky.github.io/hexowaline/)说是next版本更新到v8.5.0

没办法，那就升级next主题到v8.5.0，注意是另一个仓库！[参考这个指导进行升级](https://theme-next.js.org/docs/getting-started/upgrade.html)



如需取消某个 页面/文章 的评论，在 md 文件的 [front-matter ](https://hexo.io/docs/front-matter.html)中增加 `comments: false`  例如

```markdown
title: All tags
date: 2015-12-16 17:05:24
type: "tags"
comments: false
---
```

## 问题记录

评论功能提示：

```java
The app is archived, please restore in console before use. [400 GET https://kuj1mtnf.api.lncldglobal.com/1.1/classes/Comment]
```

解决办法：

登录LeanCloud: https://console.leancloud.app/apps, 应用列表里点击激活等一会就行。

# 添加分类、标签

[参考](https://blog.51cto.com/u_15065852/4264634)

```shell
---
title: Hello World # 标题
date: 2019/3/26 hh:mm:ss # 时间
categories: # 分类
  - 分类  # 只能由一个
tags: # 标签
  - PS3  # 能有多个
  - Games  # 一个标签一行，也可以用列表[标签1,标签2]
---
```

# 部署到git page

安装

```shell
npm install hexo-deployer-git --save
```

[参考](https://mizeri.github.io/2021/04/18/Hexo-blog-deploy/)

# 自定义首页

```
title: 让首页显示部分内容
date: 2020-02-23 22:55:10
description: 这是显示在首页的概述，正文内容均会被隐藏。
```

## 头像

```
avatar:
  # Replace the default image and set the url here.
  url: /images/weixingongzhonghaologo.png
```

# 配置文章模板

[参考](https://shmilybaozi.github.io/2018/11/05/hexo%E6%96%87%E7%AB%A0%E6%A8%A1%E6%9D%BF%E8%AE%BE%E7%BD%AE/)

