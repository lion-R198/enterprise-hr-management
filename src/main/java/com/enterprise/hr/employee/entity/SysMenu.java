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
 * 菜单权限实体（对应表 {@code sys_menu}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_menu")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 父菜单ID（NULL=顶级菜单） */
    @TableField("parent_id")
    private Long parentId;

    /** 菜单名称 */
    @TableField("menu_name")
    private String menuName;

    /** 菜单类型：1 目录 / 2 菜单 / 3 按钮 */
    @TableField("menu_type")
    private Integer menuType;

    /** 前端路由地址，如 /employee */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 权限标识，如 employee:list */
    private String perms;

    /** 菜单图标 */
    private String icon;

    /** 显示排序，升序 */
    @TableField("sort_no")
    private Integer sortNo;

    /** 是否显示：1 显示 / 0 隐藏 */
    private Integer visible;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;

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
