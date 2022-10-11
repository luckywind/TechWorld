[参考](https://www.computerworld.com/article/2833435/how-to-interpret-cpu-load-on-linux.html)

uptime

![2_15.png](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/2_15-100522188-orig.png)

小数代表过去1分钟、5分钟、15分钟cpu平均负载；可以分析高负载是暂时的，还是长期的。但什么是高负载呢？数字代表请求cpu资源的任务量，1.0代表一个100%的cpu核的资源；超过1.0的部分代表等待cpu处理的进程量。

需要说明的是，这些数字和cpu核数相关，如果有4个cpu，4.0才意味着所有核完全被占用。通常70%是比较健康的状态，即每个cpu核负载0.7。

