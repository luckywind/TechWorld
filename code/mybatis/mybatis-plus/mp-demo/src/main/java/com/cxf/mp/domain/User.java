package com.cxf.mp.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-08-04
 */
@ApiModel(value = "com-cxf-mp-domain-User")
@Data
@TableName(value = "`user`")
public class User {

  @TableId(value = "id", type = IdType.AUTO)
  @ApiModelProperty(value = "")
  private Integer id;

  @TableField(value = "username")
  @ApiModelProperty(value = "")
  private String username;

  @TableField(value = "password")
  @ApiModelProperty(value = "")
  private String password;

  /**
   * 逻辑删除
   * select = false，代表select语句不会把逻辑删除字段查出来
   * 注意：自定义的查询方法并不会自动识别逻辑删除，有两个办法解决
   * 1.如果方法接收一个Wrapper参数，则可以在参数里加限定条件
   * 2.如果你的方法不接受Wrapper参数，则可以在自定义sql语句中加上限定条件
   *
   */
  @TableField(value = "deleted",select = false)
  @TableLogic //意味着这是逻辑删除字段
  @ApiModelProperty(value = "逻辑删除")
  private Integer deleted;
}
