package com.cxf.batishelper.controller;

import com.cxf.batishelper.model.Student;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xxx.com>
 * Date:2020-06-28
 */
@RunWith(SpringRunner.class)
@SpringBootTest
class StudentControllerTest {
    @Autowired
    private StudentController studentController;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(studentController).build();
    }

    @Test
    void insert() throws Exception {
        Student student = new Student();
        student.setName("mock");
        student.setAge(18);
        Gson gson = new Gson();
        RequestBuilder builder = MockMvcRequestBuilders
                .post("/student/insert")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(gson.toJson(student));
        MvcResult result = mvc.perform(builder).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    void deleteTest() throws Exception {
        RequestBuilder builder = MockMvcRequestBuilders
                .delete("/student/1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON_UTF8);
        MvcResult result = mvc.perform(builder).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }


    @Test
    void update() throws Exception {
        Student student = new Student();
        student.setName("update");
        student.setAge(18);
        Gson gson = new Gson();
        RequestBuilder builder = MockMvcRequestBuilders
                .put("/student/1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(gson.toJson(student));
        MvcResult result = mvc.perform(builder).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    void select() throws Exception {
        RequestBuilder builder = MockMvcRequestBuilders
                .get("/student/1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON_UTF8);
        MvcResult result = mvc.perform(builder).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }



}
