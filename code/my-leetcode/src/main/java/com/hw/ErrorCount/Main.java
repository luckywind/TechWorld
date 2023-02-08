package com.hw.ErrorCount;

/**
 * Copyright (c) 2015 xx Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xx.com>
 * @Date 2023-02-06
 * @Desc
 */
import java.util.Scanner;
import java.util.*;
// 注意类名必须为 Main, 不要有任何 package xxx 信息
public class Main {
  public static void main(String[] args) {


    Scanner in = new Scanner(System.in);
    // 注意 hasNext 和 hasNextLine 的区别
    TreeMap<String,Integer> map=new TreeMap<>();
    while (in.hasNextInt()) { // 注意 while 处理多个 case
      String line=in.nextLine();

      String[] f=line.split(" ")[0].split("\\\\");
      String file=f[f.length-1];
      if(file.length()>16) file=file.substring(file.length()-16);
      String rn=line.split(" ")[1];
      String key=file+" "+rn;
      if(map.size()<8)
        map.put(key,map.getOrDefault(key,0));
    }

    for(String key:map.keySet()){
      System.out.println(key.split(" ")[0]+" "
          +key.split(" ")[1] + " " + map.get(key));

    }


  }
}
