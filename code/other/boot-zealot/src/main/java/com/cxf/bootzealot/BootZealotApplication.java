package com.cxf.bootzealot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.cxf.bootzealot.mapper")
@EnableConfigurationProperties
public class BootZealotApplication {

    static final Logger logger = LoggerFactory.getLogger(BootZealotApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BootZealotApplication.class, args);
        logger.info("running");
    }

}
