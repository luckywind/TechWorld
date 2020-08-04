package com.cxf.mp.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-08-04
 */
@Data
public class User {
 private Long id;
 private String username;
 private String password;
}
