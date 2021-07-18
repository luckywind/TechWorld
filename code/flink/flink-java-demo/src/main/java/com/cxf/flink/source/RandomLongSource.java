package com.cxf.flink.source;

import java.util.Random;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2021-05-28
 * @Desc
 */
public class RandomLongSource extends RichParallelSourceFunction {

  private volatile boolean cancelled = false; //注意volatile修饰
  private Random random;

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);
    random = new Random();
  }

  @Override
  public void run(SourceContext sourceContext) throws Exception {
    while (!cancelled) {
      Thread.sleep(1000);
      long nextLong = random.nextLong();
      synchronized (sourceContext.getCheckpointLock()) {
        sourceContext.collect(nextLong);
      }
    }

  }

  @Override
  public void cancel() {
    cancelled = true;
  }
}
