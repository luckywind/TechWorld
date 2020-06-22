# ThreadPoolTaskExecutor类

1. 它是一个允许通过bean style("corePoolSize", "maxPoolSize", "keepAliveSeconds", "queueCapacity" 属性)配置ThreadPoolExecutor的JavaBean。且会把ThreadPoolExecutor导出为一个Spring TaskExecutor.
2. 默认配置是1个核心线程，不限制线程池大小和队列大小，等价于 `Executors.newSingleThreadExecutor()`
3. 这个类实现了Spring 的TaskExecutor接口和Executor接口