package com.xiaomi.mapper;

import com.xiaomi.model.po.People;

public interface PeopleMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(People record);

    int insertSelective(People record);

    People selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(People record);

    int updateByPrimaryKey(People record);
}