package com.enterprise.hr.salary.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 月度薪资记录出参 VO（F-05-02 核算列表 / F-05-06 我的薪资条 / 薪资条明细）。
 *
 * <p>在 {@code salary_record} 全字段基础上补齐前端表格所需的三个关联展示字段
 * {@code employee_name} / {@code emp_no} / {@code dept_name}（均保持蛇形）。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SalaryRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 员工 ID */
    private Long employeeId;

    /** 员工姓名（关联 employee.name） */
    private String employeeName;

    /** 工号（关联 employee.emp_no） */
    private String empNo;

    /** 所属部门 ID */
    private Long deptId;

    /** 部门名称（关联 dept.name） */
    private String deptName;

    /** 薪资年份 */
    private Integer salaryYear;

    /** 薪资月份 */
    private Integer salaryMonth;

    /** 基本工资 */
    private BigDecimal baseSalary;

    /** 岗位工资 */
    private BigDecimal postSalary;

    /** 绩效工资 */
    private BigDecimal performanceSalary;

    /** 补贴合计 */
    private BigDecimal allowance;

    /** 加班费 */
    private BigDecimal overtimePay;

    /** 其他加项 */
    private BigDecimal otherAdd;

    /** 考勤扣款 */
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

    /** 发放状态：0 待核算 / 1 已核算 / 2 已复核 / 3 已发放 / 4 发放失败 */
    private Integer payStatus;

    /** 实际发放日期 */
    private LocalDate payDate;

    /** 发放批次号 */
    private String payBatchNo;

    /** 锁定状态：0 未锁定 / 1 已锁定（BR-05-04 发放后锁定） */
    private Integer lockStatus;

    /** 核算人用户 ID（BR-05-03 职责分离：须与复核人不同） */
    private Long calcBy;

    /** 复核人用户 ID */
    private Long auditBy;
}
