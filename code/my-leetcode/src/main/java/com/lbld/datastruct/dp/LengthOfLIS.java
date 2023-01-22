package com.lbld.datastruct.dp;

import java.util.Arrays;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-12-15
 * @Desc
 */
public class LengthOfLIS {

  public static void main(String[] args) {
    int l = new LengthOfLIS().lengthOfLIS(new int[]{10,9,2,5,3,7,101,18});
    System.out.println(l);



  }
  public int lengthOfLIS(int[] nums) {
    //动态规划问题，需要计算dp数组，其状态只能是数组索引i；
    int[] dp=new int[nums.length];
    Arrays.fill(dp,1);


    //计算dp数组
    for(int i=1;i<dp.length;i++){
      for(int j=0;j<i;j++){
        if(nums[j]<nums[i]){
          dp[i]=Math.max(dp[i],dp[j]+1);
        }
      }
    }

    //dp数组取最大
    int res=1;
    for(int i=0;i<dp.length;i++){
      res=Math.max(res,dp[i]);
    }

    return res;

  }




}
