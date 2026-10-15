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
import java.time.LocalDateTime;

/**
 * TrainingEnroll实体（对应表 {@code training_enroll}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("training_enroll")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TrainingEnroll implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 培训计划ID */
    private Long planId;

    /** 课程ID */
    private Long courseId;

    /** 报名员工ID */
    private Long employeeId;

    /** 报名时间 */
    private LocalDateTime enrollTime;

    /** 签到状态:0未签到 1已签到 2缺席 */
    private Integer signInStatus;

    /** 签到时间 */
    private LocalDateTime signInTime;

    /** 考核成绩(0-100) */
    private BigDecimal examScore;

    /** 考核结果:0未考核 1合格 2不合格 */
    private Integer passStatus;

    /** 满意度评分(1-5星) */
    private Integer satisfactionScore;

    /** 备注 */
    private String remark;

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
