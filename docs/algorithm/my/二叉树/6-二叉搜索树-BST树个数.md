[参考](https://labuladong.github.io/algo/2/21/44/)

# BST树的个数

[力扣](https://leetcode.cn/problems/unique-binary-search-trees/)

给你一个整数 `n` ，求恰由 `n` 个节点组成且节点值从 `1` 到 `n` 互不相同的 **二叉搜索树** 有多少种？返回满足题意的二叉搜索树的种数。

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/2.jpg" alt="img" style="zoom:50%;" />

n=5时,假设当前节点为3，有以下几种结果

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/1.jpeg" alt="img" style="zoom:33%;" />

遇到二叉树类型的问题，首先要想到递归遍历框架，最重要的后序位置可以接收子树传来的结果。

这题的关键是发现一个规律： 以某个元素为根节点的左BST子树个数等于比它小的所有节点构成的BST树个数，右BST子树个数等于比它大的所有节点构成的BST树个数；而以这个节点为根的BST树个数是两者乘积(**原问题需要子问题的返回值，所以，需要在后序遍历位置做处理**)。 如果我们遍历所有节点作为根节点，所有结果相加(所有节点都可以作为根节点)就是最后的答案(**不妨用一个变量来累加**)。

然后我们拆解问题，[1, n] 这n个节点，依次作为根节点，假设用mid表示，这样的BST树就有[1,mid-1]的BST树个数 * [mid+1,n]构成的BST树个数。



```java
  public int numTrees(int n) {
    return  count(1, n);
  }
  //辅助递归函数
  int count(int lo, int hi) {
    //边界条件
    if(lo>hi) return 1; //只有一种情况：空树

    int res=0; //累加中间结果
    for (int mid = lo; mid <=hi; mid++) {
      int left = count(lo, mid-1);
      int right = count(mid + 1, hi);
      res+=left*right; //递归后序位置组织子问题答案得到父问题答案
    }
    return res;
  }

```

> 这里会有重复计算，优化方法是使用备忘录

```java
 int[][] mem;
  public int numTrees(int n) {
    mem=new int[n+1][n+1];
    return  count(1, n);
  }
  //辅助递归函数
  int count(int lo, int hi) {
    //边界条件
    if(lo>hi) return 1; //只有一种情况：空树
    
    //查备忘录
    if (mem[lo][hi]>0) return mem[lo][hi];

    int res=0; //累加中间结果
    for (int mid = lo; mid <=hi; mid++) {
      int left = count(lo, mid-1);
      int right = count(mid + 1, hi);
      res+=left*right; //递归后序位置组织子问题答案得到父问题答案
    }
    //结果先放入备忘录再返回
    mem[lo][hi]=res;
    return res;
  }
```

