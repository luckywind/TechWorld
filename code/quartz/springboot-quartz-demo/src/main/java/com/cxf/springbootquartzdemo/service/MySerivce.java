package com.cxf.springbootquartzdemo.service;

import org.springframework.stereotype.Service;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-05-23
 */
@Service
public class MySerivce {
    public String sayHello(String name) {
        return "Hello " + name;
    }
}
