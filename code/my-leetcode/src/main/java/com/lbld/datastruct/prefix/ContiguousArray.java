package com.lbld.datastruct.prefix;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-08-06
 * @Desc
 * 给定一个二进制数组 nums , 找到含有相同数量的 0 和 1 的最长连续子数组，并返回该子数组的长度。
 *
 *
 * https://leetcode.cn/problems/contiguous-array/?show=1
 */
public class ContiguousArray {

  int[] preSum;


  /**
   * 1. 0全变成-1， 这样的好处：0和1相同，意味着子序列和为0，两端的前缀和相同
   * 2.
   * @param nums
   * @return
   */
  public int findMaxLength(int[] nums) {
    if (nums.length < 2) {
      return 0;
    }

    return 0;


  }

  public static void main(String[] args) {

  }

}
