package com.glmapper.spring.boot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-06-05
 */
@Service
public class TestLogService {
    private static Logger log = LoggerFactory.getLogger(TestLogService.class.getName());
    public void printLogToSpecialPackage() {
        log.info("service info ");
        log.error("service error ");

    }
}
