package com.lbld.datastruct.prefix;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-07-21
 * @Desc
 */
public class NumArray {
  int[] nums;
  // 前缀和数组
  private int[] preSum;
  public NumArray(int[] nums) {
    preSum = new int[nums.length + 1];
    // preSum[0] = 0，便于计算累加和
    for (int i = 1; i < preSum.length; i++) {
      //依次递增一个num,preSum的第i个位置存放的是前i个数的和，最大索引号是i-1
      preSum[i] = preSum[i - 1] + nums[i - 1];
    }

  }

  /* 查询闭区间 [left, right] 的累加和 */
/*  public int sumRange(int left, int right) {
    int res=0;
    for (int i = left; i <=right ; i++) {
      res += nums[i];
    }
    return res;
  }*/


  public int sumRange(int left, int right) {
    return preSum[right + 1] - preSum[left];
  }

  public static void main(String[] args) {
    NumArray numArray=new NumArray(new int[]{-2, 0, 3, -5, 2, -1});
    System.out.println(numArray.sumRange(0,2));
    System.out.println(numArray.sumRange(2,5));
    System.out.println(numArray.sumRange(0,5));
  }


}
