package com.cxf.batishelper.pojo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-05-19
 */
public class Test {
    public static void main(String[] args) {
//        System.out.println(Integer.parseInt("3000000"));
        Date date = new Date();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDateTime now = LocalDateTime.now();
        System.out.println(dtf.format(now));


        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plus(1, ChronoUnit.DAYS);
        LocalDate yesterday = tomorrow.minusDays(2);

        System.out.println(today);
        System.out.println(dtf.format(today));
        System.out.println(tomorrow);
        System.out.println(yesterday);

        String sql = "CREATE TABLE `doris_ot_%s_event` (\n" +
                "    `olap_date` int(11) NULL COMMENT \"分区日期\",\n" +
                "    `event_name` varchar(256) NULL COMMENT \"事件名称\",\n" +
                "    `time` bigint(20) NULL COMMENT \"时间戳\",\n" +
                "    `distinct_id` varchar(256) NULL COMMENT \"用户标识id\",\n" +
                "\n" +
                "    `tip_id` varchar(256) NULL COMMENT \"需求id\",\n" +
                "\n" +
                "    `tip_module_id` varchar(256) NULL COMMENT \"模块id\",\n" +
                "    `tip_module_name` varchar(256) NULL COMMENT \"模块名称\",\n" +
                "    `tip_name` varchar(256) NULL COMMENT \"需求名称\",\n" +
                "    `tip_page_id` varchar(256) NULL COMMENT \"页面id\",\n" +
                "    `tip_page_name` varchar(256) NULL COMMENT \"页面名称\",\n" +
                "    `tip_pos` varchar(256) NULL COMMENT \"顺序id\",\n" +
                "    `tip_sub_module_id` varchar(256) NULL COMMENT \"透传过来的子模块id\",\n" +
                "    `tip_sub_page_id` varchar(256) NULL COMMENT \"透传过来的子页面id\",\n" +
                "    `ref_tip_id` varchar(256) NULL COMMENT \"透传过来的需求id\",\n" +
                "    `ref_tip_module_id` varchar(256) NULL COMMENT \"透传过来的模块id\",\n" +
                "    `ref_tip_module_name` varchar(256) NULL COMMENT \"透传过来的模块名称\",\n" +
                "    `ref_tip_name` varchar(256) NULL COMMENT \"透传过来的需求名称\",\n" +
                "    `ref_tip_page_id` varchar(256) NULL COMMENT \"透传过来的页面id\",\n" +
                "    `ref_tip_page_name` varchar(256) NULL COMMENT \"透传过来的页面名称\",\n" +
                "    `ref_tip_pos` varchar(256) NULL COMMENT \"透传过来的需求id\",\n" +
                "    `ref_tip_sub_module_id` varchar(256) NULL COMMENT \"透传过来的子模块id\",\n" +
                "    `ref_tip_sub_page_id` varchar(256) NULL COMMENT \"透传过来的子页面id\"\n" +
                "    ) ENGINE=OLAP\n" +
                "    DUPLICATE KEY(`olap_date`, `event_name`, `time`, `distinct_id`)\n" +
                "COMMENT \"OLAP\"\n" +
                "PARTITION BY RANGE(`olap_date`)\n" +
                "(PARTITION %s VALUES [(\"-2147483648\"), (\"%s\")))\n" +
                "                                        DISTRIBUTED BY HASH(`distinct_id`) BUCKETS %s\n" +
                "                                        PROPERTIES (\n" +
                "                                        \"storage_type\" = \"COLUMN\"\n" +
                "                                        );";
        System.out.println(String.format(sql,"tbname",dtf.format(today),dtf.format(tomorrow),20));

    }
}
