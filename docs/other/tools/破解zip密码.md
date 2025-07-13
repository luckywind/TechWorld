如果要破解zip密码，可以试试John The Ripper。

GitHub主页是 https://github.com/openwall/john，官网 https://www.openwall.com/john 上有编译好的版本。

下载后命令行输入

```
zip2john XXX.zip >XXX.john
john XXX.john
```

等出现类似 `XXX (文件名)`的字符串时，XXX就是密码。





[参考](https://www.cnblogs.com/feixiablog/p/13712968.html)