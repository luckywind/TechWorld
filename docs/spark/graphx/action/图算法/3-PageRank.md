# PageRank

PageRank, 即网页排名，又称网页级别、Google 左侧排名或佩奇排名。它是Google 创始人拉里· 佩奇和谢尔盖· 布林于1997 年构建早期的搜索系统原型时提出的链接分析算法。目前很多重要的链接分析算法都是在PageRank 算法基础上衍生出来的。**PageRank 是Google 用于用来标识网页的等级/ 重要性的一种方法**，是Google 用来衡量一个网站的好坏的唯一标准。在揉合了诸如Title 标识和Keywords 标识等所有其它因素之后， Google 通过PageRank 来调整结果，使那些更具“等级/ 重要性”的网页在搜索结果中令网站排名获得提升，从而提高搜索结果的相关性和质量。

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/1920px-PageRank-hi-res.png" alt="img" style="zoom: 33%;" />

图中笑脸的大小跟指向自己的笑脸数量成正比

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/2019102116.gif)

alpha是阻尼系数，其意义是：任意时刻，用户访问到某页面后 继续访问下一个页面的概率，那么，1-alpha是用户停止点击，随机浏览新网页的概率。[wiki](https://zh.wikipedia.org/wiki/PageRank#%E5%AE%8C%E6%95%B4%E7%89%88%E6%9C%AC)

每次迭代每个点都计算一个PR值，直到收敛

## 主要应用

主要应用包括：

1. <font color=red>基于人到人的关系图中通过权值排名区分出关键人物</font>
2. 基于“分享”的社交网络图中对其影响力做等级划分等

## 算法流程 

1）用1/N的页面排名值（PR）初始化每个顶点，N为图中顶点总数。

2）循环：

①每个顶点，沿着出边发送PR值1/M，M为当前顶点的出度。

②当每个顶点从相邻顶点收到其发送的PR值后，合理计算这

些PR值作为当前顶点的新PR值。

③图中顶点的PR值与上一个迭代相比没有显著的变化，则退出迭代。

![image-20220322184808479](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220322184808479.png)

根据链出总数平分一个页面的PR值:

![image-20220322185003315](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/image-20220322185003315.png)

L(X)为顶点X的出度

如果应用在给社交网络上的用户推荐新人，这样就需要用到个性化PageRank算法进行个性化的定制。

个性化PageRank是PageRank的一个变种，目标是要计算**所有节点相对于目标节点的相关度**。从目标节点开始游走，每到一个节点都以d的概率停止游走并从目标节点重新开始，或者以d的概率继续游走，从当前节点指向的节点中按照均匀分布随机选择一个节点往下游走。这样经过多轮游走之后，每个顶点被访问到的概率也会收敛趋于稳定，这时就可以用概率来进行排名了。当然个性化PageRank算法也有一些缺点：（1）只有一个源顶点可以被指定；（2）不能指定每个顶点的权值。

## 实现

```scala
 val graph = GraphLoader.edgeListFile(sc, "data/graphx/followers.txt")
    // Run PageRank
    /**
     * 参数tol是精度
     */
    val ranks: VertexRDD[Double] = graph.pageRank(0.0001).vertices
    // Join the ranks with the usernames
    val users = sc.textFile("data/graphx/users.txt").map { line =>
      val fields = line.split(",")
      (fields(0).toLong, fields(1))
    }
    val ranksByUsername = users.join(ranks).map {
      case (id, (username, rank)) => (username, rank)
    }
    // Print the result
    println(ranksByUsername.collect().mkString("\n"))
    // $example off$
    spark.stop()
```

人物关注图如下：

![image-20221104163244488](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221104163244488.png)

输出如下

```shell
(justinbieber,0.15007622780470478)
(BarackObama,1.4596227918476916)
(matei_zaharia,0.7017164142469724)
(jeresig,0.9998520559494657)
(odersky,1.2979769092759237)
(ladygaga,1.3907556008752426)
```

