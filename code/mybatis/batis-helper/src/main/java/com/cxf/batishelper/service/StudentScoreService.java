package com.cxf.batishelper.service;

import java.util.List;
import com.cxf.batishelper.model.StudentScore;
    /** 
* Copyright (c) 2015 xxx Inc. All Rights Reserved. 
* Authors: chengxingfu <chengxingfu@xxx.com>
* Date:2020-06-05 
*/
public interface StudentScoreService{


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

    int batchInsert(List<StudentScore> list);

}
