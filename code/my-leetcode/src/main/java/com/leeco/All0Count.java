package com.leeco;

import javax.sound.midi.Soundbank;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-07-23
 * @Desc
 */
public class All0Count {

  public long zeroFilledSubarray(int[] nums) {
  //全零子数组的数目
    int res = 0;
    int zero_len = 0;//连续0的个数
    for (int i = 0; i < nums.length; i++) {
      if (nums[i] == 0) {
        zero_len += 1;
      } else {//遇到非0值：
        for (int j = 1; j <=zero_len; j++) {
          res += j;
        }
        //最后清零
        zero_len = 0;
      }
    }

    //处理最后一串0
    for (int j = 1; j <=zero_len; j++) {
      res += j;
    }

    return res;
  }
  public static void main(String[] args) {


    All0Count all0Count = new All0Count();
    System.out.println(all0Count.zeroFilledSubarray(new int[]{1, 3, 0, 0, 2, 0, 0, 4}));
    System.out.println(all0Count.zeroFilledSubarray(new int[]{0,0,0,2,0,0}));
    System.out.println(all0Count.zeroFilledSubarray(new int[]{2,10,2019}));
    System.out.println(all0Count.zeroFilledSubarray(new int[]{0,0,0,0,0,0,0,0,0,0,0,1,0,0,0}));

  }

}
