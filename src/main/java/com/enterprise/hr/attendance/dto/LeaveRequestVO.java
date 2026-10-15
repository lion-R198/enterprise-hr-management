package com.enterprise.hr.attendance.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假单响应 VO。
 *
 * <p>在 {@code leave_request} 列基础上补：{@code employee_name} / {@code emp_no} /
 * {@code dept_name}（申请人信息）、{@code leave_type_name}（假别名称）、
 * {@code current_approver_name}（当前审批人姓名，已办结为 null）。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LeaveRequestVO implements Serializable, EmployeeAwareVO {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 申请单号，如 LV202600001 */
    private String requestNo;

    /** 申请人员工ID */
    private Long employeeId;

    /** 申请人姓名（联表展示字段） */
    private String employeeName;

    /** 申请人工号（联表展示字段） */
    private String empNo;

    /** 申请人部门名称（联表展示字段） */
    private String deptName;

    /** 假别ID */
    private Long leaveTypeId;

    /** 假别名称（联表展示字段） */
    private String leaveTypeName;

    /** 请假开始时间 */
    private LocalDateTime startTime;

    /** 请假结束时间 */
    private LocalDateTime endTime;

    /** 请假天数 */
    private BigDecimal days;

    /** 请假事由 */
    private String reason;

    /** 审批状态：0 草稿 / 1 审批中 / 2 已通过 / 3 已驳回 / 4 已撤销 */
    private Integer approvalStatus;

    /** 当前审批人员工ID，已办结为 null */
    private Long currentApproverId;

    /** 当前审批人姓名 */
    private String currentApproverName;

    /** 申请时间 */
    private LocalDateTime applyTime;
}
