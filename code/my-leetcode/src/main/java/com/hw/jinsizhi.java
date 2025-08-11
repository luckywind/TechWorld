package com.hw;

import java.util.Scanner;

public class jinsizhi {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] split = line.split("\\.");
            if (Integer.parseInt(split[1].substring(0, 1)) >= 5) {
                System.out.println(Integer.parseInt(split[0]) + 1);
            } else {
                System.out.println(Integer.parseInt(split[0]));
            }
        }
    }
}
