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
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xxx.com>
 * Date:2020-08-04
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class FillTest {

  @Autowired
  private UserMapper userMapper;

  @Test
  public void testinsert() {
    User u = new User();
    u.setUsername("zhangsan");
    u.setPassword("124335");
    u.setId(8);//插入时会忽略id
    int insert = userMapper.insert(u);
    System.out.println(insert);
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
