​    流处理系统需要能优雅地处理反压（backpressure）问题。反压通常产生于这样的场景：短时负载高峰导致系统接收数据的速率远高于它处理数据的速率。许多日常问题都会导致反压，例如，垃圾回收停顿可能会导致流入的数据快速堆积，或者遇到大促或秒杀活动导致流量陡增。反压如果不能得到正确的处理，可能会导致资源耗尽甚至系统崩溃。

[参考](http://wuchong.me/blog/2016/04/26/flink-internals-how-to-handle-backpressure/)

# 结论

​       Flink 不需要一种特殊的机制来处理反压，因为 Flink 中的数据传输相当于已经提供了应对反压的机制。因此，Flink 所能获得的最大吞吐量由其 pipeline 中最慢的组件决定。相对于 Storm/JStorm 的实现，Flink 的实现更为简洁优雅，源码中也看不见与反压相关的代码，无需 Zookeeper/TopologyMaster 的参与也降低了系统的负载，也利于对反压更迅速的响应。

<font color=red>这种固定大小缓冲池就像阻塞队列一样，保证了 Flink 有一套健壮的反压机制，使得 Task 生产数据的速度不会快于消费的速度</font>

# 网络传输中的内存管理

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/TB14fLsHVXXXXXWXFXXXXXXXXXX.png)

网络上传输的数据会写到Task的InputGage中，经过Task的处理后，再由Task写到ResultPartition中。每个Task都包括了输入和输出，数据都存在Buffer中。 

1. 每个TaskManager实例化一个NetworkBufferPool，它维护一定数量的内存块，代表网络传输中所有可用的内存，Task之间共享。
2. Task启动时，会为输入(IG)和输出(RP)分别创建一个LocalBufferPool并设置可申请的内存块的数量， 但这个数量会随着缓冲池的创建和销毁动态调整，以避免频繁地进入反压状态
3. <font color=red>在 Task 线程执行过程中，**当 Netty 接收端收到数据时，为了将 Netty 中的数据拷贝到 Task 中**，InputChannel（实际是 RemoteInputChannel）会向其对应的缓冲池申请内存块（上图中的①）。如果缓冲池中也没有可用的内存块且已申请的数量还没到池子上限，则会向 NetworkBufferPool 申请内存块（上图中的②）并交给 InputChannel 填上数据（上图中的③和④）。如果缓冲池已申请的数量达到上限了呢？或者 NetworkBufferPool 也没有可用内存块了呢？这时候，**Task 的 Netty Channel 会暂停读取，上游的发送端会立即响应停止发送，拓扑会进入反压状态。当 Task 线程写数据到 ResultPartition 时，也会向缓冲池请求内存块，如果没有可用内存块时，会阻塞在请求内存块的地方，达到暂停写入的目的。**</font>

   > Task的输入缓存无法申请到足够的内存时，上游发送端会立即响应停止发送，拓扑进入反压状态
   >
   > Task的输出缓存无法申请到足够的内存时，会阻塞在请求内存块的地方，达到暂停写入的目的。
4. 当一个内存块被消费完成之后（在输入端是指内存块中的字节被反序列化成对象了，在输出端是指内存块中的字节写入到 Netty Channel 了），会调用 `Buffer.recycle()` 方法，会将内存块还给 LocalBufferPool （上图中的⑤）。如果LocalBufferPool中当前申请的数量超过了池子容量（由于上文提到的动态容量，由于新注册的 Task 导致该池子容量变小），则LocalBufferPool会将该内存块回收给 NetworkBufferPool（上图中的⑥）。如果没超过池子容量，则会继续留在池子中，减少反复申请的开销。

# 反压的过程

下面这张图简单展示了两个 Task 之间的数据传输以及 Flink 如何感知到反压的：

