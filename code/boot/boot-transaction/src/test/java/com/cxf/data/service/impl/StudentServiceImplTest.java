package com.cxf.data.service.impl;

import com.cxf.data.model.Student;
import com.cxf.data.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xxx.com>
 * Date:2020-07-18
 */
@SpringBootTest
@RunWith(SpringRunner.class)
class StudentServiceImplTest {
    @Autowired
    private StudentService studentService;

    /**
     * REQUIRED 测试
     * 外层插入后，抛出除0异常，外层也跟着回滚
     */
    @Test
    void insert() {
        Student student = new Student();
        student.setId(1);
        student.setName("zhangsan");
        studentService.insert(student);
    }
}
