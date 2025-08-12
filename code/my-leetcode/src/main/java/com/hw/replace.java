package com.hw;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Scanner;

public class replace {
    public static void main(String[] args) throws IOException {
        Scanner scan = new Scanner(System.in);
        String line = scan.next();
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(line)) {
            int len = line.length();
            if (len > 2) {
                String head = line.substring(0, 1);
                sb.append(head);
                String end = line.substring(len - 1);
                System.out.println(head);
                System.out.println(end);
                String center = line.substring(1, len - 2).replace("*", "");
                sb.append(center);
                sb.append(end);
            }
            System.out.println(sb.toString());
        }
    }
}
