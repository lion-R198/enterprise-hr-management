package com.enterprise.hr.recruit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 安排面试 / 录入评价入参（{@code POST /interview-records}）。
 *
 * @author Enterprise HR Team
 */
@Data
public class InterviewRecordCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 候选人ID */
    @NotNull(message = "候选人不能为空")
    private Long candidateId;

    /** 面试类型：1 初试 / 2 复试 / 3 终试 / 4 HR 面谈 */
    @NotNull(message = "面试类型不能为空")
    private Integer interviewType;

    /** 面试时间（可为空表示待安排） */
    private LocalDateTime interviewTime;

    /** 面试地点 / 会议链接 */
    private String interviewPlace;

    /** 面试官员工ID */
    private Long interviewerId;

    /** 面试评分（0-100） */
    private Integer score;

    /** 面试评价 */
    private String evaluation;

    /** 面试结论：1 通过 / 2 待定 / 3 淘汰（空表示尚未出结论） */
    private Integer conclusion;
}
