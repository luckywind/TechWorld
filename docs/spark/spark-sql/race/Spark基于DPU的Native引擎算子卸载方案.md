1.背景介绍
Apache Spark（以下简称Spark）是一个开源的分布式计算框架，由UC Berkeley AMP Lab开发，可用于批处理、交互式查询（Spark SQL）、实时流处理（Spark Streaming）、机器学习（Spark MLlib）和图计算（GraphX）。Spark 使用内存加载保存数据并进行迭代计算，减少磁盘溢写，同时支持 Java、Scala、Python 和 R 等多种高级编程语言，这使得Spark可以应对各种复杂的大数据应用场景，例如金融、电商、社交媒体等。

Spark 经过多年发展，作为基础的计算框架，不管是在稳定性还是可扩展性方面，以及生态建设都得到了业界广泛认可。尽管Apache社区对Spark逐步引入了诸如钨丝计划、向量化 Parquet Reader 等一系列优化，整体的计算性能也有两倍左右的提升，但在 3.0 版本以后，整体计算性能的提升有所减缓，并且随着存储、网络以及IO技术的提升，CPU也逐渐成为Spark计算性能的瓶颈。如何在Spark现有框架上，增强大数据计算能力，提高CPU利用率，成为近年来业界的研究方向。

2.开源优化方案
Spark本身使用scala语言编写，整体架构基于 JVM 开发，只能利用到一些比较基础的 CPU 指令集。虽然有JIT的加持，但相比目前市面上的Native向量化计算引擎而言，性能还是有较大差距。因此考虑如何将具有高性能计算能力的Native向量引擎引用到 Spark 里来，提升 Spark 的计算性能，突破 CPU 瓶颈，成为一种可行性较高的解决方案。

随着Meta在2022年超大型数据库国际会议（VLDB）上发表论文《Velox:Meta's Unified Execution Engine》，并且Intel创建的Gluten项目基于Apache Arrow数据格式和Substrait查询计划的JNI API将Spark JVM和执行引擎解耦，从而将Velox集成到Spark中，这使得使用Spark框架+Native向量引擎的大数据加速方案成为现实。

3.DPU计算卡与软件开发平台
AI大模型的发展，金融、电商等领域数据处理需求的增加，生活应用虚拟化程度的加深，都对现代化数据中心提出严峻的考验。未来数据中心的发展趋势，逐步演变成CPU + DPU + GPU三足鼎立的情况，CPU用于通用计算，GPU用于加速计算，DPU则进行数据处理。将大数据计算卸载到具有高度定制化和数据处理优化架构的大规模数据计算DPU卡上，可以有效提高计算密集型应用场景下数据中心的性能和效率，降低其成本和能耗。

中科驭数CONFLUX®-2200D 大数据计算DPU卡主要应用于大数据计算场景。CONFLUX®-2200D通过计算DPU卸载加速，存储DPU卸载加速和网络DPU卸载加速实现大数据计算性能3-6倍提升。CONFLUX®-2200D是基于中科驭数自主知识产权的KPU（Kernel Processing Unit）架构、DOE（Data Offloading Engine）硬件数据库运算卸载引擎和LightningDMA中科驭数自主知识产权的基于DMA的直接内存写入技术提出的领域专用DPU卡。能够满足无侵入适配、自主可控、安全可靠，支持存算一体、存算分离等不同场景。

中科驭数HADOS是中科驭数推出的专用计算敏捷异构软件开发平台。HADOS®数据查询加速库通过提供基于列式数据的查询接口，供数据查询应用，目前Spark、PostgreSQL已通过插件的形式适配。支持Java、Scala、C和C++语言的函数调用，主要包括列数据管理、数据查询运行时函数、任务调度引擎、函数运算代价评估、内存管理、存储管理、硬件管理、DMA引擎、日志引擎等模块，目前对外提供数据管理、查询函数、硬件管理、文件存储相关功能API。

