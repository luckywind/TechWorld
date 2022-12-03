package com.lbld.datastruct.binaryTree;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-11-27
 * @Desc
 * 多少个BST? 递归解法，n问题如何转化为n-1问题？
 * 假如n-1个节点右x(n-1)种BST,再加一个最大的节点n，构成BST有多少种加法？
 *
 */
public class CountBST {
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


}
