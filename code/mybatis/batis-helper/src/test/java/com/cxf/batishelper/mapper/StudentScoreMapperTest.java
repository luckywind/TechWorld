package com.cxf.batishelper.mapper;

import com.cxf.batishelper.model.StudentScore;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xxx.com>
 * Date:2020-06-05
 */
@RunWith(SpringRunner.class)
@SpringBootTest
class StudentScoreMapperTest {
    @Autowired
    StudentScoreMapper mapper;
    @Test
    void insertOrUpdate() {
        StudentScore studentScore = new StudentScore();
        studentScore.setCourseId(1);
        studentScore.setStudentId(1);
        studentScore.setScore(98);
        mapper.insertOrUpdate(studentScore);
    }
}