4.Spark框架+Gluten-Velox向量化执行引擎+DPU加速卡
4.1方案简介
随着SSD和万兆网卡普及以及I/O技术的提升，Spark用户的数据负载计算能力逐渐受到CPU性能瓶颈的约束。由于Spark本身基于JVM的Task计算模型的CPU指令优化，要远远逊色于其他的Native语言（C++等），再加上开源社区的Native引擎已经发展得比较成熟，具备优秀的量化执行能力，这就使得那些现有的Spark用户，如果想要获得这些高性能计算能力就需要付出大量的迁移和运维成本。

Gluten解决了这一关键性问题，让Spark用户无需迁移，就能享受这些成熟的Native引擎带来的性能优势。**Gluten最核心的能力就是通过Spark Plugin的机制，把Spark查询计划拦截并下发给Native引擎来执行，跳过原生Spark不高效的执行路径**。整体的执行框架仍沿用Spark既有实现，并且对于Native引擎无法承接的算子，Gluten安排Fallback回正常的Spark执行路径进行计算，从而保证Spark任务执行的稳定性。同时Gluten还实现了Fallback、本地内存管理等功能，使得Spark可以更好利用Native引擎带来的高性能计算能力。

Velox是一个集合了现有各种计算引擎优化的新颖的C++数据加速库，其重新设计了数据模型以支持复杂数据类型的高效计算，并且提供可重用、可扩展、高性能且与上层软件无关的数据处理组件，用于构建执行引擎和增强数据管理系统。

由于Velox只接收完全优化的查询计划作为输入，不提供 SQL 解析器、dataframe层、其他 DSL 或全局查询优化器，专注于成为大数据计算的执行引擎。这就使得Gluten+Velox架构可以各司其职，从而实现数据库组件模块化。

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/81b72a472a05093cf0a1a362f4c2736c.png)

要将Gluten+Velox优化过的Spark计算任务卸载到DPU卡，还缺少一个异构中间层，为此中科驭数研发了HADOS异构执行库，该库提供列数据管理、数据查询运行时函数、任务调度引擎、函数运算代价评估、内存管理等多种DPU能力的API接口，并且支持Java，C++等多种大数据框架语言的调用，拥有极强的拓展性，以及与现有生态的适配性。HADOS敏捷异构软件平台可以适应复杂的大数据软件生态，在付出较小成本的情况下为多种计算场景提供DPU算力加速。Spark框架集成Gluten+Velox向量化执行引擎，然后使用HADOS平台，就可以将经过向量化优化的计算任务，利用DPU执行，从而彻底释放CPU，实现DPU高性能计算。

4.2 DPU算力卸载
velox是由C++实现的向量化计算引擎，其核心执行框架涵盖了任务（Task）、驱动（Driver）和操作器（Operator）等组件。velox将Plan转换为由PlanNode组成的一棵树，然后将PlanNode转换为Operator。Operator作为基础的算子，是实际算法执行的逻辑框架，也是实现DPU计算卸载的关键。

4.2.1 逻辑框架
Operator作为实际算法的逻辑框架，承载着各种表达式的抽象，每一个Operator中包含一个或多个表达式来实现一个复杂完整的计算逻辑块，表达式的底层是由function来具体实现。Velox向开发人员提供了API可以实现自定义scalar function，通过实现一个异构计算版本的function，然后将这个function注册到Velox的函数系统中，就可以将计算任务卸载到DPU卡上。任务执行过程如下图：

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/a89579f6d7701d02ac88ca4bf75703ed.png)

中科驭数的CONFLUX®-2200D S 大数据计算加速DPU卡可以实现列式计算，并且HADOS平台支持C++语言，所以可以直接解析Velox的向量化参数。对于列式存储的数据，经过对数据类型的简单处理之后，可以直接交给DPU执行计算任务，免去了数据行列转换的性能损失，同时也降低了DPU计算资源集成的运维难度，大大提高了Velox异构开发的效率。

4.2.2 算子卸载
以我们实现卸载的Filter算子为例，对于cast(A as bigint)>1这一具体的表达式，来探究如何实现”>”这一二元运算符的卸载。

