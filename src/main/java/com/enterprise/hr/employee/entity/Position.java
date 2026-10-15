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
 * 岗位实体（对应表 {@code position}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("position")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Position implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 岗位编码（全局唯一），如 P-TECH-001 */
    @TableField("position_code")
    private String positionCode;

    /** 岗位名称，如 Java开发工程师 */
    @TableField("position_name")
    private String positionName;

    /** 岗位序列：1 管理 / 2 技术 / 3 职能 / 4 操作 */
    @TableField("position_seq")
    private Integer positionSeq;

    /** 职级范围，如 P4-P7 */
    @TableField("job_level")
    private String jobLevel;

    /** 所属部门ID（NULL=公司通用岗位） */
    @TableField("dept_id")
    private Long deptId;

    /** 岗位职责 */
    private String duty;

    /** 任职要求 */
    private String requirement;

    /** 岗位编制上限（0=不限） */
    @TableField("headcount_limit")
    private Integer headcountLimit;

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
