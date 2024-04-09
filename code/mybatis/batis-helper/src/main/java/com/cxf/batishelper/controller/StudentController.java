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
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xxx.com>
 * Date:2020-05-18
 */
@RestController
@RequestMapping(value = "student")
public class StudentController {
    @Autowired
    StudentService studentService;

    /**
     * 增
     * @param student
     * @return
     */
    @PostMapping(value = "/insert")
    public ResponseData<Integer> insert(@RequestBody Student student) {
        int insert = studentService.insert(student);
        ResponseData<Integer> responseData = new ResponseData<>();
        responseData.setData(insert);
        responseData.setStatus(HttpStatus.OK.value());
        responseData.setSuccess(true);
        return responseData;
    }

    /**
     * 删
     * @param id
     * @return
     */
    @RequestMapping(value = "/{id}",method = RequestMethod.DELETE,produces = "application/json")
    public ResponseData<Object> delStudent(@PathVariable Integer id) {
        int i = studentService.deleteByPrimaryKey(id);
        ResponseData<Object> responseData = new ResponseData<>();
        responseData.setData(i);
        responseData.setStatus(HttpStatus.OK.value());
        responseData.setSuccess(true);
        return responseData;
    }

    /**
     * 改
     * @param id
     * @param student
     * @return
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, produces = "application/json")
    public ResponseData<Integer> update(@PathVariable Integer id, @RequestBody Student student) {
        student.setId(id);
        int insert = studentService.insertOrUpdateSelective(student);
        ResponseData<Integer> responseData = new ResponseData<>();
        responseData.setData(insert);
        responseData.setStatus(HttpStatus.OK.value());
        responseData.setSuccess(true);
        return responseData;
    }



    /**
     * 查
     * @param id
     * @return
     */
    @RequestMapping(value = "/{id}",method = RequestMethod.GET,produces = "application/json")
    public ResponseData<Student> getStudent(@PathVariable Integer id) {
        Student student = studentService.selectByPrimaryKey(id);
        ResponseData<Student> responseData = new ResponseData<>();
        responseData.setData(student);
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