Filter算子的Operator中会使用有一个 std::unique_ptr<ExprSet> exprs_的变量，用来执行过滤和投影的计算。ExprSet是Filter算子计算的核心，其本质是一颗表达式树。cast(A as bigint)>1的表达式树以及表达式树的静态节点类型如下：

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/9feb6e600c9b2602c56f3d0d17c3d870.png)


节点类型	作用
FieldAccessTypedExpr	表示RowVector中的某一列，作为表达式的叶子节点
ConstantTypedExpr	表示常量值，作为表达式的叶子节点
CallTypedExpr	
表示函数调用表达式，子节点表示输入参数
表示特殊类型表达式，包括
if/and/or/switch/cast/try/coalesce等

CastTypedExpr	类型转换
LambdaTypedExpr	Lambda表达式，作为叶子节点
在表达式的所有子节点执行完后，会执行applyFunction，说明当前表达式节点是一个函数调用，然后调用vectorFunction_的apply来对结果进行处理，输入是inputValues_数组，该数组长度与函数的表达式叶子节点数相等（文中示例表达式的叶子节点为2），作为函数的参数，result为输出，结果为VectorPtr，程序流程图如下：

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/bd8b2c5ce0205d6bcbda197f9c209714.png)

4.2.3 Fallback
现阶段我们只实现了Filter算子的部分表达式，后续还会继续支持更多的算子和表达式。对于一些无法执行的算子和表达式，还是需要退回给Velox，交由CPU执行，从而保证SQL的正常执行。由于处理的是列式数据，所以回退的执行计划可以不需要任何处理，就可以直接从HADOS退还给Velox，几乎无性能损失。

4.2.4 DPU资源管理
HADOS平台会对服务器的DPU资源进行统一管理。对于卸载的计算任务根据现有的DPU资源进行动态分配，从而实现计算资源的高效利用。同时HADOS平台还会对计算任务中所需的内存进行合理的分配，动态申请和释放系统内存，从而减少额外的内存开销。

4.3 加速效果
单机单线程local模式，使用1G数据集，仅卸载Filter算子的部分表达式的场景下，TPC-DS语句中有5条SQL语句，可以将使用开源方案的加速效果提升15-20%左右。q70语句，在开源方案提升100%的基础上，提升了15%；q89语句，在开源方案提升50%的基础上，提升了27%；q06在开源方案提升170%的基础上，提升了13%。

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/2e88722d099fb293b05aab9e909f9f80.png)

单一运算符场景下（SELECT a FROM t WHERE a = 100），使用DPU运算符相比 Spark原生的运算符的加速比最高达到12.7。

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/66104645f7cc32eedf0a8be2dec6e857.png)

5.不足和展望
中科驭数HADOS敏捷异构软件平台可以十分轻松地与现有开源大数据加速框架相结合，并且为开源框架提供丰富的算力卸载功能。HADOS平台在完美发挥开源加速框架优势的前提下，为大数据任务提供硬件加速能力。由于现在我们只实现了较小部分算子卸载的验证，在执行具有复杂算子操作的SQL时无法发挥出DPU的全部实力，并且因为开源方案在设计之处并没有考虑到使用DPU硬件，所以在磁盘IO，算子优化等方面的性能还有待优化。后续我们也会从一下几个方面来进一步做特定优化：

开发更多较复杂的算子，例如重量级的聚合算子会消耗CPU大量的计算能力从而影响Spark作业，通过将聚合算子卸载到DPU硬件来解放CPU能力，从而使得加速效果更加明显；
优化DPU的磁盘读写，让DPU可以直接读取硬盘数据，省去数据在服务器内部的传输时间，可以减少数据准备阶段的性能损耗；
RDMA技术，可以直读取远端内存数据，数据传输内容直接卸载到网卡，减少数据在系统内核中额外的数据复制与移动，可以减少大数据任务计算过程中的性能损耗。


[csdn](https://blog.csdn.net/yusur/article/details/140048638)