package com.cnsa.sparrow.base.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 基础实体
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
public class Entity<T> extends SuperEntity<T> {

    public static final String UPDATE_TIME = "updatedTime";
    public static final String UPDATE_USER = "updatedBy";

    @ApiModelProperty(value = "最后修改时间")
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    protected LocalDateTime updatedTime;

    @ApiModelProperty(value = "最后修改人ID")
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    protected T updatedBy;

    public Entity(T id, LocalDateTime createTime, T createUser, LocalDateTime updatedTime, T updatedBy) {
        super(id, createdTime, createdBy);
        this.updatedTime = updatedTime;
        this.updatedBy = updatedBy;
    }

    public Entity() {
    }

}
