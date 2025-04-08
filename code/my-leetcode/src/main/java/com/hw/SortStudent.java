package com.hw;
import java.util.*;
/*
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
 * @Created: 2025/4/8 10:21
 * @LastEditors: chengxingfu(chengxf @ yusur.tech)
 * @LastEditTime: 2025/4/8 10:21
 * @copyright: Copyright(c) 2022-2022, YUSUR Technology Co., Ltd. Learn more at www.yusur.tech.
 */
public class SortStudent {
  public static void main(String[] args) {
    Scanner in = new Scanner(System.in);
    int n=in.nextInt(); in.nextLine();
    int op=in.nextInt(); in.nextLine();
    ArrayList<Student> sts=new ArrayList<>();
    for(int i=0;i<n;i++){
      Student s= new Student(in.next(),in.nextInt());
      in.nextLine();
      sts.add(s);
    }

    Collections.sort(sts, new Comparator<Student>() {
      @Override
      public int compare(Student o1, Student o2) {
        if(op==0){
          return o2.score-o1.score;
        }
        else {
          return o1.score-o2.score;
        }
      }
    });
    for(Student s:sts){
      System.out.println(s.name +" "+s.score);
    }


  }

}
class Student{
  String name;
  int score;
  String getName(){return this.name;}
  int getScore(){return this.score;}
  void setName(String name){this.name=name;}
  void setScore(int score){this.score=score;}
  Student(String name ,int score){
    this.name=name;
    this.score=score;
  }

}
