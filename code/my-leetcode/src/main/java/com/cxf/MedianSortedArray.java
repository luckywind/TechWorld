package com.cxf;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xxx.com>
 * @Date 2021-08-02
 * @Desc
 */
public class MedianSortedArray {

  /**
   * 思路：
   * 计算合并后数组的长度l， 如果l是偶数，则中位数=(num[l/2-1] + num[l/2])/2
   * 如果是奇数，则中卫数=num[floor(l/2)]
   * @param nums1
   * @param nums2
   * @return
   */
  public double findMedianSortedArrays(int[] nums1, int[] nums2) {
    int l1 = nums1.length;
    int l2 = nums2.length;
    int l = l1 + l2;
    int index,j=0,k=0;
    int[] num=new int[l];//合并后的num

      index = (int) Math.floor(l / 2);
      for (int i = 0; i <= index; i++) {
        int il1 = 0;
        int il2 = 0;
        if (l1 > j) {
          il1 = nums1[j];
        }else {
          //此时,l1取完了，怎么办？给它个大值
          il1 = nums2[nums2.length - 1] + 1;
        }
        if (l2 > k) {
          il2 = nums2[k];
        }else{
          il2 = nums1[nums1.length - 1] + 1;
        }

        if (il1 <= il2) {
          num[i] = il1;
          j++;
        } else {
          num[i] = il2;
          k++;
        }
      }

    if (l % 2 == 1) { //奇数
      return num[index];
    } else {
      if (index > 0) {
        return (float)(num[index - 1] + num[index]) / 2;
      } else {
        return num[index];
      }
    }

  }

  public static void main(String[] args) {
    int[] nums1 = new int[]{1};
    int[] nums2 = new int[]{};
    System.out.println(new MedianSortedArray().findMedianSortedArrays(nums1,nums2));
  }
}
