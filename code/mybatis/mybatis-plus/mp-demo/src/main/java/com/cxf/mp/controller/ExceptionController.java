package com.cxf.mp.controller;

import com.cxf.mp.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2020-09-19
 * @Desc
 */
@RestController
@RequestMapping("/api")
public class ExceptionController {

  @GetMapping("/illegalArgumentException")
  public void throwException() {
    throw new IllegalArgumentException();
  }

  @GetMapping("/resourceNotFoundException")
  public void throwException2() {
    throw new ResourceNotFoundException();
  }
}
