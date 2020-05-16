package com.cxf.model;

public class Employee {
    /**
     * 
     * 表字段 : employee.id
     */
    private Integer id;

    /**
     * 
     * 表字段 : employee.name
     */
    private String name;

    /**
     * 
     * 表字段 : employee.dpt_id
     */
    private Integer dpt_id;

    private Department department;

    /**
     * 获取  字段:employee.id
     *
     * @return employee.id, 
     */
    public Integer getId() {
        return id;
    }

    /**
     * 设置  字段:employee.id
     *
     * @param id the value for employee.id, 
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 获取  字段:employee.name
     *
     * @return employee.name, 
     */
    public String getName() {
        return name;
    }

    /**
     * 设置  字段:employee.name
     *
     * @param name the value for employee.name, 
     */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    /**
     * 获取  字段:employee.dpt_id
     *
     * @return employee.dpt_id, 
     */
    public Integer getDpt_id() {
        return dpt_id;
    }

    /**
     * 设置  字段:employee.dpt_id
     *
     * @param dpt_id the value for employee.dpt_id, 
     */
    public void setDpt_id(Integer dpt_id) {
        this.dpt_id = dpt_id;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department=department;
    }
}