package com.cxf;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xxx.com>
 * @Date 2021-08-03
 * @Desc
 * https://leetcode-cn.com/problems/longest-palindromic-substring/solution/xiang-xi-tong-su-de-si-lu-fen-xi-duo-jie-fa-bao-gu/
 */
public class LongestPalind {
  public String longestPalindrome(String s) {
    if (s == null || s.length() < 1) return "";
    int start = 0, end = 0;
    for (int i = 0; i < s.length(); i++) {
      int len1 = expandAroundCenter(s, i, i); //以一个字符为中心扩展
      int len2 = expandAroundCenter(s, i, i + 1); //以两个字符中间为中心扩展
      int len = Math.max(len1, len2); //取最大
      if (len > end - start) {
        start = i - (len - 1) / 2;  //开始位置，就是中心位置-半个回文的位置
        end = i + len / 2;
      }
    }
    return s.substring(start, end + 1);
  }

  private int expandAroundCenter(String s, int left, int right) {
    int L = left, R = right;
    while (L >= 0 && R < s.length() && s.charAt(L) == s.charAt(R)) {
      L--;
      R++;
    }
    //循环结束时，L和R处的字符已经不相等了
    return R - L - 1;
  }



  public static void main(String[] args) {
    String str = "dsghdfgdfdfdsf";
    System.out.println(new LongestPalind().longestPalindrome(str));
  }
}
