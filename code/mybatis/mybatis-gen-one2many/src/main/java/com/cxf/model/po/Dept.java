package com.cxf.model.po;

public class Dept {
    /**
     * 表字段 : dept.deptid
     */
    private Integer deptid;

    /**
     * 表字段 : dept.dname
     */
    private String dname;

    /**
     * 获取  字段:dept.deptid
     *
     * @return dept.deptid,
     */
    public Integer getDeptid() {
        return deptid;
    }

    /**
     * 设置  字段:dept.deptid
     *
     * @param deptid the value for dept.deptid,
     */
    public void setDeptid(Integer deptid) {
        this.deptid = deptid;
    }

    /**
     * 获取  字段:dept.dname
     *
     * @return dept.dname,
     */
    public String getDname() {
        return dname;
    }

    /**
     * 设置  字段:dept.dname
     *
     * @param dname the value for dept.dname,
     */
    public void setDname(String dname) {
        this.dname = dname == null ? null : dname.trim();
    }
}