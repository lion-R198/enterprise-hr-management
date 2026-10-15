package com.enterprise.hr.dashboard.service;

import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.dashboard.dto.DashboardChartsVO;
import com.enterprise.hr.dashboard.dto.DashboardSummaryVO;
import com.enterprise.hr.notice.entity.Notice;
import com.enterprise.hr.notice.entity.TodoTask;

/**
 * 首页看板聚合业务接口（F-01-01 ~ F-01-04）。
 *
 * <p>看板是"跨模块只读聚合"：员工、部门、考勤、待办、公告、合同、绩效七张表的数据
 * 在此收敛成四个接口。所有统计都在 Service 内完成，
 * <b>先一次性取数、再在内存分组聚合</b>，不在循环里查库。
 *
 * @author Enterprise HR Team
 */
public interface DashboardService {

    /**
     * 指标卡汇总（F-01-01）。
     *
     * <p>BR-01-01：所有指标都限定在当前登录用户的行级数据范围内
     * ——决策层 / HR / 财务 / 管理员看全量，部门主管看本部门，普通员工只看本人。
     * 返回体为驼峰字段（前端 {@code dashboard/index.vue} 直读）。
     */
    DashboardSummaryVO summary();

    /** 当前用户未处理的待办列表（F-01-02），按接收人 = 当前用户员工 ID 过滤 */
    PageResult<TodoTask> todos();

    /** 处理待办（F-01-02）：置已处理并落处理时间；非本人待办 403，待办不存在 404 */
    void handleTodo(Long id);

    /** 已发布公告列表（F-01-03）：置顶优先，其次按发布时间倒序 */
    PageResult<Notice> notices();

    /**
     * 图表聚合（F-01-04）：部门人数分布 + 近 6 个月人员流动趋势 + 绩效等级分布。
     *
     * <p>趋势与等级分布均基于真实业务数据统计（{@code employee.hire_date} /
     * {@code employee.leave_date} / {@code performance_record.grade}），
     * 不使用前端 mock 里造的假数。
     */
    DashboardChartsVO charts();
}
