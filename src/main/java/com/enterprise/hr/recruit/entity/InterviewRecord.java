package com.enterprise.hr.recruit.entity;

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
 * InterviewRecord实体（对应表 {@code interview_record}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("interview_record")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InterviewRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 候选人ID */
    private Long candidateId;

    /** 关联招聘需求ID */
    private Long recruitRequestId;

    /** 面试轮次,从1开始 */
    private Integer roundNo;

    /** 面试类型:1初试 2复试 3终试 4HR面谈 */
    private Integer interviewType;

    /** 面试时间 */
    private LocalDateTime interviewTime;

    /** 面试地点/会议链接 */
    private String interviewPlace;

    /** 面试官员工ID */
    private Long interviewerId;

    /** 面试评分(0-100) */
    private Integer score;

    /** 面试评价 */
    private String evaluation;

    /** 面试结论:1通过 2待定 3淘汰 */
    private Integer conclusion;

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
