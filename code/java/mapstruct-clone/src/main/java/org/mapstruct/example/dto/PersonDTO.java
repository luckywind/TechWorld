package org.mapstruct.example.dto;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xxx.com>
 * Date:2020-08-19
 */
@NoArgsConstructor
@AllArgsConstructor
@ToString
//@Data
public class PersonDTO {
  private Long id;
  private String name;
  /**
   * 对应 Person.user.age
   */
  private Integer age;
  private String email;
  /**
   * 与 DO 里面的字段名称(birthDay)不一致
   */
  private Date birth;
  /**
   * 对 DO 里面的字段(birthDay)进行拓展,dateFormat 的形式
   */
  private String birthDateFormat;
  /**
   * 对 DO 里面的字段(birthDay)进行拓展,expression 的形式
   */
  private String birthExpressionFormat;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Date getBirth() {
    return birth;
  }

  public void setBirth(Date birth) {
    this.birth = birth;
  }

  public String getBirthDateFormat() {
    return birthDateFormat;
  }

  public void setBirthDateFormat(String birthDateFormat) {
    this.birthDateFormat = birthDateFormat;
  }

  public String getBirthExpressionFormat() {
    return birthExpressionFormat;
  }

  public void setBirthExpressionFormat(String birthExpressionFormat) {
    this.birthExpressionFormat = birthExpressionFormat;
  }
}
