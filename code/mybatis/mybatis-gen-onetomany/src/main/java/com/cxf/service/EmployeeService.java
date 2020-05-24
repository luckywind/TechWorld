package com.cxf.service;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import com.cxf.model.Employee;
import com.cxf.mapper.EmployeeMapper;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-05-12
 */
@Service
public class EmployeeService {

    @Resource
    private EmployeeMapper employeeMapper;


    public int deleteByPrimaryKey(Integer id) {
        return employeeMapper.deleteByPrimaryKey(id);
    }


    public int insert(Employee record) {
        return employeeMapper.insert(record);
    }


    public int insertSelective(Employee record) {
        return employeeMapper.insertSelective(record);
    }


    public Employee selectByPrimaryKey(Integer id) {
        return employeeMapper.selectByPrimaryKey(id);
    }


    public int updateByPrimaryKeySelective(Employee record) {
        return employeeMapper.updateByPrimaryKeySelective(record);
    }


    public int updateByPrimaryKey(Employee record) {
        return employeeMapper.updateByPrimaryKey(record);
    }


    public int updateBatch(List<Employee> list) {
        return employeeMapper.updateBatch(list);
    }

}
