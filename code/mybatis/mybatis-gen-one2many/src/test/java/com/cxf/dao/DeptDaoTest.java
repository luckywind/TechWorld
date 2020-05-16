package com.cxf.dao;

import com.cxf.model.po.Dept;
import org.junit.Test;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-05-10
 */
public class DeptDaoTest {

    @Test
    public void testInsert() {
        Dept dept = new Dept();
        dept.setDeptid(21);
        dept.setDname("HR");
        DeptDao deptDao = new DeptDao();
        int insert = deptDao.insert(dept);
        System.out.println(insert);
        Dept dept1 = deptDao.selectByPrimaryKey(1);
        System.out.println(dept1);
    }

    @Test
    public void testInsertSelect() {
        Dept dept = new Dept();
        dept.setDeptid(1);
        dept.setDname("HR");
        DeptDao deptDao = new DeptDao();
        int insert = deptDao.insertSelective(dept);
        System.out.println(insert);
    }
}
