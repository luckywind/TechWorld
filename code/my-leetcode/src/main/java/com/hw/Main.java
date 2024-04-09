package com.hw;

import java.util.Scanner;

// 注意类名必须为 Main, 不要有任何 package xxx 信息
public class Main {
  public static void main(String[] args) {
    Scanner in = new Scanner(System.in);
    // 注意 hasNext 和 hasNextLine 的区别
    String str= "S87;S7;W56;S75;A8;S84;W23;W19;W40;D73;S87;A39;W97;W78;A53;D16;D15;A50;W41;S87;D47;W56;D56;A23;A91;S25;D61;D53;D58;W88;W58;S61;D69;W74;D89;A92;D39;D62;S78;W72;W73;W35;S76;W35;S36;W39;A4;"; //  in.nextLine();
    int left=0,right=0;
    String[] arr=str.split(";");
    for(int i=0;i<arr.length;i++){
      if(arr[i].length()<2) continue;//至少两个字符
      if(!arr[i].matches("[WASD][0-9]{1,2}")){
        continue;
      }
      String act= arr[i].substring(0,1);
      if(act.equals("A") && act.equals("S") && act.equals("W") && act.equals("D")) continue;
      String d=arr[i].substring(1);
      if(!(d.compareTo("0")>=0 && d.compareTo("99")<=0)) continue;
      int dist=Integer.valueOf(d);
      if(act.equals("A")) left-=dist;
      if(act.equals("D")) left+=dist;
      if(act.equals("S")) right-=dist;
      if(act.equals("W")) right+=dist;
    }
    System.out.println("("+left+","+right+")");

  }
}
