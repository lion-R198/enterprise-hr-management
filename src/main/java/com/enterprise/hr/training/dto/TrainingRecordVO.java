package com.enterprise.hr.training.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 培训档案响应 VO（{@code training_record} 行）。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TrainingRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 关联培训计划ID */
    private Long planId;

    /** 关联课程ID */
    private Long courseId;

    /** 课程名称（快照） */
    private String courseName;

    /** 培训类型：1 新员工 / 2 专业技能 / 3 管理能力 / 4 通用素质 / 5 合规培训 */
    private Integer trainType;

    /** 获得学时 */
    private BigDecimal hours;

    /** 培训日期 */
    private LocalDate trainDate;

    /** 考核成绩 */
    private BigDecimal examScore;

    /** 考核结果：0 未考核 / 1 合格 / 2 不合格 */
    private Integer passStatus;

    /** 证书编号 */
    private String certificateNo;

    /** 归档年度 */
    private Integer archiveYear;
}
