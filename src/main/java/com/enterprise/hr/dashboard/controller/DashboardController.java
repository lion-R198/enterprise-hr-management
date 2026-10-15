package com.enterprise.hr.dashboard.controller;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.log.OperLog;
import com.enterprise.hr.common.log.OperType;
import com.enterprise.hr.dashboard.dto.DashboardChartsVO;
import com.enterprise.hr.dashboard.dto.DashboardSummaryVO;
import com.enterprise.hr.dashboard.service.DashboardService;
import com.enterprise.hr.notice.entity.Notice;
import com.enterprise.hr.notice.entity.TodoTask;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页看板接口（F-01-01 概览 / F-01-02 待办 / F-01-03 公告 / F-01-04 图表）。
 *
 * <p><b>类级不加 {@code @RequireRoles}</b>：首页是登录后的落地页，
 * 六种角色看到的是同一套接口、不同的数据。行级范围由 Service 内的
 * {@code DataScopeHelper} 收敛（BR-01-01），而不是靠接口级角色拦截
 * ——否则"主管点进首页 403"会直接断掉导航主链路。
 *
 * @author Enterprise HR Team
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 指标卡汇总（BR-01-01 按角色数据范围）：GET /api/dashboard/summary */
    @GetMapping("/summary")
    public R<DashboardSummaryVO> summary() {
        return R.ok(dashboardService.summary());
    }

    /** 我的待办（F-01-02）：GET /api/dashboard/todos */
    @GetMapping("/todos")
    public R<PageResult<TodoTask>> todos() {
        return R.ok(dashboardService.todos());
    }

    /** 处理待办：PUT /api/dashboard/todos/{id}/handle */
    @PutMapping("/todos/{id}/handle")
    @OperLog(module = "首页看板", type = OperType.UPDATE, action = "处理待办")
    public R<Void> handleTodo(@PathVariable("id") Long id) {
        dashboardService.handleTodo(id);
        return R.ok();
    }

    /** 通知公告（F-01-03，仅已发布、置顶优先）：GET /api/dashboard/notices */
    @GetMapping("/notices")
    public R<PageResult<Notice>> notices() {
        return R.ok(dashboardService.notices());
    }

    /** 图表分析（F-01-04）：GET /api/dashboard/charts */
    @GetMapping("/charts")
    public R<DashboardChartsVO> charts() {
        return R.ok(dashboardService.charts());
    }
}
