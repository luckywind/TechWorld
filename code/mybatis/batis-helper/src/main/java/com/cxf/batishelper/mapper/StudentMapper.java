package com.cxf.batishelper.mapper;

import com.cxf.batishelper.domain.Student;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-05-17
 */
public interface StudentMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Student record);

    int insertOrUpdate(Student record);

    int insertOrUpdateSelective(Student record);

    int insertSelective(Student record);

    Student selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Student record);

    int updateByPrimaryKey(Student record);

    int updateBatch(List<Student> list);

    int updateBatchSelective(List<Student> list);

    int batchInsert(@Param("list") List<Student> list);

    List<Student> mySelectByName(@Param("name") String name);
}