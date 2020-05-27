package com.cxf.moduleweb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan(value = "com.cxf.moduleweb.mapper")
@SpringBootApplication
public class ModuleWebApplication {



    public static void main(String[] args) {
        SpringApplication.run(ModuleWebApplication.class, args);
    }

}
