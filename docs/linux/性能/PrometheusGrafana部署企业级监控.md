#  Prometheus+Grafana部署企业级监控

## **（一）监控系统的组成**

### **1、Prometheus**

    Prometheus是一个开放性的监控解决方案，用户可以非常方便的安装和使用Prometheus并且能够非常方便的对其进行扩展。Prometheus作为一个时序数据库，其实它和大家熟知的Mysql是一类的东西，都是存储数据，提供查询的，它存储了计算机系统在各个时间点上的监控数据。而Grafana仪表盘上的数据，就是通过查询Prometheus获取的。Prometheus主要用于对基础设施的监控，包括服务器(CPU、MEM等)、数据库(MYSQL、PostgreSQL等)、Web服务等，几乎所有东西都可以通过Prometheus进行监控。而它的数据，则是通过配置，建立与数据源的联系来获取的。

架构图：

![image-20230817180302290](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230817180302290.png)

Prometheus server - 收集和存储时间序列数据
Client Library: 客户端库，为需要监控的服务生成相应的metrics 并暴露给 - Prometheus server。当 Prometheus server 来 pull 时，直接返回实时状态的 metrics。
pushgateway - 对于短暂运行的任务，负责接收和缓存时间序列数据，同时也是一个数据源
exporter - 各种专用exporter，面向硬件、存储、数据库、HTTP服务等
alertmanager - 处理报警webUI等，其他各种支持的工具，本身的界面值适合用来语句查询，数据可视化，需要第三方组件，比如Grafana。
**（1）来源**
    Prometheus受启发于Google的Brogmon监控系统（相似的Kubernetes是从Google的Brog系统演变而来），从2012年开始由前Google工程师在Soundcloud以开源软件的形式进行研发，并且于2015年早期对外发布早期版本。
[Image: image.png]

#### **（2）优点**

① 易于管理
Prometheus核心部分只有一个单独的二进制文件，不存在任何的第三方依赖(数据库，缓存等等)。唯一需要的就是本地磁盘，因此不会有潜在级联故障的风险。

② 强大的数据模型
所有采集的监控数据均以指标(metric)的形式保存在内置的时间序列数据库当中(TSDB)。所有的样本除了基本的指标名称以外，还包含一组用于描述该样本特征的标签。
如下所示：

```
http_request_status{code='200',content_path='/api/path', environment='produment'} => [value1@timestamp1,value2@timestamp2...]

http_request_status{code='200',content_path='/api/path2', environment='produment'} => [value1@timestamp1,value2@timestamp2...]
```

每一条时间序列由指标名称(Metrics Name)以及一组标签(Labels)唯一标识。每条时间序列按照时间的先后顺序存储一系列的样本值。
表示维度的标签可能来源于你的监控对象的状态，比如code=404或者content_path=/api/path。也可能来源于的你的环境定义，比如environment=produment。基于这些Labels我们可以方便地对监控数据进行聚合，过滤，裁剪。

③ 强大的查询语言PromQL
Prometheus内置了一个强大的数据查询语言PromQL。 通过PromQL可以实现对监控数据的查询、聚合。同时PromQL也被应用于数据可视化(如Grafana)以及告警当中。

④ 高效易于集成
对于单一Prometheus Server实例而言它可以处理：

* 数以百万的监控指标
* 每秒处理数十万的数据点。
* 使用Prometheus可以快速搭建监控服务，并且可以非常方便地在应用程序中进行集成。目前支持： Java， JMX， Python， Go，Ruby， .Net， Node.js等等语言的客户端SDK，基于这些SDK可以快速让应用程序纳入到Prometheus的监控当中，或者开发自己的监控数据收集程序

### **2、Grafana**

是一个监控仪表系统，由Grafana Labs公司开源的的一个系统监测 (System Monitoring) 工具。帮助用户简化监控的复杂度，用户只需要提供需要监控的数据，它就可以生成各种可视化仪表。同时它还支持报警功能，可以在系统出现问题时通知用户。并且Grafana不仅仅只支持Prometheus作为查询的数据库，它还支持如下：

