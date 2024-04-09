package com.cxf.data;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.cxf.data.mapper")
@SpringBootApplication
public class BootTransactionApplication {

    public static void main(String[] args) {
        SpringApplication.run(BootTransactionApplication.class, args);
    }

}
