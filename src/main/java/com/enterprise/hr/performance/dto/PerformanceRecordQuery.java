package com.enterprise.hr.performance.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 考核记录分页查询入参（F-08-03，{@code GET /performance-records/page}）。
 *
 * <p>查询参数驼峰命名，与前端 {@code performance/record.vue} 的
 * {@code { current, size, planId, status }} 一致。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PerformanceRecordQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 考核方案 ID；为空表示全部方案 */
    private Long planId;

    /** 流程状态：1 待自评 / 2 待主管评 / 3 待复核 / 4 待员工确认 / 5 已完成 */
    private Integer status;
}
