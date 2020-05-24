package com.cxf.batishelper.service.impl;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import com.cxf.batishelper.model.Student;

import java.util.List;

import com.cxf.batishelper.mapper.StudentMapper;
import com.cxf.batishelper.service.StudentService;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-05-17
 */
@Service
public class StudentServiceImpl implements StudentService {

    @Resource
    private StudentMapper studentMapper;

    @Override
    public int deleteByPrimaryKey(Integer id) {
        return studentMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(Student record) {
        return studentMapper.insert(record);
    }

    @Override
    public int insertOrUpdate(Student record) {
        return studentMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(Student record) {
        return studentMapper.insertOrUpdateSelective(record);
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

    @Override
    public int updateBatch(List<Student> list) {
        return studentMapper.updateBatch(list);
    }

    @Override
    public int batchInsert(List<Student> list) {
        return studentMapper.batchInsert(list);
    }

    public int updateBatchSelective(List<Student> list) {
        return studentMapper.updateBatchSelective(list);
    }
}