* Prometheus 
* Graphite
* OpenTSDB
* InfluxDB
* MySQL/PostgreSQL
* Microsoft SQL Serve
* 等等

其实 Prometheus 开发了一套仪表盘系统 [PromDash](https://github.com/prometheus-junkyard/promdash)，不过很快这套系统就被废弃了，官方开始推荐使用 Grafana 来对 Prometheus 的指标数据进行可视化，这不仅是因为 Grafana 的功能非常强大，而且它和 Prometheus 可以完美的无缝融合。
安装 Grafana，可以使用最简单的 [Docker 安装方式](http://docs.grafana.org/installation/docker/)：

```
$ docker run -d -p 3000:3000 grafana/grafana
```

运行上面的 docker 命令，Grafana 就安装好了！你也可以采用其他的安装方式，参考 [官方的安装文档](http://docs.grafana.org/)。

### **3、数据源**


    在Prometheus的架构设计中，Prometheus并不直接服务监控特定的目标，就比如我们监控linux系统，Prometheus不会自己亲自去监控linux的各项指标。其主要任务负责数据的收集，存储并且对外提供数据查询支持。一般是一个Exporter服务提供的。
[Image: image.png]
## **（二）Prometheus重要组成部分**

### **1、Exporter**


    Exporter是一个相对开放的概念，不是专门指某一个程序。它可以是一个独立运行的程序，独立于监控目标以外(如Node Exporter程序，独立于操作系统，却能获取到系统各类指标)。也可以是直接内置在监控目标中的代码(如在项目代码层面接入普罗米修斯API，实现指标上报)。总结下来就是，只要能够向Prometheus提供标准格式的监控样本数据，那就是一个Exporter。Prometheus周期性的从Exporter暴露的HTTP服务地址(通常是/metrics)拉取监控样本数据。
一般来说可以将Exporter分为2类：

* 直接采集：这一类Exporter直接内置了对Prometheus监控的支持，比如cAdvisor，Kubernetes，Etcd，Gokit等，都直接内置了用于向Prometheus暴露监控数据的端点。
* 间接采集：间接采集，原有监控目标并不直接支持Prometheus，因此我们需要通过Prometheus提供的Client Library编写该监控目标的监控采集程序。例如： Mysql Exporter，JMX Exporter，Consul Exporter等。

    后面如果要在我们的C++项目里采集业务数据的话就得去利用间接采集的方法获取数据，可以利用第三方库****[**prometheus-cpp**](https://github.com/jupp0r/prometheus-cpp)****

### **2、AlertManager**

     在Prometheus Server中支持基于PromQL创建告警规则，如果满足PromQL定义的规则，则会产生一条告警，而告警的后续处理流程则由AlertManager进行管理。在AlertManager中我们可以与邮件，Slack等等内置的通知方式进行集成，也可以通过Webhook自定义告警处理方式。AlertManager即Prometheus体系中的告警处理中心。

### **3、PromQL**

    Prometheus通过PromQL用户可以非常方便地对监控样本数据进行统计分析，PromQL支持常见的运算操作符，同时PromQL中还提供了大量的内置函数可以实现对数据的高级处理。PromQL作为Prometheus的核心能力除了实现数据的对外查询和展现，同时告警监控也是依赖PromQL实现的。

#### **（1）基本用法**

##### **①查询时间序列**

当 Prometheus 通过 Exporter 采集到相应的监控指标样本数据后，我们就可以通过PromQL 对监控样本数据进行查询。
当我们直接使用监控指标名称查询时，可以查询该指标下的所有时间序列。如：

```
prometheus_http_requests_total                  
等同于： 
prometheus_http_requests_total{}     
```

该表达式会返回指标名称为 prometheus_http_requests_total 的所有时间序列
PromQL 还支持用户根据时间序列的标签匹配模式来对时间序列进行过滤，目前主要支持两种匹配模式：完全匹配和正则匹配。
➢ PromQL 支持使用 = 和 != 两种完全匹配模式：
⚫ 通过使用 label=value 可以选择那些标签满足表达式定义的时间序列；
⚫ 反之使用 label!=value 则可以根据标签匹配排除时间序列；
➢ PromQL 还可以支持使用正则表达式作为匹配条件，多个表达式之间使用 | 进行分离：
⚫ 使用 label=~regx 表示选择那些标签符合正则表达式定义的时间序列；
⚫ 反之使用 label!~regx 进行排除；
例如，如果想查询多个环节下的时间序列序列可以使用如下表达式：

```
prometheus_http_requests_total{environment=~"staging|testing|development",method!="GET
"}
排除用法
prometheus_http_requests_total{environment!~"staging|testing|development",method!="GET
"}
```

##### **②范围查询**

直接通过类似于 PromQL 表达式 httprequeststotal 查询时间序列时，返回值中只会包含该时间序列中的最新的一个样本值，这样的返回结果我们称之为瞬时向量。而相应的这样的表达式称之为 瞬时向量表达式。
而如果我们想过去一段时间范围内的样本数据时，我们则需要使用区间向量表达式。区间向量表达式和瞬时向量表达式之间的差异在于在区间向量表达式中我们需要定义时间选择的范围，时间范围通过时间范围选择器 [] 进行定义。 例如，通过以下表达式可以选择
最近 5 分钟内的所有样本数据：
prometheus_http_requests_total{}[5m]
该表达式将会返回查询到的时间序列中最近 5 分钟的所有样本数据
通过区间向量表达式查询到的结果我们称为区间向量。 除了使用 m 表示分钟以外，
PromQL 的时间范围选择器支持其它时间单位：

```
s - 秒
m - 分钟
h - 小时
d - 天
w - 周
y - 年
```

##### **③时间位移操作**

在瞬时向量表达式或者区间向量表达式中，都是以当前时间为基准：

```
prometheus_http_requests_total{} # 瞬时向量表达式，选择当前最新的数据 
prometheus_http_requests_total{}[5m] # 区间向量表达式，选择以当前时间为基准，5 分钟内的数据
```

而如果我们想查询，5 分钟前的瞬时样本数据，或昨天一天的区间内的样本数据呢? 这个时候我们就可以使用位移操作，位移操作的关键字为 ****offset****。 可以使用 offset 时间位移操作：

```
prometheus_http_requests_total{} offset 5m
prometheus_http_requests_total{}[1d] offset 1d
```

##### **④使用聚合操作**

一般来说，如果描述样本特征的标签(label)在并非唯一的情况下，通过 PromQL 查询数据，会返回多条满足这些特征维度的时间序列。而 PromQL 提供的聚合操作可以用来对这些时间序列进行处理，形成一条新的时间序列：

```
# 查询系统所有 http 请求的总量
sum(prometheus_http_requests_total)
# 按照 mode 计算主机 CPU 的平均使用时间
avg(node_cpu_seconds_total) by (mode)
# 按照主机查询各个主机的 CPU 使用率
sum(sum(irate(node_cpu_seconds_total{mode!='idle'}[5m]))     /     sum(irate(node_cpu_seconds_total [5m]))) by (instance)
```

##### **⑤标量和字符串**

除了使用瞬时向量表达式和区间向量表达式以外，PromQL 还直接支持用户使用标量(Scalar)和字符串(String)。
➢ 标量（Scalar）：一个浮点型的数字值
标量只有一个数字，没有时序。 例如：10
需要注意的是，当使用表达式 count(prometheus_http_requests_total)，返回的数据类型，依然是瞬时向量。用户可以通过内置函数scalar()将单个瞬时向量转换为标量。
➢ 字符串（String）：一个简单的字符串值
直接使用字符串，作为PromQL 表达式，则会直接返回字符串。

```
"this is a string"
'these are unescaped: \n \\ \t'
`these are not unescaped: \n ' " \t
```

#### **（2）PromQL 操作符**

使用PromQL 除了能够方便的按照查询和过滤时间序列以外，PromQL 还支持丰富的操作符，用户可以使用这些操作符对进一步的对事件序列进行二次加工。这些操作符包括：数学运算符，逻辑运算符，布尔运算符等等。

##### **①数学运算**

PromQL 支持的所有数学运算符如下所示：

```
+ (加法)
- (减法)
* (乘法)
/ (除法)
% (求余)
^ (幂运算)
```

##### **②布尔运算**

 Prometheus 支持以下布尔运算符如下：

```
== (相等)
!= (不相等)
>(大于)
< (小于)
>= (大于等于)
<= (小于等于)
```

#### **（3）集合运算符**

使用瞬时向量表达式能够获取到一个包含多个时间序列的集合，我们称为瞬时向量。通过集合运算，可以在两个瞬时向量与瞬时向量之间进行相应的集合操作。
目前，Prometheus 支持以下集合运算符：

```
and (并且)
or (或者)
unless (排除)
```

vector1 and vector2 会产生一个由 vector1 的元素组成的新的向量。该向量包含vector1 中完全匹配 vector2 中的元素组成。
vector1 or vector2 会产生一个新的向量，该向量包含 vector1 中所有的样本数据，以及 vector2 中没有与 vector1 匹配到的样本数据。
vector1 unless vector2 会产生一个新的向量，新向量中的元素由 vector1 中没有与vector2 匹配的元素组成。

#### **（4）操作符优先级**

对于复杂类型的表达式，需要了解运算操作的运行优先级。例如，查询主机的 CPU 使用率，可以使用表达式：

```
100 * (1 - avg (irate(node_cpu_seconds_total{mode='idle'}[5m])) by(job) ) 
```

其中irate 是PromQL中的内置函数，用于计算区间向量中时间序列每秒的即时增长率。在 PromQL 操作符中优先级由高到低依次为：

```
^
*, /, %
+, -
==, !=, <=, =, >
and, unless
or
```

#### **（5）PromQL 聚合操作**

Prometheus 还提供了下列内置的聚合操作符，这些操作符作用域瞬时向量。可以将瞬时表达式返回的样本数据进行 聚合，形成一个新的时间序列

```
sum (求和)
min (最小值)
max (最大值)
avg (平均值)

stddev (标准差)
stdvar (标准差异)
count (计数)
count_values (对 value 进行计数)
bottomk (后 n 条时序)
topk (前n 条时序)
quantile (分布统计)
```

使用聚合操作的语法如下：

```
<aggr-op>([parameter,] <vector expression>) [without|by (<label list>)] 
```

其中只有 count_values , quantile , topk , bottomk 支持参数(parameter)。
without 用于从计算结果中移除列举的标签，而保留其它标签。by 则正好相反，结果向量中只保留列出的标签，其余标签则移除。通过 without 和 by 可以按照样本的问题对数据进行聚合。

## **（三）样本数据**

    一般来说访问Exporter暴露的HTTP服务，就能获取到了一系列的监控指标。而这些监控指标便是Prometheus可以采集到当前主机所有监控指标的样本数据。
这是我部署的node Exporter服务的样本数据
[Image: image.png]****一条样本数据 = 样本名称（指标）+标签（键值对中的键）+对应的值（右大括号后的值则是该监控样本监控下的具体值）****

#### **1、样本(sample)**

    Prometheus会将所有采集到的样本数据以****时间序列(time-series)****的方式保存在内存数据库中，并且定时保存到硬盘上。时间序列保存方式是指按照时间戳和值的序列顺序存放，也称之为向量(vector)。 每条时间序列通过指标名称(metrics name)和一组标签集(labelset)命名。如下图所示，可以将向量理解为一个以时间为X轴，值为Y轴的数字矩阵：
[Image: image.png]在时间序列中的每一个点(即图上的小黑点)称为一个****样本(sample)****，样本由以下三部分组成：

* 指标(metric)：metric name和描述当前样本特征的labelsets，也就是图中的`A{a="x",b="y"}`；
* 时间戳(timestamp)：一个精确到毫秒的时间戳，也就是小黑点对应的x轴的值；
* 样本值(value)： 一个float64的浮点型数据表示当前样本的值，即小黑点对应的y轴的值；

即样本可表示为：

```
A{a="x",b="y"}@1434417560938 => 94355
```

其中1434417560938是时间戳，94355是值。

#### **2、指标（Metric）**

在形式上，所有的指标(Metric)都通过如下格式标示：

```
<metric name>{<label name>=<label value>, ...}
```

指标的名称(metric name)可以反映被监控样本的含义(比如，http**request_total - 表示当前系统接收到的HTTP请求总量)。指标名称只能由ASCII字符、数字、下划线以及冒号组成并必须符合正则表达式[a-zA-Z**:]a-zA-Z0-9_:*。

#### **3.标签(label)**

标签反映了当前样本的特征维度，通过这些维度Prometheus可以对样本数据进行过滤，聚合等。标签的名称只能由ASCII字符、数字以及下划线组成并满足正则表达式a-zA-Z_*。
其中以****_作为前缀****的标签，是系统保留的关键字，只能在系统内部使用。标签的值则可以包含任何Unicode编码的字符。

## **（四）实操**

### **1、使用官方提供的exporter及模板库**

监控Linux服务器各项指标和mysql服务各项指标
（1）下载并安装exporter
https://prometheus.io/download/
（2）修改Prometheus配置
[Image: image.png]
（3）重启Prometheus
（4）打开Prometheus面板，发现已经出现了名为linux的target
[Image: image.png]（5）通过http服务获取到样本数据
http:://ip:port/metrics

[Image: image.png]
（6）创建一个datasource

[Image: image.png]
（7）导入官方提供的Dashboard
https://grafana.com/grafana/dashboards/
再添加本次要使用的模板
选择数据源，导入该模板就可以了。
[Image: image.png]

### **2、自定义**

Prometheus 提供了 [官方版 Golang 库](https://github.com/prometheus/client_golang) 用于采集并暴露监控数据，下面让我们来快速的来使用官方库来采集程序内相关的数据，以及其它一些基本简单的示例，并使用 Prometheus 监控服务来采集指标展示数据。
这里有一些官方的客户端库和一些非官方的客户端库，囊括了大部分语言
https://prometheus.io/docs/instrumenting/clientlibs/?spm=a2c4g.11186623.0.0.7efb574dnsOMA2

#### **（1）Prometheus 指标的四种类型**

##### ① Counter（计数器)

counter metric 是一个只能递增的value
看一下它的接口

```
type Counter interface {
    Metric
    Collector

    // Inc increments the counter by 1. Use Add to increment it by arbitrary
    // non-negative values.
    Inc()
    // Add adds the given value to the counter. It panics if the value is <
    // 0.
    Add(float64)
}
```

Inc方法，默认+1；Add方法，可以增加自定义的数量。
适用场景：

* 记录不同的API的请求数量
* 记录业务里某个错误码触发数量
* 记录一段时间内风控规则的使用数量

##### ② Gauge（仪表盘）

是一个可增可减的指标
接口：

```
type Gauge interface {
    Metric
    Collector

    // Set sets the Gauge to an arbitrary value.
    Set(float64)
    // Inc increments the Gauge by 1. Use Add to increment it by arbitrary
    // values.
    Inc()
    // Dec decrements the Gauge by 1. Use Sub to decrement it by arbitrary
    // values.
    Dec()
    // Add adds the given value to the Gauge. (The value can be negative,
    // resulting in a decrease of the Gauge.)
    Add(float64)
    // Sub subtracts the given value from the Gauge. (The value can be
    // negative, resulting in an increase of the Gauge.)
    Sub(float64)

    // SetToCurrentTime sets the Gauge to the current Unix time in seconds.
    SetToCurrentTime()
}
```

适用场景：

* 记录服务的内存的占用
* 记录服务CPU的占用
* 记录队列的长度
* 某时刻协议来的数量等

##### ③Histograms（直方图、柱状图）

histograms是一个 直方图度量类型，用于测量落在定义的桶中的数据的值。比如用在测量我们的请求大部分的延迟是落在哪一个区间。这个时候Prometheus不会存储每一次请求所消耗的时间，而是会将每一个请求按照消耗时间看是分配到哪一个bucket。默认的buckets有：****.005, .01, .025, .05, .075, .1, .25, .5, .75, 1, 2.5, 5, 7.5, 10.**** 一般来说很少有超过10秒的请求了。当然如果我们有需要，也是可以定制化。
接口：

```
type Histogram interface {
    Metric
    Collector

    // Observe adds a single observation to the histogram. Observations are
    // usually positive or zero. Negative observations are accepted but
    // prevent current versions of Prometheus from properly detecting
    // counter resets in the sum of observations. (The experimental Native
    // Histograms handle negative observations properly.) See
    // https://prometheus.io/docs/practices/histograms/#count-and-sum-of-observations
    // for details.
    Observe(float64)
}
```

使用场景：

* 比如我们的API服务的请求耗时，在所有的bucket分布情况。
* 比如我们消费者处理某个事件的耗时，在所有的bucket分布情况。

##### ④ Summaries（）

****summaries和histograms有很多相似的地方。而不同的地方有以下几点：****

* Histograms 是基于桶来统计数据的，而 Summaries 是基于分位数来统计数据的。
* histograms 分位数的计算是在Prometheus上面，而summaries是在APP服务上就进行了计算。因此summaries也没办法针对多个应用进行聚合。
* summaries适用于需要计算准确的分位数，但不能确定值的范围是什么。

```
// To create Summary instances, use NewSummary.
type Summary interface {
    Metric
    Collector

    // Observe adds a single observation to the summary. Observations are
    // usually positive or zero. Negative observations are accepted but
    // prevent current versions of Prometheus from properly detecting
    // counter resets in the sum of observations. See
    // https://prometheus.io/docs/practices/histograms/#count-and-sum-of-observations
    // for details.
    Observe(float64)
}
```

比如我们的API服务的请求耗时，大部分是落在哪个区间。

#### **（2）cgo**

Prometheus是go语言写的，官方库也是go、java、python等，没有C和C++官方库，而非官方库prometheus-cpp的使用看起来较为繁琐，不太友好，所以我就直接使用go语言写代码，包装接口，再利用cgo机制，将接口让C程序或者C++程序调用
Go语言通过自带的一个叫CGO的工具来支持C语言函数调用，同时我们可以用Go语言导出C动态库接口给其他语言使用。


## **（五）利用Granfana搭建告警系统**

我利用qq邮箱向开发人员发送告警邮件
（1）需要开启pop3协议 密码为验证中后的授权码，qq邮箱客户端→设置→账户
[Image: image.png]收费的，一条一毛钱，运营商代扣
（2）开启smtp配置
[Image: image.png]（3）在Granfana里添加一个告警通道

[Image: image.png]（4）配置一下要接收这个告警邮件的账户
[Image: image.png]（5）测试一下，发送成功了
[Image: image.png][Image: image.png]（6）设置一个告警规则，到达警告条件以后发送告警邮件
[Image: image.png][Image: image.png]

**至此，我们获得了一套可视化监控告警系统！！！**


## ****参考资料：****

prometheus参考资料：
https://yunlzheng.gitbook.io/prometheus-book/
https://zhuanlan.zhihu.com/p/434353542
prometheus git仓库：https://github.com/prometheus/prometheus
搭建环境：https://blog.csdn.net/qq_31725371/article/details/114697770
自定义绘图：https://blog.csdn.net/admin321123/article/details/127590704
官方提供的仪表盘盘：https://grafana.com/grafana/dashboards/?dataSource=influxdb
cgo：http://caibaojian.com/go/09.0.html















