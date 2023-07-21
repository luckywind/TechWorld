# 时序图

https://zhuanlan.zhihu.com/p/70261692

https://blog.sidneyyi.com/view/uml%2FUML%E6%97%B6%E5%BA%8F%E5%9B%BE

[mermaid语法](http://blog.lisp4fun.com/2017/11/21/mermaiduse)

[me rmaid介绍](https://zhuanlan.zhihu.com/p/627356428)

## 基本语法

1. 换行<br>

2. 注释%%

3. **激活/去激活， 有两种方式箭头后使用+/-     或者activate/deactivate xx**  

4. 注释

   ```yaml
   Note [ right of | left of | over ] [Actor]: Text in note content
   注: Actor 可以是多个，通过逗号分割，例如：
   Note over Alice,John: A typical interaction
   ```

```yaml
sequenceDiagram
    participant Alice
    participant John
    Alice ->> John:  实线带箭头: ->>
    John -->> Alice: 虚线带箭头: -->>
    Alice -> John : 实线不带箭头: ->
    activate John
    Note over Alice,John: 这个注释在两个人的上方
    John --> Alice : 虚线不带箭头: -->
    deactivate John
    Alice -x John : 实线结尾带X: -x
    John --x Alice : 虚线结尾带X: --x
```



```mermaid
sequenceDiagram
    participant Alice
    participant John
    Alice ->> John:  实线带箭头: ->>
    John -->> Alice: 虚线带箭头: -->>
    Alice ->+ John : 实线不带箭头: ->
    %%    activate John
    note over Alice,John: 这个注释在两个人的上方
    John -->- Alice : 虚线不带箭头: -->
    %%deactivate John
    Alice -x John : 实线结尾带X: -x
    John --x Alice : 虚线结尾带X: --x
```



## 组合片段

| 片段类型 | 名称   | 说明                                                         |
| :------- | :----- | :----------------------------------------------------------- |
| Opt      | 选项   | if语句的图形化表示。                                         |
| Alt      | 抉择   | if else语句的图形化表示。                                    |
| Loop     | 循环   | for语句的图形化表示。                                        |
| Par      | 并行   | 指此片段中的消息可以并行交错。                               |
| Break    | 中断   | 指此中断此片段中的其余部分消息。                             |
| Seq      | 弱顺序 | 指相同生命线的消息不能并行交错，但不同生命线的消息可以并行交错。 |
| Strict   | 强顺序 | 指此片段中的消息必须按给定顺序执行。                         |
| Critical | 关键   | 主要用在`Par`或`Seq`片段中，指此片段中的消息只能原子化处理，不能并行。 |
| Consider | 考虑   | 指片段中指定的消息才是主要需要考虑的，其他的可以忽略。       |
| Ignore   | 忽略   | 指片段中指定的消息可以忽略。                                 |
| Assert   | 断言   | 指一定会发生的消息片段。                                     |
| Neg      | 否定   | 指不得发生的消息片段。                                       |

### 循环

在条件满足时，重复发出消息序列。相当于编程语言中的 while 语句。

```
sequenceDiagram
    网友 ->> + X宝 : 网购钟意的商品
    X宝 -->> - 网友 : 下单成功
    
    loop 一天七次
        网友 ->> + X宝 : 查看配送进度
        X宝 -->> - 网友 : 配送中
    end
```

```mermaid
sequenceDiagram
    网友 ->> + X宝 : 网购钟意的商品
    X宝 -->> - 网友 : 下单成功
    
    loop 一天七次
        网友 ->> + X宝 : 查看配送进度
        X宝 -->> - 网友 : 配送中
    end
```

### 选择alt

在多个条件中作出判断，每个条件将对应不同的消息序列。相当于 if 及 else if 语句。

```
sequenceDiagram    
    土豪 ->> 取款机 : 查询余额
    取款机 -->> 土豪 : 余额
    
    alt 余额 > 5000
        土豪 ->> 取款机 : 取上限值 5000 块
    else 5000 < 余额 < 100
        土豪 ->> 取款机 : 有多少取多少
    else 余额 < 100
        土豪 ->> 取款机 : 退卡
    end
    
    取款机 -->> 土豪 : 退卡
```

```mermaid
sequenceDiagram    
    土豪 ->> 取款机 : 查询余额
    取款机 -->> 土豪 : 余额
    
    alt 余额 > 5000
        土豪 ->> 取款机 : 取上限值 5000 块
    else 5000 < 余额 < 100
        土豪 ->> 取款机 : 有多少取多少
    else 余额 < 100
        土豪 ->> 取款机 : 退卡
    end
    
    取款机 -->> 土豪 : 退卡

```

### 可选opt

```
sequenceDiagram
    老板C ->> 员工C : 开始实行996
    
    opt 永不可能
        员工C -->> 老板C : 拒绝
    end
```

```mermaid
sequenceDiagram
    老板C ->> 员工C : 开始实行996
    
    opt 永不可能
        员工C -->> 老板C : 拒绝
    end
```

### 并行par

```
sequenceDiagram
    老板C ->> 员工C : 开始实行996
    
    par 并行
        员工C ->> 员工C : 刷微博
    and
        员工C ->> 员工C : 工作
    and
        员工C ->> 员工C : 刷朋友圈
    end
    
    员工C -->> 老板C : 9点下班
```

```mermaid
sequenceDiagram
    老板C ->> 员工C : 开始实行996
    
    par 并行
        员工C ->> 员工C : 刷微博
    and
        员工C ->> 员工C : 工作
    and
        员工C ->> 员工C : 刷朋友圈
    end
    
    员工C -->> 老板C : 9点下班
```

# 流程图

1. LR:流程图方向，TDLR分别代表上下左右，两个方向组成一个流动方向
2. 支持虚线与实线，有箭头与无箭头、有文字与无文字。分别是
   ---、
   -.-、 
   -->、
   -.->、
   --文字-->、-.文字.->、--文字---、-.文字.- 支持子图
3. 元素表示法

| 表述       | 说明         | 含义                                               |
| ---------- | ------------ | -------------------------------------------------- |
| id[文字]   | 矩形节点     | 表示过程                                           |
| id(文字)   | 圆角矩形节点 | 表示开始与结束                                     |
| id((文字)) | 圆形节点     | 表示连接。为避免流程过长或有交叉，可将流程切开成对 |
| id{文字}   | 菱形节点     | 表示判断、决策                                     |
| id>文字 ]  | 右向旗帜节点 |                                                    |

```shell
graph TB;
subgraph 分情况
A(开始)-->B{判断}
end
B--第一种情况-->C[第一种方案]
B--第二种情况-->D[第二种方案]
B--第三种情况-->F{第三种方案}
subgraph 分种类
F-.第1个.->J((测试圆形))
F-.第2个.->H>右向旗帜形]
end
H---I(测试完毕)
C--票数100---I(测试完毕)
D---I(测试完毕)
J---I(测试完毕)
```





```mermaid
graph TB;
subgraph 分情况
A(开始)-->B{判断}
end
B--第一种情况-->C[第一种方案]
B--第二种情况-->D[第二种方案]
B--第三种情况-->F{第三种方案}
subgraph 分种类
F-.第1个.->J((测试圆形))
F-.第2个.->H>右向旗帜形]
end
H---I(测试完毕)
C--票数100---I(测试完毕)
D---I(测试完毕)
J---I(测试完毕)
```









```shell
graph LR
  单独节点
  开始 -- 带注释写法1 --> 结束
  开始 -->|带注释写法2| 结束
  实线开始 --- 实线结束
  实线开始 --> 实线结束
  实线开始 -->|带注释| 实线结束
  虚线开始 -.- 虚线结束
  虚线开始 -.-> 虚线结束
  虚线开始 -.->|带注释| 虚线结束
  粗线开始 === 粗线结束
  粗线开始 ==> 粗线结束
  粗线开始 ==>|带注释| 粗线结束
  subgraph 子图标题
    子图开始 --> 子图结束
  end
  节点1[方形文本框] --> 节点2{菱形文本框}
  节点3(括号文本框) --> 节点4((圆形文本框))
```



```mermaid
graph LR
  单独节点
  开始 -- 带注释写法1 --> 结束
  开始 -->|带注释写法2| 结束
  实线开始 --- 实线结束
  实线开始 --> 实线结束
  实线开始 -->|带注释| 实线结束
  虚线开始 -.- 虚线结束
  虚线开始 -.-> 虚线结束
  虚线开始 -.->|带注释| 虚线结束
  粗线开始 === 粗线结束
  粗线开始 ==> 粗线结束
  粗线开始 ==>|带注释| 粗线结束
  subgraph 子图标题
    子图开始 --> 子图结束
  end
  节点1[方形文本框] --> 节点2{菱形文本框}
  节点3(括号文本框) --> 节点4((圆形文本框))
```

