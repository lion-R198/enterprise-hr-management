
package com.enterprise.hr.employee.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门实体（对应表 {@code dept}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("dept")
public class Dept implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 部门编码（全局唯一），如 D1001 */
    @TableField("dept_code")
    private String deptCode;

    /** 部门名称 */
    private String name;

    /** 上级部门ID（NULL=顶级部门） */
    @TableField("parent_id")
    private Long parentId;

    /** 部门负责人姓名（快照，便于快速展示） */
    private String leader;

    /** 部门负责人（员工ID） */
    @TableField("leader_id")
    private Long leaderId;

    /** 组织类型：1 公司 / 2 中心/事业部 / 3 部门 / 4 班组 */
    @TableField("dept_type")
    private Integer deptType;

    /** 层级深度，顶级=1 */
    @TableField("dept_level")
    private Integer deptLevel;

    /** 层级路径，如 /1/5/9/，便于递归查询 */
    @TableField("dept_path")
    private String deptPath;

    /** 编制上限（0=不限） */
    @TableField("headcount_limit")
    private Integer headcountLimit;

    /** 部门联系电话 */
    private String phone;

    /** 显示排序，升序 */
    @TableField("sort_no")
    private Integer sortNo;

    /** 状态：1 正常 / 0 停用 */
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