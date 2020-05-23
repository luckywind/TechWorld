# Invalid bound statement (not found)

[参考](https://www.jianshu.com/p/800fe918cc7a)

出现这个错误时，按以下步骤检查一般就会解决问题：
 1：检查xml文件所在package名称是否和Mapper interface所在的包名一一对应；
 **2：检查xml的namespace是否和xml文件的package名称一一对应；**
 3：检查方法名称是否对应；
 4：去除xml文件中的中文注释；
 5：随意在xml文件中加一个空格或者空行然后保存。

我就是遇到第二种情况了

