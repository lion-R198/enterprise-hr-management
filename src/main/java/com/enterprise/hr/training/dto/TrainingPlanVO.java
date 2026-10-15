package com.enterprise.hr.training.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 培训计划响应 VO。
 *
 * <p>响应主体为蛇形字段，但 {@code enrollCount}（已报名人数）与 {@code courseNames}
 * （关联课程名）是前端 {@code training/plan.vue} 已消费的驼峰字段，用 {@link JsonProperty} 固定，
 * 不可随命名策略改写。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TrainingPlanVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 计划编号，如 TP20260001 */
    private String planCode;

    /** 计划名称 */
    private String planName;

    /** 所属年度 */
    private Integer planYear;

    /** 所属季度：0 全年 / 1-4 对应 Q1-Q4 */
    private Integer planQuarter;

    /** 培训类型：1 新员工 / 2 专业技能 / 3 管理能力 / 4 通用素质 / 5 合规培训 */
    private Integer trainType;

    /** 培训对象范围：1 全体 / 2 指定部门 / 3 指定岗位 / 4 指定人员 */
    private Integer targetScope;

    /** 指定部门ID列表，逗号分隔 */
    private String targetDeptIds;

    /** 预算金额 */
    private BigDecimal budgetAmount;

    /** 实际支出 */
    private BigDecimal actualAmount;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 组织人员工ID */
    private Long organizerId;

    /** 组织人姓名（联表展示字段） */
    private String organizerName;

    /** 计划说明 */
    private String description;

    /** 状态：1 草稿 / 2 已发布 / 3 进行中 / 4 已完成 / 5 已取消 */
    private Integer status;

    /** 已报名人数（联表统计字段，前端 plan.vue 读 row.enrollCount） */
    @JsonProperty("enrollCount")
    private Integer enrollCount;

    /** 关联课程名称，多个以「、」连接（前端 plan.vue 读 row.courseNames） */
    @JsonProperty("courseNames")
    private String courseNames;
}
