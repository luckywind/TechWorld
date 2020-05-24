package com.cxf.dao;

import com.cxf.utils.MybatisUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-05-11
 */
public class EmployeeDao implements EmployeeMapper {
    final Logger log = Logger.getLogger(EmployeeDao.class);
    MybatisUtils mybatisUtils = new MybatisUtils();
    SqlSession sqlSession = null;

    @Override
    public int deleteByPrimaryKey(Integer id) {
        return 0;
    }

    @Override
    public int insert(Employee record) {
        int insert = 0;
        try {
            sqlSession = mybatisUtils.getSqlSession();
            EmployeeMapper employeeMapper = sqlSession.getMapper(EmployeeMapper.class);
            insert = employeeMapper.insert(record);
            sqlSession.commit();
        } catch (IOException e) {
            log.error(e.getMessage());
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
        return insert;
    }

    @Override
    public int insertSelective(Employee record) {
        return 0;
    }

    @Override
    public Employee selectByPrimaryKey(Integer id) {
        try {
            SqlSession sqlSession = mybatisUtils.getSqlSession();
            EmployeeMapper mapper = sqlSession.getMapper(EmployeeMapper.class);
            Employee employee = mapper.selectByPrimaryKey(id);
            return employee;
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public int updateByPrimaryKeySelective(Employee record) {
        return 0;
    }

    @Override
    public int updateByPrimaryKey(Employee record) {
        return 0;
    }

    @Override
    public int insertBatchSelective(List<Employee> records) {
        return 0;
    }

    @Override
    public int updateBatchByPrimaryKeySelective(List<Employee> records) {
        return 0;
    }
}
