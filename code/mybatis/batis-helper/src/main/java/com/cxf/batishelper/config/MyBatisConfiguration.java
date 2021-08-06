package com.cxf.batishelper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xxx.com>
 * Date:2020-05-27
 */
@Configuration
public class MyBatisConfiguration {
    @Bean
    public SQLStatsInterceptor sqlStatsInterceptor(){
        SQLStatsInterceptor sqlStatsInterceptor = new SQLStatsInterceptor();
        Properties properties = new Properties();
        properties.setProperty("dialect", "mysql");
        sqlStatsInterceptor.setProperties(properties);
        return sqlStatsInterceptor;
    }

    @Bean
    MyPageInterceptor pageInterceptor() {
        MyPageInterceptor myPageInterceptor = new MyPageInterceptor();
        Properties properties = new Properties();
        properties.setProperty("limit", "2");
        properties.setProperty("dbType", "mysql");
        myPageInterceptor.setProperties(properties);
        return myPageInterceptor;
    }
}