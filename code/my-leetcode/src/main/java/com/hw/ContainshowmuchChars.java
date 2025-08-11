package com.hw;

import java.util.Scanner;

public class ContainshowmuchChars {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);

            if (scan.hasNextLine()) {
                String line = scan.nextLine();
//                String str = line.split(" ")[0].toUpperCase();
//                String chr = line.split(" ")[1].toUpperCase();
                String str = line.toUpperCase();
                String chr = "";
                if (scan.hasNextLine()) {
                    chr = scan.nextLine().toUpperCase();
                }
                char[] chars = str.toCharArray();
                int count = 0;
                for (int i = 0; i < chars.length; i++) {
                    if (chr.equals(new String(String.valueOf(chars[i])))) {
                        count++;
                    }
                }
                System.out.println(count);
            }
    }
}



//class Main {
//    public static void main(String[] args)
//    {
//        int count = 0;
//        Scanner sc=new Scanner(System.in);
//        String string = sc.nextLine();
//        String sub = sc.nextLine();
//        char a = sub.charAt(sub.length()-1);
//        for(int i=0;i<string.length();i++)
//        {
//            if(string.charAt(i)==a)
//                count++;
//        }
//        System.out.println(count);
//    }
//
//}
