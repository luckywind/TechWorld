package com.cxf.mp.component;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xxx.com>
 * Date:2020-08-05
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

  @Override
  public void insertFill(MetaObject metaObject) {
    boolean hasSetter = metaObject.hasSetter("createTime");
    if (hasSetter) {
      //fieldName填实体类的属性名，不是数据库字段名
      setFieldValByName("createTime", LocalDateTime.now(), metaObject);
    }

  }

  @Override
  public void updateFill(MetaObject metaObject) {
    setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
  }
}
