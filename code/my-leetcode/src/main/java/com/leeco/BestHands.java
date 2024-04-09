package com.leeco;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-07-23
 * @Desc
 */
public class BestHands {
  public String bestHand(int[] ranks, char[] suits) {
    String res = "High Card";
    //验证是否同花：5张花色相同
    int sameCnt = 1;
    for (int i = 1; i < suits.length; i++) {
      if (suits[i] == suits[0]) {
        sameCnt += 1;
      }
    }
    if (sameCnt == 5) {
      return "Flush";
    }

    //计算每个数字出现的次数
    int[] count = new int[14];
    for (int i = 0; i < ranks.length; i++) {
      count[ranks[i]] += 1;
    }

    for (int i = 0; i < count.length; i++) {
      if (count[i] >= 3) {
        return "Three of a Kind";
      }
    }

    for (int i = 0; i < count.length; i++) {
      if (count[i] >= 2) {
        return "Pair";
      }
    }



    return res;

  }

  public static void main(String[] args) {
    BestHands hands = new BestHands();
    System.out.println(hands.bestHand(new int[]{13, 2, 3, 1, 9}, "aaaaa".toCharArray()));
    System.out.println(hands.bestHand(new int[]{4,4,2,4,4}, "daabc".toCharArray()));
    System.out.println(hands.bestHand(new int[]{10,10,2,12,9}, "abcad".toCharArray()));
    System.out.println(hands.bestHand(new int[]{10,7,2,12,9}, "abcad".toCharArray()));
  }

}
