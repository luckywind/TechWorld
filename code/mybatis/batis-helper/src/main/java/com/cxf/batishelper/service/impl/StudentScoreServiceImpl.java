package com.cxf.batishelper.service.impl;

import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import com.cxf.batishelper.mapper.StudentScoreMapper;
import com.cxf.batishelper.model.StudentScore;
import com.cxf.batishelper.service.StudentScoreService;
/** 
* Copyright (c) 2015 XiaoMi Inc. All Rights Reserved. 
* Authors: chengxingfu <chengxingfu@xiaomi.com>
* Date:2020-06-05 
*/
@Service
public class StudentScoreServiceImpl implements StudentScoreService{

    @Resource
    private StudentScoreMapper studentScoreMapper;

    @Override
    public int deleteByPrimaryKey(Integer id) {
        return studentScoreMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(StudentScore record) {
        return studentScoreMapper.insert(record);
    }

    @Override
    public int insertOrUpdate(StudentScore record) {
        return studentScoreMapper.insertOrUpdate(record);
    }

    @Override
    public int insertOrUpdateSelective(StudentScore record) {
        return studentScoreMapper.insertOrUpdateSelective(record);
    }

    @Override
    public int insertSelective(StudentScore record) {
        return studentScoreMapper.insertSelective(record);
    }

    @Override
    public StudentScore selectByPrimaryKey(Integer id) {
        return studentScoreMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByPrimaryKeySelective(StudentScore record) {
        return studentScoreMapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public int updateByPrimaryKey(StudentScore record) {
        return studentScoreMapper.updateByPrimaryKey(record);
    }

    @Override
    public int updateBatch(List<StudentScore> list) {
        return studentScoreMapper.updateBatch(list);
    }

    @Override
    public int updateBatchSelective(List<StudentScore> list) {
        return studentScoreMapper.updateBatchSelective(list);
    }

    @Override
    public int batchInsert(List<StudentScore> list) {
        return studentScoreMapper.batchInsert(list);
    }

}
