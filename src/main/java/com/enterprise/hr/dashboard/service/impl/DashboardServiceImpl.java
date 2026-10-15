package com.enterprise.hr.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.attendance.entity.AttendanceRecord;
import com.enterprise.hr.attendance.mapper.AttendanceRecordMapper;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.security.DataScopeHelper;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.config.EhrProperties;
import com.enterprise.hr.dashboard.dto.DashboardChartsVO;
import com.enterprise.hr.dashboard.dto.DashboardSummaryVO;
import com.enterprise.hr.dashboard.dto.NameValueVO;
import com.enterprise.hr.dashboard.dto.TrendPointVO;
import com.enterprise.hr.dashboard.service.DashboardService;
import com.enterprise.hr.employee.entity.Dept;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.entity.EmployeeContract;
import com.enterprise.hr.employee.mapper.DeptMapper;
import com.enterprise.hr.employee.mapper.EmployeeContractMapper;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import com.enterprise.hr.notice.entity.Notice;
import com.enterprise.hr.notice.entity.TodoTask;
import com.enterprise.hr.notice.mapper.NoticeMapper;
import com.enterprise.hr.notice.mapper.TodoTaskMapper;
import com.enterprise.hr.performance.entity.PerformancePlan;
import com.enterprise.hr.performance.entity.PerformanceRecord;
import com.enterprise.hr.performance.mapper.PerformancePlanMapper;
import com.enterprise.hr.performance.mapper.PerformanceRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 首页看板业务实现。
 *
 * <p>取数策略：先把需要的行一次性查出（{@code selectList} / {@code selectCount}），
 * 再在内存里用 Map 分组聚合。整页指标最多 6 次数据库往返，
 * 与"循环里逐行 count"相比对数据库的压力是常数级。
 *
 * <p>数据范围（BR-01-01）复用 {@link DataScopeHelper}，判定口径与薪资、绩效等模块完全一致
 * ——同一角色在不同页面看到的人数不会是两套数字。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    /** 考勤异常口径：2 迟到 / 3 早退 / 4 缺卡 / 5 旷工 */
    private static final List<Integer> ABNORMAL_ATTENDANCE_STATUS = List.of(
            BizConst.ATTEND_LATE, BizConst.ATTEND_EARLY, BizConst.ATTEND_MISSING, BizConst.ATTEND_ABSENT);

    /** 方案状态：2 已发布 / 3 评分中 / 4 已结束（取"最近一个已发布方案"时的候选集合） */
    private static final List<Integer> PUBLISHED_PLAN_STATUS = List.of(2, 3, 4);

    /** 合同状态：1 生效中（已到期/已解除的合同不再计入到期预警） */
    private static final int CONTRACT_ACTIVE = 1;

    /** 绩效等级全集，保证图表五个等级稳定出现（无数据时为 0） */
    private static final List<String> GRADES = List.of("S", "A", "B", "C", "D");

    /** 趋势图的月份窗口：近 6 个月（含当月） */
    private static final int TREND_MONTHS = 6;

    private final EmployeeMapper employeeMapper;
    private final DeptMapper deptMapper;
    private final EmployeeContractMapper employeeContractMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final TodoTaskMapper todoTaskMapper;
    private final NoticeMapper noticeMapper;
    private final PerformancePlanMapper performancePlanMapper;
    private final PerformanceRecordMapper performanceRecordMapper;
    private final EhrProperties ehrProperties;

    // ------------------------------------------------------------------ F-01-01

    @Override
    public DashboardSummaryVO summary() {
        LoginUser user = UserContext.require();
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

        // BR-01-01：先把范围内的员工一次性取出，后续所有人数类指标都在这份数据上算
        List<Employee> scoped = loadScopedEmployees(user);
        long totalEmployees = 0L;
        long monthHired = 0L;
        long monthLeft = 0L;
        Set<Long> activeDeptIds = new HashSet<>();
        Set<Long> scopedEmployeeIds = new HashSet<>();
        for (Employee employee : scoped) {
            if (employee.getId() != null) {
                scopedEmployeeIds.add(employee.getId());
            }
            if (isActive(employee.getStatus())) {
                totalEmployees++;
                if (employee.getDeptId() != null) {
                    activeDeptIds.add(employee.getDeptId());
                }
            }
            if (employee.getHireDate() != null && YearMonth.from(employee.getHireDate()).equals(currentMonth)) {
                monthHired++;
            }
            if (employee.getLeaveDate() != null && YearMonth.from(employee.getLeaveDate()).equals(currentMonth)) {
                monthLeft++;
            }
        }

        // 编制上限合计：只对"在职员工实际覆盖到的部门"求和（一次批量查询）
        long headcountLimit = 0L;
        if (!activeDeptIds.isEmpty()) {
            for (Dept dept : deptMapper.selectBatchIds(activeDeptIds)) {
                if (dept.getHeadcountLimit() != null) {
                    headcountLimit += dept.getHeadcountLimit();
                }
            }
        }

        // 今日考勤异常：当天 + 结果状态 2/3/4/5 + 范围内员工
        long attendanceAbnormal = 0L;
        if (!scopedEmployeeIds.isEmpty()) {
            attendanceAbnormal = count(attendanceRecordMapper.selectCount(
                    Wrappers.<AttendanceRecord>lambdaQuery()
                            .eq(AttendanceRecord::getAttendanceDate, today)
                            .in(AttendanceRecord::getAttendanceStatus, ABNORMAL_ATTENDANCE_STATUS)
                            .in(AttendanceRecord::getEmployeeId, scopedEmployeeIds)));
        }

        // 待我审批：接收人是当前用户 + 未处理 + task_type = 1（审批类）
        long pendingApprovals = 0L;
        if (user.getEmployeeId() != null) {
            pendingApprovals = count(todoTaskMapper.selectCount(
                    Wrappers.<TodoTask>lambdaQuery()
                            .eq(TodoTask::getReceiverId, user.getEmployeeId())
                            .eq(TodoTask::getHandleStatus, BizConst.TODO_PENDING)
                            .eq(TodoTask::getTaskType, BizConst.TODO_TYPE_APPROVAL)));
        }

        // 合同到期预警：生效中 + 到期日在 (今天, 今天 + warnDays] 区间内
        long contractExpiring = 0L;
        if (!scopedEmployeeIds.isEmpty()) {
            LocalDate deadline = today.plusDays(contractWarnDays());
            contractExpiring = count(employeeContractMapper.selectCount(
                    Wrappers.<EmployeeContract>lambdaQuery()
                            .in(EmployeeContract::getEmployeeId, scopedEmployeeIds)
                            .isNotNull(EmployeeContract::getEndDate)
                            .gt(EmployeeContract::getEndDate, today)
                            .le(EmployeeContract::getEndDate, deadline)
                            .eq(EmployeeContract::getStatus, CONTRACT_ACTIVE)));
        }

        return DashboardSummaryVO.builder()
                .totalEmployees(totalEmployees)
                .monthHired(monthHired)
                .monthLeft(monthLeft)
                .deptCount((long) activeDeptIds.size())
                .headcountLimit(headcountLimit)
                .attendanceAbnormal(attendanceAbnormal)
                .pendingApprovals(pendingApprovals)
                .contractExpiring(contractExpiring)
                .build();
    }

    // ------------------------------------------------------------------ F-01-02

    @Override
    public PageResult<TodoTask> todos() {
        LoginUser user = UserContext.require();
        if (user.getEmployeeId() == null) {
            // 非员工账号（如纯运维账号）没有待办，返回空列表而非报错
            return PageResult.list(List.of());
        }
        List<TodoTask> list = todoTaskMapper.selectList(Wrappers.<TodoTask>lambdaQuery()
                .eq(TodoTask::getReceiverId, user.getEmployeeId())
                .eq(TodoTask::getHandleStatus, BizConst.TODO_PENDING)
                .orderByAsc(TodoTask::getId));
        return PageResult.list(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleTodo(Long id) {
        LoginUser user = UserContext.require();
        if (id == null) {
            throw BizException.badRequest("待办 id 不能为空");
        }
        TodoTask task = todoTaskMapper.selectById(id);
        if (task == null) {
            throw BizException.notFound("待办不存在");
        }
        // BR-01-02：待办是"派给某个人的"，改别人的待办等同于越权
        if (!Objects.equals(task.getReceiverId(), user.getEmployeeId())) {
            throw BizException.forbidden("无权处理他人待办");
        }

        TodoTask update = new TodoTask();
        update.setId(task.getId());
        update.setHandleStatus(BizConst.TODO_HANDLED);
        update.setHandleTime(LocalDateTime.now());
        todoTaskMapper.updateById(update);
    }

    // ------------------------------------------------------------------ F-01-03

    @Override
    public PageResult<Notice> notices() {
        List<Notice> list = noticeMapper.selectList(Wrappers.<Notice>lambdaQuery()
                .eq(Notice::getStatus, BizConst.NOTICE_PUBLISHED)
                .orderByDesc(Notice::getIsTop)
                .orderByDesc(Notice::getPublishTime));
        return PageResult.list(list);
    }

    // ------------------------------------------------------------------ F-01-04

    @Override
    public DashboardChartsVO charts() {
        return DashboardChartsVO.builder()
                .deptDistribution(buildDeptDistribution())
                .trend(buildTrend())
                .perfGrades(buildPerfGrades())
                .build();
    }

    /**
     * 部门人数分布：取组织类型为「中心/事业部(2)」「部门(3)」的部门，
     * 统计其下非离职员工人数。员工数据一次性查出后在内存按 dept_id 分组。
     */
    private List<NameValueVO> buildDeptDistribution() {
        List<Dept> depts = deptMapper.selectList(Wrappers.<Dept>lambdaQuery()
                .select(Dept::getId, Dept::getName, Dept::getSortNo)
                .in(Dept::getDeptType, BizConst.DEPT_TYPE_CENTER, BizConst.DEPT_TYPE_DEPT)
                .orderByAsc(Dept::getSortNo)
                .orderByAsc(Dept::getId));
        if (depts.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> countByDept = new HashMap<>();
        List<Employee> employees = employeeMapper.selectList(Wrappers.<Employee>lambdaQuery()
                .select(Employee::getId, Employee::getDeptId)
                .ne(Employee::getStatus, BizConst.EMPLOYEE_RESIGNED));
        for (Employee employee : employees) {
            if (employee.getDeptId() != null) {
                countByDept.merge(employee.getDeptId(), 1L, Long::sum);
            }
        }

        List<NameValueVO> list = new ArrayList<>(depts.size());
        for (Dept dept : depts) {
            list.add(NameValueVO.builder()
                    .name(dept.getName())
                    .value(countByDept.getOrDefault(dept.getId(), 0L))
                    .build());
        }
        return list;
    }

    /**
     * 近 6 个月（含当月）人员流动趋势。
     *
     * <p>数据源为真实的 {@code employee.hire_date} / {@code employee.leave_date}：
     * 一次查询把窗口内有入职或离职动作的员工取出，再按 {@code yyyy-MM} 在内存分桶，
     * 因此没有动作的月份会稳定输出 0，而不是缺月份导致折线错位。
     */
    private List<TrendPointVO> buildTrend() {
        YearMonth current = YearMonth.now();
        YearMonth first = current.minusMonths(TREND_MONTHS - 1L);
        LocalDate from = first.atDay(1);
        LocalDate to = current.atEndOfMonth();

        // 月份桶按时间升序预置，保证 X 轴顺序稳定
        Map<String, long[]> buckets = new LinkedHashMap<>();
        for (int i = TREND_MONTHS - 1; i >= 0; i--) {
            buckets.put(current.minusMonths(i).toString(), new long[2]);
        }

        List<Employee> employees = employeeMapper.selectList(Wrappers.<Employee>lambdaQuery()
                .select(Employee::getId, Employee::getHireDate, Employee::getLeaveDate)
                .and(w -> w.between(Employee::getHireDate, from, to)
                        .or().between(Employee::getLeaveDate, from, to)));
        for (Employee employee : employees) {
            if (employee.getHireDate() != null) {
                long[] bucket = buckets.get(YearMonth.from(employee.getHireDate()).toString());
                if (bucket != null) {
                    bucket[0]++;
                }
            }
            if (employee.getLeaveDate() != null) {
                long[] bucket = buckets.get(YearMonth.from(employee.getLeaveDate()).toString());
                if (bucket != null) {
                    bucket[1]++;
                }
            }
        }

        List<TrendPointVO> trend = new ArrayList<>(buckets.size());
        buckets.forEach((month, counter) -> trend.add(TrendPointVO.builder()
                .month(month)
                .hired(counter[0])
                .left(counter[1])
                .build()));
        return trend;
    }

    /**
     * 绩效等级分布：取最近一个已发布（status ∈ {2,3,4}）的方案，
     * 按 {@code performance_record.grade} 分组计数，S~D 五个等级恒定输出。
     */
    private List<NameValueVO> buildPerfGrades() {
        Map<String, Long> countByGrade = new HashMap<>();
        List<PerformancePlan> plans = performancePlanMapper.selectList(Wrappers.<PerformancePlan>lambdaQuery()
                .select(PerformancePlan::getId)
                .in(PerformancePlan::getStatus, PUBLISHED_PLAN_STATUS)
                .orderByDesc(PerformancePlan::getId)
                .last("LIMIT 1"));
        if (!plans.isEmpty()) {
            Long planId = plans.get(0).getId();
            List<PerformanceRecord> records = performanceRecordMapper.selectList(
                    Wrappers.<PerformanceRecord>lambdaQuery()
                            .select(PerformanceRecord::getGrade)
                            .eq(PerformanceRecord::getPlanId, planId));
            for (PerformanceRecord record : records) {
                if (record.getGrade() != null) {
                    countByGrade.merge(record.getGrade(), 1L, Long::sum);
                }
            }
        }

        List<NameValueVO> grades = new ArrayList<>(GRADES.size());
        for (String grade : GRADES) {
            grades.add(NameValueVO.builder()
                    .name(grade)
                    .value(countByGrade.getOrDefault(grade, 0L))
                    .build());
        }
        return grades;
    }

    // ------------------------------------------------------------------ 内部工具

    /**
     * 按 BR-01-01 取当前用户可见范围内的员工。
     *
     * <p>判定口径与 mock 的 {@code scopeEmployees} 一致：
     * HR / ADMIN / FINANCE / EXECUTIVE 全量，部门主管本部门，其余仅本人。
     */
    private List<Employee> loadScopedEmployees(LoginUser user) {
        LambdaQueryWrapper<Employee> wrapper = Wrappers.<Employee>lambdaQuery()
                .select(Employee::getId, Employee::getStatus, Employee::getDeptId,
                        Employee::getHireDate, Employee::getLeaveDate);
        if (!DataScopeHelper.isUnlimited(user)) {
            if (DataScopeHelper.isSelfOnly(user)) {
                if (user.getEmployeeId() == null) {
                    return List.of();
                }
                wrapper.eq(Employee::getId, user.getEmployeeId());
            } else {
                if (user.getDeptId() == null) {
                    return List.of();
                }
                wrapper.eq(Employee::getDeptId, user.getDeptId());
            }
        }
        return employeeMapper.selectList(wrapper);
    }

    /** 在职 = 在职(1) 或 试用(2)，与前端"含试用期"的口径一致 */
    private boolean isActive(Integer status) {
        return status != null
                && (status == BizConst.EMPLOYEE_ACTIVE || status == BizConst.EMPLOYEE_PROBATION);
    }

    /** 合同到期预警天数，取配置 {@code ehr.business.contract-warn-days}（缺省 60） */
    private int contractWarnDays() {
        int days = ehrProperties.getBusiness().getContractWarnDays();
        return days > 0 ? days : 60;
    }

    private long count(Long value) {
        return value == null ? 0L : value;
    }
}
