
package com.cxf.data.mapper;

import com.cxf.data.model.Course;

/** 
* Copyright (c) 2015 xxx Inc. All Rights Reserved. 
* Authors: chengxingfu <chengxingfu@xxx.com>
* Date:2020-07-18 
*/
public interface CourseMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Course record);

    int insertSelective(Course record);

    Course selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Course record);

    int updateByPrimaryKey(Course record);
}