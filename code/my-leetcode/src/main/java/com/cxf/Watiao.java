package com.cxf;

import java.util.HashMap;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2021-08-05
 * @Desc
 */
public class Watiao {

   public int numWays(int n) {
     HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
     if (n == 1) {
       return 1;
     }
     if (n == 2) {
       return 2;
     }
     if (map.containsKey(n)) {
       return map.get(n);
     } else {
       map.put(n, numWays(n - 1) + numWays(n - 2));
       return map.get(n);
     }
  }
  public static void main(String[] args) {
    System.out.println(new Watiao().numWays(10));
  }

}
