![image-20231227102054033](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231227102054033.png)

左上：iowait高的部分在等待数据，iowait降下来，cpu利用率马上上去开始计算

左下：

网络带宽只有1000M，导致CPU iowait很高，造成CPU的浪费

红色部分，本地回环(进程间通信，进程1的用户态->内核态->进程2的用户态)