package com.hw;

import java.util.ArrayList;
import java.util.Scanner;

public class wangqiangShoping {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String line = scanner.nextLine();
        int money = Integer.parseInt(line.split(" ")[0]);
        int goods = Integer.parseInt(line.split(" ")[1]);
        ArrayList<wangqiangShoping.goods> list = new ArrayList<>();
        for (int i = 0; i < goods; i++) {
            line = scanner.nextLine();
            int price = Integer.parseInt(line.split(" ")[0]);
            int ipt = Integer.parseInt(line.split(" ")[1]);
            int master = Integer.parseInt(line.split(" ")[2]);
            wangqiangShoping.goods tmpgood = new goods(price, ipt, master);
            list.add(tmpgood);
        }

        int max;
        int sum;
        for (int i = 0; i < list.size(); i++) {

        }


    }


    static class goods {
        int price;
        int ipt;
        int master;
        int buy_times;

        public goods(int price, int ipt, int master) {
            this.price = price;
            this.ipt = ipt;
            this.master = master;
        }

    }
}
