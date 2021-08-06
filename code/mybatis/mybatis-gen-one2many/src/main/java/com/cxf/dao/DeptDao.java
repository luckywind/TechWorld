package com.cxf.dao;

import com.cxf.mapper.DeptMapper;
import com.cxf.model.po.Dept;
import com.cxf.utils.MybatisUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xxx.com>
 * Date:2020-05-10
 */
public class DeptDao implements DeptMapper {
    final Logger log = Logger.getLogger(DeptMapper.class.getName());
    MybatisUtils mybatisUtils = new MybatisUtils();
    SqlSession sqlSession = null;

    public static void main(String[] args) {
        DeptDao.testInsert();
    }

    public static void testInsert() {
        Dept dept = new Dept();
        dept.setDeptid(21);
        dept.setDname("HR");
        DeptDao deptDao = new DeptDao();
        int insert = deptDao.insert(dept);
        System.out.println(insert);
        Dept dept1 = deptDao.selectByPrimaryKey(1);
        System.out.println(dept1);
    }

    public int deleteByPrimaryKey(Integer deptid) {
        try {
            SqlSession sqlSession = mybatisUtils.getSqlSession();
            DeptMapper deptDao = sqlSession.getMapper(DeptMapper.class);
            int ret = deptDao.deleteByPrimaryKey(deptid);
            return ret;
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return 0;
    }

    public int insert(Dept record) {
        try {
            SqlSession sqlSession = mybatisUtils.getSqlSession();
            DeptMapper deptDao = sqlSession.getMapper(DeptMapper.class);
            int ret = deptDao.insert(record);
            return ret;
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return 0;
    }

    public int insertSelective(Dept record) {
        try {
            SqlSession sqlSession = mybatisUtils.getSqlSession();
            DeptMapper deptDao = sqlSession.getMapper(DeptMapper.class);
            int ret = deptDao.insertSelective(record);
            return ret;
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return 0;
    }

    public Dept selectByPrimaryKey(Integer deptid) {
        try {
            SqlSession sqlSession = mybatisUtils.getSqlSession();
            DeptMapper deptDao = sqlSession.getMapper(DeptMapper.class);
            return deptDao.selectByPrimaryKey(deptid);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public int updateByPrimaryKeySelective(Dept record) {
        try {
            SqlSession sqlSession = mybatisUtils.getSqlSession();
            DeptMapper deptDao = sqlSession.getMapper(DeptMapper.class);
            int ret = deptDao.updateByPrimaryKeySelective(record);
            return ret;
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return 0;
    }

    public int updateByPrimaryKey(Dept record) {
        try {
            SqlSession sqlSession = mybatisUtils.getSqlSession();
            DeptMapper deptDao = sqlSession.getMapper(DeptMapper.class);
            int ret = deptDao.updateByPrimaryKey(record);
            return ret;
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return 0;
    }

    public int insertBatchSelective(List<Dept> records) {
        try {
            SqlSession sqlSession = mybatisUtils.getSqlSession();
            DeptMapper deptDao = sqlSession.getMapper(DeptMapper.class);
            int ret = deptDao.insertBatchSelective(records);
            return ret;
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return 0;
    }

    public int updateBatchByPrimaryKeySelective(List<Dept> records) {
        try {
            SqlSession sqlSession = mybatisUtils.getSqlSession();
            DeptMapper deptDao = sqlSession.getMapper(DeptMapper.class);
            int ret = deptDao.updateBatchByPrimaryKeySelective(records);
            return ret;
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return 0;
    }
}
