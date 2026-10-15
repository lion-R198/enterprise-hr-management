package com.enterprise.hr.training.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 我的培训报名 VO（{@code training_enroll} 行 + 关联计划信息）。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TrainingEnrollVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 培训计划ID */
    private Long planId;

    /** 培训计划名称（联表展示字段） */
    private String planName;

    /** 课程ID */
    private Long courseId;

    /** 报名员工ID */
    private Long employeeId;

    /** 报名时间 */
    private LocalDateTime enrollTime;

    /** 签到状态：0 未签到 / 1 已签到 / 2 缺席 */
    private Integer signInStatus;

    /** 签到时间 */
    private LocalDateTime signInTime;

    /** 考核成绩 */
    private BigDecimal examScore;

    /** 考核结果：0 未考核 / 1 合格 / 2 不合格 */
    private Integer passStatus;

    /** 满意度评分（1-5 星） */
    private Integer satisfactionScore;

    /** 培训计划状态：1 草稿 / 2 已发布 / 3 进行中 / 4 已完成 / 5 已取消 */
    private Integer status;

    /** 培训计划开始日期 */
    private LocalDate startDate;

    /** 培训计划结束日期 */
    private LocalDate endDate;
}
