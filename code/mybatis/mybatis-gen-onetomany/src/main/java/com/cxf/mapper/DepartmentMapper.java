package com.cxf.mapper;

import com.cxf.model.Department;

import java.util.List;

public interface DepartmentMapper {
    /**
     * 根据主键删除数据库的记录,department
     *
     * @param id
     */
    int deleteByPrimaryKey(Integer id);

    /**
     * 新写入数据库记录,department
     *
     * @param record
     */
    int insert(Department record);

    /**
     * 动态字段,写入数据库记录,department
     *
     * @param record
     */
    int insertSelective(Department record);

    /**
     * 根据指定主键获取一条数据库记录,department
     *
     * @param id
     */
    Department selectByPrimaryKey(Integer id);

    /**
     * 动态字段,根据主键来更新符合条件的数据库记录,department
     *
     * @param record
     */
    int updateByPrimaryKeySelective(Department record);

    /**
     * 根据主键来更新符合条件的数据库记录,department
     *
     * @param record
     */
    int updateByPrimaryKey(Department record);

    int insertBatchSelective(List<Department> records);

    int updateBatchByPrimaryKeySelective(List<Department> records);
}