package com.cxf.batishelper.mapper;

import com.cxf.batishelper.model.Student;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-05-19
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

    List<Student> selectByMap(Map<String, Object> map);

    List<Student> selectByidAndName(@Param("id") Integer id, @Param("name") String name);

    List<Student> selectByNameAndAge(@Param("name") String name, @Param("age") Integer age);

    List<Student> selectByIdList(@Param("idCollection") Collection<Integer> idCollection);

    List<Student> selectByPage(@Param("param") Map param);

}
