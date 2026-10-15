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
 * TrainingRecord实体（对应表 {@code training_record}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("training_record")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TrainingRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 关联培训计划ID */
    private Long planId;

    /** 关联课程ID */
    private Long courseId;

    /** 课程名称(快照,防课程改名影响历史) */
    private String courseName;

    /** 培训类型:1新员工 2专业技能 3管理能力 4通用素质 5合规培训 */
    private Integer trainType;

    /** 获得学时 */
    private BigDecimal hours;

    /** 培训日期 */
    private LocalDate trainDate;

    /** 考核成绩 */
    private BigDecimal examScore;

    /** 考核结果:0未考核 1合格 2不合格 */
    private Integer passStatus;

    /** 证书编号 */
    private String certificateNo;

    /** 归档年度 */
    private Integer archiveYear;

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
