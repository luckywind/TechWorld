package com.hw;

import java.util.ArrayList;
import java.util.Scanner;

public class chicken {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextInt()) {
            int i1 = scanner.nextInt();
            ArrayList<String> res = new ArrayList<>();
            int N=100;
            int jg,jm,jc;
            //  5  3   1/3
            // i

            for (int i = 0; i < N ; i+=3) {
                for (jg = 0; jg < N /5; jg++) {
                    for (jm = 0; jm < N / 3; jm++) {
                        if (i / 3 + jg * 5 + jm * 3 == 100) {
                            if (i + jg + jm == 100) {
                                res.add(jg + " " + jm + " " + i);
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < res.size(); i++) {
                String s =  res.get(i);
                System.out.println(s);
            }

        }
    }
}
