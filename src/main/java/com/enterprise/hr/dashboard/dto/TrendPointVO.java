package com.enterprise.hr.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 人员流动趋势点（F-01-04 近 6 个月折线图）。
 *
 * <p>字段名驼峰，前端 {@code dashboard/index.vue} 读取
 * {@code t.month} / {@code t.hired} / {@code t.left} 作为 X 轴与两条折线。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 统计月份，格式 {@code yyyy-MM} */
    private String month;

    /** 当月入职人数 */
    private Long hired;

    /** 当月离职人数 */
    private Long left;
}
