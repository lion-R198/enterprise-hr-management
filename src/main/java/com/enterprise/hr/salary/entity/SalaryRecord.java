package com.enterprise.hr.salary.entity;

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
 * SalaryRecord实体（对应表 {@code salary_record}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("salary_record")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SalaryRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 所属部门ID(冗余,便于按部门统计人力成本) */
    private Long deptId;

    /** 薪资年份,如 2026 */
    private Integer salaryYear;

    /** 薪资月份:1-12 */
    private Integer salaryMonth;

    /** 基本工资 */
    private BigDecimal baseSalary;

    /** 岗位工资 */
    private BigDecimal postSalary;

    /** 绩效工资(按考核结果浮动) */
    private BigDecimal performanceSalary;

    /** 补贴合计 */
    private BigDecimal allowance;

    /** 加班费 */
    private BigDecimal overtimePay;

    /** 其他加项(补发等) */
    private BigDecimal otherAdd;

    /** 考勤扣款(迟到/早退/旷工) */
    private BigDecimal attendanceDeduct;

    /** 其他扣项 */
    private BigDecimal otherDeduct;

    /** 应发合计 */
    private BigDecimal grossPay;

    /** 社保个人缴纳 */
    private BigDecimal socialPersonal;

    /** 社保企业缴纳 */
    private BigDecimal socialCompany;

    /** 公积金个人缴纳 */
    private BigDecimal fundPersonal;

    /** 公积金企业缴纳 */
    private BigDecimal fundCompany;

    /** 个人所得税 */
    private BigDecimal taxAmount;

    /** 实发合计 */
    private BigDecimal netPay;

    /** 发放状态:0待核算 1已核算 2已复核 3已发放 4发放失败 */
    private Integer payStatus;

    /** 实际发放日期 */
    private LocalDate payDate;

    /** 发放批次号 */
    private String payBatchNo;

    /** 锁定状态:0未锁定 1已锁定(禁改) */
    private Integer lockStatus;

    /**
     * 核算人用户ID（须与复核人不同，BR-05-03 职责分离）。
     *
     * <p>该列由 2026-09-15 的增量 DDL 补入：此前 {@code audit_by} 的列注释已写明
     * 「不得与核算人相同」，但表内并无核算人列，只能把 {@code CALC_BY=<userId>} 标记
     * 塞进业务备注 {@code remark}，既占用了备注字段，也让内控规则依赖自由文本。
     * 现改为独立列承载，{@code remark} 归还给业务备注使用。
     */
    private Long calcBy;

    /** 复核人用户ID(不得与核算人相同) */
    private Long auditBy;

    /** 备注 */
    private String remark;

    /** 逻辑删除:0未删除 1已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
