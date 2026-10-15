package com.enterprise.hr.performance.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.common.security.RequireRoles;
import com.enterprise.hr.performance.dto.PerformancePlanCreateReq;
import com.enterprise.hr.performance.dto.PerformancePlanCreateVO;
import com.enterprise.hr.performance.dto.PerformancePlanQuery;
import com.enterprise.hr.performance.dto.PerformancePlanVO;
import com.enterprise.hr.performance.dto.SimpleResultVO;
import com.enterprise.hr.performance.service.PerformancePlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 考核方案接口（F-08-01 列表 / 新增草稿 / 发布）。
 *
 * <p>类级开放给绩效相关的四类角色（列表需要供主管与员工按方案筛选自己的考核记录）；
 * 「新增」与「发布」用方法级注解收窄到 HR——发布是不可逆的全员建档动作，必须专人操作。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/performance-plans")
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE, RoleCode.EXECUTIVE})
@RequiredArgsConstructor
public class PerformancePlanController {

    private final PerformancePlanService performancePlanService;

    /** 方案分页（F-08-01）：GET /api/performance-plans/page?current=1&size=10&status= */
    @GetMapping("/page")
    public R<PageResult<PerformancePlanVO>> page(@Valid PerformancePlanQuery query) {
        return R.ok(performancePlanService.page(query));
    }

    /** 新增考核方案（落草稿）：POST /api/performance-plans */
    @PostMapping
    @RequireRoles({RoleCode.HR})
    @OperLog(module = "绩效考核", type = OperType.INSERT, action = "新增考核方案")
    public R<PerformancePlanCreateVO> create(@Valid @RequestBody PerformancePlanCreateReq req) {
        return R.ok(performancePlanService.create(req));
    }

    /** 发布方案（BR-08-03，发布即生成全员考核记录）：PUT /api/performance-plans/{id}/publish */
    @PutMapping("/{id}/publish")
    @RequireRoles({RoleCode.HR})
    @OperLog(module = "绩效考核", type = OperType.APPROVE, action = "发布考核方案")
    public R<SimpleResultVO> publish(@PathVariable("id") Long id) {
        performancePlanService.publish(id);
        return R.ok(SimpleResultVO.builder().success(true).build());
    }
}
