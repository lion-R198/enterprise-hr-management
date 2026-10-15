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
 * 角色实体（对应表 {@code sys_role}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SysRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 角色编码（唯一），如 ROLE_HR */
    @TableField("role_code")
    private String roleCode;

    /** 角色名称，如 人事专员 */
    @TableField("role_name")
    private String roleName;

    /** 角色职责描述 */
    private String description;

    /** 数据范围：1 全部 / 2 本部门 / 3 本部门及下级 / 4 仅本人 / 5 自定义 */
    @TableField("data_scope")
    private Integer dataScope;

    /** 自定义数据范围部门ID列表，逗号分隔 */
    @TableField("data_scope_depts")
    private String dataScopeDepts;

    /** 角色类型：1 系统内置（不可删除） / 2 自定义 */
    @TableField("role_type")
    private Integer roleType;

    /** 显示排序，升序 */
    @TableField("sort_no")
    private Integer sortNo;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;

    /** 备注 */
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
