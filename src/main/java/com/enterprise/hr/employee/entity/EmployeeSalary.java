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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工薪资标准实体（对应表 {@code employee_salary}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("employee_salary")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EmployeeSalary implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 员工ID */
    @TableField("employee_id")
    private Long employeeId;

    /** 套用的薪资标准模板ID */
    @TableField("standard_id")
    private Long standardId;

    /** 基本工资 */
    @TableField("base_salary")
    private BigDecimal baseSalary;

    /** 岗位工资 */
    @TableField("post_salary")
    private BigDecimal postSalary;

    /** 绩效工资基数 */
    @TableField("performance_salary")
    private BigDecimal performanceSalary;

    /** 固定补贴合计 */
    private BigDecimal allowance;

    /** 社保缴费基数 */
    @TableField("social_base")
    private BigDecimal socialBase;

    /** 公积金缴费基数 */
    @TableField("fund_base")
    private BigDecimal fundBase;

    /** 公积金缴存比例（%） */
    @TableField("fund_rate")
    private BigDecimal fundRate;

    /** 调薪原因 */
    @TableField("adjust_reason")
    private String adjustReason;

    /** 生效日期 */
    @TableField("effective_date")
    private LocalDate effectiveDate;

    /** 是否当前生效：1 是 / 0 否（历史记录） */
    @TableField("is_current")
    private Integer isCurrent;

    /** 审批状态：0 待审 / 1 已生效 / 2 已驳回 */
    @TableField("approval_status")
    private Integer approvalStatus;

    /** 备注 */
    private String remark;

    /** 操作人用户ID */
    @TableField("created_by")
    private Long createdBy;

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
