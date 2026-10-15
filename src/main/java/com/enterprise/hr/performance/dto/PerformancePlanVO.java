package com.enterprise.hr.performance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考核方案出参 VO（F-08-01 方案列表）。
 *
 * <p>响应字段蛇形（与 {@code performance_plan} 列名一致），仅 {@code recordCount}
 * 因前端 {@code performance/plan.vue} 的 {@code el-table-column prop="recordCount"}
 * 直读而保持驼峰。
 *
 * @author Enterprise HR Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PerformancePlanVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 方案编号 */
    private String planCode;

    /** 方案名称 */
    private String planName;

    /** 考核周期：1 月度 / 2 季度 / 3 半年度 / 4 年度 */
    private Integer periodType;

    /** 考核年度 */
    private Integer planYear;

    /** 考核季度 */
    private Integer planQuarter;

    /** 考核月份 */
    private Integer planMonth;

    /** 考核开始日期 */
    private LocalDate startDate;

    /** 考核截止日期 */
    private LocalDate endDate;

    /** 考核范围：1 全体 / 2 指定部门 / 3 指定岗位 / 4 指定人员 */
    private Integer scopeType;

    /** 指定部门 ID 列表，逗号分隔 */
    private String scopeDeptIds;

    /** 等级规则串 */
    private String gradeRule;

    /** 是否强制分布：1 是 / 0 否 */
    private Integer forceDistribution;

    /** 等级分布比例 */
    private String distributionRule;

    /** 员工自评权重（%） */
    private BigDecimal selfWeight;

    /** 主管评分权重（%） */
    private BigDecimal leaderWeight;

    /** 隔级复核权重（%） */
    private BigDecimal reviewWeight;

    /** 创建人员工 ID */
    private Long creatorId;

    /** 创建人姓名（关联 employee.name） */
    private String creatorName;

    /** 方案说明 */
    private String description;

    /** 状态：1 草稿 / 2 已发布 / 3 评分中 / 4 已结束 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 考核人数（该方案下已有的考核记录条数；前端 plan.vue 读 recordCount，必须保持驼峰） */
    @JsonProperty("recordCount")
    private Long recordCount;
}
