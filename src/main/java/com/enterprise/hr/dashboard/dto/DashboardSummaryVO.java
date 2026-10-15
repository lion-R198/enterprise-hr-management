package com.enterprise.hr.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 首页指标卡出参（F-01-01，{@code GET /dashboard/summary}）。
 *
 * <p><b>本 VO 刻意不标 {@code @JsonNaming(SnakeCaseStrategy)}</b>：
 * 首页看板是按角色看数据的"总览面板"，前端 {@code dashboard/index.vue} 直接消费
 * {@code s.totalEmployees} / {@code s.headcountLimit} / {@code s.attendanceAbnormal}
 * 等驼峰 key，故字段名直接写成驼峰 Java 字段名，Jackson 默认策略序列化后即为驼峰。
 * 这是契约中唯一一处"响应也走驼峰"的场景，改动字段名即破坏前端。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 在职人数（含试用期），即 {@code employee.status ∈ {1 在职, 2 试用}} */
    private Long totalEmployees;

    /** 本月入职人数（按 {@code employee.hire_date} 落在当月统计） */
    private Long monthHired;

    /** 本月离职人数（按 {@code employee.leave_date} 落在当月统计） */
    private Long monthLeft;

    /** 部门数（在职员工实际覆盖到的部门数量） */
    private Long deptCount;

    /** 编制上限合计（上述部门 {@code dept.headcount_limit} 之和） */
    private Long headcountLimit;

    /** 今日考勤异常条数（{@code attendance_status ∈ {2 迟到, 3 早退, 4 缺卡, 5 旷工}}） */
    private Long attendanceAbnormal;

    /** 待我审批数：当前用户未处理且 {@code task_type = 1} 的待办条数 */
    private Long pendingApprovals;

    /** 合同到期预警数：{@code ehr.business.contract-warn-days} 天内到期且尚未过期的生效合同 */
    private Long contractExpiring;
}
