package com.cxf.mapper;

import com.cxf.model.po.Dept;
import java.util.List;

public interface DeptMapper {
    /**
     *  根据主键删除数据库的记录,dept
     *
     * @param deptid
     */
    int deleteByPrimaryKey(Integer deptid);

    /**
     *  新写入数据库记录,dept
     *
     * @param record
     */
    int insert(Dept record);

    /**
     *  动态字段,写入数据库记录,dept
     *
     * @param record
     */
    int insertSelective(Dept record);

    /**
     *  根据指定主键获取一条数据库记录,dept
     *
     * @param deptid
     */
    Dept selectByPrimaryKey(Integer deptid);

    /**
     *  动态字段,根据主键来更新符合条件的数据库记录,dept
     *
     * @param record
     */
    int updateByPrimaryKeySelective(Dept record);

    /**
     *  根据主键来更新符合条件的数据库记录,dept
     *
     * @param record
     */
    int updateByPrimaryKey(Dept record);

    int insertBatchSelective(List<Dept> records);

    int updateBatchByPrimaryKeySelective(List<Dept> records);
}