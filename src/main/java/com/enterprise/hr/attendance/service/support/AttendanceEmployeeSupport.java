package com.enterprise.hr.attendance.service.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.attendance.dto.EmployeeAwareVO;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.security.DataScopeHelper;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.employee.entity.Dept;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.mapper.DeptMapper;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 员工/部门展示信息查询支持（考勤模块共用）。
 *
 * <p>请假、加班、出差、月报、打卡记录五类列表都要补 {@code employee_name} / {@code emp_no} /
 * {@code dept_name}，且都要按"本部门"做行级数据范围过滤。把这部分查询收敛到一处，
 * 避免五个 Service 各写一遍 N+1 的逐行查员工。
 *
 * <p>所有方法均为<b>批量</b>查询（一次 {@code IN} 拿全部），列表页不会因行数放大 SQL 次数。
 *
 * @author Enterprise HR Team
 */
@Component
@RequiredArgsConstructor
public class AttendanceEmployeeSupport {

    private final EmployeeMapper employeeMapper;
    private final DeptMapper deptMapper;

    /** 按 ID 查员工，查不到抛 404 */
    public Employee require(Long employeeId) {
        if (employeeId == null) {
            throw new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "当前登录账号未关联员工档案");
        }
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "员工不存在: " + employeeId);
        }
        return employee;
    }

    /** 员工姓名，查不到返回 {@code #id}（与前端 mock 的 empName 兜底一致，避免展示空白） */
    public String nameOf(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        Employee employee = employeeMapper.selectById(employeeId);
        return employee == null ? "#" + employeeId : employee.getName();
    }

    /** 批量查员工：{@code id → Employee} */
    public Map<Long, Employee> loadEmployees(Collection<Long> employeeIds) {
        Set<Long> ids = distinct(employeeIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Employee> list = employeeMapper.selectList(
                Wrappers.<Employee>lambdaQuery().in(Employee::getId, ids));
        Map<Long, Employee> map = new HashMap<>(list.size() * 2);
        for (Employee employee : list) {
            map.put(employee.getId(), employee);
        }
        return map;
    }

    /** 批量查部门名：{@code deptId → dept.name} */
    public Map<Long, String> loadDeptNames(Collection<Long> deptIds) {
        Set<Long> ids = distinct(deptIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Dept> list = deptMapper.selectList(Wrappers.<Dept>lambdaQuery().in(Dept::getId, ids));
        Map<Long, String> map = new HashMap<>(list.size() * 2);
        for (Dept dept : list) {
            map.put(dept.getId(), dept.getName());
        }
        return map;
    }

    /**
     * 批量补全列表 VO 的 {@code employee_name} / {@code emp_no} / {@code dept_name}。
     *
     * <p>一次员工查询 + 一次部门查询覆盖整页数据，避免逐行查库。
     */
    public void fillEmployeeFields(List<? extends EmployeeAwareVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        List<Long> employeeIds = new ArrayList<>(vos.size());
        for (EmployeeAwareVO vo : vos) {
            employeeIds.add(vo.getEmployeeId());
        }
        Map<Long, Employee> employees = loadEmployees(employeeIds);

        List<Long> deptIds = new ArrayList<>(employees.size());
        for (Employee employee : employees.values()) {
            deptIds.add(employee.getDeptId());
        }
        Map<Long, String> deptNames = loadDeptNames(deptIds);

        for (EmployeeAwareVO vo : vos) {
            Employee employee = employees.get(vo.getEmployeeId());
            if (employee == null) {
                continue;
            }
            vo.setEmployeeName(employee.getName());
            vo.setEmpNo(employee.getEmpNo());
            vo.setDeptName(deptNames.get(employee.getDeptId()));
        }
    }

    /**
     * 查询指定部门下的员工 ID 列表。
     *
     * <p>用于 {@code leave_request} / {@code overtime_request} / {@code business_trip} /
     * {@code attendance_record} 这类<b>没有 dept_id 冗余列</b>的表做部门维度行级过滤：
     * 先解析出本部门员工 ID，再用 {@code IN} 收敛可见行。
     */
    public List<Long> employeeIdsOfDept(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        List<Employee> list = employeeMapper.selectList(
                Wrappers.<Employee>lambdaQuery()
                        .select(Employee::getId)
                        .eq(Employee::getDeptId, deptId));
        List<Long> ids = new ArrayList<>(list.size());
        for (Employee employee : list) {
            ids.add(employee.getId());
        }
        return ids;
    }

    /** 部门负责人（员工ID），无部门或未设置负责人返回 null */
    public Long deptLeaderId(Long deptId) {
        if (deptId == null) {
            return null;
        }
        Dept dept = deptMapper.selectById(deptId);
        return dept == null ? null : dept.getLeaderId();
    }

    /**
     * 解析当前用户在"员工维度"上的可见范围（NF-10）。
     *
     * <p>返回值语义（调用方据此拼 WHERE，不要自行再判断角色，否则容易出现口径不一致）：
     * <ul>
     *     <li>{@code null} —— 不受限（管理员 / 人事 / 财务 / 决策层），不加员工条件</li>
     *     <li>非空列表 —— 只能看这些员工的行；空列表表示无任何可见数据</li>
     * </ul>
     *
     * <p>适用于没有 {@code dept_id} 冗余列的考勤业务表
     * （{@code leave_request} / {@code overtime_request} / {@code business_trip} / {@code attendance_record}）。
     */
    public List<Long> resolveEmployeeScope(LoginUser user) {
        if (user == null) {
            return Collections.emptyList();
        }
        if (DataScopeHelper.isUnlimited(user)) {
            return null;
        }
        if (DataScopeHelper.isSelfOnly(user)) {
            return user.getEmployeeId() == null
                    ? Collections.emptyList()
                    : Collections.singletonList(user.getEmployeeId());
        }
        // 部门主管：本部门员工（下级部门暂不展开，与 mock 契约一致）
        return employeeIdsOfDept(user.getDeptId());
    }

    private static Set<Long> distinct(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> set = new LinkedHashSet<>();
        for (Long id : ids) {
            if (Objects.nonNull(id)) {
                set.add(id);
            }
        }
        return set;
    }
}
