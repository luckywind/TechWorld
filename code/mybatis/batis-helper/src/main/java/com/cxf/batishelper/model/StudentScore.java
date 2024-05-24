package com.cxf.batishelper.model;

import java.io.Serializable;

/** 
* Copyright (c) 2015 xxx Inc. All Rights Reserved. 
* Authors: chengxingfu <chengxingfu@xxx.com>
* Date:2020-06-05 
*/
public class StudentScore implements Serializable {
    private Integer id;

    private Integer studentId;

    private Integer courseId;

    private Integer score;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", studentId=").append(studentId);
        sb.append(", courseId=").append(courseId);
        sb.append(", score=").append(score);
        sb.append("]");
        return sb.toString();
    }
}