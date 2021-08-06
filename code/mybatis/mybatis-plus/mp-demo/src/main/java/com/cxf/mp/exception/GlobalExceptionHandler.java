package com.cxf.mp.exception;

import com.cxf.mp.controller.ExceptionController;
import com.cxf.mp.controller.UserController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xxx.com>
 * @Date 2020-09-19
 * @Desc
 */

@ControllerAdvice(assignableTypes = {ExceptionController.class})
//@ControllerAdvice() //全局异常处理类
@ResponseBody
public class GlobalExceptionHandler {

  ErrorResponse illegalArgumentResponse = new ErrorResponse(new IllegalArgumentException("参数错误!"));
  ErrorResponse resourseNotFoundResponse = new ErrorResponse(new ResourceNotFoundException("Sorry, the resourse not found!"));

  @ExceptionHandler(value = Exception.class)// 拦截所有异常, 这里只是为了演示，一般情况下一个方法特定处理一种异常
  public ResponseEntity<ErrorResponse> exceptionHandler(Exception e) {

    if (e instanceof IllegalArgumentException) {
      return ResponseEntity.status(400).body(illegalArgumentResponse);
    } else if (e instanceof ResourceNotFoundException) {
      return ResponseEntity.status(404).body(resourseNotFoundResponse);
    }
    return null;
  }
}
