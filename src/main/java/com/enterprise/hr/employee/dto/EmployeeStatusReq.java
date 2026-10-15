package com.enterprise.hr.employee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 员工状态变更入参（{@code PUT /employees/{id}/status}）。
 *
 * <p>状态变更属于"改变系统状态"的写操作，必须留痕（BR-02-04），
 * 服务端在写入 {@code employee_change} 时使用 {@code reason} 作为异动原因，
 * 离职时同时写入 {@code leave_reason}。
 *
 * @author Enterprise HR Team
 */
@Data
public class EmployeeStatusReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标状态：0 离职 / 1 在职 / 2 试用 / 3 待入职 */
    @NotNull(message = "目标状态不能为空")
    private Integer status;

    /** 变更原因（离职原因 / 转正说明等） */
    private String reason;
}
