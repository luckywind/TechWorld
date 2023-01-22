package com.cxf.sort;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-12-26
 * @Desc
 */
public class sortTest {
  public void  insertSort(int[] nums) {
    /**
     * 插入排序，依次插入使得[0,i]有序
     */
    for (int i = 1; i < nums.length; i++) { //从第2个数据开始插入
      int j=i-1;
      int insertNum=nums[i];//先把待插入元素保存下来，找到合适位置插入
      while (j >= 0 && nums[j] > insertNum) {  //这种情况需要向后移动
          nums[j+1]=nums[j];
          j--;
      }
      //至此，nums[j]<=insertNum了， insertNum直接放到它后面即可
      nums[j + 1] = insertNum;
    }
  }


  //选择排序
  public void selectSort(int[] nums) {
    int minIdx=0,tmp=0;
    for (int i = 0; i < nums.length; i++) {
      minIdx=i;
      for (int j = i; j < nums.length; j++) {
        if (nums[j] < nums[i]) {
          minIdx=j;
        }
      }
      tmp = nums[i];
      nums[i] = nums[minIdx];
      nums[minIdx]=tmp;
    }
  }
}
