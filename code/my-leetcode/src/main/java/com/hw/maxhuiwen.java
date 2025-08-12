package com.hw;

import java.util.HashMap;

public class maxhuiwen {
    public static void main(String[] args) {
        String str = "gabcdcbaef";
        HashMap<Integer, String> huiwens = new HashMap<>();
        int max=1;
        for (int begin = 0; begin < str.length() - 1; begin++) {
            for (int end = begin + 2; end < str.length()+1; end++) {
                String substring = str.substring(begin, end);
                if (ishuiwen(substring)) {
                    huiwens.put(substring.length(), substring);
                    System.out.println("hunwen");
                    if (substring.length() >max) {
                        max = substring.length();
                    }
                }

            }
        }

        System.out.println(huiwens.get(max));
    }
    public static boolean ishuiwen(String str) {
        String rev = new StringBuffer(str).reverse().toString();
        return str.equals(rev);
    }
}
