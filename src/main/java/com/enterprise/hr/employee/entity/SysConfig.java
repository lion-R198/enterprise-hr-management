package com.enterprise.hr.employee.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统参数实体（对应表 {@code sys_config}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_config")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SysConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 参数键（唯一），如 attendance.lock.day */
    @TableField("config_key")
    private String configKey;

    /** 参数名称，如 考勤封账日 */
    @TableField("config_name")
    private String configName;

    /** 参数值 */
    @TableField("config_value")
    private String configValue;

    /** 参数分组：attendance考勤 / salary薪资 / recruit招聘 / system系统 */
    @TableField("config_group")
    private String configGroup;

    /** 值类型：1 字符串 / 2 数字 / 3 布尔 / 4 JSON */
    @TableField("value_type")
    private Integer valueType;

    /** 是否系统内置：1 是（禁止删除） / 0 否 */
    @TableField("is_system")
    private Integer isSystem;

    /** 备注说明 */
    private String remark;

    /** 逻辑删除：0 未删除 / 1 已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
