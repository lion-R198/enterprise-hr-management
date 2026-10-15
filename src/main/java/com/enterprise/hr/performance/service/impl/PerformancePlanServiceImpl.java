package com.enterprise.hr.performance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.common.util.BizNoGenerator;
import com.enterprise.hr.employee.entity.Dept;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.mapper.DeptMapper;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import com.enterprise.hr.performance.dto.PerformancePlanCreateReq;
import com.enterprise.hr.performance.dto.PerformancePlanCreateVO;
import com.enterprise.hr.performance.dto.PerformancePlanQuery;
import com.enterprise.hr.performance.dto.PerformancePlanVO;
import com.enterprise.hr.performance.entity.PerformancePlan;
import com.enterprise.hr.performance.entity.PerformanceRecord;
import com.enterprise.hr.performance.mapper.PerformancePlanMapper;
import com.enterprise.hr.performance.mapper.PerformanceRecordMapper;
import com.enterprise.hr.performance.service.PerformancePlanService;
import com.enterprise.hr.performance.service.support.GradeRuleResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 考核方案业务实现：列表 / 新增（草稿）/ 发布。
 *
 * <p>业务规则与默认值全部落在此层，Controller 只做参数绑定与响应封装。
 * 所有"按页查出的关联展示字段"（创建人姓名、考核人数）均以
 * <b>两次批量查库 + 内存分组</b> 的方式补齐，杜绝循环内查库。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class PerformancePlanServiceImpl implements PerformancePlanService {

    /** 默认等级规则：与前端 mock 及 {@code performance/plan.vue} 表单初值一致 */
    private static final String DEFAULT_GRADE_RULE = GradeRuleResolver.DEFAULT_RULE;
    /** 默认考核周期：季度 */
    private static final int DEFAULT_PERIOD_TYPE = 2;
    /** 默认考核范围：全体 */
    private static final int DEFAULT_SCOPE_TYPE = 1;
    /** 默认自评权重（%） */
    private static final BigDecimal DEFAULT_SELF_WEIGHT = new BigDecimal("20");
    /** 默认主管评分权重（%） */
    private static final BigDecimal DEFAULT_LEADER_WEIGHT = new BigDecimal("70");
    /** 默认隔级复核权重（%） */
    private static final BigDecimal DEFAULT_REVIEW_WEIGHT = new BigDecimal("10");

    // ---------------- performance_plan.status（BizConst 只覆盖 performance_record.status，方案状态在此局部定义） ----------------
    /** 方案状态：1 草稿（新增即草稿，必须显式发布） */
    private static final int PLAN_STATUS_DRAFT = 1;
    /** 方案状态：3 评分中（发布后进入，前端 perfPlanStatus 字典同值） */
    private static final int PLAN_STATUS_SCORING = 3;

    /** 归档状态：0 未归档（{@code performance_record.archive_status}，BizConst 无对应常量） */
    private static final int ARCHIVE_NONE = 0;

    private final PerformancePlanMapper performancePlanMapper;
    private final PerformanceRecordMapper performanceRecordMapper;
    private final EmployeeMapper employeeMapper;
    private final DeptMapper deptMapper;

    // ------------------------------------------------------------------ F-08-01

    @Override
    public PageResult<PerformancePlanVO> page(PerformancePlanQuery query) {
        PerformancePlanQuery condition = query == null ? new PerformancePlanQuery() : query;

        LambdaQueryWrapper<PerformancePlan> wrapper = Wrappers.<PerformancePlan>lambdaQuery();
        if (condition.getStatus() != null) {
            wrapper.eq(PerformancePlan::getStatus, condition.getStatus());
        }
        // 方案编号倒序：新方案永远排在最前，便于 HR 立即看到刚建的计划
        wrapper.orderByDesc(PerformancePlan::getId);

        IPage<PerformancePlan> result = performancePlanMapper.selectPage(condition.toPage(), wrapper);
        List<PerformancePlanVO> records = toVOList(result.getRecords());
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    // ------------------------------------------------------------------ 新增

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PerformancePlanCreateVO create(PerformancePlanCreateReq req) {
        LoginUser user = UserContext.require();

        PerformancePlan plan = new PerformancePlan();
        plan.setPlanCode(nextPlanCode());
        plan.setPlanName(req.getPlanName().trim());
        plan.setPeriodType(defaultInt(req.getPeriodType(), DEFAULT_PERIOD_TYPE));
        plan.setPlanYear(req.getPlanYear());
        plan.setPlanQuarter(req.getPlanQuarter());
        plan.setPlanMonth(req.getPlanMonth());
        plan.setStartDate(req.getStartDate());
        plan.setEndDate(req.getEndDate());
        plan.setScopeType(defaultInt(req.getScopeType(), DEFAULT_SCOPE_TYPE));
        plan.setGradeRule(StringUtils.hasText(req.getGradeRule()) ? req.getGradeRule().trim() : DEFAULT_GRADE_RULE);
        plan.setForceDistribution(defaultInt(req.getForceDistribution(), 0));
        plan.setDistributionRule(req.getDistributionRule());
        plan.setSelfWeight(defaultDecimal(req.getSelfWeight(), DEFAULT_SELF_WEIGHT));
        plan.setLeaderWeight(defaultDecimal(req.getLeaderWeight(), DEFAULT_LEADER_WEIGHT));
        plan.setReviewWeight(defaultDecimal(req.getReviewWeight(), DEFAULT_REVIEW_WEIGHT));
        // 创建人取当前登录用户关联员工；非员工账号（纯运维）允许为空
        plan.setCreatorId(user.getEmployeeId());
        plan.setDescription(req.getDescription());
        // 新增即草稿：必须显式发布才会生成考核记录（BR-08-03）
        plan.setStatus(PLAN_STATUS_DRAFT);
        performancePlanMapper.insert(plan);

        return PerformancePlanCreateVO.builder()
                .success(true)
                .id(plan.getId())
                .planCode(plan.getPlanCode())
                .build();
    }

    // ------------------------------------------------------------------ 发布

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        PerformancePlan plan = requirePlan(id);

        // BR-08-03：仅草稿（status = 1）可发布，重复发布/发布已结束方案一律拒绝
        Integer status = plan.getStatus();
        if (status == null || status != PLAN_STATUS_DRAFT) {
            throw new BizException(ErrorCode.PERF_STATUS_ILLEGAL, "仅草稿状态可发布");
        }

        PerformancePlan update = new PerformancePlan();
        update.setId(plan.getId());
        // 评分中：与 mock 的 p.status = 3 一致
        update.setStatus(PLAN_STATUS_SCORING);
        performancePlanMapper.updateById(update);

        // 发布即生成全员考核记录：范围与 mock 一致，取所有非离职员工（status != 0，含在职/试用/待入职）
        List<Employee> employees = employeeMapper.selectList(Wrappers.<Employee>lambdaQuery()
                .select(Employee::getId, Employee::getDeptId, Employee::getManagerId)
                .ne(Employee::getStatus, BizConst.EMPLOYEE_RESIGNED));
        if (employees.isEmpty()) {
            return;
        }

        // 幂等：已存在考核记录的员工直接跳过，避免重复发布产生重复单据
        Set<Long> existingEmployeeIds = performanceRecordMapper.selectList(
                        Wrappers.<PerformanceRecord>lambdaQuery()
                                .select(PerformanceRecord::getEmployeeId)
                                .eq(PerformanceRecord::getPlanId, plan.getId()))
                .stream()
                .map(PerformanceRecord::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 主管兜底：manager_id 为空的员工取其部门负责人；部门一次性批量查出，避免循环查库
        Map<Long, Long> deptLeaderMap = loadDeptLeaders(employees);

        for (Employee employee : employees) {
            if (existingEmployeeIds.contains(employee.getId())) {
                continue;
            }
            PerformanceRecord record = new PerformanceRecord();
            record.setPlanId(plan.getId());
            record.setEmployeeId(employee.getId());
            record.setDeptId(employee.getDeptId());
            record.setLeaderId(resolveLeaderId(employee, deptLeaderMap));
            // 待自评：流程起点（BR-08-01 状态机 1 → 2 → 4 → 5）
            record.setStatus(BizConst.PERF_WAIT_SELF);
            record.setEmployeeConfirmStatus(BizConst.CONFIRM_NONE);
            record.setArchiveStatus(ARCHIVE_NONE);
            performanceRecordMapper.insert(record);
        }
    }

    // ------------------------------------------------------------------ 内部工具

    private PerformancePlan requirePlan(Long id) {
        if (id == null) {
            throw BizException.badRequest("考核方案 id 不能为空");
        }
        PerformancePlan plan = performancePlanMapper.selectById(id);
        if (plan == null) {
            throw BizException.notFound("考核方案不存在");
        }
        return plan;
    }

    /** 方案编号：{@code PF + 年份 + 5 位流水}，流水取当年同前缀已有方案数 + 1 */
    private String nextPlanCode() {
        Long used = performancePlanMapper.selectCount(Wrappers.<PerformancePlan>lambdaQuery()
                .likeRight(PerformancePlan::getPlanCode, BizNoGenerator.yearPrefix("PF")));
        return BizNoGenerator.next("PF", (used == null ? 0L : used) + 1);
    }

    /**
     * 批量补齐 {@code creator_name} 与 {@code recordCount}。
     *
     * <p>两次查询即可覆盖整页数据：一次查创建人、一次按方案 ID 集合查考核记录，
     * 分组统计在内存完成。
     */
    private List<PerformancePlanVO> toVOList(List<PerformancePlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return List.of();
        }

        Set<Long> creatorIds = plans.stream()
                .map(PerformancePlan::getCreatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Employee> creatorMap = new HashMap<>();
        if (!creatorIds.isEmpty()) {
            creatorMap = employeeMapper.selectBatchIds(creatorIds).stream()
                    .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
        }

        List<Long> planIds = plans.stream()
                .map(PerformancePlan::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Long> recordCountMap = new HashMap<>();
        if (!planIds.isEmpty()) {
            List<PerformanceRecord> records = performanceRecordMapper.selectList(
                    Wrappers.<PerformanceRecord>lambdaQuery()
                            .select(PerformanceRecord::getPlanId)
                            .in(PerformanceRecord::getPlanId, planIds));
            for (PerformanceRecord record : records) {
                recordCountMap.merge(record.getPlanId(), 1L, Long::sum);
            }
        }

        List<PerformancePlanVO> list = new ArrayList<>(plans.size());
        for (PerformancePlan plan : plans) {
            Employee creator = creatorMap.get(plan.getCreatorId());
            list.add(PerformancePlanVO.builder()
                    .id(plan.getId())
                    .planCode(plan.getPlanCode())
                    .planName(plan.getPlanName())
                    .periodType(plan.getPeriodType())
                    .planYear(plan.getPlanYear())
                    .planQuarter(plan.getPlanQuarter())
                    .planMonth(plan.getPlanMonth())
                    .startDate(plan.getStartDate())
                    .endDate(plan.getEndDate())
                    .scopeType(plan.getScopeType())
                    .scopeDeptIds(plan.getScopeDeptIds())
                    .gradeRule(plan.getGradeRule())
                    .forceDistribution(plan.getForceDistribution())
                    .distributionRule(plan.getDistributionRule())
                    .selfWeight(plan.getSelfWeight())
                    .leaderWeight(plan.getLeaderWeight())
                    .reviewWeight(plan.getReviewWeight())
                    .creatorId(plan.getCreatorId())
                    .creatorName(creator == null ? null : creator.getName())
                    .description(plan.getDescription())
                    .status(plan.getStatus())
                    .createdAt(plan.getCreatedAt())
                    .updatedAt(plan.getUpdatedAt())
                    .recordCount(recordCountMap.getOrDefault(plan.getId(), 0L))
                    .build());
        }
        return list;
    }

    /** 批量取本批员工所涉部门的负责人（仅查一次） */
    private Map<Long, Long> loadDeptLeaders(List<Employee> employees) {
        Set<Long> deptIds = new HashSet<>();
        for (Employee employee : employees) {
            if (employee.getManagerId() == null && employee.getDeptId() != null) {
                deptIds.add(employee.getDeptId());
            }
        }
        if (deptIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> map = new HashMap<>();
        for (Dept dept : deptMapper.selectBatchIds(deptIds)) {
            if (dept.getLeaderId() != null) {
                map.put(dept.getId(), dept.getLeaderId());
            }
        }
        return map;
    }

    /** 考核主管：优先直属主管（manager_id），为空时兜底部门负责人 */
    private Long resolveLeaderId(Employee employee, Map<Long, Long> deptLeaderMap) {
        if (employee.getManagerId() != null) {
            return employee.getManagerId();
        }
        return employee.getDeptId() == null ? null : deptLeaderMap.get(employee.getDeptId());
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }
}
