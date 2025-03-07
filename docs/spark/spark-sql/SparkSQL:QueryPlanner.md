把逻辑计划转为物理计划的抽象类，子类负责指定GenericStrategy列表，每个都返回一系列可选的物理计划。如果一个strategy不能plan计划树中的所有算子，则可以调用GenericStrategy.planLater并得到一个placeHolder对象，将会由其他的strategy收集并填充。

![image-20230605090358124](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230605090358124.png)