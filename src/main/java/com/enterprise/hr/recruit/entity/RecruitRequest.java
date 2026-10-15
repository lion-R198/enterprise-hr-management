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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * RecruitRequest实体（对应表 {@code recruit_request}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("recruit_request")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RecruitRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 需求单号(唯一),如 RC20260901001 */
    private String requestNo;

    /** 需求部门ID */
    private Long requestDeptId;

    /** 招聘岗位ID */
    private Long positionId;

    /** 岗位名称(快照) */
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

    /** 学历要求:1博士 2硕士 3本科 4大专 5中专 6高中 7不限 */
    private Integer educationReq;

    /** 经验要求(年) */
    private Integer experienceYears;

    /** 岗位职责 */
    private String jobDescription;

    /** 任职要求 */
    private String jobRequirement;

    /** 期望到岗日期 */
    private LocalDate hopeArriveDate;

    /** 紧急程度:1普通 2紧急 3非常紧急 */
    private Integer urgency;

    /** 提报人员工ID */
    private Long applicantId;

    /** 提报时间 */
    private LocalDateTime applyTime;

    /** 审批状态:0草稿 1审批中 2已通过 3已驳回 */
    private Integer approvalStatus;

    /** 当前审批人员工ID */
    private Long currentApproverId;

    /** 需求状态:1进行中 2已完成 3已取消 */
    private Integer status;

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
