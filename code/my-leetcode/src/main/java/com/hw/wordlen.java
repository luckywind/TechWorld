package com.hw;

import java.util.Scanner;

public class wordlen {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        if (scan.hasNextLine()) {
            String line = scan.nextLine();
            String[] split = line.split(" ");
            System.out.println(split[split.length-1].length());
        }

    }
}
