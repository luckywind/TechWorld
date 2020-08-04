package com.cxf.mp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan(value = "com.cxf.mp.mapper")
@SpringBootApplication
public class mpApp {

    public static void main(String[] args) {
        SpringApplication.run(mpApp.class, args);
    }

}
