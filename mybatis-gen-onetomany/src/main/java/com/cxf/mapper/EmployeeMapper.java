package com.cxf.mapper;

import com.cxf.model.Employee;
import java.util.List;

public interface EmployeeMapper {
    /**
     *  根据主键删除数据库的记录,employee
     *
     * @param id
     */
    int deleteByPrimaryKey(Integer id);

    /**
     *  新写入数据库记录,employee
     *
     * @param record
     */
    int insert(Employee record);

    /**
     *  动态字段,写入数据库记录,employee
     *
     * @param record
     */
    int insertSelective(Employee record);

    /**
     *  根据指定主键获取一条数据库记录,employee
     *
     * @param id
     */
    Employee selectByPrimaryKey(Integer id);

    /**
     *  动态字段,根据主键来更新符合条件的数据库记录,employee
     *
     * @param record
     */
    int updateByPrimaryKeySelective(Employee record);

    /**
     *  根据主键来更新符合条件的数据库记录,employee
     *
     * @param record
     */
    int updateByPrimaryKey(Employee record);

    int insertBatchSelective(List<Employee> records);

    int updateBatchByPrimaryKeySelective(List<Employee> records);
}