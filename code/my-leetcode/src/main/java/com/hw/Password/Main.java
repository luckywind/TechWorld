package com.hw.Password;


import java.util.Scanner;

// 注意类名必须为 Main, 不要有任何 package xxx 信息
public class Main {
  public static void main(String[] args) {
    Scanner in = new Scanner(System.in);
    // 注意 hasNext 和 hasNextLine 的区别
    while (in.hasNextLine()) { // 注意 while 处理多个 case
      String line=in.nextLine();
      if(line.length()<=8)
        System.out.println( "NG");
      line=line.replace(" ","");

      int n=0;

      if(line.matches("[0-9]"))n++;
      if(line.matches("[A-Z]"))n++;
      if(line.matches("[a-z]"))n++;
      if(line.matches("[^a-zA-Z0-9]"))n++;
      if(n>3)System.out.println("OK");
      else System.out.println( "NG");
    }
  }
}
