package com.hw;

import java.util.*;

public class xwjm {
    public static void main(String[] args) {


        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNextLine()) {
            String src = scanner.nextLine().toLowerCase();
            HashMap<String, Integer> liter = new HashMap<>();

            liter.put("a", 1);
            liter.put("b", 2);
            liter.put("c", 3);
            liter.put("d", 4);
            liter.put("e", 5);
            liter.put("f", 6);
            liter.put("g", 7);
            liter.put("h", 8);
            liter.put("i", 9);
            liter.put("j", 10);
            liter.put("k", 11);
            liter.put("l", 12);
            liter.put("m", 13);
            liter.put("n", 14);
            liter.put("o", 15);
            liter.put("p", 16);
            liter.put("q", 17);
            liter.put("r", 18);
            liter.put("s", 19);
            liter.put("t", 20);
            liter.put("u", 21);
            liter.put("v", 22);
            liter.put("w", 23);
            liter.put("x", 24);
            liter.put("y", 25);
            liter.put("z", 26);
            HashMap<String, Integer> shu = new HashMap<>();
            shu.put("zero", 0);
            shu.put("one", 1);
            shu.put("two", 2);
            shu.put("three", 3);
            shu.put("four", 4);
            shu.put("five", 5);
            shu.put("six", 6);
            shu.put("seven", 7);
            shu.put("eight", 8);
            shu.put("nine", 9);
            String[] zero = "zero".split("");
            HashMap<String, List<String>> shuMapArr = new HashMap<>();

            //英文转换为 字母 list
            Set<Map.Entry<String, Integer>> entries = shu.entrySet();
            for (Map.Entry<String, Integer> entry : entries) {
                shuMapArr.put(entry.getKey(),Arrays.asList(entry.getKey().split("")));
            }

            //输入字符串转为 字母 list
            String[] srcArr = src.split("");
            ArrayList<String> srcList = new ArrayList<String>();
            for (int i = 0; i < srcArr.length; i++) {
                srcList.add(srcArr[i]);
            }


            Set<Map.Entry<String, List<String>>> entries1 = shuMapArr.entrySet();
            //记录字符串出现次数
            HashMap<String, Integer> shuMaptimes = new HashMap<>();
            int times=0;


            for (Map.Entry<String, List<String>> entry : entries1) {
                times=0;
                while (srcList.containsAll(entry.getValue())) {
                    shuMaptimes.put(entry.getKey(), ++times);
                    //这里需要循环下remove多少次
                    srcList.removeAll(entry.getValue());
                }
            }

            /**
             * 如果此时srcList为空 ，说明统计正确，否则要修改remove的次数
             */

            //把统计结果拼接
            Set<Map.Entry<String, Integer>> entries2 = shuMaptimes.entrySet();
            StringBuilder  sb=new StringBuilder();
            for (Map.Entry<String, Integer> entry : entries2) {
                if (entry.getValue() > 0) {
                    for (int i = 0; i < entry.getValue(); i++) {
                        sb.append(shu.get(entry.getKey()));
                    }
                }
            }

            System.out.println(sb.toString());

        }
    }
}
