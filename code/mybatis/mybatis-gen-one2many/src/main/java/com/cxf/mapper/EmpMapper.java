package com.cxf.mapper;

import com.cxf.model.po.Emp;
import java.util.List;

public interface EmpMapper {
    /**
     *  根据主键删除数据库的记录,emp
     *
     * @param id
     */
    int deleteByPrimaryKey(Integer id);

    /**
     *  新写入数据库记录,emp
     *
     * @param record
     */
    int insert(Emp record);

    /**
     *  动态字段,写入数据库记录,emp
     *
     * @param record
     */
    int insertSelective(Emp record);

    /**
     *  根据指定主键获取一条数据库记录,emp
     *
     * @param id
     */
    Emp selectByPrimaryKey(Integer id);

    /**
     *  动态字段,根据主键来更新符合条件的数据库记录,emp
     *
     * @param record
     */
    int updateByPrimaryKeySelective(Emp record);

    /**
     *  根据主键来更新符合条件的数据库记录,emp
     *
     * @param record
     */
    int updateByPrimaryKey(Emp record);

    int insertBatchSelective(List<Emp> records);

    int updateBatchByPrimaryKeySelective(List<Emp> records);
}