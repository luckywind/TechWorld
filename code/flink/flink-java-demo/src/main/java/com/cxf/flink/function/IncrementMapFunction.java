package com.cxf.flink.function;

import org.apache.flink.api.common.functions.MapFunction;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xxx.com>
 * @Date 2021-05-28
 * @Desc
 */
public class IncrementMapFunction implements MapFunction<Long, Long> {


  @Override
  public Long map(Long aLong) throws Exception {
    return aLong+1;
  }
}
