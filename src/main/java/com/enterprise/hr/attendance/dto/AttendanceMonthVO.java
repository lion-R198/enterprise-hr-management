package com.enterprise.hr.attendance.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考勤月报响应 VO（BR-04-03 封账后禁改）。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AttendanceMonthVO implements Serializable, EmployeeAwareVO {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 员工姓名（联表展示字段） */
    private String employeeName;

    /** 员工工号（联表展示字段） */
    private String empNo;

    /** 部门名称（联表展示字段） */
    private String deptName;

    /** 所属部门ID */
    private Long deptId;

    /** 统计年份 */
    private Integer recordYear;

    /** 统计月份 */
    private Integer recordMonth;

    /** 应出勤天数 */
    private BigDecimal shouldDays;

    /** 实际出勤天数 */
    private BigDecimal actualDays;

    /** 迟到次数 */
    private Integer lateCount;

    /** 早退次数 */
    private Integer earlyCount;

    /** 旷工次数 */
    private Integer absentCount;

    /** 缺卡次数 */
    private Integer missingCount;

    /** 请假天数合计 */
    private BigDecimal leaveDays;

    /** 加班时长合计（小时） */
    private BigDecimal overtimeHours;

    /** 出差天数合计 */
    private BigDecimal tripDays;

    /** 封账状态：0 未封账 / 1 已封账（禁改） */
    private Integer lockStatus;

    /** 封账时间 */
    private LocalDateTime lockTime;

    /** 封账操作人用户ID */
    private Long lockBy;

    /** 备注 */
    private String remark;
}
