package com.cxf.sort;

import com.sun.tools.javac.util.ArrayUtils;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-11-29
 * @Desc
 */
public class QuickSort {

  public static void main(String[] args) {
    int[] nums = new int[]{4,3,7,10, 1, 6, 3, 2, 5};
    sort(nums);
    for (int i = 0; i < nums.length; i++) {
      System.out.println(nums[i]);
    }
  }
  public static void sort(int[] nums){
    sort(nums,0,nums.length-1);
  }

  public static void sort(int[] nums, int lo, int hi) {
    //边界条件
    if (lo>=hi) return;
    int pivot = nums[lo];  //枢纽元，需要排序的元素
    /***
     * 前序位置把它排好哈
     */
    int i=lo+1,j=hi; //  定义区间[lo, i)<=pivot,   (j,hi]>pivot。  寻找逆序，并交换
    while (i <= j) { //直到相遇
      //从左边寻找第一额大于pivot的下标
      while (i<hi && nums[i]<=pivot) i++; // 此时 nums[i]>pivot
      //从右边寻找第一个小于等于pivot的下标
      while (j>lo && nums[j]>pivot) j--; //  此时 nums[j]<=pivot
      if (i >= j) {  //相遇后退出循环
        break;
      }
      SortUtils.swap(nums,i,j);
    }
    //最后pivot放到合适位置
    SortUtils.swap(nums,lo,j);//因为逆序(i,j)完成交换后，nums[j]就变成了第一个大于pivot的元素
    //此时，pivot已经排好序

    //开始解决子问题，pivot左边的元素满足nums[x]<=pivot, 最大的那个下标即是i-1
    sort(nums,lo,j-1 );  //递归排左边
    sort(nums,j+1,hi );  //递归排右边

  }

}
