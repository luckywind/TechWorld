# 自定义代码模版

1. 新建一个custom模版组，并在该模版组里新建模版
2. 选择上下文， 这里选择Java，这样只在Java代码里有效
3. 贴入模版代码，类名可以起一个变量，`$className$`
4. 编辑变量，Expression选择className()，会自动给你替换类名
5. 勾选静态导入

![image-20200802225850777](https://tva1.sinaimg.cn/large/007S8ZIlly1ghcv1difmej315v0u07ri.jpg)

## logger

```java
private static final Logger logger = LoggerFactory.getLogger($className$.class);
```

# 从命令行打开项目

idea每次打开项目要用鼠标点击好几次，甚是麻烦。于是google到了如下方法：

Tools->Create Command-line Launcher

![image-20200727195159666](https://gitee.com/luckywind/PigGo/raw/master/image/image-20200727195159666.png)

然后cd 到项目根目录,一个命令打开项目！简直爽爆！

```shell
idea .
```

