package com.enterprise.hr.performance.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 考核方案分页查询入参（F-08-01，{@code GET /performance-plans/page}）。
 *
 * <p>查询参数一律驼峰命名，与前端 {@code api/performance.js#pagePerfPlans} 透传的
 * {@code { current, size, status }} 一致。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PerformancePlanQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 方案状态：1 草稿 / 2 已发布 / 3 评分中 / 4 已结束；为空表示不筛选 */
    private Integer status;
}
