package com.cxf.mp.exception;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2020-09-19
 * @Desc
 */
public class ErrorResponse {
  private String message;
  private String errorTypeName;
  public ErrorResponse(Exception e) {
    this(e.getClass().getName(), e.getMessage());
  }

  public ErrorResponse(String errorTypeName, String message) {
    this.errorTypeName = errorTypeName;
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getErrorTypeName() {
    return errorTypeName;
  }

  public void setErrorTypeName(String errorTypeName) {
    this.errorTypeName = errorTypeName;
  }



}
