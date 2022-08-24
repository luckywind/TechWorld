package com.lbld.datastruct.prefix;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-08-12
 * @Desc
 */
public class SubArrayNum {

  public int countSubArray(int[] nums) {
    int ans = 0, pre = 0;
    for (int i = 0; i < nums.length; i++) {
      pre += 1;
      ans += pre;
    }
    return ans;
  }

}
