1. 多读书
2. 记忆技巧：助记符
3. 创建学习环境： 书、电影
4. 放到上下文中
5. 生活中学习： 记录不认识的短语、句子
6. 增加互动性

# anki

## 下载

[飞书文档](https://k4p0ly83qw.feishu.cn/docx/TVpudhy5IoY8yMxoYILc21Qrnwb)介绍了如何使用商家提供的AppleID下载：核心就是到已购项目下找到然后下载：

号hdsd@lista.cc

密Aa663399



## 卡片获取

1. 官方平台
2. 三方平台

用户 -> 记忆库 -> 牌组 ->卡片

[anki基础模块插件](https://windsuzu.medium.com/anki-%E5%85%A8%E6%94%BB%E7%95%A5-%E5%9F%BA%E7%A4%8E-%E6%A8%A1%E6%9D%BF-%E6%8F%92%E4%BB%B6-%E5%B7%A5%E5%85%B7%E5%88%86%E4%BA%AB-6dc4eb15f51c)

[Improve Speaking with "Peppa Pig" (看小猪佩奇学外语) S1 Part 1/2](https://ankiweb.net/shared/info/340007010)

[Improve Speaking with "Peppa Pig" (看小猪佩奇学外语) S2 Part 2/2](https://ankiweb.net/shared/info/970292710)

模板：

1. Basic:   输入Back
2. 

## 复习参数设置

❤️**每日上限**
最好是 100 以内，因为短时间学习太多，会造成后面某一天复习的压力非常大。复习的卡片一般设置成新学习卡片数量的 10 倍就可以，或者不限制它

❤️**新卡片**

1. 初学间隔

间隔之间请用空格分隔。 

- 第一个间隔为学习新卡时，选择「重来」后卡片再次展示的间隔时间（默认 1 分钟）。
-  第二个间隔为学习新卡时，选择「良好」后进入下一阶段的间隔时间（默认 10 分钟）。
通过所有阶段都后，卡片将转为复习卡片择日展示。⁨间隔时间通常应设为分钟（如 5m）或天（如 2d），但亦可设为小时（如 1h）或秒（如 30s）。⁩

2. 毕业：在最后一个学习阶段选择「良好」后，再次展示卡片的间隔天数。

3. 简单：再次展示卡片的间隔天数。



![image-20250326182214947](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250326182214947.png)

❤️遗忘

这里“重学阶段”10m ，代表 10 分钟后会再次复习。

“记忆难点阈值”和“记忆难点处理”代表当点击“重来”多少次后就判断为难点，对应要进行什么操作。

比如可以选择对这些难点卡片自动“仅加标签”，方便后面我们通过标签对这些难点卡片进行筛选。

也可以选择“暂停卡片”，不再对它安排复习。

❤️FSRS复习算法

极大提高了复习算法的科学性。避免为了记住极少数知识点而大量复习。

**留存率**：想保证记住的百分比，默认0.9







牌组新学上限就是20：

![image-20250326182347841](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250326182347841.png)

每个卡牌展示的再现时间选项：

![image-20250326182423723](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250326182423723.png)





## 制卡

1. 填空题 挖空
   设置填空
   - 相同卡片(同时出现)
   - 新增卡片(依次出现)

豆包列出RAZ的词汇表，使用工具自动生成卡片



# Gelingo

使用[https://gitcode.com/gh_mirrors/ge/genanki](https://gitcode.com/gh_mirrors/ge/genanki/?utm_source=artical_gitcode&index=top&type=href&) 库开发的一个APP

牌组追加： 只要导入同名牌组，  但是默认每次生成的牌组都会带一个日期，所以注意把日期去掉

1. 优化例句 ok

2. 父子牌组 ok
   双冒号分割标题

3. 断点生成 ok
   译文和例句保存到一行中
4. 无界面生成 ok
5. 加密

Regular Card 
正面展示中文，背面展示中文/英文

Reverse Card
正面展示英文，背面展示英文/中文

一个单词可以两边都填，那么会生成两张卡片





1. 无界面生成的文本格式要求

内容可以是单词，可以是一句话、短语等， 用换行或者$分割

行前如果有`^REV^` 代表正面英文，背面中文

牌组包的重命名可以利用Anki来完成：先导入，然后重命名后再导出。

## 脚本化

```shell
python bin/create.py 输入路径 english 牌组名称 rev  --output 输出路径
```

rev意味着正面是英语，背面是汉语，reg反之。

## 模板定制

这个文件lib/language_card_templates.py包含卡片的模板，目前是写死的，可以修改为可参数化。



## 加密

[一个加密思路](https://zhuanlan.zhihu.com/p/673033848)，[视频](https://www.bilibili.com/video/BV1pe411r7qA?spm_id_from=333.788.recommend_more_video.0&vd_source=fa2aaef8ece31d2c310d46092c301b46)

## 问题记录

### 图片爬取

默认从Google爬取图片，爬多了会触发谷歌的反爬策略，报如下异常：

```python
2025-04-10 16:13:17,219 - INFO - WDM - ====== WebDriver manager ======
2025-04-10 16:13:17,381 - INFO - WDM - Get LATEST chromedriver version for google-chrome
2025-04-10 16:13:17,559 - INFO - WDM - Get LATEST chromedriver version for google-chrome
2025-04-10 16:13:17,754 - INFO - WDM - Driver [/Users/chengxingfu/.wdm/drivers/chromedriver/mac64/116.0.5845.96/chromedriver-mac-x64/chromedriver] found in cache
2025-04-10 16:13:20,113 - INFO - icrawler.crawler - start crawling...
2025-04-10 16:13:20,113 - INFO - icrawler.crawler - starting 1 feeder threads...
2025-04-10 16:13:20,114 - INFO - feeder - thread feeder-001 exit
2025-04-10 16:13:20,114 - INFO - icrawler.crawler - starting 1 parser threads...
2025-04-10 16:13:20,114 - INFO - icrawler.crawler - starting 1 downloader threads...
2025-04-10 16:13:20,754 - INFO - parser - parsing result page https://www.google.com/search?q=spin&ijn=0&start=0&tbs=&tbm=isch
Exception in thread parser-001:
Traceback (most recent call last):
  File "/Users/chengxingfu/.pyenv/versions/3.9.7/lib/python3.9/threading.py", line 973, in _bootstrap_inner
    self.run()
  File "/Users/chengxingfu/.pyenv/versions/3.9.7/lib/python3.9/threading.py", line 910, in run
    self._target(*self._args, **self._kwargs)
  File "/Users/chengxingfu/.pyenv/versions/env-genlingo/lib/python3.9/site-packages/icrawler/parser.py", line 93, in worker_exec
    for task in self.parse(response, **kwargs):
TypeError: 'NoneType' object is not iterable
```

关于这个问题的讨论，[见这篇文章](https://github.com/hellock/icrawler/issues/107)。 解决办法是切换网络，例如切换一个代理。或者换用BingImageCrawler代替。