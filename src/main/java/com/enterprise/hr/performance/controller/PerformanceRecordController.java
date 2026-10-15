package com.enterprise.hr.performance.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.performance.dto.ConfirmReq;
import com.enterprise.hr.performance.dto.PerformanceRecordQuery;
import com.enterprise.hr.performance.dto.PerformanceRecordVO;
import com.enterprise.hr.performance.dto.ScoreReq;
import com.enterprise.hr.performance.dto.ScoreResultVO;
import com.enterprise.hr.performance.dto.SelfScoreResultVO;
import com.enterprise.hr.performance.dto.SimpleResultVO;
import com.enterprise.hr.performance.service.PerformanceRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 绩效考核记录接口（F-08-03 自评 → 主管评分 → 员工确认，BR-08-01 申诉）。
 *
 * <p>类级四角色可访问，行级数据范围（员工仅本人 / 主管仅本部门 / HR 全量）在 Service 收敛；
 * 「谁能在哪条记录上做什么动作」由 Service 的本人校验与状态机共同把关，
 * 因此这里不再用方法级注解收窄角色——主管评分需要 HR 与 MANAGER 共享。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/performance-records")
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE, RoleCode.EXECUTIVE})
@RequiredArgsConstructor
public class PerformanceRecordController {

    private final PerformanceRecordService performanceRecordService;

    /** 考核记录分页（F-08-03，按数据范围过滤）：GET /api/performance-records/page?current=1&size=20&planId=&status= */
    @GetMapping("/page")
    public R<PageResult<PerformanceRecordVO>> page(@Valid PerformanceRecordQuery query) {
        return R.ok(performanceRecordService.page(query));
    }

    /** 员工自评：PUT /api/performance-records/{id}/self-score */
    @PutMapping("/{id}/self-score")
    @OperLog(module = "绩效考核", type = OperType.UPDATE, action = "员工自评")
    public R<SelfScoreResultVO> selfScore(@PathVariable("id") Long id, @Valid @RequestBody ScoreReq req) {
        return R.ok(performanceRecordService.selfScore(id, req));
    }

    /** 主管评分（按方案权重算总分并定档）：PUT /api/performance-records/{id}/leader-score */
    @PutMapping("/{id}/leader-score")
    @OperLog(module = "绩效考核", type = OperType.UPDATE, action = "主管评分")
    public R<ScoreResultVO> leaderScore(@PathVariable("id") Long id, @Valid @RequestBody ScoreReq req) {
        return R.ok(performanceRecordService.leaderScore(id, req));
    }

    /** 员工确认结果 / 申诉（BR-08-01）：PUT /api/performance-records/{id}/confirm */
    @PutMapping("/{id}/confirm")
    @OperLog(module = "绩效考核", type = OperType.APPROVE, action = "考核结果确认")
    public R<SimpleResultVO> confirm(@PathVariable("id") Long id,
                                     @Valid @RequestBody(required = false) ConfirmReq req) {
        performanceRecordService.confirm(id, req);
        return R.ok(SimpleResultVO.builder().success(true).build());
    }
}
