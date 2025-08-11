package com.hw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;

public class mingmingrandom {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        while (scan.hasNextLine()) {  //注意：有多组测试用例，要用循环处理

            int cnt = Integer.valueOf(scan.nextLine());
            HashSet<Integer> integers = new HashSet<>();
            for (int i = 0; i < cnt; i++) {
                integers.add(Integer.valueOf(scan.nextLine()));
            }
            ArrayList<Integer> list = new ArrayList<>(integers);
            Collections.sort(list);
            for (int i = 0; i < list.size(); i++) {
                System.out.println(list.get(i));
            }
        }
    }
}
