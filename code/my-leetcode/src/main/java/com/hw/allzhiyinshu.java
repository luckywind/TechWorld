package com.hw;


import java.util.ArrayList;
import java.util.Scanner;

public class allzhiyinshu {
    public static void main(String[] args) {
        Scanner scann = new Scanner(System.in);
        while (scann.hasNextLine()) {
            int num = Integer.parseInt(scann.nextLine());
            ArrayList<Integer> arr = new ArrayList<>();
            for (int i = 2; i<= num; i++)
            {
                if (num % i == 0 && isprime(i)) {
                    arr.add(i);
                }
            }
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < arr.size(); i++) {
                sb.append(arr.get(i) + " ");
            }
            System.out.println(sb.toString());
        }

    }

    public static boolean isprime(int n) {
        if (n == 2) {
            return true;
        }
        for (int i = 2; i < Math.sqrt(n); i++) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }

}
