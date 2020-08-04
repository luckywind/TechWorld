package com.cxf.mp.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.cxf.mp.domain.User;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-08-04
 */
public interface UserMapper extends BaseMapper<User> {

  @Select("select * from user ${ew.customSqlSegment}")
  List<User> mySelectList(@Param(Constants.WRAPPER) Wrapper<User> wrapper);
}
