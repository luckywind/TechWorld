package com.cxf.model;

public class Department {
    /**
     * 
     * 表字段 : department.id
     */
    private Integer id;

    /**
     * 
     * 表字段 : department.name
     */
    private String name;

    /**
     * 获取  字段:department.id
     *
     * @return department.id, 
     */
    public Integer getId() {
        return id;
    }

    /**
     * 设置  字段:department.id
     *
     * @param id the value for department.id, 
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 获取  字段:department.name
     *
     * @return department.name, 
     */
    public String getName() {
        return name;
    }

    /**
     * 设置  字段:department.name
     *
     * @param name the value for department.name, 
     */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }
}