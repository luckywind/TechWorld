# 什么是classpath?

## 简介      

 classpath是jvm的一个参数，它指定了用户的类和包的位置，可以理解为一个目录。Java通过classpath查找类库。和类的动态加载行为类似，当执行Java程序时，jvm查找并加载类，classpath负责告诉Java去哪里找定义这些类的文件。

​		jvm按照如下顺序搜索类：

1. Bootstrap类： java平台的关键类
2. Extension类: JRE/JDK扩展路径下的包
3. 自定义包和库

默认只有jdk标准api和扩展包无需设置即可找到，自定义类和库必须配置到命令行中

## 怎么设置classpath

当java运行时使用到一个类名时，它会查找classpath变量中的所有路径，如果找不到就跑异常。设置classpath就和设置path环境变量一样。

