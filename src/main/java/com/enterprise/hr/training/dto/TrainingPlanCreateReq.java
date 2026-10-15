package com.enterprise.hr.training.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增培训计划入参（{@code POST /training-plans}）。
 *
 * @author Enterprise HR Team
 */
@Data
public class TrainingPlanCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 计划名称，如 2026年Q1新员工培训 */
    @NotBlank(message = "计划名称不能为空")
    private String planName;

    /** 所属年度 */
    @NotNull(message = "所属年度不能为空")
    private Integer planYear;

    /** 所属季度：0 全年 / 1-4 对应 Q1-Q4；空则按全年处理 */
    private Integer planQuarter;

    /** 培训类型：1 新员工 / 2 专业技能 / 3 管理能力 / 4 通用素质 / 5 合规培训；空则按 2 处理 */
    private Integer trainType;

    /** 培训对象范围：1 全体 / 2 指定部门 / 3 指定岗位 / 4 指定人员；空则按 1 处理 */
    private Integer targetScope;

    /** 预算金额 */
    private BigDecimal budgetAmount;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 计划说明 */
    private String description;
}
