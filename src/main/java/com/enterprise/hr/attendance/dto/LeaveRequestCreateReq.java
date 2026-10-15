package com.enterprise.hr.attendance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假申请入参（{@code POST /leave-requests}）。
 *
 * <p>请求体驼峰命名，字段名与前端 {@code attendance/leave.vue} 表单一致。
 * 业务规则校验（BR-04-01 时间重叠 / BR-04-02 余额）在 Service 层落地。
 *
 * @author Enterprise HR Team
 */
@Data
public class LeaveRequestCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 假别ID */
    @NotNull(message = "假别不能为空")
    private Long leaveTypeId;

    /** 请假开始时间 */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /** 请假结束时间 */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /** 请假天数（支持 0.5 天） */
    @NotNull(message = "请假天数不能为空")
    @DecimalMin(value = "0.5", message = "请假天数不得小于 0.5 天")
    private BigDecimal days;

    /** 请假事由 */
    private String reason;
}
