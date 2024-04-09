package com.lbld.datastruct.prefix;

import java.util.HashMap;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-08-04
 * @Desc
 * 给你一个整数数组 nums 和一个整数 k ，请你统计并返回 该数组中和为 k 的连续子数组的个数 。
 *
 *  
 *
 * 示例 1：
 *
 * 输入：nums = [1,1,1], k = 2
 * 输出：2
 * 示例 2：
 *
 * 输入：nums = [1,2,3], k = 3
 * 输出：2
 *  
 *
 * 提示：
 *
 * 1 <= nums.length <= 2 * 104
 * -1000 <= nums[i] <= 1000
 * -107 <= k <= 107
 *
 *
 * 来源：力扣（LeetCode）
 * 链接：https://leetcode.cn/problems/subarray-sum-equals-k
 * 著作权归领扣网络所有。商业转载请联系官方授权，非商业转载请注明出处。
 */
public class SumEqualK {

  int[] preSum;


  /**
   * 前缀和出现的次数
   * 利用前缀和技巧把这个问题简化成了两数和问题
   * 而两数和问题的优化解法就是利用map记录下差出现的次数
   *
   * 直接记录下有几个 sum[j] 和 sum[i] - k 相等
   * @param nums
   * @param k
   * @return
   */
  int subarraySum(int[] nums, int k) {
    int n = nums.length;
    // map：前缀和 -> 该前缀和出现的次数
    HashMap<Integer, Integer>
        preSum = new HashMap<>();
    // base case
    /**
     * case:
     * map中的值是前缀和出现的次数
     * 本算法如何使用这个次数的呢？
     *    仔细思考一下，如果当前前缀和正好是k,那差就是0，算作一个解，ans应该加1，所以前缀和为0出现次数初始化为1
     */
    preSum.put(0, 1);

    int ans = 0, sum0_i = 0;
    /**
     * sum0_i就是前缀和的某一项，map里存储它出现的次数
     * 对某个数字nums[i]：
     *   计算一个前缀和sum0_i, 以及该前缀和与k的差sum0_j
     *   查看有几个值为sum0_j的前缀和，加到结果里去
     *   更新前缀和map
     *
     */
    for (int i = 0; i < n; i++) {
      sum0_i += nums[i];
      // 这是我们想找的前缀和 nums[0..j]
      int sum0_j = sum0_i - k;
      // 如果前面有这个前缀和，则直接更新答案
      if (preSum.containsKey(sum0_j))
        ans += preSum.get(sum0_j);
      // 把前缀和 nums[0..i] 加入并记录出现次数
      preSum.put(sum0_i,
          preSum.getOrDefault(sum0_i, 0) + 1);
    }
    return ans;
  }


  /**
   * O^2
   * @param nums
   * @param k
   * @return
   */
  public int subarraySumO2(int[] nums, int k) {
   this.preSum=new int[nums.length + 1] ;
    preSum[0] = 0;
    for (int i = 1; i < preSum.length; i++) {
      preSum[i] = preSum[i - 1] + nums[i];
    }

    int n = 0;
    for (int i = 1; i < preSum.length-1; i++) {
      for (int j = 0; j < i; j++) {
        if (preSum[i] - preSum[j] == k) {
          n++;
        }
      }
    }
    return n;
  }
}
