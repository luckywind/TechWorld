package com.cxf.bootzealot.model;

/** 
* Copyright (c) 2015 XiaoMi Inc. All Rights Reserved. 
* Authors: chengxingfu <chengxingfu@xiaomi.com>
* Date:2020-07-14 
*/
public class User {
    private Integer id;

    private String name;

    private Integer age;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}