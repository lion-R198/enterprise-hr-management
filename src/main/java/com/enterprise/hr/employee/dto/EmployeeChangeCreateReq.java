package com.enterprise.hr.employee.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工异动办理入参（{@code POST /employee-changes}）。
 *
 * <p>必填 {@code employeeId / changeType / effectDate}；异动前的部门、职位、薪资
 * 由服务端从员工主数据取快照，前端只需要给出"异动后"的目标值。
 *
 * @author Enterprise HR Team
 */
@Data
public class EmployeeChangeCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 员工 ID（必填） */
    private Long employeeId;

    /** 异动类型（必填）：3 调岗 / 4 调部门 / 5 晋升 / 6 降级 等 */
    private Integer changeType;

    /** 生效日期（必填） */
    private LocalDate effectDate;

    /** 异动后部门 ID（不传视为不变） */
    private Long afterDeptId;

    /** 异动后岗位 ID（不传视为不变） */
    private Long afterPositionId;

    /** 异动后薪资（不传视为不变） */
    private BigDecimal afterSalary;

    /** 异动原因 */
    private String reason;

    /** 备注 */
    private String remark;
}
