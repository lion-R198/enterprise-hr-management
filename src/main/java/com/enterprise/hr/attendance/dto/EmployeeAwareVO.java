package com.enterprise.hr.attendance.dto;

/**
 * 需要补全"申请人展示字段"的响应 VO 契约。
 *
 * <p>考勤模块的请假 / 加班 / 出差 / 月报 / 打卡记录列表都要补
 * {@code employee_name} / {@code emp_no} / {@code dept_name} 三个联表展示字段，
 * 前端 {@code el-table-column prop="employee_name"} 直接绑定。
 *
 * <p>抽出该接口后，{@code AttendanceEmployeeSupport#fillEmployeeFields} 可用一次批量查询
 * 完成所有列表的字段补全，避免每个 Service 各写一遍"查员工 → 查部门 → set"。
 *
 * @author Enterprise HR Team
 */
public interface EmployeeAwareVO {

    /** 员工ID（关联键，用于批量查询） */
    Long getEmployeeId();

    /** 员工姓名（联表展示字段） */
    void setEmployeeName(String employeeName);

    /** 员工工号（联表展示字段） */
    void setEmpNo(String empNo);

    /** 所属部门名称（联表展示字段） */
    void setDeptName(String deptName);
}
