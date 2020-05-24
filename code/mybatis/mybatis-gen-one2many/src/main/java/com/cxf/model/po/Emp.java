package com.cxf.model.po;

public class Emp {
    /**
     * 表字段 : emp.id
     */
    private Integer id;

    /**
     * 表字段 : emp.name
     */
    private String name;

    /**
     * 表字段 : emp.deptid
     */
    private Integer deptid;

    private Dept dept;

    /**
     * 获取  字段:emp.id
     *
     * @return emp.id,
     */
    public Integer getId() {
        return id;
    }

    /**
     * 设置  字段:emp.id
     *
     * @param id the value for emp.id,
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 获取  字段:emp.name
     *
     * @return emp.name,
     */
    public String getName() {
        return name;
    }

    /**
     * 设置  字段:emp.name
     *
     * @param name the value for emp.name,
     */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    /**
     * 获取  字段:emp.deptid
     *
     * @return emp.deptid,
     */
    public Integer getDeptid() {
        return deptid;
    }

    /**
     * 设置  字段:emp.deptid
     *
     * @param deptid the value for emp.deptid,
     */
    public void setDeptid(Integer deptid) {
        this.deptid = deptid;
    }

    public Dept getDept() {
        return dept;
    }

    public void setDept(Dept dept) {
        this.dept = dept;
    }
}