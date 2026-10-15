package com.enterprise.hr.performance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.security.DataScopeHelper;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.employee.entity.Dept;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.mapper.DeptMapper;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import com.enterprise.hr.performance.dto.ConfirmReq;
import com.enterprise.hr.performance.dto.PerformanceRecordQuery;
import com.enterprise.hr.performance.dto.PerformanceRecordVO;
import com.enterprise.hr.performance.dto.ScoreReq;
import com.enterprise.hr.performance.dto.ScoreResultVO;
import com.enterprise.hr.performance.dto.SelfScoreResultVO;
import com.enterprise.hr.performance.entity.PerformancePlan;
import com.enterprise.hr.performance.entity.PerformanceRecord;
import com.enterprise.hr.performance.mapper.PerformancePlanMapper;
import com.enterprise.hr.performance.mapper.PerformanceRecordMapper;
import com.enterprise.hr.performance.service.PerformanceRecordService;
import com.enterprise.hr.performance.service.support.GradeRuleResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 绩效考核记录业务实现：数据范围分页 / 自评 / 主管评分 / 员工确认申诉。
 *
 * <p>状态机（BR-08-01）：
 * <pre>
 * 1 待自评 --员工自评--> 2 待主管评 --主管评分--> 4 待员工确认 --员工确认--> 5 已完成
 *                                              └--员工申诉--> 仍为 4（confirm_status = 2，等复核）
 * </pre>
 *
 * <p>金额与分数一律 {@link BigDecimal}，总分按 2 位小数四舍五入；
 * 等级判定委托 {@link GradeRuleResolver}，阈值来自方案 {@code grade_rule} 而非代码常量。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class PerformanceRecordServiceImpl implements PerformanceRecordService {

    /** 百分数基准 */
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    /** 权重缺省值：自评 20%（方案未配置时兜底） */
    private static final BigDecimal DEFAULT_SELF_WEIGHT = new BigDecimal("20");
    /** 权重缺省值：主管 70%（方案未配置时兜底） */
    private static final BigDecimal DEFAULT_LEADER_WEIGHT = new BigDecimal("70");
    /** 分数区间下界 */
    private static final BigDecimal SCORE_MIN = BigDecimal.ZERO;
    /** 分数区间上界 */
    private static final BigDecimal SCORE_MAX = new BigDecimal("100");

    private final PerformanceRecordMapper performanceRecordMapper;
    private final PerformancePlanMapper performancePlanMapper;
    private final EmployeeMapper employeeMapper;
    private final DeptMapper deptMapper;

    // ------------------------------------------------------------------ F-08-03 列表

    @Override
    public PageResult<PerformanceRecordVO> page(PerformanceRecordQuery query) {
        PerformanceRecordQuery condition = query == null ? new PerformanceRecordQuery() : query;
        LoginUser user = UserContext.require();

        LambdaQueryWrapper<PerformanceRecord> wrapper = Wrappers.<PerformanceRecord>lambdaQuery();
        // NF-10 行级数据范围：员工仅本人、主管仅本部门、HR/ADMIN/EXECUTIVE 全量
        if (!DataScopeHelper.isUnlimited(user)) {
            if (DataScopeHelper.isSelfOnly(user)) {
                wrapper.eq(PerformanceRecord::getEmployeeId, user.getEmployeeId());
            } else {
                wrapper.eq(PerformanceRecord::getDeptId, user.getDeptId());
            }
        }
        if (condition.getPlanId() != null) {
            wrapper.eq(PerformanceRecord::getPlanId, condition.getPlanId());
        }
        if (condition.getStatus() != null) {
            wrapper.eq(PerformanceRecord::getStatus, condition.getStatus());
        }
        // 排序：方案倒序 → 状态升序 → 员工 ID 升序，保证分页结果稳定可预期
        wrapper.orderByDesc(PerformanceRecord::getPlanId)
                .orderByAsc(PerformanceRecord::getStatus)
                .orderByAsc(PerformanceRecord::getEmployeeId);

        IPage<PerformanceRecord> result = performanceRecordMapper.selectPage(condition.toPage(), wrapper);
        List<PerformanceRecordVO> records = toVOList(result.getRecords());
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    // ------------------------------------------------------------------ 员工自评

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SelfScoreResultVO selfScore(Long id, ScoreReq req) {
        LoginUser user = UserContext.require();
        PerformanceRecord record = requireRecord(id);

        // NF-10：自评只能由被考核人本人提交
        if (!Objects.equals(record.getEmployeeId(), user.getEmployeeId())) {
            throw BizException.forbidden("仅可提交本人自评");
        }
        // BR-08-01：状态机 1(待自评) → 2(待主管评)
        Integer status = record.getStatus();
        if (status == null || status != BizConst.PERF_WAIT_SELF) {
            throw new BizException(ErrorCode.PERF_STATUS_ILLEGAL, "当前状态不允许自评");
        }
        BigDecimal score = requireScore(req, "自评分需在 0-100 之间");

        PerformanceRecord update = new PerformanceRecord();
        update.setId(record.getId());
        update.setSelfScore(score);
        update.setStatus(BizConst.PERF_WAIT_LEADER);
        performanceRecordMapper.updateById(update);

        BigDecimal selfWeight = resolveSelfWeight(record.getPlanId());
        return SelfScoreResultVO.builder()
                .success(true)
                .weightNote("自评权重 " + plain(selfWeight) + "%")
                .build();
    }

    // ------------------------------------------------------------------ 主管评分

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScoreResultVO leaderScore(Long id, ScoreReq req) {
        LoginUser user = UserContext.require();
        PerformanceRecord record = requireRecord(id);

        // 权限：仅部门主管 / 人事可评分（ADMIN 由拦截器内置放行）
        if (!user.hasAnyRole(RoleCode.MANAGER, RoleCode.HR)) {
            throw BizException.forbidden("无评分权限");
        }
        // BR-08-01：状态机 2(待主管评) → 4(待员工确认)
        Integer status = record.getStatus();
        if (status == null || status != BizConst.PERF_WAIT_LEADER) {
            throw new BizException(ErrorCode.PERF_STATUS_ILLEGAL, "当前状态不允许主管评分");
        }
        BigDecimal score = requireScore(req, "评分需在 0-100 之间");

        PerformancePlan plan = record.getPlanId() == null ? null : performancePlanMapper.selectById(record.getPlanId());
        BigDecimal selfWeight = weightOf(plan == null ? null : plan.getSelfWeight(), DEFAULT_SELF_WEIGHT);
        BigDecimal leaderWeight = weightOf(plan == null ? null : plan.getLeaderWeight(), DEFAULT_LEADER_WEIGHT);

        // 最终得分 = 自评 × 自评权重% + 主管评 × 主管权重%（BR-08-03，隔级复核权重在复核环节再引入）
        BigDecimal selfScore = record.getSelfScore() == null ? BigDecimal.ZERO : record.getSelfScore();
        BigDecimal finalScore = selfScore.multiply(selfWeight)
                .add(score.multiply(leaderWeight))
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        // 等级由方案 grade_rule 解析定档，代码不写死阈值
        String grade = GradeRuleResolver.resolve(plan == null ? null : plan.getGradeRule(), finalScore);

        PerformanceRecord update = new PerformanceRecord();
        update.setId(record.getId());
        update.setLeaderScore(score);
        update.setFinalScore(finalScore);
        update.setGrade(grade);
        update.setStatus(BizConst.PERF_WAIT_CONFIRM);
        performanceRecordMapper.updateById(update);

        return ScoreResultVO.builder()
                .success(true)
                .finalScore(finalScore)
                .grade(grade)
                .build();
    }

    // ------------------------------------------------------------------ 员工确认 / 申诉

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id, ConfirmReq req) {
        LoginUser user = UserContext.require();
        PerformanceRecord record = requireRecord(id);

        // NF-10：确认与申诉都只能由被考核人本人发起
        if (!Objects.equals(record.getEmployeeId(), user.getEmployeeId())) {
            throw BizException.forbidden("仅可确认本人结果");
        }
        // BR-08-01：仅"待员工确认"可确认，其它状态拒绝
        Integer status = record.getStatus();
        if (status == null || status != BizConst.PERF_WAIT_CONFIRM) {
            throw new BizException(ErrorCode.PERF_STATUS_ILLEGAL, "当前状态不允许确认");
        }

        PerformanceRecord update = new PerformanceRecord();
        update.setId(record.getId());
        if (Boolean.TRUE.equals(req == null ? null : req.getAppeal())) {
            // BR-08-01 申诉分支：记录申诉理由，流程状态不推进，等待 HR / 复核人处理
            update.setEmployeeConfirmStatus(BizConst.CONFIRM_APPEALED);
            update.setAppealReason(req.getReason() == null ? "" : req.getReason());
        } else {
            // BR-08-01 确认分支：确认即归档完成（status = 5）
            update.setEmployeeConfirmStatus(BizConst.CONFIRM_ACCEPTED);
            update.setStatus(BizConst.PERF_ARCHIVED);
        }
        performanceRecordMapper.updateById(update);
    }

    // ------------------------------------------------------------------ 内部工具

    private PerformanceRecord requireRecord(Long id) {
        if (id == null) {
            throw BizException.badRequest("考核记录 id 不能为空");
        }
        PerformanceRecord record = performanceRecordMapper.selectById(id);
        if (record == null) {
            throw BizException.notFound("考核记录不存在");
        }
        return record;
    }

    /** 分数区间校验：0~100 之外一律拒绝；返回归一化后的 2 位小数分值 */
    private BigDecimal requireScore(ScoreReq req, String message) {
        BigDecimal score = req == null ? null : req.getScore();
        if (score == null || score.compareTo(SCORE_MIN) < 0 || score.compareTo(SCORE_MAX) > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, message);
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveSelfWeight(Long planId) {
        if (planId == null) {
            return DEFAULT_SELF_WEIGHT;
        }
        PerformancePlan plan = performancePlanMapper.selectById(planId);
        return weightOf(plan == null ? null : plan.getSelfWeight(), DEFAULT_SELF_WEIGHT);
    }

    private BigDecimal weightOf(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    /** 去掉无意义的尾随 0（20.00 → 20），与前端 mock 的展示口径一致 */
    private String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    /**
     * 批量补齐 {@code employee_name} / {@code emp_no} / {@code dept_name}
     * / {@code leader_name} / {@code plan_name}。
     *
     * <p>三次批量查库覆盖整页：员工（被考核人 + 主管合并一次）、部门、方案，
     * 关联与拼装在内存完成。
     */
    private List<PerformanceRecordVO> toVOList(List<PerformanceRecord> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        Set<Long> employeeIds = new HashSet<>();
        for (PerformanceRecord record : records) {
            if (record.getEmployeeId() != null) {
                employeeIds.add(record.getEmployeeId());
            }
            if (record.getLeaderId() != null) {
                employeeIds.add(record.getLeaderId());
            }
        }
        Map<Long, Employee> employeeMap = new HashMap<>();
        if (!employeeIds.isEmpty()) {
            employeeMap = employeeMapper.selectBatchIds(employeeIds).stream()
                    .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
        }

        Set<Long> deptIds = records.stream()
                .map(PerformanceRecord::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Dept> deptMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            deptMap = deptMapper.selectBatchIds(deptIds).stream()
                    .collect(Collectors.toMap(Dept::getId, d -> d, (a, b) -> a));
        }

        Set<Long> planIds = records.stream()
                .map(PerformanceRecord::getPlanId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> planNameMap = new HashMap<>();
        if (!planIds.isEmpty()) {
            planNameMap = performancePlanMapper.selectBatchIds(planIds).stream()
                    .collect(Collectors.toMap(PerformancePlan::getId, PerformancePlan::getPlanName, (a, b) -> a));
        }

        List<PerformanceRecordVO> list = new ArrayList<>(records.size());
        for (PerformanceRecord record : records) {
            Employee employee = employeeMap.get(record.getEmployeeId());
            Employee leader = employeeMap.get(record.getLeaderId());
            Dept dept = deptMap.get(record.getDeptId());
            list.add(PerformanceRecordVO.builder()
                    .id(record.getId())
                    .planId(record.getPlanId())
                    .planName(planNameMap.get(record.getPlanId()))
                    .employeeId(record.getEmployeeId())
                    .employeeName(employee == null ? null : employee.getName())
                    .empNo(employee == null ? null : employee.getEmpNo())
                    .deptId(record.getDeptId())
                    .deptName(dept == null ? null : dept.getName())
                    .leaderId(record.getLeaderId())
                    .leaderName(leader == null ? null : leader.getName())
                    .selfScore(record.getSelfScore())
                    .leaderScore(record.getLeaderScore())
                    .reviewScore(record.getReviewScore())
                    .finalScore(record.getFinalScore())
                    .grade(record.getGrade())
                    .deptRank(record.getDeptRank())
                    .employeeConfirmStatus(record.getEmployeeConfirmStatus())
                    .appealReason(record.getAppealReason())
                    .appealResult(record.getAppealResult())
                    .archiveStatus(record.getArchiveStatus())
                    .archiveTime(record.getArchiveTime())
                    .status(record.getStatus())
                    .remark(record.getRemark())
                    .createdAt(record.getCreatedAt())
                    .updatedAt(record.getUpdatedAt())
                    .build());
        }
        return list;
    }
}
