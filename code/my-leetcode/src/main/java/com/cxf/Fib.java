package com.cxf;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xxx.com>
 * @Date 2021-08-05
 * @Desc
 */
public class Fib {

  static int fib(int n) {
    if (n == 1 || n == 2) { //边界条件
      return 1;
    }
    /**
     * 假设上一个状态我们算出来了，
     * 下一个状态是什么？
     */
    return fib(n - 1) + fib(n - 2); //上一个状态到下一个状态的转换
  }

  public static void main(String[] args) {
//    System.out.println(fib(3));
    System.out.println(loopFib(4));
  }

  static int loopFib(int n) {
    if (n == 1 || n == 2) {
      return 1;
    }
    int n1=1, n2=1,tmp;  //n1是n-2,n2是n-1
    for (int i = 2; i < n; i++) {
      tmp = n1; //先把n1存起来，再更新它俩
      n1 = n2;
      n2 = tmp + n2;
      System.out.println("n1:"+n1+",n2:"+n2);
    }
    return n2;
  }
}
