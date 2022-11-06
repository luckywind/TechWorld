package com.cxf.arr;

import java.util.Arrays;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-11-04
 * @Desc
 */
public class MergeArr {

  public static void main(String[] args) {
    int[] a = new int[7];
    a[0]=1;
    a[1]=3;
    a[2]=4;
    int[] b = new int[]{1, 2,3,4};

    int[] res = merge(a, 3, b, b.length);
    System.out.println(Arrays.toString(res));


  }


  public  static  int[] merge(int[] nums1, int m, int[] nums2, int n) {
    int i = m - 1, j = n - 1;
    int idx = m + n - 1;
    while (i >= 0 || j >= 0) {
      if (i >= 0 && j >= 0) {
        nums1[idx--] = nums1[i] >= nums2[j] ? nums1[i--] : nums2[j--];
      } else if (i >= 0) {
        nums1[idx--] = nums1[i--];
      } else {
        nums1[idx--] = nums2[j--];
      }
    }
    return nums1;
  }

}
