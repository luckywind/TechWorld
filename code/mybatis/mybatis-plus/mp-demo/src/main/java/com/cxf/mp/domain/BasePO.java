package com.cxf.mp.domain;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-05-12
 */

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 基础实体类
 */
@Data
public class BasePO implements Serializable {


    private static final long serialVersionUID = 1L;
    @TableField(value = "create_time",fill = FieldFill.INSERT)
    @ApiModelProperty(value="")
    public LocalDateTime createTime;

    @TableField(value = "update_time",fill = FieldFill.UPDATE)
    @ApiModelProperty(value="")
    public LocalDateTime updateTime;

    @TableField(value = "deleted",select = false)
    @TableLogic //意味着这是逻辑删除字段
    @ApiModelProperty(value = "逻辑删除")
    public Integer deleted;

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
