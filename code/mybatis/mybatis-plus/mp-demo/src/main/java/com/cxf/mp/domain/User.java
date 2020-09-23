package com.cxf.mp.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Date;
import lombok.Data;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-08-05
 */
@ApiModel(value = "com-cxf-mp-domain-User")
@Data
@TableName(value = "`user`")
public class User extends BasePO{

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
   */
  @TableField(value = "deleted")
  @ApiModelProperty(value = "逻辑删除")
  private Integer deleted;

}
