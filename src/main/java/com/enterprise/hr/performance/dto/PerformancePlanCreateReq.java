package com.enterprise.hr.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增考核方案入参（{@code POST /performance-plans}）。
 *
 * <p>请求体驼峰命名（不标 {@code @JsonNaming}），字段与前端
 * {@code performance/plan.vue} 的新增表单一一对应。
 *
 * <p>缺省值不在此处填充，统一落在 Service：{@code periodType=2}、{@code scopeType=1}、
 * {@code gradeRule} 默认串、权重 {@code 20/70/10}，保证"表单不传即走默认"的语义
 * 与前端 mock 完全一致。
 *
 * @author Enterprise HR Team
 */
@Data
public class PerformancePlanCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 方案名称，如「2026年Q4绩效考核」 */
    @NotBlank(message = "方案名称不能为空")
    private String planName;

    /** 考核年度，如 2026 */
    @NotNull(message = "考核年度不能为空")
    private Integer planYear;

    /** 考核季度：1-4（周期为季度时有效） */
    private Integer planQuarter;

    /** 考核月份：1-12（周期为月度时有效） */
    private Integer planMonth;

    /** 考核周期：1 月度 / 2 季度 / 3 半年度 / 4 年度；缺省 2 */
    private Integer periodType;

    /** 考核开始日期 */
    private LocalDate startDate;

    /** 考核截止日期 */
    private LocalDate endDate;

    /** 考核范围：1 全体 / 2 指定部门 / 3 指定岗位 / 4 指定人员；缺省 1 */
    private Integer scopeType;

    /** 等级规则串，如 {@code S:90+,A:80-89,B:70-79,C:60-69,D:<60} */
    private String gradeRule;

    /** 是否强制分布：1 是 / 0 否；缺省 0 */
    private Integer forceDistribution;

    /** 等级分布比例，如 {@code S:10,A:20,B:50,C:15,D:5} */
    private String distributionRule;

    /** 员工自评权重（%）；缺省 20 */
    private BigDecimal selfWeight;

    /** 主管评分权重（%）；缺省 70 */
    private BigDecimal leaderWeight;

    /** 隔级复核权重（%）；缺省 10 */
    private BigDecimal reviewWeight;

    /** 方案说明 */
    private String description;
}
