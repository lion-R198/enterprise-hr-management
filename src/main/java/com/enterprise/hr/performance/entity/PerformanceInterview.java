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
import java.time.LocalDateTime;

/**
 * PerformanceInterview实体（对应表 {@code performance_interview}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("performance_interview")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PerformanceInterview implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 绩效考核记录ID */
    private Long recordId;

    /** 被面谈员工ID */
    private Long employeeId;

    /** 面谈人(主管)员工ID */
    private Long interviewerId;

    /** 面谈时间 */
    private LocalDateTime interviewTime;

    /** 面谈地点 */
    private String interviewPlace;

    /** 本期成绩与亮点 */
    private String achievement;

    /** 不足与改进方向 */
    private String shortcoming;

    /** 下期目标 */
    private String nextGoal;

    /** 员工反馈与诉求 */
    private String employeeFeedback;

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
