package com.enterprise.hr.attendance.dto;

import com.enterprise.hr.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 考勤月报分页查询入参（F-04-06，{@code GET /attendance-months/page}）。
 *
 * @author Enterprise HR Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceMonthQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 统计年份，如 2026 */
    private Integer year;

    /** 统计月份 1-12 */
    private Integer month;

    /** 封账状态：0 未封账 / 1 已封账；空则不筛选 */
    private Integer lockStatus;
}
