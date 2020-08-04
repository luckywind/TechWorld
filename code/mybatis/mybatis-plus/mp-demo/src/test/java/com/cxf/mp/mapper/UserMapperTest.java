package com.cxf.mp.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.cxf.mp.domain.User;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-08-04
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class UserMapperTest {

  @Autowired
  private UserMapper userMapper;

  @Test
  public void testSelect() {
    List<User> userList = userMapper.selectList(null);
    for (User user : userList) {
      System.out.println(user);
    }
  }
}
