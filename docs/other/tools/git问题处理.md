# 问题处理

## 文件过大

```shell
git filter-branch --tree-filter 'rm -rf path/to/your/file' HEAD
git push
```

