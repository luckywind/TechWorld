package com.cxf.mp.controller;

import com.cxf.mp.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2020-09-19
 * @Desc
 */
@RestController
@RequestMapping("/api")
public class ResponseStatusExceptionController {

  @GetMapping("/resourceNotFoundException2")
  public void throwException3() {
    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Sorry, the resourse not found!", new ResourceNotFoundException());

  }
}
