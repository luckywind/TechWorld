package com.lbld.datastruct.dp;


import java.util.Arrays;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-12-14
 * @Desc
 * https://leetcode.cn/problems/coin-change/
 * 给你一个整数数组 coins ，表示不同面额的硬币；以及一个整数 amount ，表示总金额。
 *
 * 计算并返回可以凑成总金额所需的 最少的硬币个数 。如果没有任何一种硬币组合能组成总金额，返回 -1 。
 *
 * 你可以认为每种硬币的数量是无限的。
 */
public class CoinChange {
  int[] mem=null;

  public static void main(String[] args) {
    CoinChange change = new CoinChange();
    int res = change.coinChange(new int[]{2}, 3);
    System.out.println(res);
  }
  public int coinChange(int[] coins, int amount) {
    //这里可以初始化mem, 它是
    mem = new int[amount + 1];
    Arrays.fill(mem,-666);
    return dp(coins, amount);
  }

  private int dp(int[] coins, int amount) {


     //边界条件优先，然后才是备忘录
    int res=Integer.MAX_VALUE;
    if(amount==0) return 0;
    if(amount<0) return -1;

    if (mem[amount] != -666) {
      return mem[amount];
    }


    for(int coin: coins){
      int subProblem = dp(coins, amount - coin);
      if(subProblem ==-1) continue;   //当前子问题无解，跳到下一个子问题
      res= Math.min(res, subProblem +1);

    }
    int result = res == Integer.MAX_VALUE ? -1 : res;
    mem[amount] = result;
    return result;

  }


}
