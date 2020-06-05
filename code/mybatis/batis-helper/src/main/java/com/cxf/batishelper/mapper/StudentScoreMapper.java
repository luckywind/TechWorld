package com.cxf.batishelper.mapper;

import com.cxf.batishelper.model.StudentScore;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 
* Copyright (c) 2015 XiaoMi Inc. All Rights Reserved. 
* Authors: chengxingfu <chengxingfu@xiaomi.com>
* Date:2020-06-05 
*/
public interface StudentScoreMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(StudentScore record);

    int insertOrUpdate(StudentScore record);

    int insertOrUpdateSelective(StudentScore record);

    int insertSelective(StudentScore record);

    StudentScore selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(StudentScore record);

    int updateByPrimaryKey(StudentScore record);

    int updateBatch(List<StudentScore> list);

    int updateBatchSelective(List<StudentScore> list);

    int batchInsert(@Param("list") List<StudentScore> list);
}