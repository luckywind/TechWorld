package com.cxf.batishelper;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan(value = "com.cxf.batishelper.mapper")
@SpringBootApplication
public class BatisHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatisHelperApplication.class, args);
    }

}
