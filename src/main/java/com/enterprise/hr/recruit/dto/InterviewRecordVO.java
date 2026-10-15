package com.enterprise.hr.recruit.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 面试记录响应 VO。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InterviewRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 候选人ID */
    private Long candidateId;

    /** 候选人姓名（联表展示字段） */
    private String candidateName;

    /** 关联招聘需求ID */
    private Long recruitRequestId;

    /** 面试轮次，从 1 开始 */
    private Integer roundNo;

    /** 面试类型：1 初试 / 2 复试 / 3 终试 / 4 HR 面谈 */
    private Integer interviewType;

    /** 面试时间 */
    private LocalDateTime interviewTime;

    /** 面试地点 / 会议链接 */
    private String interviewPlace;

    /** 面试官员工ID */
    private Long interviewerId;

    /** 面试官姓名（联表展示字段） */
    private String interviewerName;

    /** 面试评分（0-100） */
    private Integer score;

    /** 面试评价 */
    private String evaluation;

    /** 面试结论：1 通过 / 2 待定 / 3 淘汰 */
    private Integer conclusion;
}
