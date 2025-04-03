package com.hw;/*
 * Copyright 2023-2025, YUSUR Technology Co., Ltd.
 *
 * Licensed under the YUSUR License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://gitee.com/yusur_2018/spark-race/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Scanner;

/**
 * @Description:
 * @Author: chengxingfu(chengxingfu @ yusur.tech)
 * @Created: 2025/4/3 08:17
 * @LastEditors: chengxingfu(chengxf @ yusur.tech)
 * @LastEditTime: 2025/4/3 08:17
 * @copyright: Copyright(c) 2022-2022, YUSUR Technology Co., Ltd. Learn more at www.yusur.tech.
 */
public class CutArr {

  public static void main(String[] args) {
    Scanner in = new Scanner(System.in);
    int n=in.nextInt();
    in.nextLine();
    int[] nums=new int[n];
    int[] zhengshu=new int[n];//记录正数
    for(int i=0;i<n;i++){
      nums[i]=in.nextInt();
      zhengshu[i]=1;
    }
    int[] presum=new int[n];//前缀和数组
    int[] zspresum=new int[n];//zhengshu的前缀和
    int res=0;
    int sum=0;
    int zssum=0;
    for(int i=0;i<n;i++){
      sum+=nums[i];
      presum[i]=sum;
      zssum+=zhengshu[i];
      zspresum[i]=zssum;
    }
    // 分别代表两个刀，l < r  把数组分成[0..l-1] [l,..r-1] [r,..n-1] 也就是在起点处切分
    // int l=1,r=n-2;
    for(int l=1;l<n-3;l++){ //l必须从第二个开始，倒数第三个结束
      for(int r=l+1;r<n-2;r++){//r必须比l大，且倒数第二个结束
        //计算各部分的和 以及正数个数
        int p1=presum[l-1], pn1=zspresum[l-1];
        int p2=presum[r-1]-p1,pn2=zspresum[r-1]-pn1;
        int p3=presum[n-1]-p2,pn3=zspresum[n-1]-pn2;
        if((pn1>0 && pn2>0) && ((pn3>0 && p1==p2 )&& ( p1==p3))){
          res++;
        }


      }
    }
    System.out.println(res);


  }

}
