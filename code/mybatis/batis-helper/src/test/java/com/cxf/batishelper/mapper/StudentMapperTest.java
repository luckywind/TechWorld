package com.cxf.batishelper.mapper;

import com.cxf.batishelper.model.Student;
import org.apache.ibatis.executor.result.DefaultResultContext;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-05-17
 */
public class StudentMapperTest {
    private static StudentMapper mapper;
    static Logger log = LoggerFactory.getLogger(StudentMapperTest.class);

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


    @Test
    public void testSelectByMap()  {
        HashMap<String , Object> mapParam = new HashMap<>();
        mapParam.put("id", 1);
        mapParam.put("name", "xiaoming");
        List<Student> students = mapper.selectByMap(mapParam);
        for (Student student : students) {
            System.out.println(student);
        }
    }


    @Test
    public void testSelectByIdAndName()  {
        List<Student> students = mapper.selectByidAndName(1,"xiaoming");
        for (Student student : students) {
            System.out.println(student);
        }
    }


    /**
     * 注意啊，这里分页插件无效！
     */
    @Test
    public void testSelectByPage()  {
        HashMap<String , Integer> page = new HashMap<>();
        page.put("currPage", 1);
        page.put("pageSize", 2);
        List<Student> students = mapper.selectByPage(page);
        for (Student student : students) {
            System.out.println(student);
        }
    }

}
