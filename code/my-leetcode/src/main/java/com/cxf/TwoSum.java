package com.cxf;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2021-08-01
 * @Desc
 * 题目要返回两数下标，map里的值是下标，key是数字，
 * 遍历数组时，每次去查这个map是否有当前需要的diff，
 * 如果没有，就把自己放到map里，如果有则返回当前下标与diff下标
 */
public class TwoSum {

  public static int[] findIndex(int[] nums,int target) {
    HashMap<Integer, Integer> diffMap = new HashMap<>();
    for (int i = 0; i < nums.length; i++) {
      int num = nums[i];
      int diff = target - num;
      if (diffMap.containsKey(diff)) {
        return new int[]{diffMap.get(diff), i};
      } else {
        diffMap.put(num, i);
      }
    }
    try {
      throw new Exception("没有这样的数!");
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
  public static void main(String[] args) {
    try {
      int[] index = findIndex(new int[]{1, 3, 3, 4, 5}, 5);
      System.out.println(Arrays.toString(index));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
