package com.cxf.dao;

import com.cxf.mapper.DepartmentMapper;
import com.cxf.model.Department;
import com.cxf.utils.MybatisUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xxx.com>
 * Date:2020-05-11
 */
public class DepartmentDao implements DepartmentMapper {
    final Logger log = Logger.getLogger(DepartmentMapper.class);
    MybatisUtils mybatisUtils = new MybatisUtils();
    SqlSession sqlSession = null;

    public int deleteByPrimaryKey(Integer id) {

        return 0;
    }

    public int insert(Department record) {
        int insert = 0;
        try {
            sqlSession = mybatisUtils.getSqlSession();
            DepartmentMapper departmentMapper = sqlSession.getMapper(DepartmentMapper.class);
            insert = departmentMapper.insert(record);
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

    public int insertSelective(Department record) {
        int insert = 0;
        try {
            sqlSession = mybatisUtils.getSqlSession();
            DepartmentMapper departmentMapper = sqlSession.getMapper(DepartmentMapper.class);
            insert = departmentMapper.insert(record);
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

    public Department selectByPrimaryKey(Integer id) {
        Department department = null;
        try {
            sqlSession = mybatisUtils.getSqlSession();
            DepartmentMapper departmentMapper = sqlSession.getMapper(DepartmentMapper.class);
            department = departmentMapper.selectByPrimaryKey(id);
            sqlSession.commit();
        } catch (IOException e) {
            log.error(e.getMessage());
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
        return department;
    }

    public int updateByPrimaryKeySelective(Department record) {
        int update = 0;
        try {
            sqlSession = mybatisUtils.getSqlSession();
            DepartmentMapper departmentMapper = sqlSession.getMapper(DepartmentMapper.class);
            update = departmentMapper.updateByPrimaryKeySelective(record);
            sqlSession.commit();
        } catch (IOException e) {
            log.error(e.getMessage());
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
        return update;
    }

    public int updateByPrimaryKey(Department record) {
        int update = 0;
        try {
            sqlSession = mybatisUtils.getSqlSession();
            DepartmentMapper departmentMapper = sqlSession.getMapper(DepartmentMapper.class);
            update = departmentMapper.updateByPrimaryKey(record);
            sqlSession.commit();
        } catch (IOException e) {
            log.error(e.getMessage());
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
        return update;

    }

    public int insertBatchSelective(List<Department> records) {
        int insert = 0;
        try {
            sqlSession = mybatisUtils.getSqlSession();
            DepartmentMapper departmentMapper = sqlSession.getMapper(DepartmentMapper.class);
            insert = departmentMapper.insertBatchSelective(records);
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

    public int updateBatchByPrimaryKeySelective(List<Department> records) {

        int update = 0;
        try {
            sqlSession = mybatisUtils.getSqlSession();
            DepartmentMapper departmentMapper = sqlSession.getMapper(DepartmentMapper.class);
            update = departmentMapper.updateBatchByPrimaryKeySelective(records);
            sqlSession.commit();
        } catch (IOException e) {
            log.error(e.getMessage());
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
        return update;
    }
}
