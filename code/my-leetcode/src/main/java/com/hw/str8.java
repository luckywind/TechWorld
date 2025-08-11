package com.hw;

import java.util.ArrayList;
import java.util.Scanner;

public class str8 {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        while (scan.hasNextLine()) {
            String str1 = scan.nextLine();

            String zero = "00000000";
            StringBuilder sb = new StringBuilder(zero);
            ArrayList<String> trunks = new ArrayList<>();
            int times = str1.length() / 8;
            int tails = str1.length() % 8;
            for (int i = 0; i < times; i++) {
                trunks.add(str1.substring(i * 8,(i+1)*8 ));
            }
            if (tails>0) {
                trunks.add(sb.replace(0, tails,str1.substring(times*8,times*8+tails)).toString());
            }

            for (int i = 0; i < trunks.size(); i++) {
                String s = trunks.get(i);
                System.out.println(s);
            }
        }

    }
}
