package com.cxf.batishelper.controller;

import com.cxf.batishelper.model.Student;
import com.cxf.batishelper.pojo.ResponseData;
import com.cxf.batishelper.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-05-18
 */
@RestController
@RequestMapping(value = "student")
public class StudentController {
    @Autowired
    StudentService studentService;

    @PostMapping(value = "/insert")
    public ResponseData<Integer> insert(@RequestBody Student student) {
        int insert = studentService.insert(student);
        ResponseData<Integer> responseData = new ResponseData<>();
        responseData.setData(insert);
        responseData.setStatus(HttpStatus.OK.value());
        responseData.setSuccess(true);
        return responseData;
    }
}
