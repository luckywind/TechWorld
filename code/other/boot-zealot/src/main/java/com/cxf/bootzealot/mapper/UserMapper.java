package com.cxf.bootzealot.mapper;

import com.cxf.bootzealot.model.User;

/** 
* Copyright (c) 2015 xxx Inc. All Rights Reserved. 
* Authors: chengxingfu <chengxingfu@xxx.com>
* Date:2020-07-14 
*/
public interface UserMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(User record);

    int insertSelective(User record);

    User selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(User record);

    int updateByPrimaryKey(User record);
}