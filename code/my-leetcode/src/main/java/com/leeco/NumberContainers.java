package com.leeco;

import java.util.HashMap;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-07-23
 * @Desc
 */
public class NumberContainers {

  private HashMap<Integer, Integer> map=new HashMap<>();

  //维护最小下标
  private HashMap<Integer, Integer> minindex=new HashMap<>();


  public void change(int index, int number) {
    int old_num;
    boolean dup = false;
    if (map.containsKey(index)) {
      old_num = map.get(index);
      dup = true;
    }
    map.put(index, number);

    //更新最小下标
    /**
     * 当前numer下标更新
     * 被替换的numer下标更新
     */
    Integer old_index = minindex.getOrDefault(number, index);
    int new_indx = old_index < index ? old_index : index;
    minindex.put(number, new_indx);

    if (dup) {

    }




  }

  public int find(int number) {
    return minindex.getOrDefault(number, -1);
  }


  public static void main(String[] args) {

  }

}
