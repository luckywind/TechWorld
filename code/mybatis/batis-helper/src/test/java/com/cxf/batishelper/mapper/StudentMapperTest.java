package com.cxf.batishelper.mapper;

import com.cxf.batishelper.domain.Student;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-05-17
 */
public class StudentMapperTest {
    private static StudentMapper mapper;

    @BeforeClass
    public static void setUpMybatisDatabase() {
        SqlSessionFactory builder = new SqlSessionFactoryBuilder().build(StudentMapperTest.class.getClassLoader().getResourceAsStream("mybatisTestConfiguration/StudentMapperTestConfiguration.xml"));
        //you can use builder.openSession(false) to not commit to database
        mapper = builder.getConfiguration().getMapper(StudentMapper.class, builder.openSession(true));
    }

    @Test
    public void testMySelectByName() throws FileNotFoundException {
        List<Student> xiaoming =
                mapper.mySelectByName("xiaoming");
        for (Student student : xiaoming) {
            System.out.println(student);
        }
    }
}
