package com.enterprise.hr.attendance.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出差单响应 VO。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BusinessTripVO implements Serializable, EmployeeAwareVO {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 申请单号，如 BT202600001 */
    private String requestNo;

    /** 申请人员工ID */
    private Long employeeId;

    /** 申请人姓名（联表展示字段） */
    private String employeeName;

    /** 申请人工号（联表展示字段） */
    private String empNo;

    /** 申请人部门名称（联表展示字段） */
    private String deptName;

    /** 出差目的地 */
    private String destination;

    /** 出差事由 */
    private String purpose;

    /** 出差开始时间 */
    private LocalDateTime startTime;

    /** 出差结束时间 */
    private LocalDateTime endTime;

    /** 出差天数 */
    private BigDecimal days;

    /** 交通方式：1 飞机 / 2 高铁 / 3 汽车 / 4 自驾 / 5 其他 */
    private Integer transport;

    /** 预算金额 */
    private BigDecimal budgetAmount;

    /** 审批状态：0 草稿 / 1 审批中 / 2 已通过 / 3 已驳回 / 4 已撤销 */
    private Integer approvalStatus;

    /** 当前审批人员工ID，已办结为 null */
    private Long currentApproverId;

    /** 申请时间 */
    private LocalDateTime applyTime;
}
