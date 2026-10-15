package com.enterprise.hr.performance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PerformanceIndicator实体（对应表 {@code performance_indicator}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("performance_indicator")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PerformanceIndicator implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 考核方案ID */
    private Long planId;

    /** 被考核员工ID */
    private Long employeeId;

    /** 指标名称,如 需求交付及时率 */
    private String indicatorName;

    /** 指标类型:1KPI 2OKR 3能力 4态度 */
    private Integer indicatorType;

    /** 指标权重(%),同一员工合计应为100 */
    private BigDecimal weight;

    /** 目标值,如 95% */
    private String targetValue;

    /** 实际完成值 */
    private String actualValue;

    /** 评分标准说明 */
    private String scoreStandard;

    /** 自评得分(0-100) */
    private BigDecimal selfScore;

    /** 主管评分(0-100) */
    private BigDecimal leaderScore;

    /** 最终得分(按权重汇总) */
    private BigDecimal finalScore;

    /** 指标来源:1主管下达 2员工自拟 3系统模板 */
    private Integer source;

    /** 员工确认状态:0待确认 1已确认 */
    private Integer confirmStatus;

    /** 显示排序,升序 */
    private Integer sortNo;

    /** 逻辑删除:0未删除 1已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
