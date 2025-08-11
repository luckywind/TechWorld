package com.hw;

import java.util.*;

public class poker {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        ArrayList<String> list = new ArrayList<String>();
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            list.add(line);
            if (list.size() == 5) {
                String type = getType(list);
                System.out.println("牌型："+type);
                list.clear();
            }
        }
    }

    private static String getType(ArrayList<String> list) {
        String type="其他";
        int number;
        ArrayList nums = new ArrayList<Integer>();
        HashSet<Integer> numSet = new HashSet<>();
        ArrayList<String> color = new ArrayList<String>();
        Set<String> colSet = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            String line = list.get(i);
            String num = line.split(" ")[0];
            String cor = line.split(" ")[1];
            switch (num) {
                case "J": number = 11;
                    break;
                case "Q": number = 12;
                    break;
                case "K": number = 13;
                    break;
                case "A": number =14;
                default: number = Integer.parseInt(num);
            }
            nums.add(number);
            numSet.add(number);
            color.add(cor);
            colSet.add(cor);
            int colSetSize = colSet.size();
            int numSetSize = numSet.size();
            int max = (int)Collections.max(nums);
            int min = (int) Collections.min(nums);
            if (colSetSize == 1) {
                if (max == min + 4) {
                    type = "同花顺";
                } else {
                    type = "同花";
                }
            }
            if (numSetSize == 2) {
                type = "四条";
            }
            if (numSetSize == 3) {
                type = "三条";
            }
            if (numSetSize == 2) {
                type = "葫芦";
            }
            if (max == min + 4) {
                if (colSetSize == 1) {
                    type = "同花顺";
                } else {
                    type = "顺子";
                }
            }


        }

        return type;
    }
}
