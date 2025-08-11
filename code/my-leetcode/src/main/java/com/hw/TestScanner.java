package com.hw;


import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;


/**
 * next()会按换行/空格分割
 * nextLine()按换行分割
 *
 * 注意：如果先用nextInt()， 控制台输入一个数字并回车， 实际上nextInt只读到那个数字，后面的回车符会算作下一个输入！！
 */
public class TestScanner {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
//        while (scan.hasNext()) {
//            String str1 = scan.next();
//            System.out.println("输入的数据为：" + str1);
//        }
//        scan.close();
//
        Integer line = scan.nextInt();
        System.out.println(line);

        while (scan.hasNextLine()) {
            String str2 = scan.nextLine();
            System.out.println("输入的数据为：" + str2);
            System.out.println(str2.split(" "));
        }
        scan.close();


    }

    public void test() {
        Scanner scan = new Scanner(System.in);
        int nrow = scan.nextInt();
        HashMap<Integer, Integer> result = new HashMap<>();
        String line = null;
//        for (int i = 0; i < nrow; i++) {
        while (scan.hasNextLine()) {
            line = scan.nextLine();
            System.out.println(line);
            int key = Integer.parseInt(line.split(" ")[0]);
            int value = Integer.parseInt(line.split(" ")[1]);
            if (result.containsKey(key)) {
                result.put(key, result.get(key) + value);
            } else {
                result.put(key, value);
            }
        }
//        }
        Set<Map.Entry<Integer, Integer>> entries = result.entrySet();
        for (Map.Entry<Integer,Integer> entry: entries
                ) {
            System.out.println(entry.getKey()+" "+entry.getValue());
        }
    }
}