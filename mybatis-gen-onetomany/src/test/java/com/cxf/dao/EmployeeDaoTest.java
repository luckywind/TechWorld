package com.cxf.dao;

import junit.framework.TestCase;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-05-11
 */
public class EmployeeDaoTest extends TestCase {
    EmployeeDao dao = new EmployeeDao();
    public void testDeleteByPrimaryKey() {
    }

    public void testInsert() {

        Employee employee = new Employee();
        employee.setName("zhansan");
        employee.setDpt_id(1);
        int insert = dao.insert(employee);
        System.out.println(insert);
    }

    public void testInsertSelective() {
    }

    public void testSelectByPrimaryKey() {
        Employee employee = dao.selectByPrimaryKey(1);
        System.out.println(employee);

    }

    public void testUpdateByPrimaryKeySelective() {
    }

    public void testUpdateByPrimaryKey() {
    }

    public void testInsertBatchSelective() {
    }

    public void testUpdateBatchByPrimaryKeySelective() {
    }
}