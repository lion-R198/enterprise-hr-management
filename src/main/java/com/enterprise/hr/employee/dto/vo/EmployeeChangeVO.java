package com.enterprise.hr.employee.dto.vo;

import com.enterprise.hr.employee.entity.EmployeeChange;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工异动记录 VO（{@code GET /employees/{id}/changes} 与 {@code GET /employee-changes/page}）。
 *
 * <p>异动表只存部门 ID，前端 {@code changes.vue} 需要展示"异动前 / 异动后"的中文部门名，
 * 因此补 {@code employee_name} / {@code before_dept_name} / {@code after_dept_name}
 * （由 Service 批量 IN 查询一次补齐，不做逐行查库）。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EmployeeChangeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 员工 ID */
    private Long employeeId;

    /** 员工姓名（关联补齐） */
    private String employeeName;

    /** 员工工号（关联补齐） */
    private String empNo;

    /** 异动类型：1 入职 / 2 转正 / 3 调岗 / 4 调部门 / 5 晋升 / 6 降级 / 7 离职 / 8 返聘 */
    private Integer changeType;

    /** 异动前部门 ID */
    private Long beforeDeptId;

    /** 异动前部门名（关联补齐） */
    private String beforeDeptName;

    /** 异动后部门 ID */
    private Long afterDeptId;

    /** 异动后部门名（关联补齐） */
    private String afterDeptName;

    /** 异动前职位 */
    private String beforePosition;

    /** 异动后职位 */
    private String afterPosition;

    /** 异动前薪资 */
    private BigDecimal beforeSalary;

    /** 异动后薪资 */
    private BigDecimal afterSalary;

    /** 生效日期 */
    private LocalDate effectDate;

    /** 异动原因 */
    private String reason;

    /** 关联申请单号 */
    private String applyNo;

    /** 审批状态：0 待提交 / 1 审批中 / 2 已通过 / 3 已驳回 */
    private Integer approvalStatus;

    /** 经办人员工 ID */
    private Long operatorId;

    /** 备注 */
    private String remark;

    /** 实体 → VO（employeeName / 部门名由 Service 批量补齐） */
    public static EmployeeChangeVO from(EmployeeChange change) {
        if (change == null) {
            return null;
        }
        EmployeeChangeVO vo = new EmployeeChangeVO();
        vo.setId(change.getId());
        vo.setEmployeeId(change.getEmployeeId());
        vo.setChangeType(change.getChangeType());
        vo.setBeforeDeptId(change.getBeforeDeptId());
        vo.setAfterDeptId(change.getAfterDeptId());
        vo.setBeforePosition(change.getBeforePosition());
        vo.setAfterPosition(change.getAfterPosition());
        vo.setBeforeSalary(change.getBeforeSalary());
        vo.setAfterSalary(change.getAfterSalary());
        vo.setEffectDate(change.getEffectDate());
        vo.setReason(change.getReason());
        vo.setApplyNo(change.getApplyNo());
        vo.setApprovalStatus(change.getApprovalStatus());
        vo.setOperatorId(change.getOperatorId());
        vo.setRemark(change.getRemark());
        return vo;
    }
}
