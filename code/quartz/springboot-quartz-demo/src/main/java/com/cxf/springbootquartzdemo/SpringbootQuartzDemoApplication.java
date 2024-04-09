package com.cxf.springbootquartzdemo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class SpringbootQuartzDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootQuartzDemoApplication.class, args);
    }
}
