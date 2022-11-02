package com.cxf.sort;

import java.util.Arrays;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-10-29
 * @Desc
 * 希尔排序，插入排序的升级版
 * 插入排序，需要一个一个往后移动，慢慢把插入的小数送到前面，
 * 希尔排序可以设置一个步长，得到一个子数组，子数组内使用插入排序
 * 当步长为1时就退化为简单插入排序，从而完成整体排序
 */
public class ShellSort {

  public static void main(String[] args) {
    int[] a = {1, 8, 2, 3, 1, 2};
//    shellSort(a);
    quickSort(a,0,a.length-1);
    System.out.println(Arrays.toString(a));


  }


  public static void shellSort(int[] arr) {
    int length = arr.length;
    int temp;
    for (int step = length / 2; step >= 1; step /= 2) {// 步长/2逐渐减少 产生多个子序列
      for (int i = step; i < length; i++) {  //对每个子序列按照插入排序的思想排序
        temp = arr[i];
        int j = i - step;
        while (j >= 0 && arr[j] > temp) {
          arr[j + step] = arr[j];
          j -= step;
        }
        arr[j + step] = temp;
      }
    }
  }


  public static void selectSort(int[] a) {
    int minIndex;
    int tmp;
    for (int i = 0; i < a.length; i++) {
      minIndex = i; //当前最小值索引

      for (int j = i; j < a.length; j++) {
        if (a[j]<a[minIndex]) { //找到更小的
          minIndex = j;
        }
      }

      tmp = a[i];
      a[i] = a[minIndex];
      a[minIndex] = tmp;
    }

  }


  /**
   *
   * @param numbers
   * @param start    排序区间起点
   * @param end     排序区间终点
   */
  public  static void quickSort(int[] numbers, int start, int end){
    if (start < end) {
      int base = numbers[start]; //pivot
      int temp;
      /**
       * 定义两个指针，分别找出逆序，并完成交换以解决逆序
       */
      int i=start,j=end;
      do {
          //i负责找比枢纽元大的
        while (numbers[i]<base && i<end)  i++;
          //j负责寻找比枢纽元小的
        while (numbers[j]>base && j>start) j--;

        if (i <= j) {
          temp = numbers[i];
          numbers[i] = numbers[j];
          numbers[j]=temp;
          i++;
          j--;
        }

      } while (i <= j);

      if (start < j) {
        quickSort(numbers, start,j);
      }

      if (end > i) {
        quickSort(numbers,i,end);
      }


    }
  }




}
