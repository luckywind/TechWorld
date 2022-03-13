PageRank, 即网页排名，又称网页级别、Google 左侧排名或佩奇排名。它是Google 创始人拉里· 佩奇和谢尔盖· 布林于1997 年构建早期的搜索系统原型时提出的链接分析算法。目前很多重要的链接分析算法都是在PageRank 算法基础上衍生出来的。**PageRank 是Google 用于用来标识网页的等级/ 重要性的一种方法**，是Google 用来衡量一个网站的好坏的唯一标准。在揉合了诸如Title 标识和Keywords 标识等所有其它因素之后， Google 通过PageRank 来调整结果，使那些更具“等级/ 重要性”的网页在搜索结果中令网站排名获得提升，从而提高搜索结果的相关性和质量。

<img src="https://gitee.com/luckywind/PigGo/raw/master/image/1920px-PageRank-hi-res.png" alt="img" style="zoom: 33%;" />

图中笑脸的大小跟指向自己的笑脸数量成正比

![img](https://gitee.com/luckywind/PigGo/raw/master/image/2019102116.gif)

alpha是阻尼系数，其意义是：任意时刻，用户访问到某页面后 继续访问下一个页面的概率，那么，1-alpha是用户停止点击，随机浏览新网页的概率。[wiki](https://zh.wikipedia.org/wiki/PageRank#%E5%AE%8C%E6%95%B4%E7%89%88%E6%9C%AC)

每次迭代每个点都计算一个PR值，直到收敛