[![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/TB1rCIvJpXXXXcKXXXXXXXXXXXX.png)](http://img3.tbcdn.cn/5476e8b07b923/TB1rCIvJpXXXXcKXXXXXXXXXXXX)

1. 记录“A”进入了 Flink 并且被 Task 1 处理。（这里省略了 Netty 接收、反序列化等过程）
2. 记录被序列化到 buffer 中。
3. 该 buffer 被发送到 Task 2，然后 Task 2 从这个 buffer 中读出记录。

**不要忘了：记录能被 Flink 处理的前提是，必须有空闲可用的 Buffer。**

结合上面两张图看：Task 1 在输出端有一个相关联的 LocalBufferPool（称缓冲池1），Task 2 在输入端也有一个相关联的 LocalBufferPool（称缓冲池2）。如果缓冲池1中有空闲可用的 buffer 来序列化记录 “A”，我们就序列化并发送该 buffer。

- <font color=red>本地传输：如果 Task 1 和 Task 2 运行在同一个 worker 节点（TaskManager），该 buffer 可以直接交给下一个 Task。一旦 Task 2 消费了该 buffer，则该 buffer 会被缓冲池1回收。如果 Task 2 的速度比 1 慢，那么 buffer 回收的速度就会赶不上 Task 1 取 buffer 的速度，导致缓冲池1无可用的 buffer，Task 1 等待在可用的 buffer 上。最终形成 Task 1 的降速。</font>
- 远程传输：如果 Task 1 和 Task 2 运行在不同的 worker 节点上，那么 buffer 会在发送到网络（TCP Channel）后被回收。在接收端，会从 LocalBufferPool 中申请 buffer，然后拷贝网络中的数据到 buffer 中。如果没有可用的 buffer，会停止从 TCP 连接中读取数据。在输出端，通过<font color=red>Netty 的水位值机制</font>来保证不往网络中写入太多数据（后面会说）。如果网络中的数据（Netty输出缓冲中的字节数）超过了高水位值，我们会等到其降到低水位值以下才继续写入数据。这保证了网络中不会有太多的数据。如果接收端停止消费网络中的数据（由于接收端缓冲池没有可用 buffer），网络中的缓冲数据就会堆积，那么发送端也会暂停发送。另外，这会使得发送端的缓冲池得不到回收，writer 阻塞在向 LocalBufferPool 请求 buffer，阻塞了 writer 往 ResultSubPartition 写数据。

  > 输入端，没有足够的buffer时会停止读取数据
  >
  > 输出端，通过Netty的水位值机制保证不往网络中写入太多数据，甚至停止发送。 从而发送端的缓冲池得不到回收，writer阻塞。

<font color=red>这种固定大小缓冲池就像阻塞队列一样，保证了 Flink 有一套健壮的反压机制，使得 Task 生产数据的速度不会快于消费的速度</font>。我们上面描述的这个方案可以从两个 Task 之间的数据传输自然地扩展到更复杂的 pipeline 中，保证反压机制可以扩散到整个 pipeline。

# 反压监控

<font color=red>如果一个 Task 因为反压而降速了，那么它会卡在向 `LocalBufferPool` 申请内存块上，通过不断地采样每个 task 的 stack trace 就可以实现反压监控。</font>

# 反压问题处理方案

## 定位问题

1. checkpoint生成超时

   > barrier流动慢，导致checkpoint生成时间长

2. 观察buffer缓冲区使用情况

3. job的jobGraph中观察背压状态，大量HIGH告警

## 解决办法

1. <font color=red>调整并行度让处理速度慢的task与前面的task形成算子链</font>
   算子链条件：

   1.上下游的并行度一致

   2.下游节点的入度为1 （也就是说下游节点没有来自其他节点的输入）

   3.上下游节点都在同一个 slot group 中（下面会解释 slot group）

   4.下游节点的 chain 策略为 ALWAYS（可以与上下游链接，map、flatmap、filter等默认是ALWAYS）

   5.上游节点的 chain 策略为 ALWAYS 或 HEAD（只能与下游链接，不能与上游链接，Source默认是HEAD）

   6.两个节点间数据分区方式是 forward（参考[理解数据流的分区](https://links.jianshu.com/go?to=http%3A%2F%2Fwuchong.me%2Fblog%2F2016%2F05%2F09%2Fflink-internals-understanding-execution-resources%2F)）

   7.用户没有禁用 chain

<font color=red>算子链的好处： 链化成算子链可以减少线程与线程间的切换和数据缓冲的开销，并在降低延迟的同时提高整体吞吐量。</font>

2. 开启槽(slot)共享

默认情况下，Flink 允许同一个job里的不同的子任务可以共享同一个slot，即使它们是不同任务的子任务但是可以分配到同一个slot上。 

<font color=red>一个 slot 可以保存整个管道pipeline,减少了数据在不同机器不同分区之间的传输损耗</font>