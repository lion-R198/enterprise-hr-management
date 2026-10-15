package com.enterprise.hr.recruit.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 招聘需求响应 VO。
 *
 * <p>响应统一蛇形字段；{@code dept_name} / {@code applicant_name} 为联表展示字段，
 * 由 Service 批量补齐（一次 IN 查询后内存映射，避免逐行查库）。
 *
 * @author Enterprise HR Team
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RecruitRequestVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 需求单号，如 RC202600001 */
    private String requestNo;

    /** 需求部门ID */
    private Long requestDeptId;

    /** 需求部门名称（联表展示字段） */
    private String deptName;

    /** 招聘岗位ID */
    private Long positionId;

    /** 招聘岗位名称（快照） */
    private String positionName;

    /** 招聘人数 */
    private Integer recruitCount;

    /** 已录用人数 */
    private Integer hiredCount;

    /** 职级 */
    private String jobLevel;

    /** 薪资区间下限 */
    private BigDecimal salaryMin;

    /** 薪资区间上限 */
    private BigDecimal salaryMax;

    /** 学历要求：1 博士 / 2 硕士 / 3 本科 / 4 大专 / 5 中专 / 6 高中 / 7 不限 */
    private Integer educationReq;

    /** 经验要求（年） */
    private Integer experienceYears;

    /** 岗位职责 */
    private String jobDescription;

    /** 任职要求 */
    private String jobRequirement;

    /** 期望到岗日期 */
    private LocalDate hopeArriveDate;

    /** 紧急程度：1 普通 / 2 紧急 / 3 非常紧急 */
    private Integer urgency;

    /** 提报人员工ID */
    private Long applicantId;

    /** 提报人姓名（联表展示字段） */
    private String applicantName;

    /** 提报时间 */
    private LocalDateTime applyTime;

    /** 审批状态：0 草稿 / 1 审批中 / 2 已通过 / 3 已驳回 */
    private Integer approvalStatus;

    /** 当前审批人员工ID，办结后为 null */
    private Long currentApproverId;

    /** 需求状态：1 进行中 / 2 已完成 / 3 已取消 */
    private Integer status;
}
