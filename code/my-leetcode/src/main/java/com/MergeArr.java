package com;

/**
 * Copyright (c) 2015 xx Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xx.com>
 * @Date 2023-02-07
 * @Desc
 */
public class MergeArr {

  public static void main(String[] args) {


    int[] a=new int[]{1,3,6,7};
    int[] b=new int[]{2,4};

    int m=a.length, n=b.length;
    int[] res=new int[m+n];

    int j=0,k=0; //j遍历a, k遍历b
    for(int i=0;i<res.length;i++){
      if(j>=m){
        //a遍历完了
        res[i]=b[k];
        k++;
        continue;
      }

      if(k>=n){
        //b遍历完了
        res[i]=a[j];
        j++;
        continue;
      }


      if(a[j]<b[k]){
        res[i]=a[j];
        j++;
      }else{
        res[i]=b[k];
        k++;
      }

    }
    for(int i=0;i<res.length;i++){
      System.out.println(res[i]);
    }

  }

}
