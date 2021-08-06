package com.cxf.data.service;

import com.cxf.data.model.Student;
    /** 
* Copyright (c) 2015 xxx Inc. All Rights Reserved. 
* Authors: chengxingfu <chengxingfu@xxx.com>
* Date:2020-07-18 
*/
public interface StudentService{


    int deleteByPrimaryKey(Integer id);

    int insert(Student record);

    int insertSelective(Student record);

    Student selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Student record);

    int updateByPrimaryKey(Student record);

}
