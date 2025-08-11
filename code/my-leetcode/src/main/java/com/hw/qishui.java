package com.hw;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.Scanner;

public class qishui {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(new InputStreamReader(new FileInputStream(args[0])));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                int num = Integer.parseInt(line);
                System.out.println(num/2);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
