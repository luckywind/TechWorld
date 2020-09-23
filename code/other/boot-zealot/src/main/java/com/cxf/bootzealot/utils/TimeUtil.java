package com.cxf.bootzealot.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-07-16
 */
public class TimeUtil {
    public static void main(String[] args) {
//        currentTime();
//        with();
//        of();
        parse();
//        format();
//        ofpattern();
    }

    private static void ofpattern() {
        ZonedDateTime now = ZonedDateTime.now();
        System.out.println("当前时间是: " + now);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd H:m:s");
        System.out.println("另一种表示形式:" + now.format(formatter));
    }

    private static void format() {
        ZonedDateTime now = ZonedDateTime.now();
        System.out.println("当前时间是: " + now);
        System.out.println("另一种表示形式:" + now.format(DateTimeFormatter.RFC_1123_DATE_TIME));
    }

    private static void parse() {
        LocalDateTime datetime = LocalDateTime.parse("2012-10-10T21:58:00");
        System.out.println("日期时间是:" + datetime);

        LocalDate date = LocalDate.parse("2012-10-10");
        System.out.println("日期是: " + date);

        LocalTime time = LocalTime.parse("21:58:01");
        System.out.println("时间是: " + time);


        System.out.println("格式化与解析");
        ZonedDateTime now = ZonedDateTime.now();
        System.out.println("当前时间是: " + now);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd H:m:s");
        String text = now.format(formatter);
        System.out.println("另一种表示形式:" + text );
        LocalDateTime parsed = LocalDateTime.parse(text, formatter);
        System.out.println("解析后:" + parsed );
    }

    private static void of() {
        LocalDate date = LocalDate.of(2018, Month.OCTOBER, 01);
        System.out.println("日期是: " + date);
        LocalTime time = LocalTime.of(22, 15);
        System.out.println("时间是: " + time);
    }

    private static void with() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime date2 = now.withDayOfMonth(10).withYear(2021);
        System.out.println("新的日期时间: "+date2);
    }

    private static void currentTime() {
        LocalDateTime now = LocalDateTime.now();
        System.out.println(now);
        System.out.println("toLocalDate: "+now.toLocalDate());
        System.out.println("toLocalTime: "+now.toLocalTime());
        System.out.println("getYear: "+now.getYear());
        System.out.println("getDayOfYear: "+now.getDayOfYear());
        System.out.println("getDayOfWeek: "+now.getDayOfWeek());
        System.out.println("getMonth: "+now.getMonth());
        System.out.println("getDayOfMonth: "+now.getDayOfMonth());
        System.out.println("getHour: "+now.getHour());
        System.out.println("getMinute: "+now.getMinute());
        System.out.println("getSecond: "+now.getSecond());
    }
}
