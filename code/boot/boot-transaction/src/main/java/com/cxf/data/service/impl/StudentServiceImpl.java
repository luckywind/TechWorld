package com.cxf.data.service.impl;

import com.cxf.data.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import com.cxf.data.model.Student;
import com.cxf.data.mapper.StudentMapper;
import com.cxf.data.service.StudentService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
* Copyright (c) 2015 XiaoMi Inc. All Rights Reserved. 
* Authors: chengxingfu <chengxingfu@xiaomi.com>
* Date:2020-07-18 
*/
@Service
@Transactional(propagation = Propagation.REQUIRED)
public class StudentServiceImpl implements StudentService{
    @Autowired
    private CourseService courseService;

    @Resource
    private StudentMapper studentMapper;

    @Override
    public int deleteByPrimaryKey(Integer id) {
        return studentMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(Student record) {
        int insert = studentMapper.insert(record);
        courseService.deleteByPrimaryKey(1);
        int res = 1 / 0;
        return insert;
    }

    @Override
    public int insertSelective(Student record) {
        return studentMapper.insertSelective(record);
    }

    @Override
    public Student selectByPrimaryKey(Integer id) {
        return studentMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByPrimaryKeySelective(Student record) {
        return studentMapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public int updateByPrimaryKey(Student record) {
        return studentMapper.updateByPrimaryKey(record);
    }

}
