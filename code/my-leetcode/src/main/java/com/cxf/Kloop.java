package com.cxf;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xxx.com>
 * @Date 2021-08-05
 * @Desc
 */
public class Kloop {
    static     int[] arr=new int[100];
  public static void main(String[] args) {
    print_loop(2,10,2);
  }


  static  void print_loop(int k, int n, int total_k) {
    if (k == 0) {
      for (int i = total_k; i >= 1; i--) {
        if (i != total_k) System.out.print(" ");
        System.out.print(arr[i]);
      }
      System.out.println();
      return ;
    }
    for (int i = 1; i <= n; i++) {
      arr[k] = i;
      print_loop(k - 1, n, total_k);
    }
    return ;
  }

}
