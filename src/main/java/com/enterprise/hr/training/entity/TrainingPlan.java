package com.enterprise.hr.training.entity;

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
 * TrainingPlan实体（对应表 {@code training_plan}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("training_plan")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TrainingPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 计划编号(唯一) */
    private String planCode;

    /** 计划名称,如 2026年Q1新员工培训 */
    private String planName;

    /** 所属年度 */
    private Integer planYear;

    /** 所属季度:0全年 1-4对应Q1-Q4 */
    private Integer planQuarter;

    /** 培训类型:1新员工 2专业技能 3管理能力 4通用素质 5合规培训 */
    private Integer trainType;

    /** 培训对象范围:1全体 2指定部门 3指定岗位 4指定人员 */
    private Integer targetScope;

    /** 指定部门ID列表,逗号分隔 */
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

    /** 计划说明 */
    private String description;

    /** 状态:1草稿 2已发布 3进行中 4已完成 5已取消 */
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
