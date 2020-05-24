package com.cxf.pool.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-05-24
 */
@Configuration
//表明这是线程池的配置类
@EnableAsync
public class ExecutorConfig {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorConfig.class);

    @Bean
    public Executor asyncServiceExecutor() {
        logger.info("start asyncServiceExecutor");
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        ThreadPoolTaskExecutor executor = new VisiableThreadPoolTaskExecutor();
        //Configure Number of Core Threads
        executor.setCorePoolSize(5);
        //Configure the maximum number of threads
        executor.setMaxPoolSize(5);
        //Configure queue size
        executor.setQueueCapacity(99999);
        //Configure the name prefix of threads in the thread pool
        executor.setThreadNamePrefix("async-service-");

        // rejection-policy: How to handle new tasks when pool has reached max size
        // CALLER_RUNS: Do not execute tasks in new threads, but in threads where the caller resides.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //Perform initialization
        executor.initialize();
        return executor;
    }
}