package com.cxf.mybatis_boot.dao;

import com.cxf.mybatis_boot.Entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-04-22
 */
@Mapper
public interface UserDao {
    /**
     * 通过名字查询用户信息
     * @param name
     * @return
     */
    @Select("select * from user where name=#{name}")
    User findUserByName(@Param("name") String name);

    @Select("select * from user")
    List<User> findAllUser();
    /**
     * 插入用户信息
     */
    @Insert("INSERT INTO user(name, age,money) VALUES(#{name}, #{age}, #{money})")
    void insertUser(@Param("name") String name, @Param("age") Integer age, @Param("money") Double money);

    /**
     * 根据 id 更新用户信息
     */
    @Update("UPDATE  user SET name = #{name},age = #{age},money= #{money} WHERE id = #{id}")
    void updateUser(@Param("name") String name, @Param("age") Integer age, @Param("money") Double money,
                    @Param("id") int id);

    /**
     * 根据 id 删除用户信息
     */
    @Delete("DELETE from user WHERE id = #{id}")
    void deleteUser(@Param("id") int id);



}
