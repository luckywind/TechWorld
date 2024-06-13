# yarn-client模式

Application Master进程只负责向YARN申请资源。

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/1*Nbm9q1VB8eRF1z7dSp-obA.png" alt="img" style="zoom:50%;" />

 

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/1250469-20180204232052545-738862055.png)

## 执行流程

1. 客户端提交一个Application，在客户端启动一个**Driver进程。**
2. **Driver进程会向**RS(ResourceManager)发送请求，启动AM(ApplicationMaster)的资源。
3. RS收到请求，随机选择一台NM(NodeManager)启动AM。这里的NM相当于Standalone中的Worker节点
4. AM启动后，会向RS请求一批container资源，用于启动Executor.
5. RS会找到一批NM返回给AM,用于启动Executor。
6. AM会向NM发送命令启动Executor。
7. Executor**启动后，会反向注册给Driver**，Driver发送task到Executor,执行情况和结果返回给Driver端。

## **总结**

​    1、Yarn-client模式同样是适用于**测试**，**因为Driver运行在本地**，Driver会与yarn集群中的Executor**进行大量的通信**，会造成客户机网卡流量的大量增加

​    2、 **ApplicationMaster的作用：**

​         为当前的Application申请资源

​         给NodeManager发送消息启动Executor。

​    注意：**ApplicationMaster**有launchExecutor和**申请资源**的功能，**并没有作业调度的功能。**

# yarn-cluster模式

更适合生产模式，Driver运行在Application Master进程中，该进程同时负责驱动应用程序，以及向YARN申请资源。

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/1*bnW_o3Iz6dV2qR4G198cgQ.png" alt="img" style="zoom:50%;" />

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/1250469-20180205000026123-370362254.png)

- **执行流程**

1. 客户机提交Application应用程序，**发送请求到RS(ResourceManager),请求启动AM(ApplicationMaster)**。
2. RS收到请求后随机在一台NM(NodeManager)上启动**AM（相当于Driver端）。**
3. AM启动，**AM发送请求到RS，**请求一批container用于启动Executor。
4. RS返回一批NM节点给AM。
5. **AM连接到NM,发送请求到NM启动Executor**。
6. **Executor反向注册到AM所在的节点的Driver。Driver发送task到Executor。**

- **总结**

​    1、Yarn-Cluster主要用于生产环境中，因为Driver运行在Yarn集群中某一台nodeManager中，每次提交任务的Driver所在的机器都是随机的**，**不会产生某一台机器网卡流量激增的现象**，**缺点是任务提交后不能看到日志。只能通过yarn查看日志。

​     **2.ApplicationMaster的作用：**

- 为当前的Application申请资源

-  给nodemanager发送消息 启动Excutor。

- **任务调度。(这里和client模式的区别是AM具有调度能力，因为其就是Driver端，包含Driver进程)**

 