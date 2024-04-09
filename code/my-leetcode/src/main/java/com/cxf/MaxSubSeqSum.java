package com.cxf;

/**
 *
 * @author chengxingfu <chengxingfu@.com>
 * @Date 2021-08-06
 * @Desc
 */
public class MaxSubSeqSum {
  public int maxSubArray(int[] nums) {
    if (nums.length == 1) {
      return nums[0];
    }

    int dp=nums[0];
    int res = dp;
    for (int i = 1; i < nums.length; i++) {
      dp = Math.max(dp + nums[i], nums[i]);
      res = Math.max(res, dp);
    }
    return res;
  }

  public static void main(String[] args) {
    int[] nums = new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4};
    System.out.println(new MaxSubSeqSum().maxSubArray(nums));
  }
}
