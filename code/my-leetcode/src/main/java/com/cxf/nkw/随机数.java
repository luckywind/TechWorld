package com.cxf.nkw;

import java.util.HashSet;
import java.util.Scanner;

// 注意类名必须为 Main, 不要有任何 package xxx 信息
public class 随机数 {
  public static void main(String[] args) {
    Scanner in = new Scanner(System.in);
    // 注意 hasNext 和 hasNextLine 的区别
    int n=in.nextInt();
    int tmp=0;

    HashSet<Integer> set = new HashSet<>();
    for(int i=0;i<n;i++){
      tmp=in.nextInt();
      set.add(tmp);
    }

//    int[] arr=new int[set.size()];
    Integer[] arr = set.toArray(new Integer[0]);

    //对arr排序
    int target=0;
    for(int i=0;i<arr.length;i++){
      target=arr[i];
      int j=i;
      while(j>=0){
        if(arr[j]>arr[i]){
          tmp=arr[j];
          arr[j]=target;
          arr[i]=tmp;
        }
        j--;
      }
    }

    for(int i=0;i<arr.length;i++){
      System.out.println(arr[i]);
    }


  }
}
