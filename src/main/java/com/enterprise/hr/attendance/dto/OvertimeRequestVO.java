package com.enterprise.hr.attendance.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 加班单响应 VO。
 *
 * <p>{@code start_time} / {@code end_time} 序列化为 {@code HH:mm:ss}，
 * 前端 {@code attendance/overtime.vue} 用 {@code .slice(0, 5)} 显示为 {@code HH:mm}。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OvertimeRequestVO implements Serializable, EmployeeAwareVO {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 申请单号，如 OT202600001 */
    private String requestNo;

    /** 申请人员工ID */
    private Long employeeId;

    /** 申请人姓名（联表展示字段） */
    private String employeeName;

    /** 申请人工号（联表展示字段） */
    private String empNo;

    /** 申请人部门名称（联表展示字段） */
    private String deptName;

    /** 加班日期 */
    private LocalDate overtimeDate;

    /** 加班开始时间 */
    private LocalTime startTime;

    /** 加班结束时间 */
    private LocalTime endTime;

    /** 加班时长（小时） */
    private BigDecimal hours;

    /** 加班类型：1 工作日 / 2 休息日 / 3 法定节假日 */
    private Integer overtimeType;

    /** 补偿方式：1 调休 / 2 计薪 */
    private Integer compensateType;

    /** 加班事由 */
    private String reason;

    /** 审批状态：0 草稿 / 1 审批中 / 2 已通过 / 3 已驳回 / 4 已撤销 */
    private Integer approvalStatus;

    /** 当前审批人员工ID，已办结为 null */
    private Long currentApproverId;

    /** 申请时间 */
    private LocalDateTime applyTime;
}
