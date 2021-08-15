[力扣](https://leetcode-cn.com/problems/qing-wa-tiao-tai-jie-wen-ti-lcof/)

一只青蛙一次可以跳上1级台阶，也可以跳上2级台阶。求该青蛙跳上一个 n 级的台阶总共有多少种跳法。

答案需要取模 1e9+7（1000000007），如计算初始结果为：1000000008，请返回 1。

# 思路

## 递归解法

递归解法：自顶向下的思路。假设跳n级台阶有f(n)种跳法。我们推演一下看看相邻两个n之间的规律：

跳上10级台阶，可以先跳上9级台阶f(9)，然后再跳到第10级， 这种情况跳法就是f(9)。

此外，还可以先跳到8级，然后再跳2级(为啥没有连跳两次一级？因为这种包含在先跳到9级里了)，这种情况跳法就是f(8)。

因此规律如下：

`f(10)=f(9)+f(8)`

有了这个递推关系，再就是边界情况了，跳一级台阶只有一个方式，即f(1)=1，跳两级台阶有2种方式，即f(2)=2.

# 代码如下

```java
   public int numWays(int n) {
     if (n == 1) {
       return 1;
     }
     if (n == 2) {
       return 2;
     }
     return numWays(n - 1) + numWays(n - 2);
  }
```

但是这种方法的时间复杂度是O(n^2),复杂度太高。 容易发现f(n)被计算了多次。一个办法就是把他们记下来，防止重算。

```java
public int numWays(int n) {
     HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
     if (n == 1) {
       return 1;
     }
     if (n == 2) {
       return 2;
     }
     if (map.containsKey(n)) {
       return map.get(n);
     } else {
       map.put(n, numWays(n - 1) + numWays(n - 2));
       return map.get(n);
     }
  }
```

