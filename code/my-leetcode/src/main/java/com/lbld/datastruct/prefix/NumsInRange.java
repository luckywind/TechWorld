package com.lbld.datastruct.prefix;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-07-23
 * @Desc
 */
public class NumsInRange {

  private int numsBetween(int[] scores,int start,int end){
    //scores数组中在start,end之间的有多少个？
    // 试卷满分 100 分
    int[] count = new int[100 + 1];
// 记录每个分数有几个同学
    for (int score : scores) {
      count[score]++;
    }
// 构造前缀和 ，  这里i位置累加的已经包含分数为i的同学了
    for (int i = 1; i < count.length; i++)
      count[i] = count[i] + count[i-1];

// 利用 count 这个前缀和数组进行分数段查询
    return count[end] - count[start-1];
  }
  public static void main(String[] args) {
    NumsInRange inRange = new NumsInRange();
    int i = inRange.numsBetween(new int[]{1, 2, 3, 4, 5, 6, 7}, 2, 4);
    System.out.println(i);
  }

}
