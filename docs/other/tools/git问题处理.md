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

