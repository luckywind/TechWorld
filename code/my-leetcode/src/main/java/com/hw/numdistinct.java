package com.hw;


import java.util.HashSet;
import java.util.Scanner;

public class numdistinct {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int num = scanner.nextInt();
        HashSet<Integer> integers = new HashSet<>();
        int res=0;
        int tmp;
        while (num / 10 > 0) {
            tmp = num % 10;
            if (! integers.contains(tmp)) {
                integers.add(tmp);
                res=res*10 +tmp;
            }
            num=num/10;
        }
        tmp=num;
        if (! integers.contains(tmp)) {
            integers.add(tmp);
            res=res*10 +tmp;
        }
        System.out.println(res);
    }
}
