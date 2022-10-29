package com.cxf.sort;

import java.util.Arrays;
import sun.security.util.ArrayUtil;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-10-28
 * @Desc
 */
public class InsertSort {

  public static void main(String[] args) {
    int[] arr = {1, 8, 2, 3, 1, 2};
    for (int i = 1; i < arr.length; i++) {
      //移动之前需要把当前元素存下来
      int insertNum = arr[i];
      // 依次往前比较，如果大于就向后移动，否则就插入并结束
      int j=i-1; //从前面那个开始比较
      while (j >= 0 && arr[j] > insertNum) {//这个insertNum不能写成arr[i]了，想想为啥？
        //满足条件，则向后移动
        arr[j + 1] = arr[j];
        j--;
      }
      // 至此，j已经不满足条件了，即 a[j]<=insertNum;
      // 另外，在上一轮迭代 a[j+1]已经移动到a[j+2]了，所以
      arr[j + 1] = insertNum;
    }

      System.out.println(Arrays.toString(arr));
  }

}
