[youtube](https://www.youtube.com/watch?v=rpKjcMoega0&t=1309s)

[失败重试机制](https://www.codenong.com/cs106931464/)

1. 普通task失败，会计算失败次数，达到maxTaskFailures将终止stage和job
2. Fetch Failure不会计算失败次数，而是计算stage失败次数，达到maxStageFailures则终止job

stage重试： 节点失败

<img src="https://gitee.com/luckywind/PigGo/raw/master/image/image-20211204155728792.png" alt="image-20211204155728792" style="zoom:50%;" />

<img src="https://gitee.com/luckywind/PigGo/raw/master/image/image-20211204155751613.png" alt="image-20211204155751613" style="zoom:50%;" />

