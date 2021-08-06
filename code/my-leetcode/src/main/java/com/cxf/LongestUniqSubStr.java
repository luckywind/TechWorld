package com.cxf;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xxx.com>
 * @Date 2021-08-01
 * @Desc 给定一个字符串 s ，请你找出其中不含有重复字符的 最长子串 的长度。
 */
public class LongestUniqSubStr {
  public int lengthOfLongestSubstring(String s) {
    // 记录字符上一次出现的位置
    int[] last = new int[128];
    for(int i = 0; i < 128; i++) {
      last[i] = -1;
    }
    int n = s.length();

    int res = 0;
    int start = 0; // 窗口开始位置
    for(int i = 0; i < n; i++) {
      int index = s.charAt(i);
      start = Math.max(start, last[index] + 1);
      res   = Math.max(res, i - start + 1);
      last[index] = i;
    }

    return res;
  }

  public static void main(String[] args) {
    String str = "dsgfdhfgfdhjfkgjlfg;fglgh";
    System.out.println(new LongestUniqSubStr().lengthOfLongestSubstring(str));
  }

}
