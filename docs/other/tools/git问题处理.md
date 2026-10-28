# 问题处理

## 文件过大

```shell
git filter-branch --tree-filter 'rm -rf path/to/your/file' HEAD
git push
```

## 下载子目录

例如目录：https://github.com/rieckpil/blog-tutorials/tree/master/custom-maven-archetype

使用svn co, 把tree/master换成trunk

```shell
svn co https://github.com/rieckpil/blog-tutorials/trunk/custom-maven-archetype custom-maven-archetype
```

# 搜索

http://x.com/saccc_c/status/2039280139046154325



## 直接搜索

搜索词前添加awesome 

## 闲逛搜索

左上角菜单选择 Explore 可以看到 github 的推荐和 Trading

## 高级搜索

[github.com/search](http://github.com/search)，  点击 advanced search  

在高级搜索中，你可以限定关键词、作者、stars数等等，非常实用。 
