建议研读[《JVM内存管理和垃圾回收》](https://mp.weixin.qq.com/s?__biz=MzI0Mjc0MDU2NQ==&mid=2247484038&idx=1&sn=bbb34a500613ae1416c0828a8ac799fd&chksm=e976febcde0177aa9cf4723306de80a49166e262c4e6cc806ccd6f5384d58c0a7dde2b77185b&scene=21#wechat_redirect)、[《JVM垃圾回收器、内存分配与回收策略》](https://mp.weixin.qq.com/s?__biz=MzI0Mjc0MDU2NQ==&mid=2247484040&idx=1&sn=5b45eac62e99bd134110afc66c86ba60&chksm=e976feb2de0177a42b820b1e25116c0855622329d1b63f01b12b9c5b89e763badb95a5a7da2b&scene=21#wechat_redirect)、[《内存泄漏、内存溢出和堆外内存，JVM优化配置参数》](https://mp.weixin.qq.com/s?__biz=MzI0Mjc0MDU2NQ==&mid=2247484099&idx=1&sn=5755c366d08e82886bf0c6af9d6cf6cb&chksm=e976fef9de0177ef1e6d4dad6aa0ab6363e54b316c64f51aef3ac1ba4a5c6d4336d8c2b6ddc4&scene=21#wechat_redirect)。

# [metaspace in java8](http://java-latte.blogspot.com/2014/03/metaspace-in-java-8.html)

[参考](http://xmlandmore.blogspot.com/2014/08/hotspot-understanding-metaspace-in-jdk-8.html)

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/jvm_metapsace.png)

metaSpace内存分配模型：

1. 描述类metadata的类被移除
2. 分配多映射虚拟内存空间
3. 空虚拟内存空间被回收
4. 最小化碎片策略

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/metaspace_allocation_java_latte.png)

> 虚拟内存空间被多个ClassLoader分配

## 调优

 -XX:MaxMetaspaceSize  元空间最大值，默认无限大，需要做一下限制

-XX:MetaspaceSize： 初始大小，

-XX:CompressedClassSpaceSize=1G.