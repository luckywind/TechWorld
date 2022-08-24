package com.lbld.datastruct.prefix;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-08-04
 * @Desc
 * https://leetcode.cn/problems/count-of-range-sum/?show=1
 * 给你一个整数数组 nums 以及两个整数 lower 和 upper 。求数组中，
 * 值位于范围 [lower, upper] （包含 lower 和 upper）之内的"区间和"的个数 。
 *
 * 区间和 S(i, j) 表示在 nums 中，位置从 i 到 j 的元素之和，包含 i 和 j (i ≤ j)。
  题目比较难理解：
  是区间和 在[lower, upper]里面， 统计区间和 的个数
 */
public class CountOfRangeSum {

  public static int countRangeSum(int[] nums, int lower, int upper) {
    if (nums == null || nums.length == 0) {
      return 0;
    }
    long[] sum = new long[nums.length];
    sum[0] = nums[0];
    for (int i = 1; i < nums.length; i++) {
      sum[i] = sum[i - 1] + nums[i];
    }
    return process(sum, 0, sum.length - 1, lower, upper);
  }

  public static int process(long[] sum, int L, int R, int lower, int upper) {
    if (L == R) {
      return sum[L] >= lower && sum[L] <= upper ? 1 : 0;
    }

    int M = L + ((R - L) >> 1);
    return process(sum, L, M, lower, upper) + process(sum, M + 1, R, lower, upper) + merge(sum, L, M, R, lower, upper);
  }

  public static int merge(long[] arr, int L, int M, int R, int lower, int upper) {
    int ans = 0;
    int rangeL = L;
    int ranfeR = L;
    for (int i = M + 1; i <= R; i++) {
      long min = arr[i] - upper;
      long max = arr[i] - lower;

      while (ranfeR <= M && arr[ranfeR] <= max) {
        ranfeR++;
      }
      while (rangeL <= M && arr[rangeL] < min) {
        rangeL++;
      }

      ans += ranfeR - rangeL;
    }

    long[] help = new long[R - L + 1];
    int i = 0;
    int p1 = L;
    int p2 = M + 1;

    while (p1 <= M && p2 <= R) {
      help[i++] = arr[p1] <= arr[p2] ? arr[p1++] : arr[p2++];
    }

    while (p1 <= M) {
      help[i++] = arr[p1++];
    }

    while (p2 <= R) {
      help[i++] = arr[p2++];
    }

    for (i = 0; i < help.length; i++) {
      arr[L + i] = help[i];
    }

    return ans;
  }

  /**
   *   作者：zhanhuawang
   *   链接：https://leetcode.cn/problems/count-of-range-sum/solution/shi-yong-by-zhanhuawang-17ed/
   */

}
