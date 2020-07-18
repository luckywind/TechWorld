package com.cxf.data.service.impl;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import com.cxf.data.mapper.CourseMapper;
import com.cxf.data.model.Course;
import com.cxf.data.service.CourseService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
* Copyright (c) 2015 XiaoMi Inc. All Rights Reserved. 
* Authors: chengxingfu <chengxingfu@xiaomi.com>
* Date:2020-07-18 
*/
@Transactional(propagation = Propagation.NESTED)
@Service
public class CourseServiceImpl implements CourseService{

    @Resource
    private CourseMapper courseMapper;

    @Override
    public int deleteByPrimaryKey(Integer id) {

        return courseMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(Course record) {
        return courseMapper.insert(record);
    }

    @Override
    public int insertSelective(Course record) {
        return courseMapper.insertSelective(record);
    }

    @Override
    public Course selectByPrimaryKey(Integer id) {
        return courseMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByPrimaryKeySelective(Course record) {
        return courseMapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public int updateByPrimaryKey(Course record) {
        return courseMapper.updateByPrimaryKey(record);
    }

}
