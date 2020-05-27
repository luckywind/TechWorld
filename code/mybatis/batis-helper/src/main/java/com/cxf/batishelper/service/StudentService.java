package com.cxf.batishelper.service;

import com.cxf.batishelper.model.Student;

import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-05-17
 */
public interface StudentService {


    int deleteByPrimaryKey(Integer id);

    int insert(Student record);

    int insertOrUpdate(Student record);

    int insertOrUpdateSelective(Student record);

    int insertSelective(Student record);

    Student selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Student record);

    int updateByPrimaryKey(Student record);

    int updateBatch(List<Student> list);

    int batchInsert(List<Student> list);

    int updateBatchSelective(List<Student> list);

    List<Student> selectByPage(int currPage, int pageSize);
}







