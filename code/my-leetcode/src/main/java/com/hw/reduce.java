package com.hw;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class reduce {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        int nrow = Integer.parseInt(scan.nextLine());
        HashMap<Integer, Integer> result = new HashMap<>();
        String line = null;
        for (int i = 0; i < nrow; i++) {
                line = scan.nextLine();
                int key = Integer.parseInt(line.split(" ")[0]);
                int value = Integer.parseInt(line.split(" ")[1]);
                if (result.containsKey(key)) {
                    result.put(key, result.get(key) + value);
                } else {
                    result.put(key, value);
                }
        }
        Set<Map.Entry<Integer, Integer>> entries = result.entrySet();
        for (Map.Entry<Integer,Integer> entry: entries
             ) {
            System.out.println(entry.getKey()+" "+entry.getValue());
        }
    }
}
