package com.cxf.batishelper.controller;

import com.cxf.batishelper.model.Student;
import com.cxf.batishelper.pojo.ResponseData;
import com.cxf.batishelper.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

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

    @RequestMapping(value = "/first")
    public ResponseData<Student> first() {
        Student student = studentService.selectByPrimaryKey(1);
        ResponseData<Student> responseData = new ResponseData<>();
        responseData.setData(student);
        responseData.setStatus(HttpStatus.OK.value());
        responseData.setSuccess(true);
        return responseData;
    }

    @RequestMapping(value = "/page/{currPage}/{pageSize}")
    public ResponseData<List<Student>> page(@PathVariable("currPage") Integer currPage,
                                            @PathVariable("pageSize") Integer pageSize) {

        List<Student> students = studentService.selectByPage(currPage,pageSize);
        ResponseData<List<Student>> responseData = new ResponseData<>();
        responseData.setData(students);
        responseData.setStatus(HttpStatus.OK.value());
        responseData.setSuccess(true);
        return responseData;
    }
}
