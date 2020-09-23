package com.cxf.mp.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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

  /**
   * selectList
   * 查询所有，包括逻辑删除的
   */
  @Test
  public void testSelect() {
    List<User> userList = userMapper.selectList(null);
    for (User user : userList) {
      System.out.println(user);
    }
  }

  @Test
  public void testupdate() {
    User u = new User();
    u.setId(1);
    u.setUsername("cxf");
    int i = userMapper.updateById(u);
    System.out.println(i);
  }

  @Test
  public void mySelectTest() {
    List<User> userList = userMapper.mySelectList(Wrappers.<User>lambdaQuery()
        .eq(User::getPassword,"45")
        .eq(User::getDeleted,0));
    userList.forEach(System.out::println);
  }
}
