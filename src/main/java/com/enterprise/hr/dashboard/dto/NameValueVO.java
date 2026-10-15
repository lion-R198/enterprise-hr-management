package com.enterprise.hr.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用「名称-数值」图表项（F-01-04 部门分布与绩效等级分布共用）。
 *
 * <p>沿用 ECharts 饼图默认的 {@code {name, value}} 数据格式，前端
 * {@code dashboard/index.vue} 直接把它塞进 {@code series[0].data}，无需二次映射。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NameValueVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 维度名称（部门名 / 绩效等级 S~D） */
    private String name;

    /** 数值（人数） */
    private Long value;
}
