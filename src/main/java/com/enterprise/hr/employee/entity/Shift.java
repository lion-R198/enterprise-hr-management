package com.enterprise.hr.employee.entity;

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
 * 班次定义实体（对应表 {@code shift}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("shift")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Shift implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 班次编码（唯一），如 STD/DAY/NIGHT */
    @TableField("shift_code")
    private String shiftCode;

    /** 班次名称，如 标准班 */
    @TableField("shift_name")
    private String shiftName;

    /** 上班时间 */
    @TableField("work_start")
    private String workStart;

    /** 下班时间 */
    @TableField("work_end")
    private String workEnd;

    /** 休息时长（分钟） */
    @TableField("rest_minutes")
    private Integer restMinutes;

    /** 标准工时（小时） */
    @TableField("standard_hours")
    private BigDecimal standardHours;

    /** 迟到容忍（分钟），超出计迟到 */
    @TableField("late_tolerance")
    private Integer lateTolerance;

    /** 早退容忍（分钟） */
    @TableField("early_tolerance")
    private Integer earlyTolerance;

    /** 弹性上班提前分钟数 */
    @TableField("flex_start")
    private Integer flexStart;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;

    /** 显示排序，升序 */
    @TableField("sort_no")
    private Integer sortNo;

    /** 备注 */
    private String remark;

    /** 逻辑删除：0 未删除 / 1 已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
