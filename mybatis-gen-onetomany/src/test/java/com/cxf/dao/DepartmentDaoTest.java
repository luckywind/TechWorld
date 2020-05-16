package com.cxf.mapper;

import com.cxf.dao.DepartmentDao;
import com.cxf.model.Department;
import junit.framework.TestCase;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-05-11
 */
public class DepartmentMapperTest extends TestCase {
    DepartmentDao dao = new DepartmentDao();

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
    }

    public void testDeleteByPrimaryKey() {
    }

    public void testInsert() {
        Department department = new Department();
        department.setId(1);
        department.setName("财务");
        int insert = dao.insert(department);
        System.out.println(insert);
    }

    public void testInsertSelective() {
        Department department = new Department();
        department.setId(2);
        department.setName("人事");
        int insert = dao.insert(department);
        System.out.println(insert);
    }

    public void testSelectByPrimaryKey() {
        Department department1 = dao.selectByPrimaryKey(1);
        System.out.println(department1);
    }

    public void testUpdateByPrimaryKeySelective() {
        Department department = new Department();
        department.setId(2);
        department.setName("人事1");
        int update = dao.updateByPrimaryKeySelective(department);
        System.out.println(update);
    }

    public void testUpdateByPrimaryKey() {
    }

    public void testInsertBatchSelective() {
    }

    public void testUpdateBatchByPrimaryKeySelective() {
    }
}