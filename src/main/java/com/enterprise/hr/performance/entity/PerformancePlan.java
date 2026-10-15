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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PerformancePlan实体（对应表 {@code performance_plan}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("performance_plan")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PerformancePlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 方案编号(唯一) */
    private String planCode;

    /** 方案名称,如 2026年Q1绩效考核 */
    private String planName;

    /** 考核周期:1月度 2季度 3半年度 4年度 */
    private Integer periodType;

    /** 考核年度 */
    private Integer planYear;

    /** 考核季度:1-4(周期为季度时有效) */
    private Integer planQuarter;

    /** 考核月份:1-12(周期为月度时有效) */
    private Integer planMonth;

    /** 考核开始日期 */
    private LocalDate startDate;

    /** 考核截止日期 */
    private LocalDate endDate;

    /** 考核范围:1全体 2指定部门 3指定岗位 4指定人员 */
    private Integer scopeType;

    /** 指定部门ID列表,逗号分隔 */
    private String scopeDeptIds;

    /** 等级规则,如 S:90+,A:80-89,B:70-79,C:60-69,D:<60 */
    private String gradeRule;

    /** 是否强制分布:1是 0否 */
    private Integer forceDistribution;

    /** 等级分布比例,如 S:10,A:20,B:50,C:15,D:5 */
    private String distributionRule;

    /** 员工自评权重(%) */
    private BigDecimal selfWeight;

    /** 主管评分权重(%) */
    private BigDecimal leaderWeight;

    /** 隔级复核权重(%) */
    private BigDecimal reviewWeight;

    /** 创建人员工ID */
    private Long creatorId;

    /** 方案说明 */
    private String description;

    /** 状态:1草稿 2已发布 3评分中 4已结束 */
    private Integer status;

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
