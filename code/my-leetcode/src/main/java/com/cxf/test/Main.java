package com.cxf.test;/*
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

/**
 * @Description:
 * @Author: chengxingfu(chengxingfu @ yusur.tech)
 * @Created: 2025/3/14 08:31
 * @LastEditors: chengxingfu(chengxf @ yusur.tech)
 * @LastEditTime: 2025/3/14 08:31
 * @copyright: Copyright(c) 2022-2022, YUSUR Technology Co., Ltd. Learn more at www.yusur.tech.
 */
public class Main {

  public static void main(String[] args) {
    char[] ch = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    int[] arr=new int[26];
    for (int i = 0; i <ch.length ; i++) {
      System.out.println(String.valueOf(ch[i])+","+String.valueOf(ch[i]-'a'));
    }
    int max = Math.max(1, 2);
  }

}
