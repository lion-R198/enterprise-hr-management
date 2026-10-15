package com.enterprise.hr.employee.service.support;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.employee.dto.vo.EmployeeChangeVO;
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
 * 员工 / 部门展示信息查询支持（组织与员工模块共用）。
 *
 * <p>员工列表、异动列表、合同列表都要补 {@code dept_name} / {@code employee_name}，
 * 部门树与岗位列表还要补"在岗人数"。这些关联 ID 在同一页数据里高度重复，
 * 统一收敛在这里，<b>全部为批量 IN 查询 + 内存映射</b>：
 * 列表页的 SQL 次数与页大小无关，杜绝在循环里逐条查库（N+1）。
 *
 * @author Enterprise HR Team
 */
@Component
@RequiredArgsConstructor
public class EmployeeInfoSupport {

    /** 分组统计 SQL 的别名字段 */
    private static final String COL_DEPT_ID = "dept_id";
    private static final String COL_POSITION_ID = "position_id";
    private static final String COL_COUNT = "cnt";

    private final EmployeeMapper employeeMapper;
    private final DeptMapper deptMapper;

    /** 批量查员工：{@code employeeId → Employee} */
    public Map<Long, Employee> loadEmployees(Collection<Long> employeeIds) {
        Set<Long> ids = distinct(employeeIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Employee> list = employeeMapper.selectList(
                Wrappers.<Employee>lambdaQuery()
                        .select(Employee::getId, Employee::getName, Employee::getEmpNo, Employee::getDeptId)
                        .in(Employee::getId, ids));
        Map<Long, Employee> map = new HashMap<>(list.size() * 2);
        for (Employee employee : list) {
            map.put(employee.getId(), employee);
        }
        return map;
    }

    /** 批量查员工姓名：{@code employeeId → name}，查不到返回 {@code #id} 便于排查脏数据 */
    public Map<Long, String> loadEmployeeNames(Collection<Long> employeeIds) {
        Map<Long, Employee> employees = loadEmployees(employeeIds);
        Map<Long, String> map = new HashMap<>(employees.size() * 2);
        employees.forEach((id, employee) -> map.put(id, employee.getName()));
        return map;
    }

    /** 批量查部门名：{@code deptId → dept.name} */
    public Map<Long, String> loadDeptNames(Collection<Long> deptIds) {
        Set<Long> ids = distinct(deptIds);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Dept> list = deptMapper.selectList(
                Wrappers.<Dept>lambdaQuery().select(Dept::getId, Dept::getName).in(Dept::getId, ids));
        Map<Long, String> map = new HashMap<>(list.size() * 2);
        for (Dept dept : list) {
            map.put(dept.getId(), dept.getName());
        }
        return map;
    }

    /** 单个部门名，查不到返回 null */
    public String deptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        Dept dept = deptMapper.selectById(deptId);
        return dept == null ? null : dept.getName();
    }

    /**
     * 批量补全异动 VO：{@code employee_name} / {@code emp_no} / 异动前后部门名。
     *
     * <p>整页数据只需两次 IN 查询（员工一次 + 部门一次）。
     */
    public void fillChangeFields(List<EmployeeChangeVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        List<Long> employeeIds = new ArrayList<>(vos.size());
        List<Long> deptIds = new ArrayList<>(vos.size() * 2);
        for (EmployeeChangeVO vo : vos) {
            employeeIds.add(vo.getEmployeeId());
            deptIds.add(vo.getBeforeDeptId());
            deptIds.add(vo.getAfterDeptId());
        }
        Map<Long, Employee> employees = loadEmployees(employeeIds);
        Map<Long, String> deptNames = loadDeptNames(deptIds);
        for (EmployeeChangeVO vo : vos) {
            Employee employee = employees.get(vo.getEmployeeId());
            vo.setEmployeeName(employee == null ? null : employee.getName());
            vo.setEmpNo(employee == null ? null : employee.getEmpNo());
            vo.setBeforeDeptName(vo.getBeforeDeptId() == null ? null : deptNames.get(vo.getBeforeDeptId()));
            vo.setAfterDeptName(vo.getAfterDeptId() == null ? null : deptNames.get(vo.getAfterDeptId()));
        }
    }

    /**
     * 各部门在岗人数：{@code deptId → count}（{@code status != 0} 即非离职）。
     *
     * <p>用于部门树 / 部门列表的编制占用率（F-03-04），一次分组查询覆盖整棵树。
     */
    public Map<Long, Long> countByDept() {
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.select(COL_DEPT_ID, "COUNT(*) AS " + COL_COUNT)
                .ne("status", BizConst.EMPLOYEE_RESIGNED)
                .isNotNull(COL_DEPT_ID)
                .groupBy(COL_DEPT_ID);
        return toCountMap(employeeMapper.selectMaps(wrapper), COL_DEPT_ID);
    }

    /** 各岗位在编人数：{@code positionId → count}（{@code status != 0} 即非离职） */
    public Map<Long, Long> countByPosition() {
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.select(COL_POSITION_ID, "COUNT(*) AS " + COL_COUNT)
                .ne("status", BizConst.EMPLOYEE_RESIGNED)
                .isNotNull(COL_POSITION_ID)
                .groupBy(COL_POSITION_ID);
        return toCountMap(employeeMapper.selectMaps(wrapper), COL_POSITION_ID);
    }

    /** 指定部门下在岗员工 ID：用于按部门维度做行级过滤的场景 */
    public List<Long> activeEmployeeIdsOfDept(Long deptId) {
        if (deptId == null) {
            return Collections.emptyList();
        }
        List<Employee> list = employeeMapper.selectList(
                Wrappers.<Employee>lambdaQuery()
                        .select(Employee::getId)
                        .eq(Employee::getDeptId, deptId));
        return list.stream().map(Employee::getId).toList();
    }

    // ------------------------------------------------------------------ 内部

    /** 分组统计结果 → {@code key → count} */
    private static Map<Long, Long> toCountMap(List<Map<String, Object>> rows, String keyColumn) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> map = new HashMap<>(rows.size() * 2);
        String camelKey = camelCase(keyColumn);
        for (Map<String, Object> row : rows) {
            Object key = row.get(keyColumn);
            if (key == null) {
                // 兼容 map-underscore-to-camel-case 等映射配置下列名被转驼峰的情况
                key = row.get(camelKey);
            }
            Object count = row.get(COL_COUNT);
            if (key instanceof Number keyNumber && count instanceof Number countNumber) {
                map.put(keyNumber.longValue(), countNumber.longValue());
            }
        }
        return map;
    }

    /** {@code dept_id} → {@code deptId} */
    private static String camelCase(String column) {
        StringBuilder sb = new StringBuilder(column.length());
        boolean upper = false;
        for (char c : column.toCharArray()) {
            if (c == '_') {
                upper = true;
                continue;
            }
            sb.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return sb.toString();
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
