package com.glmapper.spring.boot.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-06-05
 */
public class MyTask {
    private static final Logger log = LoggerFactory.getLogger(MyTask.class.getName());

    public void run() {
        log.info("task info ");
        log.error("task error");
    }
}
