# 创建githubPages

1. 创建一个仓库*username*.github.io。username一定要替换成自己的

2. ```shell
   git clone https://github.com/username/username.github.io
   ```

3. ```shell
   cd username.github.io
   echo "Hello World" > index.md
   git add --all
   git commit -m "Initial commit"
   git push -u origin main
   ```

4. ```shell
   浏览器访问 https://username.github.io.
   ```



是不是很简单？

实际上，我们要新增一篇文章，只需要在docs目录下新增一个md文件，然后在index.md里加入他的链接即可！

当然如果为了更加美化，或者zhuangbi，可以使用jekyll定制网站

这里站点源默认是main分支的根目录，站点源下必须有一个index文件，如果网站不可访问了，可能是切换了站点源，但是没有新建index文件。

## 问题记录

### 不能渲染子目录

[参考](https://stackoverflow.com/questions/38363590/gh-pages-subdirectory-files-arent-shown-up)

让gh-pages不使用Jekyll处理和发布文件，直接拷贝到web服务即可。只需要在根目录创建一个空的 **.nojekyll** 文件

# Jekyll

Jekyll是一个内置支持gitHub Pages的静态网站生成器

## 安装

[参考](https://jekyllrb.com/docs/installation/macos/)

### 安装ruby

```shell
xcode-select --install
# ruby v2.5.0以上
/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
brew install ruby
#添加brew ruby path
echo 'export PATH="/usr/local/opt/ruby/bin:$PATH"' >> ~/.bash_profile

```

### 安装jekyll

```shell
gem install --user-install bundler jekyll
# 注意X.X是ruby的版本号，要替换
echo 'export PATH="$HOME/.gem/ruby/X.X.0/bin:$PATH"' >> ~/.bash_profile
```

确保gem 指向home目录

```shell
gem env
```

## 使用

```shell
jekyll new myblog #在myblog目录创建一个网站
# 构建并启动服务
cd myblog
bundle exec jekyll serve
bundle exec jekyll serve --livereload  #这个参数可以实时更新
```

这样通过浏览器访问http://localhost:4000就行了

## 使用jekyll创建一个githubpage站点

### GitHub创建仓库

创建一个仓库(如果是个人/组织站点，名称必须是<user>.github.io或者<organization>.github.io)

### 创建站点

```shell
git init REPOSITORY-NAME
cd REPOSITORY-NAME
确定站点源，见下一节

```

### 确定站点源

所谓站点源就是站点所用源文件存放的分支和目录，不要把敏感文件放进来。个人和组织的默认站点源是默认分支的根目录，项目站点是gh-pages分支的根目录。

如何配置呢？

1. 在github上进入仓库，点击settings

   ![image-20201018122012266](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20201018122012266.png)

