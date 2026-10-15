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
 * 薪资标准模板实体（对应表 {@code salary_standard}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("salary_standard")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SalaryStandard implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 标准编码（唯一），如 STD-TECH-P5 */
    @TableField("standard_code")
    private String standardCode;

    /** 标准名称，如 P5技术岗标准 */
    @TableField("standard_name")
    private String standardName;

    /** 适用岗位序列：1 管理 / 2 技术 / 3 职能 / 4 操作 */
    @TableField("position_seq")
    private Integer positionSeq;

    /** 适用职级，如 P5/M2 */
    @TableField("job_level")
    private String jobLevel;

    /** 基本工资 */
    @TableField("base_salary")
    private BigDecimal baseSalary;

    /** 岗位工资 */
    @TableField("post_salary")
    private BigDecimal postSalary;

    /** 绩效工资（基数） */
    @TableField("performance_salary")
    private BigDecimal performanceSalary;

    /** 固定补贴合计 */
    private BigDecimal allowance;

    /** 生效日期 */
    @TableField("effective_date")
    private LocalDate effectiveDate;

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
