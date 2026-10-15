package com.enterprise.hr.recruit.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Offer 响应 VO。
 *
 * <p>联表展示字段：{@code candidate_name}（候选人）、{@code dept_name}（录用部门）、
 * {@code position_name}（录用岗位，取自关联招聘需求快照）。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OfferVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** Offer 编号 */
    private String offerNo;

    /** 候选人ID */
    private Long candidateId;

    /** 候选人姓名（联表展示字段） */
    private String candidateName;

    /** 关联招聘需求ID */
    private Long recruitRequestId;

    /** 录用部门ID */
    private Long deptId;

    /** 录用部门名称（联表展示字段） */
    private String deptName;

    /** 录用岗位ID */
    private Long positionId;

    /** 录用岗位名称（取自关联招聘需求快照） */
    private String positionName;

    /** 定级 */
    private String jobLevel;

    /** 转正后月薪 */
    private BigDecimal offerSalary;

    /** 试用期月数 */
    private Integer probationMonths;

    /** 试用期月薪 */
    private BigDecimal probationSalary;

    /** 预计入职日期 */
    private LocalDate entryDate;

    /** 审批状态：0 草稿 / 1 审批中 / 2 已通过 / 3 已驳回 */
    private Integer approvalStatus;

    /** 当前审批人员工ID */
    private Long currentApproverId;

    /** Offer 发送时间 */
    private LocalDateTime sendTime;

    /** 候选人确认时间 */
    private LocalDateTime confirmTime;

    /** Offer 文件地址 */
    private String offerUrl;

    /** 状态：1 待发送 / 2 已发送 / 3 已接受 / 4 已拒绝 / 5 已过期 / 6 已入职 */
    private Integer status;
}
