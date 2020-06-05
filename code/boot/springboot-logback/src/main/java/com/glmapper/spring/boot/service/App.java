package com.glmapper.spring.boot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-06-05
 */
public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public void log(String msg) {
        log.info(msg);
    }
}
