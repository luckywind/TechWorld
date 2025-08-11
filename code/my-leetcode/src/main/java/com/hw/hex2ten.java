package com.hw;

import java.util.Scanner;

public class hex2ten {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            int num = Integer.parseInt(line.substring(2), 16);
            System.out.println(num);
        }
    }
}
