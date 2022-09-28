



### 时间含义及计算公式

![在这里插入图片描述](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image4ba2b111182042988f8a60218afa0cda.jpg)

如图所示，时间轴上面的表示Driver 记录到的各个时间，时间轴下面的表示Executor记录到的各个时间。
我们反过来，先说 Executor 记录的各个时间，再说Driver记录的各个时间。

#### Executor 中Task运行时间
Task 在Executor端运行，有三个时间段，分别是 

1. deserializeTime  task反序列化时间，
2.  executorRunTime task执行时间，
3.  serializeTime  结果序列化时间。
   （很奇怪，Task 并没有选择和Driver端一样的方式，直接计算各个阶段的起止时间，而是选择将各个阶段的运行耗时计算好，再通过 metrics 返回给Driver



#### Driver中Task运行时间

1. **LaunchTime**: 在 TaskSetManager 中，也叫 resourceOfferTime ，即 taskSet.offerTask() 成功后，开始运行 Task 的时间
2. LaunchDelay: 表示 从Stage被提交到当前Task被调度的时间，计算公式: taskInfo.launchTime - stage.submissionTime
3. finishTime: Driver最后标识该Task生命周期结束的时间, 具体是从 handleSuccessTask(taskSet, tid, serializedData) 进入 markFinished() 的时间
4. gettingResultTime: 如果我们的 Task 返回的结果比较大，Task结果返回的方式是 IndirectTaskResult，Driver 会在记录开始调用 fetchIndirectTaskResult() 进行读取结果的时间。（实际上应该还包括对读取的数据的deserialize 时间）。如果是 DirectTaskResult ，该时间为 0。 因为代码风格问题，在偏前段的代码中，改名叫 resultFetchStart， gettingResultTime 被重新定义为函数 = {launchTime + duration - fetchStart }
5. duration: = finishTime - launchTime
6. schedulerDelay

```scala
gettingResultTime = launchTime + duration - fetchStart
schedulerDelay =  math.max(0, 
	duration - runTime - deserializeTime - serializeTime - gettingResultTime
	)
就是紫色部分的时间
```





### 参考

[各种时间含义](https://blog.csdn.net/wankunde/article/details/121403842)