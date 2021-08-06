package util;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xxx.com>
 * @Date 2021-02-23
 * @Desc
 */
public class EmeiUtil {

    /**
     * 判断IMEI号是否合法，合法返回true，不合法返回false
     *
     * @param imei IMEI号
     * @return if 合法 true else false
     */
    public static Boolean isCorrectImei(String imei) {
        final int imeiLength = 15;
        if (imei.length() == imeiLength) {
            int check = Integer.valueOf(imei.substring(14));
            imei = imei.substring(0, 14);
            char[] imeiChar = imei.toCharArray();
            int resultInt = 0;
            for (int i = 0; i < imeiChar.length; i++) {
                int a = Integer.parseInt(String.valueOf(imeiChar[i]));
                i++;
                final int temp = Integer.parseInt(String.valueOf(imeiChar[i])) * 2;
                final int b = temp < 10 ? temp : temp - 9;
                resultInt += a + b;
            }
            resultInt %= 10;
            resultInt = resultInt == 0 ? 0 : 10 - resultInt;
            if (resultInt == check) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println(isCorrectImei("00558f25f3a72c852c0def47cb3a4dfe"));
        System.out.println(isCorrectImei("093d587c828e185e4f43ee1368f38c7c"));
    }
}
