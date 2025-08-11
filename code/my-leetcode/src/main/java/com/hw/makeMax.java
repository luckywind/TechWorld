package com.hw;

import java.util.*;

public class makeMax {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        String line = scan.nextLine();
        char[] chars = line.toCharArray();
//        HashSet<Integer> ints = new HashSet<>();
        SortedSet<Integer> ints = new TreeSet<>();
        for (int i = 0; i < chars.length; i++) {
            ints.add(Integer.parseInt(String.valueOf(chars[i])));
        }

        while (!ints.isEmpty()) {
            Integer last = ints.last();
            System.out.print(last);
            ints.remove(last);
        }


    }
}
