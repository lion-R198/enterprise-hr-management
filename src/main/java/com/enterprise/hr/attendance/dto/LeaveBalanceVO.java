package com.enterprise.hr.attendance.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 我的假期余额响应 VO（{@code GET /leave-balances/my}）。
 *
 * <p>前端 {@code attendance/leave.vue} 读取 {@code leave_type_name} / {@code used_days} /
 * {@code total_days} / {@code remain_days} 渲染进度条。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LeaveBalanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 假别ID */
    private Long leaveTypeId;

    /** 假别名称（联表展示字段） */
    private String leaveTypeName;

    /** 所属年度 */
    private Integer balanceYear;

    /** 年度总额度（天） */
    private BigDecimal totalDays;

    /** 上年结转天数 */
    private BigDecimal carryOverDays;

    /** 已使用天数 */
    private BigDecimal usedDays;

    /** 剩余可用天数 */
    private BigDecimal remainDays;

    /** 额度失效日期 */
    private LocalDate expireDate;
}
