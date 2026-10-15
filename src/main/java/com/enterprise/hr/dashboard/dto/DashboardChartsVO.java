package com.enterprise.hr.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 首页图表聚合出参（F-01-04，{@code GET /dashboard/charts}）。
 *
 * <p>三个维度一次返回，前端只发一次请求即可渲染柱/折线/饼三张图。
 * 字段名驼峰（不标 {@code @JsonNaming}），与 {@code dashboard/index.vue}
 * 的 {@code data.deptDistribution} / {@code data.trend} / {@code data.perfGrades} 对齐。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardChartsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 部门人数分布：[{name: 部门名, value: 在职人数}]，取 dept_type 为 2/3 的部门 */
    private List<NameValueVO> deptDistribution;

    /** 近 6 个月（含当月）人员流动趋势，按月份升序 */
    private List<TrendPointVO> trend;

    /** 绩效等级分布：[{name: S~D, value: 人数}]，取最近一个已发布方案 */
    private List<NameValueVO> perfGrades;
}
