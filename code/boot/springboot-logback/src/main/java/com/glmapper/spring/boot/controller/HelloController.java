package com.glmapper.spring.boot.controller;

import com.glmapper.spring.boot.service.TestLogService;
import com.glmapper.spring.boot.task.MyTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-06-05
 */
@RestController
public class HelloController {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(HelloController.class);
    @Autowired
    private TestLogService testLogService;

    @GetMapping("/hello")
    public String hello(){
        LOGGER.info("GLMAPPER-SERVICE:info");
        LOGGER.error("GLMAPPER-SERVICE:error");
        testLogService.printLogToSpecialPackage();
        MyTask myTask = new MyTask();
        myTask.run();
        return "hello spring boot";
    }


}
